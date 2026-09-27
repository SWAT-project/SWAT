from __future__ import annotations
from dataclasses import dataclass, field
from typing import Optional, Tuple
import json

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
    # Only for a call site with a CALL edge: where the caller resumes after the callee returns,
    # and the handler each caught exception type resumes at if the callee throws.
    return_site: SANode | None = field(default=None, repr=False)
    handlers: dict[str, SANode] = field(default_factory=dict, repr=False)

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
            continue
        frame, below = pop(current_stack)
        if current.canReturn:
            worklist.append((frame.return_site, below))
        for chain in current.escapes:
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
            for s in [n.next_fallthrough, n.return_site, *n.handlers.values()]:
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
            entry, ret = n.next_fallthrough, n.return_site
            ras = ras or entry.reachesAssertSameLevel
            if entry.canReturn:
                ras = ras or ret.reachesAssertSameLevel
                cr = cr or ret.canReturn
                esc |= ret.escapes
            for chain in entry.escapes:
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


# Edge types that continue within the method along the one fall-through slot. THROW is an explicit
# throw to where it provably goes; SWAT reports no branch for an ATHROW, so it walks like NORMAL.
FALLTHROUGH_EDGES = {"NORMAL", "FALSE_BRANCH", "PHANTOM_FALSE_BRANCH", "THROW"}


class SAGraph:
    def __init__(self):
        self.json_graph = {}
        self.nodes: dict[str, SANode] = {}
        self.entry_node: SANode | None = None


    def load_json_graph(self, path: str):
        with open(path, "r") as f:
            self.json_graph = json.load(f)

        for json_node in self.json_graph["nodes"]:
            id = json_node["id"]
            chain = json_node.get("exceptionTypeChain")
            # Older graphs carry no isIncomplete field; absent means the node is faithful.
            self.nodes[id] = SANode(id, isIncomplete=json_node.get("isIncomplete", False),
                                    exceptionChain=tuple(chain) if chain is not None else None)

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
                source.add_fallthrough_child(target)
                # Graphs from before the call stack carry no return address. Their call sites then
                # walk like a plain fall-through, their exits find an empty stack and give up, and
                # they are marked context-insensitively.
                if "returnSite" in json_edge:
                    source.return_site = self.nodes[json_edge["returnSite"]]
                    source.handlers = {t: self.nodes[h] for t, h in json_edge.get("handlers", {}).items()}
                else:
                    legacy_calls = True

            elif etype == "RETURN":
                source.add_return_child(target)

            elif etype == "EXCEPTIONAL_RETURN":
                source.add_exceptional_return_child(target)

            elif etype in FALLTHROUGH_EDGES:
                source.add_fallthrough_child(target)

            else:
                raise ValueError(f"Unknown edge type {etype} from {source.id} to {target.id}")

            target.prev.append(source)


        self.entry_node = self.nodes[self.json_graph["entryNodeId"]]

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



if __name__ == "__main__":
    import sys
    tree = SAGraph()
    tree.load_json_graph(sys.argv[1])
    print(tree.entry_node)
    print("not reachesAssertSameLevel:", [n.id for n in tree.nodes.values() if not n.reachesAssertSameLevel])
