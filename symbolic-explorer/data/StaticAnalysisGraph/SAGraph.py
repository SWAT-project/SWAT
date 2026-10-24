from __future__ import annotations
from dataclasses import dataclass, field
from typing import Optional, Tuple
from collections import deque
import json
import math
import re

# The walk's call stack: a persistent linked-list stack of call-site nodes, innermost call on top, each
# cell also carrying the stack's depth. `None` is the empty stack, i.e. the walk is in main's
# frame. Every dfs branch extends the stack it was handed without mutating it, so sibling branches
# never see each other's frames. Build and take apart with push() / pop().
CallStack = Optional[Tuple["SANode", "CallStack", int]]

# Bounds on a single walk_till_branch() call. A branch-free infinite loop, or unbounded recursion
# with no branch before the recursive call, would otherwise never reach a branch. Hitting either
# bound gives up with "no information", which is always safe.
MAX_WALK_STEPS = 100_000
MAX_STACK_DEPTH = 10_000

@dataclass(eq=False)
class SANode:
    id: str
    # What an assert is reachable from here by, evaluated against the walk's call stack in
    # is_interesting(). Computed by compute_summaries(); see there for the exact definitions.
    #   reachesAssertSameLevel: without leaving this node's frame (calls in and back out allowed)
    #   canReturn:              this method's normal exit is reachable, likewise
    #   escapes:                the exceptions (as superclass chains) that can leave this frame
    reachesAssertSameLevel: bool = False
    canReturn: bool = False
    escapes: frozenset[tuple[str, ...]] = frozenset()
    isAssert: bool = False # an assertion point, including those the extractor reports for incomplete nodes
    isPhantomGuard: bool = False # a phantom guard is a generated explicit check for an implicit exception, e.g. `if (op2 != 0)` before an IDIV.
    # The static analysis could not expand something here (a call whose target it could not pin
    # down, or a throw whose destination it could not), so the statements and branches of
    # whatever was left out are missing from the graph. We walk this graph in step with a real
    # execution, matching branch for branch by position, so past this node the execution reports
    # branches the graph has no counterpart for and every later pairing would be shifted by them.
    # walk_till_branch() therefore stops here and reports "no information", which callers already
    # treat as "everything is interesting".
    isIncomplete: bool = False
    # A method exit (a return). The walk resumes at the return site of the frame on top of its
    # call stack.
    isExit: bool = False
    # Only for an uncaught[T] node, where exception T leaves its method: T followed by its
    # superclasses up to java.lang.Throwable. The walk unwinds its call stack to the first frame
    # with a handler for one of these, most specific first.
    exceptionChain: tuple[str, ...] | None = None
    prev: list[SANode] = field(default_factory=list, repr=False) # preceding nodes
    next_fallthrough: SANode | None = None
    next_branched: SANode | None = None
    next_exceptional: list[SANode] = field(default_factory=list, repr=False)
    # RETURN edges (exit -> every return site of the method) and EXCEPTIONAL_RETURN edges
    # (uncaught[T] -> every handler T can unwind to). The walk never follows them -- which one
    # applies depends on the call stack -- but they connect callers and callees for the backward
    # marking of paths that lead to an assert.
    next_returns: list[SANode] = field(default_factory=list, repr=False)
    next_exceptional_returns: list[SANode] = field(default_factory=list, repr=False)
    # Only for a call site with a CALL edge: the entries of the methods it may call (one, or
    # several known overrides; with one it is also next_fallthrough, which the walk follows),
    # where the caller resumes after the callee returns, and the handler each caught exception
    # type resumes at if the callee throws.
    callees: list[SANode] = field(default_factory=list, repr=False)
    return_site: SANode | None = field(default=None, repr=False)
    handlers: dict[str, SANode] = field(default_factory=dict, repr=False)
    # Only for a call site: for each pseudo exception type (see is_pseudo), the handlers of the call
    # site that might catch it. An exception of a pseudo type is not known precisely, so it may be
    # caught by any of them or by none and unwind further.
    unknown_handlers: dict[str, list[SANode]] = field(default_factory=dict, repr=False)
    # The method this node belongs to (the extractor's "method: <signature>" metadata).
    method: str | None = None
    # For a branch or a call site, its key as the executor reports it (see check_branch), and the
    # graph's keys and root method, shared by all nodes of a graph.
    key: str | None = None
    graph_index: GraphIndex | None = field(default=None, repr=False)
    # How many branches must still be decided on the shortest path from here to an assertion point,
    # counting this node if it is a branch. Computed by compute_assert_distances(). Context-
    # insensitive, so only a heuristic for ordering the search; pruning never depends on it.
    assertDistance: float = math.inf

    def has_fallthrough_child(self):
        return self.next_fallthrough is not None

    def has_branched_child(self):
        return self.next_branched is not None

    def get_fallthrough_child(self) -> SANode:
        assert self.next_fallthrough is not None
        return self.next_fallthrough

    def get_branched_child(self) -> SANode:
        assert self.next_branched is not None
        return self.next_branched

    def is_branch(self):
        if self.has_branched_child():
            assert self.has_fallthrough_child()
            return True
        return False

    def is_call_site(self):
        return self.return_site is not None

    def walk_till_branch(self, stack: CallStack = None) -> tuple[SANode, CallStack] | None:
        """
        Follows the graph from this node to the next branch, and returns it with the call stack
        in effect there -- or None if the graph cannot say where the execution goes.

        Crosses method boundaries on the way: a call into a branch-free method goes in and back
        out within one invocation, so the stack is pushed and popped here rather than in dfs.
        Idempotent on a branch node.
        """
        node = self
        for _ in range(MAX_WALK_STEPS):
            if node.isIncomplete: # the graph stops being faithful here, so stop trusting it
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
                node = node.get_fallthrough_child() # the CALL edge, into the callee
                continue
            if node.has_fallthrough_child():
                node = node.get_fallthrough_child()
                continue
            if node.isExit:
                if stack is None: # returning from main, or from a frame we never saw entered
                    return None
                frame, stack = pop(stack)
                node = frame.return_site
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

    def add_fallthrough_child(self, child: SANode):
        assert self.next_fallthrough is None, f"SANode {self.id} has multiple fallthrough edges!"
        self.next_fallthrough = child

    def add_branched_child(self, child: SANode):
        assert self.next_branched is None, f"SANode {self.id} has multiple branched edges!"
        self.next_branched = child

    def add_exceptional_child(self, child: SANode):
        self.next_exceptional.append(child)

    def add_return_child(self, child: SANode):
        self.next_returns.append(child)

    def add_exceptional_return_child(self, child: SANode):
        self.next_exceptional_returns.append(child)


def push(stack: CallStack, call_site: SANode) -> CallStack:
    return (call_site, stack, depth(stack) + 1)


def pop(stack: CallStack) -> tuple[SANode, CallStack]:
    assert stack is not None, "pop from an empty call stack"
    return stack[0], stack[1]


def depth(stack: CallStack) -> int:
    return 0 if stack is None else stack[2]


def unwind(exception_chain: tuple[str, ...], stack: CallStack) -> tuple[SANode, CallStack] | None:
    """
    Pops frames off `stack` until one catches an exception with the given superclass chain.

    Returns the handler to resume at and the stack below the catching frame, or None if no frame
    catches it and the exception leaves main. The most specific handler of a frame wins, matching
    the rule the extractor wires its EXCEPTIONAL_RETURN edges with.
    """
    while stack is not None:
        frame, stack = pop(stack)
        for exception_type in exception_chain:
            handler = frame.handlers.get(exception_type)
            if handler is not None:
                return handler, stack
    return None


def is_pseudo(exception_chain: tuple[str, ...]) -> bool:
    """
    Whether an escape is of a pseudo exception type: an exception the graph knows may occur but not
    its type -- "?Exception" (unguarded: a null dereference, a cast, anything a library call
    throws) and "?Error". The extractor lists which handlers of each call site might catch one
    (SANode.unknown_handlers). It may be caught by any of them, or by none.
    """
    return exception_chain[0].startswith("?")


def unwind_pseudo(pseudo_type: str, stack: CallStack) -> list[tuple[SANode, CallStack]]:
    """Every handler an exception of a pseudo type may resume at, with the stack below its frame."""
    resumed = []
    while stack is not None:
        frame, stack = pop(stack)
        resumed.extend((handler, stack) for handler in frame.unknown_handlers.get(pseudo_type, []))
    return resumed


@dataclass(eq=False)
class GraphIndex:
    """The nodes of a graph by key (branches and call sites; see check_branch), and main's method."""
    keys: dict[str, SANode]
    root_method: str | None
    # Application code that library code may call back can reach an assert (the extractor's
    # callbacksReachingAssert). Such a callback may also run once main's frame is gone (a shutdown
    # hook, a finalizer, a thread still running), so leaving main is then interesting too.
    callbacks_reach_assert: bool = False


# The executor's view of the call stack at a branch, as the walk tracks it from the trace (each
# branch carries the frames popped and pushed since the previous one): a CallStack whose cells hold
# the graph's call site for each frame, or None for a frame the graph has no call site for (a
# library call, a call the extractor gave no key). LOST once the trace popped more frames than are
# known, e.g. returning from a frame entered before tracing began.
LOST = "LOST"


def apply_stack_delta(stack, pops: int, pushes: tuple, index: GraphIndex | None):
    """`stack` (a trace stack, see LOST) after `pops` frames were popped and `pushes` pushed."""
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
        stack = push(stack, call_site)
    return stack


def same_stacks(walk: CallStack, trace) -> bool | None:
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


def check_branch(node: SANode, stack: CallStack, key: str | None, trace_stack):
    """
    Whether the walk is still in step with the execution at a branch: the graph's branch `node`,
    reached with call stack `stack`, against the branch the trace reports there (its `key`) and the
    trace's call stack. False when they provably differ (a desync); None when it cannot be told (a
    key is missing on either side, or a frame has no call site); True otherwise.

    Keys name a branch by method, source line, kind and position on that line (see the executor's
    and the extractor's BranchKeys), so a pairing that slipped by one branch is caught at that very
    branch, before its pruning decision is made.
    """
    if key is None:
        return None # nothing to check against (also: a trace from before keys and stack deltas)
    if node.key is not None and not keys_match(node.key, key):
        return False
    stacks = same_stacks(stack, trace_stack)
    if stacks is False:
        return False
    if node.key is None or stacks is None:
        return None
    return True


def resync(key: str | None, trace_stack, index: GraphIndex | None) -> tuple[SANode, CallStack] | None:
    """
    Where the walk resumes after it lost the graph, at a branch: the graph's branch with the trace's
    `key`, under the trace's call stack -- or None unless every frame of that stack maps to a call
    site and each one, from main up, calls the method the next one is in.
    """
    if key is None or index is None or trace_stack is LOST:
        return None
    node = index.keys.get(key)
    if node is None or not node.is_branch():
        return None
    frames = []
    s = trace_stack
    while s is not None:
        if s[0] is None:
            return None
        frames.append(s[0])
        s = s[1]
    methods = {index.root_method}
    for call_site in reversed(frames):
        if call_site.method is None or call_site.method not in methods:
            return None
        methods = {callee.method for callee in call_site.callees} # the CALL edges' callees
    if node.method is None or node.method not in methods:
        return None
    return node, trace_stack


def is_interesting(node: SANode, stack: CallStack) -> bool:
    """
    Whether an assert is reachable from `node` when the walk is there with `stack`.

    Exact for realizable paths, i.e. those on which every return and every unwinding exception
    goes where the call stack says: an assert is either reachable within the current frame, or
    after returning -- normally or by an exception -- into a frame further down the stack, where
    the same question is asked again. Costs O(stack depth) per exception type the frame can throw.
    """
    worklist = [(node, stack)]
    # The stack cells are shared with the caller's stack and alive for the whole query, so their
    # identities are stable keys.
    seen = set()
    while worklist:
        current, current_stack = worklist.pop()
        key = (id(current), id(current_stack))
        if key in seen:
            continue
        seen.add(key)
        if current.reachesAssertSameLevel:
            return True
        if current_stack is None: # in main's frame: nothing to return into
            index = current.graph_index
            if index is not None and index.callbacks_reach_assert and (current.canReturn or current.escapes):
                return True # a callback may still run after main (see GraphIndex)
            continue
        frame, below = pop(current_stack)
        if current.canReturn:
            worklist.append((frame.return_site, below))
        for chain in current.escapes:
            if is_pseudo(chain):
                worklist.extend(unwind_pseudo(chain[0], current_stack))
                continue
            resumed = unwind(chain, current_stack)
            if resumed is not None:
                worklist.append(resumed)
    return False


def compute_summaries(nodes) -> None:
    """
    Computes each node's reachesAssertSameLevel, canReturn and escapes, as the least fixpoint of

        reachesAssertSameLevel(N) = isAssert(N)
            or  N is a call site into E, returning to R:
                    reachesAssertSameLevel(E)
                 or canReturn(E) and reachesAssertSameLevel(R)
                 or reachesAssertSameLevel(h) for each h at N catching an exception in escapes(E)
            or  otherwise: any intraprocedural successor
        canReturn(N)  = N is an exit, or likewise with canReturn in place of reachesAssert...
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
        if n.next_fallthrough is not None and not n.is_call_site():
            succ.append(n.next_fallthrough)
        return succ

    for n in nodes:
        for s in successors(n):
            dependents[id(s)].append(n)
        if n.is_call_site():
            unknown = [h for hs in n.unknown_handlers.values() for h in hs]
            for s in [*n.callees, n.return_site, *n.handlers.values(), *unknown]:
                dependents[id(s)].append(n)

    def evaluate(n: SANode) -> tuple[bool, bool, frozenset]:
        if n.isIncomplete:
            return True, True, frozenset()
        ras = n.isAssert
        cr = n.isExit
        esc = set()
        if n.exceptionChain is not None:
            esc.add(n.exceptionChain)
        for s in successors(n):
            ras = ras or s.reachesAssertSameLevel
            cr = cr or s.canReturn
            esc |= s.escapes
        if n.is_call_site():
            ret = n.return_site
            for entry in n.callees: # any of them may run
                ras = ras or entry.reachesAssertSameLevel
                if entry.canReturn:
                    ras = ras or ret.reachesAssertSameLevel
                    cr = cr or ret.canReturn
                    esc |= ret.escapes
                for chain in entry.escapes:
                    if is_pseudo(chain):
                        # Caught by any of the candidate handlers, or by none: then it escapes further.
                        for handler in n.unknown_handlers.get(chain[0], []):
                            ras = ras or handler.reachesAssertSameLevel
                            cr = cr or handler.canReturn
                            esc |= handler.escapes
                        esc.add(chain)
                        continue
                    handler = next((n.handlers[t] for t in chain if t in n.handlers), None)
                    if handler is None:
                        esc.add(chain)
                    else:
                        ras = ras or handler.reachesAssertSameLevel
                        cr = cr or handler.canReturn
                        esc |= handler.escapes
        return ras, cr, frozenset(esc)

    worklist = list(nodes)
    queued = {id(n) for n in nodes}
    while worklist:
        n = worklist.pop()
        queued.discard(id(n))
        ras, cr, esc = evaluate(n)
        if (ras, cr, esc) != (n.reachesAssertSameLevel, n.canReturn, n.escapes):
            n.reachesAssertSameLevel, n.canReturn, n.escapes = ras, cr, esc
            for d in dependents[id(n)]:
                if id(d) not in queued:
                    queued.add(id(d))
                    worklist.append(d)


def mark_assertion_path(node: SANode):
    """
    Context-insensitive marking: every node with any path to `node`, following predecessors.

    Only used for graphs from extractors that predate CALL edges with return addresses, for which
    the summaries cannot be computed. Sound but imprecise: it marks a shared callee's branch
    interesting if any caller leads to an assert.
    """
    # A worklist rather than recursion: the chain of predecessors is as long as the longest path
    # through the graph, which easily exceeds Python's recursion limit.
    worklist = [node]
    while worklist:
        current = worklist.pop()
        if current.reachesAssertSameLevel: # already marked, prevent infinite looping
            continue
        current.reachesAssertSameLevel = True
        worklist.extend(prev for prev in current.prev if not prev.reachesAssertSameLevel)


def compute_assert_distances(nodes) -> None:
    """
    Sets each node's assertDistance: the fewest branches on any path from the node to an assertion
    point, following every edge kind (calls, returns and exceptional edges included) and ignoring
    the call stack. A 0-1 BFS backwards over `prev`: leaving a branch node costs 1, anything else 0.
    """
    worklist = deque()
    for n in nodes:
        n.assertDistance = math.inf
        if n.isAssert:
            n.assertDistance = 0
            worklist.append(n)
    while worklist:
        current = worklist.popleft()
        for pred in current.prev:
            cost = 1 if pred.is_branch() else 0
            d = current.assertDistance + cost
            if d < pred.assertDistance:
                pred.assertDistance = d
                if cost == 0:
                    worklist.appendleft(pred)
                else:
                    worklist.append(pred)


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


# Edge types that continue within the method along the one fall-through slot. THROW is an explicit
# throw to where it provably goes; SWAT reports no branch for an ATHROW, so it walks like NORMAL.
FALLTHROUGH_EDGES = {"NORMAL", "FALSE_BRANCH", "PHANTOM_FALSE_BRANCH", "THROW"}


class SAGraph:
    def __init__(self):
        self.json_graph = {}
        self.nodes: dict[str, SANode] = {}
        self.entry_node: SANode | None = None

    def async_entry_sites(self) -> list[str]:
        """
        The library call sites (as `node id: method`) that may run application code outside main's
        call tree, see ASYNC_ENTRY_METHODS. The walk only follows main's call tree, so an assert
        reached from such code is on no path it knows of.
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


    def load_json_graph(self, path: str):
        with open(path, "r") as f:
            self.json_graph = json.load(f)

        for json_node in self.json_graph["nodes"]:
            id = json_node["id"]
            chain = json_node.get("exceptionTypeChain")
            # Older graphs carry no isIncomplete field; absent means the node is faithful.
            self.nodes[id] = SANode(id, isIncomplete=json_node.get("isIncomplete", False),
                                    exceptionChain=tuple(chain) if chain is not None else None)
            metadata = json_node.get("metadata")
            if isinstance(metadata, str) and metadata.startswith("method: "):
                self.nodes[id].method = metadata[len("method: "):]
            self.nodes[id].key = json_node.get("key")
        # Phantom guards whose implicit exception the property pre-check proves cannot happen
        # (the extractor's cannotRaise, see RaiseSites in cfg-extraction): their exception side
        # leads nowhere, below.
        cannot_raise = {n["id"] for n in self.json_graph["nodes"] if n.get("cannotRaise") is True}
        keys: dict[str, SANode] = {}
        duplicates = set()
        for n in self.nodes.values():
            if n.key is not None and "|*" not in n.key: # an open ordinal names no one node
                if n.key in keys:
                    duplicates.add(n.key)
                keys[n.key] = n
        for k in duplicates: # cannot tell which one is meant
            del keys[k]

        for exit_id in self.json_graph.get("exitNodeIds", []):
            self.nodes[exit_id].isExit = True

        legacy_calls = False # a CALL edge without a return address, see below
        for json_edge in self.json_graph["edges"]:
            source = self.nodes[json_edge["source"]]
            target = self.nodes[json_edge["target"]]

            etype = json_edge["type"]
            if etype == "EXCEPTION":
                source.add_exceptional_child(target)

            elif etype == "PHANTOM_TRUE_BRANCH":
                source.add_branched_child(target)
                source.isPhantomGuard = True

            elif etype == "TRUE_BRANCH":
                source.add_branched_child(target)

            elif etype == "CALL":
                # Graphs from before the call stack carry no return address. Their call sites then
                # walk like a plain fall-through, their exits find an empty stack and give up, and
                # they are marked context-insensitively.
                if "returnSite" in json_edge:
                    source.callees.append(target)
                    source.return_site = self.nodes[json_edge["returnSite"]]
                    source.handlers = {t: self.nodes[h] for t, h in json_edge.get("handlers", {}).items()}
                    source.unknown_handlers = {t: [self.nodes[h] for h in hs]
                                               for t, hs in json_edge.get("unknownHandlers", {}).items()}
                else:
                    source.add_fallthrough_child(target)
                    legacy_calls = True

            elif etype == "RETURN":
                source.add_return_child(target)

            elif etype == "EXCEPTIONAL_RETURN":
                source.add_exceptional_return_child(target)

            elif etype == "PHANTOM_FALSE_BRANCH" and source.id in cannot_raise:
                # The exception side of a guard that cannot raise: a dead end, from which nothing is
                # reachable. Should an execution take it after all, the walk ends there with no
                # information, which prunes nothing.
                dead = SANode(f"dead[{source.id}]")
                self.nodes[dead.id] = dead
                source.add_fallthrough_child(dead)
                dead.prev.append(source)
                continue

            elif etype in FALLTHROUGH_EDGES:
                source.add_fallthrough_child(target)

            else:
                raise ValueError(f"Unknown edge type {etype} from {source.id} to {target.id}")

            target.prev.append(source)


        # A call site with one callee walks into it like a fall-through.
        for n in self.nodes.values():
            if len(n.callees) == 1:
                n.add_fallthrough_child(n.callees[0])

        self.entry_node = self.nodes[self.json_graph["entryNodeId"]]
        graph_index = GraphIndex(keys, self.entry_node.method,
                                 callbacks_reach_assert=bool(self.json_graph["metadata"].get("callbacksReachingAssert")))
        for n in self.nodes.values():
            n.graph_index = graph_index

        for assertion_point_id in self.json_graph["metadata"]["assertionPointIds"]:
            self.nodes[assertion_point_id].isAssert = True
        # An incomplete node may hide an assert in the part that was not expanded, so the paths
        # leading to it have to stay interesting. The extractor already reports these as assertion
        # points; treating them as such here too means a graph that ever forgets one half still
        # errs on the safe side rather than pruning a path that leads to something we cannot see.
        for node in self.nodes.values():
            if node.isIncomplete:
                node.isAssert = True

        if legacy_calls:
            for node in self.nodes.values():
                if node.isAssert:
                    mark_assertion_path(node)
        else:
            compute_summaries(list(self.nodes.values()))
        compute_assert_distances(list(self.nodes.values()))



if __name__ == "__main__":
    import sys
    tree = SAGraph()
    tree.load_json_graph(sys.argv[1])
    print("not reachesAssertSameLevel:", [n.id for n in tree.nodes.values() if not n.reachesAssertSameLevel])
