"""
Relevance: which branches of the SA graph can affect a target point (a static slice).

A branch is *irrelevant* if neither of its outcomes can change whether a target point is reached,
nor any value that a target point or a relevant branch reads. Then flipping it cannot lead to a
violation that keeping it cannot, and its constraint does not have to hold for one either: SWAT
does not flip it, leaves its constraint out of every query (StrategyService.collect_path_constrains)
and identifies explored paths by their relevant decisions only (dfs). This is path slicing (Jhala
and Majumdar, PLDI 2005) with the slice computed statically.

What "cannot change" means here: both outcomes lead, within the branch's method, to its immediate
post-dominator, and the code in between (its *region*) contains no target point, cannot end the
program, writes no relevant location and contains no relevant branch. Then every execution leaves
the region with the same relevant state either way, and the rest runs the same.

Over-approximations that keep this sound:

* Locations come from the extractor's data-flow facts (cfg-extraction `DataflowFacts`): locals per
  method, fields by name and type (no points-to analysis), array elements by element kind, global
  state such as the input cursor and library state. The slice is flow- and context-insensitive.
* Post-dominance ignores paths that cannot end normally: an exception that no handler can catch
  harmfully (one that can reach a target, return normally, end the program or rethrow something
  harmful) ends the program with an uncaught exception, which SWAT reports, and which rules out
  SAFE (SVCompDriver). A harmful exception that may leave the method makes the branch relevant.
* A region whose code does not terminate makes its executions time out, which rules out SAFE too.

What the static analysis cannot see -- a callback from library code, a static initializer, a branch
the executor adds (choice branches), an exception the graph does not model -- is caught in dfs,
which makes an irrelevant branch relevant again (poisons it) when such a thing happens inside its
region on some path, or when a path ends abnormally inside it.
"""
from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field

ANY = "*"

# Virtual nodes of a method's control-flow graph (see _MethodCFG): its normal exit, its harmful
# exceptional exit, and the end both lead to.
EXIT = "EXIT"
EXIT_EXC = "EXIT_EXC"
END = "END"


def is_local(location: str) -> bool:
    return location.startswith("l:") or location.startswith("p:") or location == "ret"


@dataclass(eq=False)
class RegionFacts:
    """What the region of one branch contains (see the module comment)."""
    nodes: frozenset            # the SANodes of the region, in the branch's own frame
    writes: frozenset           # locations it may write (scoped), callees included
    reaches_target: bool        # contains a target point, or calls code that may reach one
    terminates: bool            # may end the program
    branches: tuple             # the branch nodes in the region (same frame)
    callee_methods: frozenset   # the methods called in the region


@dataclass
class RelevanceStats:
    branches: int = 0
    irrelevant: int = 0
    no_postdominator: int = 0
    all_relevant: bool = False
    iterations: int = 0

    def as_dict(self) -> dict:
        return dict(branches=self.branches, irrelevant=self.irrelevant,
                    no_postdominator=self.no_postdominator, all_relevant=self.all_relevant,
                    iterations=self.iterations)


class Relevance:
    """Computes SANode.irrelevant (and SANode.region) for every branch of a loaded graph."""

    def __init__(self, nodes, harmful_handler_ok=None):
        self.nodes = [n for n in nodes]
        self.stats = RelevanceStats()
        self.by_method: dict[str, list] = {}
        for n in self.nodes:
            if n.method is not None:
                self.by_method.setdefault(n.method, []).append(n)
        self.call_sites = [n for n in self.nodes if n.is_call_site()]

    # --- scoping -----------------------------------------------------------------------------

    @staticmethod
    def scoped(method: str | None, locations) -> set:
        return {f"{method}|{loc}" if is_local(loc) else loc for loc in locations}

    # --- exceptions ----------------------------------------------------------------------------

    def _compute_harmful(self):
        """
        The exception chains (and pseudo types) that some handler may catch harmfully: the handler
        may reach a target, return normally, end the program, or let a harmful exception go on.
        Anything else ends with an uncaught exception, or never ends.
        """
        handlers: dict[tuple, set] = {}
        chains = set()
        for n in self.nodes:
            chains |= set(n.escapes)
            if n.exceptionChain is not None:
                chains.add(n.exceptionChain)
        for chain in chains:
            found = set()
            for cs in self.call_sites:
                if chain[0].startswith("?"):
                    found.update(cs.unknown_handlers.get(chain[0], []))
                else:
                    for t in chain:
                        h = cs.handlers.get(t)
                        if h is not None:
                            found.add(h)
                            break
            handlers[chain] = found
        harmful = set()
        changed = True
        while changed:
            changed = False
            for chain, hs in handlers.items():
                if chain in harmful:
                    continue
                for h in hs:
                    if (h.reachesAssertSameLevel or h.canReturn or self._method_terminates.get(h.method, True)
                            or any(c in harmful for c in h.escapes)):
                        harmful.add(chain)
                        changed = True
                        break
        self.harmful = harmful

    def _is_harmful(self, chain) -> bool:
        return chain in self.harmful

    # --- the per-method control-flow graph ----------------------------------------------------

    def _successors(self, n) -> list:
        succ = []
        if n.is_call_site():
            callees = n.callees
            if any(e.canReturn for e in callees):
                succ.append(n.return_site)
            for e in callees:
                for chain in e.escapes:
                    if chain[0].startswith("?"):
                        succ.extend(n.unknown_handlers.get(chain[0], []))
                        if self._is_harmful(chain):
                            succ.append(EXIT_EXC)
                        continue
                    handler = next((n.handlers[t] for t in chain if t in n.handlers), None)
                    if handler is not None:
                        succ.append(handler)
                    elif self._is_harmful(chain):
                        succ.append(EXIT_EXC)
            succ.extend(n.next_exceptional)
            return succ
        if n.next_fallthrough is not None:
            succ.append(n.next_fallthrough)
        if n.next_branched is not None:
            succ.append(n.next_branched)
        succ.extend(n.next_exceptional)
        if n.isExit:
            succ.append(EXIT)
        if n.exceptionChain is not None and self._is_harmful(n.exceptionChain):
            succ.append(EXIT_EXC)
        return succ

    def _postdominators(self, method_nodes):
        """
        Immediate post-dominators within a method (Cooper, Harvey and Kennedy's iterative
        algorithm on the reverse graph), over the nodes that can reach END. Returns (ipdom, succ):
        ipdom maps each such node to its immediate post-dominator, succ every node to its
        successors in the method (virtual nodes included).
        """
        own = set(method_nodes)
        succ = {}
        for n in method_nodes:
            # Only successors in the same method (or virtual): a call's callee is not, nor is a
            # handler in another frame (those are reached through the call site's own edges).
            succ[n] = [s for s in self._successors(n) if s in own or isinstance(s, str)]
        succ[EXIT] = [END]
        succ[EXIT_EXC] = [END]
        succ[END] = []
        preds = {n: [] for n in succ}
        for n, ss in succ.items():
            for s in ss:
                preds[s].append(n)
        # Post-order of a DFS from END over predecessors (the reverse graph).
        order = []
        seen = {END}
        stack = [(END, iter(preds[END]))]
        while stack:
            node, it = stack[-1]
            advanced = False
            for p in it:
                if p not in seen:
                    seen.add(p)
                    stack.append((p, iter(preds[p])))
                    advanced = True
                    break
            if not advanced:
                stack.pop()
                order.append(node)
        index = {n: i for i, n in enumerate(order)}  # END has the highest
        ipdom = {END: END}
        rpo = list(reversed(order))
        changed = True
        while changed:
            changed = False
            for n in rpo:
                if n is END:
                    continue
                new = None
                for s in succ[n]:
                    if s in ipdom:
                        if new is None:
                            new = s
                        else:
                            a, b = s, new
                            while a is not b:
                                while index[a] < index[b]:
                                    a = ipdom[a]
                                while index[b] < index[a]:
                                    b = ipdom[b]
                            new = a
                if new is not None and ipdom.get(n) is not new:
                    ipdom[n] = new
                    changed = True
        return ipdom, succ

    # --- the analysis ---------------------------------------------------------------------------

    def compute(self):
        nodes = self.nodes
        # Which methods may end the program, and which global locations they may write, callees
        # included (least fixpoints over the call graph).
        callees_of: dict[str, set] = {m: set() for m in self.by_method}
        for cs in self.call_sites:
            for e in cs.callees:
                if cs.method is not None and e.method is not None:
                    callees_of.setdefault(cs.method, set()).add(e.method)
        terminates = {m: any(n.terminates for n in ns) for m, ns in self.by_method.items()}
        global_writes = {m: set() for m in self.by_method}
        for m, ns in self.by_method.items():
            for n in ns:
                for written, _ in n.dataflow:
                    global_writes[m].update(w for w in written if not is_local(w))
        changed = True
        while changed:
            changed = False
            for m, cs in callees_of.items():
                for c in cs:
                    if terminates.get(c) and not terminates.get(m):
                        terminates[m] = True
                        changed = True
                    extra = global_writes.get(c, set()) - global_writes.setdefault(m, set())
                    if extra:
                        global_writes[m] |= extra
                        changed = True
        self._method_terminates = terminates
        self._compute_harmful()

        # The data dependencies: each written location on what it may be computed from.
        deps: dict[str, set] = {}
        star_reads: set = set()

        def depend(written, read):
            if ANY in written:
                star_reads.update(read)
            for w in written:
                deps.setdefault(w, set()).update(read)

        for n in nodes:
            m = n.method
            for written, read in n.dataflow:
                depend(self.scoped(m, written), self.scoped(m, read))
            if n.call is not None and n.is_call_site():
                args = n.call.get("args", [])
                base = n.call.get("base", [])
                result = n.call.get("result")
                for e in n.callees:
                    em = e.method
                    for i, arg in enumerate(args):
                        depend({f"{em}|p:{i}"}, self.scoped(m, arg))
                    depend({f"{em}|p:this"}, self.scoped(m, base))
                    if result is not None:
                        depend(self.scoped(m, [result]), {f"{em}|ret"}
                               | self.scoped(m, n.call.get("resultAddress", [])))

        # Regions.
        regions: dict = {}
        branches = [n for n in nodes if n.is_branch()]
        self.stats.branches = len(branches)
        no_ipdom = set()
        # Statements that may throw to somewhere that matters (a local handler, or a harmful
        # escape) are implicit branches: whether they throw depends on what they read (a null
        # receiver, a cast's operand, a callee's arguments), so they get a region too, and their
        # reads become relevant when it is. They are no tree branches: nothing flips them.
        throwers = []
        for m, method_nodes in self.by_method.items():
            ipdom, succ = None, None
            mbranches = [n for n in method_nodes if n.is_branch()]
            if not mbranches and not any(n.next_exceptional or n.is_call_site() or n.exceptionChain
                                         for n in method_nodes):
                continue
            ipdom, succ = self._postdominators(method_nodes)
            mthrowers = [n for n in method_nodes if not n.is_branch() and len(set(succ[n])) >= 2]
            throwers.extend(mthrowers)
            for b in mbranches + mthrowers:
                stop = ipdom.get(b)
                if stop is None or stop == END:
                    # No post-dominator (no path to the end), or the outcomes leave the method
                    # differently: either may matter.
                    no_ipdom.add(b)
                    continue
                seen = set()
                work = [s for s in succ[b] if s is not stop]
                while work:
                    x = work.pop()
                    if x in seen or x is stop:
                        continue
                    seen.add(x)
                    work.extend(succ.get(x, ()))
                region_nodes = frozenset(x for x in seen if not isinstance(x, str))
                if EXIT_EXC in seen or EXIT in seen:
                    no_ipdom.add(b)  # cannot happen below END, but be safe
                    continue
                writes = set()
                reaches_target = False
                term = False
                rbranches = []
                cmethods = set()
                for x in region_nodes:
                    for written, _ in x.dataflow:
                        writes |= self.scoped(m, written)
                    if x.isAssert or x.isIncomplete:
                        reaches_target = True
                    if x.terminates:
                        term = True
                    if x.is_branch():
                        rbranches.append(x)
                    if x.is_call_site():
                        for e in x.callees:
                            cmethods.add(e.method)
                            writes |= global_writes.get(e.method, set())
                            if e.reachesAssertSameLevel:
                                reaches_target = True
                            if terminates.get(e.method):
                                term = True
                        if x.call is not None and x.call.get("result") is not None:
                            writes |= self.scoped(m, [x.call["result"]])
                regions[b] = RegionFacts(region_nodes, frozenset(writes), reaches_target, term,
                                         tuple(rbranches), frozenset(cmethods))
        self.stats.no_postdominator = sum(1 for b in no_ipdom if b.is_branch())

        # The least fixpoint: relevant locations and relevant branches.
        relevant_locations: set = set()
        everything = False

        def add(locations):
            nonlocal everything
            work = [l for l in locations if l not in relevant_locations]
            first = not relevant_locations
            while work:
                l = work.pop()
                if l in relevant_locations:
                    continue
                if l == ANY:
                    everything = True
                relevant_locations.add(l)
                work.extend(x for x in deps.get(l, ()) if x not in relevant_locations)
            if first and relevant_locations and star_reads:
                add(star_reads)  # a write to anything may write a relevant location

        for n in nodes:
            if n.isAssert or n.isIncomplete:
                for _, read in n.dataflow:
                    add(self.scoped(n.method, read))
                add(self.scoped(n.method, n.control))
                if n.is_call_site() and n.call is not None:
                    for arg in n.call.get("args", []):
                        add(self.scoped(n.method, arg))
                    add(self.scoped(n.method, n.call.get("base", [])))
            if n.terminates:
                add(self.scoped(n.method, n.control))

        def reads_of(n):
            """What a branch's decision depends on; for an implicit branch (a thrower), everything
            it reads."""
            if n.is_branch():
                return self.scoped(n.method, n.control)
            read = set(self.scoped(n.method, n.control))
            for _, r in n.dataflow:
                read |= self.scoped(n.method, r)
            if n.call is not None:
                for arg in n.call.get("args", []):
                    read |= self.scoped(n.method, arg)
                read |= self.scoped(n.method, n.call.get("base", []))
            return read

        relevant = set(no_ipdom)
        for b in no_ipdom:
            add(reads_of(b))
        candidates = [b for b in branches + throwers if b in regions]
        changed = True
        while changed and not everything:
            self.stats.iterations += 1
            changed = False
            # Methods that run a relevant branch, callees included.
            relevant_methods = {b.method for b in relevant if b.is_branch()}
            callers_of: dict[str, set] = {}
            for m, cs in callees_of.items():
                for c in cs:
                    callers_of.setdefault(c, set()).add(m)
            work = list(relevant_methods)
            while work:
                c = work.pop()
                for m in callers_of.get(c, ()):
                    if m not in relevant_methods:
                        relevant_methods.add(m)
                        work.append(m)
            for b in candidates:
                if b in relevant:
                    continue
                r = regions[b]
                if (r.reaches_target or r.terminates
                        or not r.writes.isdisjoint(relevant_locations)
                        or (ANY in r.writes and relevant_locations)
                        or any(x in relevant for x in r.branches)
                        or not r.callee_methods.isdisjoint(relevant_methods)):
                    relevant.add(b)
                    add(reads_of(b))
                    changed = True
        self.stats.all_relevant = everything
        self.relevant_locations = relevant_locations

        for b in branches:
            irrelevant = not everything and b not in relevant and b in regions
            b.irrelevant = irrelevant
            b.region = regions[b].nodes if irrelevant else None
        self.stats.irrelevant = sum(1 for b in branches if b.irrelevant)
        return self.stats


def in_region(region_owner, owner_stack, node, stack) -> bool | None:
    """
    Whether the walk's position (`node` under call stack `stack`) is still inside the region of the
    irrelevant branch `region_owner`, reached with `owner_stack`: in the region in the owner's own
    frame, or in a frame called from a call site in the region. None if it cannot be told.
    """
    d = 0 if owner_stack is None else owner_stack[2]
    depth = 0 if stack is None else stack[2]
    if depth < d:
        return False
    cell = stack
    inner_site = None
    while (0 if cell is None else cell[2]) > d:
        inner_site = cell[0]
        cell = cell[1]
    if cell is not owner_stack:
        return False  # the owner's frame is gone
    if inner_site is None:
        return node in region_owner.region
    return inner_site in region_owner.region
