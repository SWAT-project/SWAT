"""
The static pre-analysis (SA) graph: the extractor's interprocedural control-flow graph
(cfg-extraction, `<Target>_main_interprocedural.json`), and what dfs needs to walk it in step with
the execution tree.

Terms used here, in dfs and in Relevance:

* **target point**: a node whose execution may violate the property being checked: an assertion
  point under valid-assert, a site that may raise a RuntimeException under no-runtime-exception
  (later also shell calls, SQL statements). `SANode.isTarget`.
* **interesting**: a target point is reachable from a node, given the walk's call stack
  (`is_interesting`). dfs prunes the sides of branches that are not interesting.
* **call stack** (`CallStack`): the call sites the walk entered to get where it is.
* **trace stack**: the call stack as the trace reports it (the frames popped and pushed before
  each branch), which the walk is checked against (`check_branch`) and rebuilt from (`resync`).
* **key**: `owner.name(desc)|line|kind|ordinal`, the name of a branch or call site that both the
  executor and the extractor compute (their `BranchKeys`).
* **lost**: the walk has no graph node (None). Then nothing is pruned, which is always safe.
"""
from __future__ import annotations
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Any, Final, Literal, Optional, Tuple, TypedDict, Union, cast
from collections import deque
from enum import Enum
import json
import math
import re

if TYPE_CHECKING:
    from data.StaticAnalysisGraph.Relevance import RelevanceStats

# An exception type followed by its superclasses up to java.lang.Throwable, e.g.
# ("java.lang.ArithmeticException", "java.lang.RuntimeException", ..., "java.lang.Throwable"); or
# a pseudo type alone, e.g. ("?Exception",) (see is_pseudo).
ExceptionChain = Tuple[str, ...]

# An abstract location of the data-flow facts (cfg-extraction DataflowFacts): a local `l:<name>`, a
# parameter `p:<n>` or `p:this`, the return value `ret` (these three are per method: Relevance
# scopes them as `<method>|<location>`), a field `f:<name>:<type>`, the elements of arrays
# `a:<element kind>`, global state `g:<name>` (the input cursor, library state, output, the
# exception in flight), or `*`, which stands for every location.
Location = str
# What a statement does to locations: those it may write, and those it reads to compute them.
Effect = Tuple[Tuple[Location, ...], Tuple[Location, ...]]


class CallFacts(TypedDict, total=False):
    """The data-flow facts of an expanded call site (the extractor's `call` field)."""
    args: list[list[Location]]   # per argument, the locations its value comes from
    base: list[Location]         # the receiver's locations (empty for a static call)
    result: Location             # where the result goes, if it is used
    resultAddress: list[Location]  # what the result's destination is addressed by (an array index, ...)


# The walk's call stack: a persistent linked-list stack of call-site nodes, innermost call on top, each
# cell also carrying the stack's depth. `None` is the empty stack, i.e. the walk is in main's
# frame. Every dfs branch extends the stack it was handed without mutating it, so sibling branches
# never see each other's frames. Build and take apart with push() / pop().
CallStack = Optional[Tuple["SANode", "CallStack", int]]

# Bounds on a single walk_till_branch() call. A branch-free infinite loop, or unbounded recursion
# with no branch before the recursive call, would otherwise never reach a branch. Hitting either
# bound loses the walk, which is always safe.
MAX_WALK_STEPS = 100_000
MAX_STACK_DEPTH = 10_000


@dataclass(eq=False)
class SANode:
    id: str
    # What a target point is reachable from here by, evaluated against the walk's call stack in
    # is_interesting(). Computed by compute_summaries(); see there for the exact definitions.
    #   reachesTargetSameLevel: without leaving this node's frame (calls in and back out allowed)
    #   canReturn:              this method's normal exit is reachable, likewise
    #   escapes:                the exceptions (as superclass chains) that can leave this frame
    reachesTargetSameLevel: bool = False
    canReturn: bool = False
    escapes: frozenset[ExceptionChain] = frozenset()
    # A target point of the property being checked (see the module comment). Incomplete nodes are
    # target points too.
    isTarget: bool = False
    # The extractor could not expand something here (a call whose target it could not pin down, or
    # a throw whose destination it could not), so whatever was left out is missing from the graph
    # and the walk cannot pair the branches that follow. walk_till_branch() stops here (the walk is
    # lost), and the node counts as a target point, so the paths leading to it stay interesting.
    isIncomplete: bool = False
    # A method exit (a return). The walk resumes at the return site of the frame on top of its
    # call stack.
    isExit: bool = False
    # Only for an uncaught[T] node, where exception T leaves its method: T followed by its
    # superclasses up to java.lang.Throwable. The walk unwinds its call stack to the first frame
    # with a handler for one of these, most specific first.
    exceptionChain: ExceptionChain | None = None
    # Predecessors along every edge kind (calls, RETURN and EXCEPTIONAL_RETURN edges included), for
    # the target distances.
    prev: list[SANode] = field(default_factory=list, repr=False)
    # The successors within the method: a branch has both, other nodes at most next_fallthrough,
    # a call site none (see callees).
    next_fallthrough: SANode | None = None
    next_branched: SANode | None = None
    # EXCEPTION edges, inferred by SootUp. The walk never follows them; the summaries count them.
    next_exceptional: list[SANode] = field(default_factory=list, repr=False)
    # Only for a call site (a node with CALL edges): the entries of the methods it may call (one,
    # or several known overrides), where the caller resumes after the callee returns, and the
    # handler each caught exception type resumes at if the callee throws.
    callees: list[SANode] = field(default_factory=list, repr=False)
    return_site: SANode | None = field(default=None, repr=False)
    handlers: dict[str, SANode] = field(default_factory=dict, repr=False)
    # Only for a call site: for each pseudo exception type (see is_pseudo), the handlers of the call
    # site that might catch it. An exception of a pseudo type is not known precisely, so it may be
    # caught by any of them or by none and unwind further.
    unknown_handlers: dict[str, list[SANode]] = field(default_factory=dict, repr=False)
    # The method this node belongs to (the extractor's "method: <signature>" metadata).
    method: str | None = None
    # For a branch or a call site, its key (see check_branch); and the graph's keys and root
    # method, shared by all nodes of a graph.
    key: str | None = None
    graph_index: GraphIndex | None = field(default=None, repr=False)
    # How many branches must still be decided on the shortest path from here to a target point,
    # counting this node if it is a branch. Computed by compute_target_distances(). Context-
    # insensitive, so only a heuristic for ordering the search (--sa-rank-distance); pruning never
    # depends on it.
    targetDistance: float = math.inf
    # The extractor's data-flow facts (cfg-extraction DataflowFacts), for the relevance analysis:
    # (written, read) location pairs, what the node's control decision reads, whether it may end
    # the program, and for an expanded call site its arguments, receiver and result.
    dataflow: tuple[Effect, ...] = ()
    control: tuple[Location, ...] = ()
    terminates: bool = False
    call: CallFacts | None = field(default=None, repr=False)
    # Set by Relevance, for a branch. irrelevant: it cannot affect the targets, so dfs slices it.
    # summary_ok: its region changes nothing relevant except locals of its own method, so dfs may
    # honor a conditional loop summary of it. region: for either, the nodes of its own frame up to
    # its immediate post-dominator.
    irrelevant: bool = False
    summary_ok: bool = False
    region: frozenset[SANode] | None = field(default=None, repr=False)
    # Hand-written facts (user facts, only with --user-facts): the irrelevance fact covering this
    # node (the extractor's userFact); for a branch, whether it is irrelevant only if the graph's
    # irrelevance facts hold (Relevance; dfs then counts them for the verdict).
    user_fact: str | None = None
    irrelevant_assuming: bool = False

    def is_branch(self) -> bool:
        return self.next_branched is not None

    def sides(self) -> tuple[SANode, SANode]:
        """A branch's successors: (fall-through, branched)."""
        assert self.next_fallthrough is not None and self.next_branched is not None, f"{self.id} is no branch"
        return self.next_fallthrough, self.next_branched

    def resume_site(self) -> SANode:
        """A call site's return site, where its frame resumes when the callee returns."""
        assert self.return_site is not None, f"{self.id} is no call site"
        return self.return_site

    def is_call_site(self) -> bool:
        return self.return_site is not None

    def handler_for(self, chain: ExceptionChain) -> SANode | None:
        """At a call site: the handler that catches an exception with this superclass chain, the
        most specific one first (the rule the extractor wires its EXCEPTIONAL_RETURN edges with)."""
        for exception_type in chain:
            handler = self.handlers.get(exception_type)
            if handler is not None:
                return handler
        return None

    def walk_till_branch(self, stack: CallStack = None) -> tuple[SANode, CallStack] | None:
        """
        Follows the graph from this node to the next branch, and returns it with the call stack
        in effect there -- or None (lost) if the graph cannot say where the execution goes.

        Crosses method boundaries on the way: a call into a branch-free method goes in and back
        out within one invocation, so the stack is pushed and popped here rather than in dfs.
        Idempotent on a branch node.
        """
        node = self
        for _ in range(MAX_WALK_STEPS):
            if node.isIncomplete: # the graph stops being faithful here
                return None
            if node.is_branch():
                return node, stack
            if node.is_call_site():
                if len(node.callees) != 1:
                    # Several possible callees: which one runs only the trace can say, so the walk
                    # stops here and finds its place again inside the callee (resync).
                    return None
                if depth(stack) >= MAX_STACK_DEPTH:
                    return None
                stack = push(stack, node)
                node = node.callees[0]
                continue
            if node.next_fallthrough is not None:
                node = node.next_fallthrough
                continue
            if node.isExit:
                if stack is None: # returning from main, or from a frame we never saw entered
                    return None
                frame, stack = pop(stack)
                node = frame.resume_site()
                continue
            if node.exceptionChain is not None:
                if is_pseudo(node.exceptionChain):
                    return None # where an exception of unknown type goes is not known
                resumed = unwind(node.exceptionChain, stack)
                if resumed is None: # the exception leaves main
                    return None
                node, stack = resumed
                continue
            return None # a dead end the graph has no continuation for
        return None


# --- call stacks and exceptions ----------------------------------------------------------------

def push(stack: CallStack, call_site: SANode) -> CallStack:
    return (call_site, stack, depth(stack) + 1)


def pop(stack: CallStack) -> tuple[SANode, CallStack]:
    assert stack is not None, "pop from an empty call stack"
    return stack[0], stack[1]


def depth(stack: CallStack | TraceFrames) -> int:
    return 0 if stack is None else stack[2]


def unwind(exception_chain: ExceptionChain, stack: CallStack) -> tuple[SANode, CallStack] | None:
    """
    Pops frames off `stack` until one catches an exception with the given superclass chain.

    Returns the handler to resume at and the stack below the catching frame, or None if no frame
    catches it and the exception leaves main.
    """
    while stack is not None:
        frame, stack = pop(stack)
        handler = frame.handler_for(exception_chain)
        if handler is not None:
            return handler, stack
    return None


def is_pseudo(exception_chain: ExceptionChain) -> bool:
    """
    Whether an escape is of a pseudo exception type: an exception the graph knows may occur but not
    its type -- "?Exception" (unguarded: a null dereference, a cast, anything a library call
    throws) and "?Error". The extractor lists which handlers of each call site might catch one
    (SANode.unknown_handlers). It may be caught by any of them, or by none.
    """
    return exception_chain[0].startswith("?")


def unwind_pseudo(pseudo_type: str, stack: CallStack) -> list[tuple[SANode, CallStack]]:
    """Every handler an exception of a pseudo type may resume at, with the stack below its frame."""
    resumed: list[tuple[SANode, CallStack]] = []
    while stack is not None:
        frame, stack = pop(stack)
        resumed.extend((handler, stack) for handler in frame.unknown_handlers.get(pseudo_type, []))
    return resumed


# --- keys: checking the walk against the trace, and finding the graph again --------------------

@dataclass(eq=False)
class GraphIndex:
    """The nodes of a graph by key (branches and call sites; see check_branch), and main's method."""
    keys: dict[str, SANode]
    root_method: str | None
    # Application code that library code may call back can reach an assert (the extractor's
    # callbacksReachingAssert). Such a callback may also run once main's frame is gone (a shutdown
    # hook, a finalizer, a thread still running), so leaving main is then interesting too.
    callbacks_reach_assert: bool = False
    # Hand-written irrelevance facts (user facts): those the relevance analysis refuted, which dfs
    # ignores where the executor applied them, and those it used.
    refuted_facts: frozenset[str] = frozenset()
    user_facts: frozenset[str] = frozenset()


# The trace stack: the executor's view of the call stack at a branch, as the walk tracks it from the
# trace (each branch carries the frames popped and pushed since the previous one). Its frames are
# cells like a CallStack's, but hold the graph's call site for each frame, or None for a frame the
# graph has no call site for (a library call, a call the extractor gave no key); a CallStack is a
# TraceFrames whose frames are all known (see dfs, which shares the cells of the two). The trace
# stack is LOST once the trace popped more frames than are known, e.g. returning from a frame
# entered before tracing began.
class _Lost(Enum):
    LOST = "LOST"


LOST: Final = _Lost.LOST
TraceFrames = Optional[Tuple[Optional["SANode"], "TraceFrames", int]]
TraceStack = Union[TraceFrames, Literal[_Lost.LOST]]


def apply_stack_delta(stack: TraceStack, pops: int, pushes: tuple[str | None, ...],
                      index: GraphIndex | None) -> TraceStack:
    """The trace stack `stack` after `pops` frames were popped and `pushes` pushed (the keys of
    the call sites that pushed them, None where the executor could not name one)."""
    if stack is LOST:
        return LOST
    for _ in range(pops):
        if stack is None:
            return LOST
        stack = stack[1]
    for key in pushes:
        call_site = index.keys.get(key) if index is not None and key is not None else None
        if call_site is not None and not call_site.is_call_site():
            call_site = None
        stack = (call_site, stack, depth(stack) + 1)
    return stack


def same_stacks(walk: CallStack, trace: TraceStack) -> bool | None:
    """
    Whether the walk's call stack is the one the trace reports: True, False, or None where the
    trace stack has frames without a call site (and agrees everywhere else). Compares from the top
    down to the first cell the two share, so after the walk's stack has been adopted for the trace
    (see dfs) this costs only the frames changed since.
    """
    if trace is LOST:
        return None
    known = True
    while walk is not trace:
        if walk is None or trace is None or walk[2] != trace[2]:
            return False
        if trace[0] is None:
            known = False
        elif trace[0] is not walk[0]:
            return False
        walk, trace = walk[1], trace[1]
    return True if known else None


def keys_match(graph_key: str, trace_key: str) -> bool:
    """
    Whether a graph node's key names the trace's instruction. A graph key may leave the ordinal
    open (`...|kind|*`, where the extractor could not tell same-kind instructions on a line apart):
    it then matches any ordinal.
    """
    if "|*" not in graph_key:
        return graph_key == trace_key
    prefix, rest = graph_key.split("|*", 1)
    if not trace_key.startswith(prefix + "|"):
        return False
    tail = trace_key[len(prefix) + 1:]
    digits = len(tail) - len(tail.lstrip("0123456789"))
    return digits > 0 and tail[digits:] == rest


def check_branch(node: SANode, stack: CallStack, key: str | None, trace_stack: TraceStack) -> bool | None:
    """
    Whether the walk is still in step with the execution at a branch: the graph's branch `node`,
    reached with call stack `stack`, against the branch the trace reports there (its `key`) and the
    trace stack. False when they provably differ (a desync); None when it cannot be told (a key is
    missing on either side, or a frame has no call site); True otherwise.

    Keys name a branch by method, source line, kind and position on that line (see the executor's
    and the extractor's BranchKeys), so a pairing that slipped by one branch is caught at that very
    branch, before its pruning decision is made.
    """
    if key is None:
        return None # the executor could not name the branch
    if node.key is not None and not keys_match(node.key, key):
        return False
    stacks = same_stacks(stack, trace_stack)
    if stacks is False:
        return False
    if node.key is None or stacks is None:
        return None
    return True


def resync(key: str | None, trace_stack: TraceStack, index: GraphIndex | None) -> tuple[SANode, CallStack] | None:
    """
    Where a lost walk resumes, at a branch: the graph's branch with the trace's `key`, under the
    trace stack -- or None unless every frame of that stack maps to a call site and each one, from
    main up, calls the method the next one is in.
    """
    if key is None or index is None or trace_stack is LOST:
        return None
    node = index.keys.get(key)
    if node is None or not node.is_branch():
        return None
    frames: list[SANode] = []
    cell: TraceFrames = trace_stack
    while cell is not None:
        if cell[0] is None:
            return None
        frames.append(cell[0])
        cell = cell[1]
    methods = {index.root_method}
    for call_site in reversed(frames):
        if call_site.method is None or call_site.method not in methods:
            return None
        methods = {callee.method for callee in call_site.callees}
    if node.method is None or node.method not in methods:
        return None
    return node, cast(CallStack, trace_stack)  # every frame is known (checked above)


# --- which nodes are interesting ---------------------------------------------------------------

def is_interesting(node: SANode, stack: CallStack) -> bool:
    """
    Whether a target point is reachable from `node` when the walk is there with `stack`.

    Exact for realizable paths, i.e. those on which every return and every unwinding exception
    goes where the call stack says: a target point is either reachable within the current frame,
    or after returning -- normally or by an exception -- into a frame further down the stack,
    where the same question is asked again. Costs O(stack depth) per exception type the frame can
    throw.
    """
    worklist: list[tuple[SANode, CallStack]] = [(node, stack)]
    # The stack cells are shared with the caller's stack and alive for the whole query, so their
    # identities are stable keys.
    seen = set()
    while worklist:
        current, current_stack = worklist.pop()
        key = (id(current), id(current_stack))
        if key in seen:
            continue
        seen.add(key)
        if current.reachesTargetSameLevel:
            return True
        if current_stack is None: # in main's frame: nothing to return into
            index = current.graph_index
            if index is not None and index.callbacks_reach_assert and (current.canReturn or current.escapes):
                return True # a callback may still run after main (see GraphIndex)
            continue
        frame, below = pop(current_stack)
        if current.canReturn:
            worklist.append((frame.resume_site(), below))
        for chain in current.escapes:
            if is_pseudo(chain):
                worklist.extend(unwind_pseudo(chain[0], current_stack))
                continue
            resumed = unwind(chain, current_stack)
            if resumed is not None:
                worklist.append(resumed)
    return False


def compute_summaries(nodes: list[SANode]) -> None:
    """
    Computes each node's reachesTargetSameLevel, canReturn and escapes, as the least fixpoint of

        reachesTargetSameLevel(N) = isTarget(N)
            or  N is a call site into E, returning to R:
                    reachesTargetSameLevel(E)
                 or canReturn(E) and reachesTargetSameLevel(R)
                 or reachesTargetSameLevel(h) for each h at N catching an exception in escapes(E)
            or  otherwise: any intraprocedural successor
        canReturn(N)  = N is an exit, or likewise with canReturn in place of reachesTarget...
        escapes(N)    = {T} at an uncaught[T] node; at a call site the exceptions in escapes(E) no
                        handler of N catches, plus escapes(R) if canReturn(E), plus escapes(h) of
                        each handler reached; otherwise the union over intraprocedural successors.
                        A pseudo type (is_pseudo) reaches every handler that might catch it and
                        always escapes further too.

    (Reps-Horwitz-Sagiv same-level realizable paths.) The call summaries make it mutually
    recursive through callee entries, so recursion makes it cyclic; it is solved with a worklist,
    and since every value only ever grows within a finite domain it converges.

    An incomplete node might hide anything and forces both booleans true. A throw leaving the
    method does not count towards canReturn: its uncaught[T] node has no normal continuation.

    Intraprocedural successors include the SootUp-inferred EXCEPTION edges. The walk does not
    follow those, but counting them can only make more paths interesting.
    """
    dependents: dict[int, list[SANode]] = {id(n): [] for n in nodes}

    def successors(n: SANode) -> list[SANode]:
        succ = list(n.next_exceptional)
        if n.next_branched is not None:
            succ.append(n.next_branched)
        if n.next_fallthrough is not None:
            succ.append(n.next_fallthrough)
        return succ

    for n in nodes:
        for s in successors(n):
            dependents[id(s)].append(n)
        if n.is_call_site():
            unknown = [h for hs in n.unknown_handlers.values() for h in hs]
            for s in [*n.callees, n.resume_site(), *n.handlers.values(), *unknown]:
                dependents[id(s)].append(n)

    def evaluate(n: SANode) -> tuple[bool, bool, frozenset[ExceptionChain]]:
        if n.isIncomplete:
            return True, True, frozenset()
        rts = n.isTarget
        cr = n.isExit
        esc: set[ExceptionChain] = set()
        if n.exceptionChain is not None:
            esc.add(n.exceptionChain)
        for s in successors(n):
            rts = rts or s.reachesTargetSameLevel
            cr = cr or s.canReturn
            esc |= s.escapes
        if n.is_call_site():
            # Where the caller may resume after any of the callees: at the return site, and at the
            # handlers of what they throw (an exception of a pseudo type may also be caught by none).
            resumes: list[SANode] = []
            for entry in n.callees:
                rts = rts or entry.reachesTargetSameLevel
                if entry.canReturn:
                    resumes.append(n.resume_site())
                for chain in entry.escapes:
                    if is_pseudo(chain):
                        resumes.extend(n.unknown_handlers.get(chain[0], []))
                        esc.add(chain)
                        continue
                    handler = n.handler_for(chain)
                    if handler is None:
                        esc.add(chain)
                    else:
                        resumes.append(handler)
            for r in resumes:
                rts = rts or r.reachesTargetSameLevel
                cr = cr or r.canReturn
                esc |= r.escapes
        return rts, cr, frozenset(esc)

    worklist = list(nodes)
    queued = {id(n) for n in nodes}
    while worklist:
        n = worklist.pop()
        queued.discard(id(n))
        rts, cr, esc = evaluate(n)
        if (rts, cr, esc) != (n.reachesTargetSameLevel, n.canReturn, n.escapes):
            n.reachesTargetSameLevel, n.canReturn, n.escapes = rts, cr, esc
            for d in dependents[id(n)]:
                if id(d) not in queued:
                    queued.add(id(d))
                    worklist.append(d)


def compute_target_distances(nodes: list[SANode]) -> None:
    """
    Sets each node's targetDistance: the fewest branches on any path from the node to a target
    point, following every edge kind (calls, returns and exceptional edges included) and ignoring
    the call stack. A 0-1 BFS backwards over `prev`: leaving a branch node costs 1, anything else 0.
    """
    worklist: deque[SANode] = deque()
    for n in nodes:
        n.targetDistance = math.inf
        if n.isTarget:
            n.targetDistance = 0
            worklist.append(n)
    while worklist:
        current = worklist.popleft()
        for pred in current.prev:
            cost = 1 if pred.is_branch() else 0
            d = current.targetDistance + cost
            if d < pred.targetDistance:
                pred.targetDistance = d
                if cost == 0:
                    worklist.appendleft(pred)
                else:
                    worklist.append(pred)


# --- application code that runs outside main's call tree ---------------------------------------

# Library methods that run application code outside the caller's call tree: on another thread,
# after main (shutdown hooks), or when an exception leaves a thread (uncaught-exception handlers).
# Matched by name at library call sites only, so an application method of the same name, which
# the graph expands, does not count.
ASYNC_ENTRY_METHODS = {
    "addShutdownHook", "setUncaughtExceptionHandler", "setDefaultUncaughtExceptionHandler",
    "execute", "submit", "invokeAll", "invokeAny", "schedule", "scheduleAtFixedRate",
    "scheduleWithFixedDelay", "invokeLater", "invokeAndWait", "startVirtualThread",
}
# `<declaring class: return type name(parameters)>` in a Jimple invoke statement.
INVOKED_METHOD = re.compile(r"<([^:<>]+): ([^ <>]+) ([^ (<>]+)\(([^)]*)\)>")


def is_async_entry(declaring_class: str, return_type: str, name: str, params: str) -> bool:
    if name in ASYNC_ENTRY_METHODS or name.endswith("Async"): # CompletableFuture.runAsync etc.
        return True
    if name == "start": # Thread.start, also through a subclass or a Thread.Builder
        return (return_type == "void" and params == "") or declaring_class.startswith("java.lang.Thread")
    return declaring_class == "java.lang.ref.Cleaner" and name == "register"


# --- loading -----------------------------------------------------------------------------------

# Edge types that continue within the method along the one fall-through slot. THROW is an explicit
# throw to where it provably goes; SWAT reports no branch for an ATHROW, so it walks like NORMAL.
FALLTHROUGH_EDGES = {"NORMAL", "FALSE_BRANCH", "PHANTOM_FALSE_BRANCH", "THROW"}
BRANCHED_EDGES = {"TRUE_BRANCH", "PHANTOM_TRUE_BRANCH"}
# Edges only recorded in `prev` (for the target distances): which RETURN or EXCEPTIONAL_RETURN edge
# applies depends on the call stack, so the walk and the summaries use the call sites' return_site
# and handlers instead.
RETURN_EDGES = {"RETURN", "EXCEPTIONAL_RETURN"}


class SAGraph:
    def __init__(self) -> None:
        self.json_graph: dict[str, Any] = {}  # the extractor's JSON, as loaded
        self.nodes: dict[str, SANode] = {}
        self.entry_node: SANode | None = None
        # The relevance analysis' statistics, None if it did not run (see load_json_graph).
        self.relevance_stats: RelevanceStats | None = None

    def assumed_facts(self) -> list[str]:
        """
        The hand-written facts (user facts) that shaped the graph itself, so that any pruning may
        rely on them: the library facts the extractor used, and @NotCalledBack. (Irrelevance facts
        are counted where they are relied on, by dfs; @Target and summaries assume nothing here.)
        """
        facts = (self.json_graph.get("metadata") or {}).get("userFacts") or {}
        used = [f"library fact: {e}" for e in (facts.get("library") or {}).get("used", [])]
        return used + [f"@NotCalledBack {m}" for m in facts.get("notCalledBack", [])]

    def async_entry_sites(self) -> list[str]:
        """
        The library call sites (as `node id: method`) that may run application code outside main's
        call tree, see ASYNC_ENTRY_METHODS. The walk only follows main's call tree, so a target
        point reached from such code is on no path it knows of.
        """
        expanded = {e["source"] for e in self.json_graph.get("edges", []) if e["type"] == "CALL"}
        sites = []
        for json_node in self.json_graph.get("nodes", []):
            if json_node["id"] in expanded:
                continue
            match = INVOKED_METHOD.search(json_node.get("statement") or "")
            if match is not None and is_async_entry(*match.groups()):
                sites.append(f'{json_node["id"]}: {match.group(1)}.{match.group(3)}')
        return sites

    def load_json_graph(self, path: str, targets: str = "assertionPointIds", slicing: bool = True) -> None:
        """
        Loads the extractor's graph. `targets` names the metadata field with the target points of
        the property being checked: assertionPointIds (valid-assert) or runtimeExceptionPointIds
        (no-runtime-exception). A graph without it cannot be used for that property (KeyError).

        With `slicing`, and if the graph carries data-flow facts, also computes which branches are
        irrelevant to those targets (see Relevance).
        """
        with open(path, "r") as f:
            self.json_graph = json.load(f)
        metadata = self.json_graph["metadata"]
        if targets not in metadata:
            raise KeyError(f"the graph has no {targets}: it was made by an extractor that does not know this property")

        for json_node in self.json_graph["nodes"]:
            chain = json_node.get("exceptionTypeChain")
            method = json_node.get("metadata")
            node = SANode(json_node["id"],
                          isIncomplete=json_node.get("isIncomplete", False),
                          exceptionChain=tuple(chain) if chain is not None else None,
                          method=method[len("method: "):] if isinstance(method, str) and method.startswith("method: ") else None,
                          key=json_node.get("key"),
                          dataflow=tuple((tuple(w), tuple(r)) for w, r in json_node.get("dataflow", ())),
                          control=tuple(json_node.get("control", ())),
                          terminates=json_node.get("terminates", False) is True,
                          call=json_node.get("call"),
                          user_fact=json_node.get("userFact"))
            self.nodes[node.id] = node
        for exit_id in self.json_graph.get("exitNodeIds", []):
            self.nodes[exit_id].isExit = True
        # Phantom guards whose implicit exception the property pre-check proves cannot happen
        # (the extractor's cannotRaise, see RaiseSites in cfg-extraction): their exception side
        # leads nowhere, below.
        cannot_raise = {n["id"] for n in self.json_graph["nodes"] if n.get("cannotRaise") is True}
        # The side of a branch on Swat.summary() that never runs (user facts): the loop the
        # hand-written summary replaces. Also a dead end.
        dead_sides = {n["id"]: n["deadSide"] for n in self.json_graph["nodes"] if n.get("deadSide")}

        for json_edge in self.json_graph["edges"]:
            source = self.nodes[json_edge["source"]]
            target = self.nodes[json_edge["target"]]
            etype = json_edge["type"]
            if etype == "PHANTOM_FALSE_BRANCH" and source.id in cannot_raise:
                # The exception side of a guard that cannot raise: a dead end, from which nothing
                # is reachable. Should an execution take it after all, the walk is lost there,
                # which prunes nothing.
                target = SANode(f"dead[{source.id}]")
                self.nodes[target.id] = target
            elif dead_sides.get(source.id) == etype:
                target = SANode(f"dead[{source.id}]")
                self.nodes[target.id] = target
            if etype in FALLTHROUGH_EDGES:
                assert source.next_fallthrough is None, f"SANode {source.id} has multiple fallthrough edges!"
                source.next_fallthrough = target
            elif etype in BRANCHED_EDGES:
                assert source.next_branched is None, f"SANode {source.id} has multiple branched edges!"
                source.next_branched = target
            elif etype == "EXCEPTION":
                source.next_exceptional.append(target)
            elif etype == "CALL":
                source.callees.append(target)
                source.return_site = self.nodes[json_edge["returnSite"]]
                source.handlers = {t: self.nodes[h] for t, h in json_edge.get("handlers", {}).items()}
                source.unknown_handlers = {t: [self.nodes[h] for h in hs]
                                           for t, hs in json_edge.get("unknownHandlers", {}).items()}
            elif etype not in RETURN_EDGES:
                raise ValueError(f"Unknown edge type {etype} from {source.id} to {target.id}")
            target.prev.append(source)
        for n in self.nodes.values():
            assert n.next_branched is None or n.next_fallthrough is not None, f"branch {n.id} has no fallthrough edge"
            assert not (n.is_call_site() and n.next_fallthrough is not None), f"call site {n.id} has a fallthrough edge"

        keys: dict[str, SANode] = {}
        duplicates = set()
        for n in self.nodes.values():
            if n.key is not None and "|*" not in n.key: # an open ordinal names no one node
                if n.key in keys:
                    duplicates.add(n.key)
                keys[n.key] = n
        for k in duplicates: # cannot tell which one is meant
            del keys[k]

        self.entry_node = self.nodes[self.json_graph["entryNodeId"]]
        # Callbacks are only an extra concern for asserts: for runtime exceptions, every library call
        # that may call back into the application is a target point itself (see the extractor).
        graph_index = GraphIndex(keys, self.entry_node.method,
                                 callbacks_reach_assert=targets == "assertionPointIds"
                                 and bool(metadata.get("callbacksReachingAssert")))
        for n in self.nodes.values():
            n.graph_index = graph_index

        for target_id in metadata[targets]:
            self.nodes[target_id].isTarget = True
        # The extractor already reports incomplete nodes as target points; marking them here too
        # means a graph that ever forgets one still errs on the safe side.
        for node in self.nodes.values():
            if node.isIncomplete:
                node.isTarget = True

        compute_summaries(list(self.nodes.values()))
        if slicing and metadata.get("dataflowFacts"):
            from data.StaticAnalysisGraph.Relevance import Relevance
            relevance = Relevance(self.nodes.values())
            self.relevance_stats = relevance.compute()
            graph_index.refuted_facts = frozenset(getattr(relevance, "refuted_facts", ()))
            graph_index.user_facts = frozenset(getattr(relevance, "user_facts", ()))
        compute_target_distances(list(self.nodes.values()))


if __name__ == "__main__":
    import sys
    graph = SAGraph()
    graph.load_json_graph(sys.argv[1])
    print("not reachesTargetSameLevel:", [n.id for n in graph.nodes.values() if not n.reachesTargetSameLevel])
