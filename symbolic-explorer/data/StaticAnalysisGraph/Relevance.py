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

from dataclasses import dataclass
from typing import Any, Final, Iterable, Iterator, Literal, Optional, Union

from data.StaticAnalysisGraph.SAGraph import CallStack, ExceptionChain, Location, SANode, is_pseudo

ANY: Final = "*"

# Virtual nodes of a method's control-flow graph (see _postdominators): its normal exit, its
# harmful exceptional exit, and the end both lead to.
EXIT: Final = "EXIT"
EXIT_EXC: Final = "EXIT_EXC"
END: Final = "END"
# A node of a method's control-flow graph: a graph node, or a virtual one.
CfgNode = Union[SANode, Literal["EXIT", "EXIT_EXC", "END"]]
# A method by its signature; None for nodes without one (SANode.method).
Method = Optional[str]


def is_local(location: Location) -> bool:
    return location.startswith("l:") or location.startswith("p:") or location == "ret"


@dataclass(eq=False)
class RegionFacts:
    """What the region of one branch contains (see the module comment)."""
    nodes: frozenset[SANode]         # the nodes of the region, in the branch's own frame
    writes: frozenset[Location]      # locations it may write (scoped), callees included
    reaches_target: bool             # contains a target point, or calls code that may reach one
    terminates: bool                 # may end the program
    branches: tuple[SANode, ...]     # the branch nodes in the region (same frame)
    callee_methods: frozenset[Method]  # the methods called in the region


@dataclass
class RelevanceStats:
    branches: int = 0
    irrelevant: int = 0
    no_postdominator: int = 0
    all_relevant: bool = False
    iterations: int = 0
    # Hand-written irrelevance facts (user facts): those used, those refuted (see _user_facts),
    # and the branches irrelevant only if the facts hold
    user_facts: list[str] | None = None
    refuted_facts: list[str] | None = None
    irrelevant_assuming: int = 0

    def as_dict(self) -> dict[str, Any]:
        d = dict(branches=self.branches, irrelevant=self.irrelevant,
                 no_postdominator=self.no_postdominator, all_relevant=self.all_relevant,
                 iterations=self.iterations)
        if self.user_facts is not None:
            d.update(user_facts=self.user_facts, refuted_facts=self.refuted_facts,
                     irrelevant_assuming=self.irrelevant_assuming)
        return d


class Relevance:
    """Computes SANode.irrelevant, summary_ok and region for every branch of a loaded graph."""

    def __init__(self, nodes: Iterable[SANode]) -> None:
        self.nodes = list(nodes)
        self.stats = RelevanceStats()
        self.by_method: dict[str, list[SANode]] = {}
        for n in self.nodes:
            if n.method is not None:
                self.by_method.setdefault(n.method, []).append(n)
        self.call_sites = [n for n in self.nodes if n.is_call_site()]

    # --- scoping -----------------------------------------------------------------------------

    @staticmethod
    def scoped(method: str | None, locations: Iterable[Location]) -> set[Location]:
        """The locations, with those local to a method (see is_local) named `<method>|<location>`."""
        return {f"{method}|{loc}" if is_local(loc) else loc for loc in locations}

    # --- exceptions ----------------------------------------------------------------------------

    def _compute_harmful(self) -> None:
        """
        The exception chains (and pseudo types) that some handler may catch harmfully: the handler
        may reach a target, return normally, end the program, or let a harmful exception go on.
        Anything else ends with an uncaught exception, or never ends.
        """
        handlers: dict[ExceptionChain, set[SANode]] = {}
        chains: set[ExceptionChain] = set()
        for n in self.nodes:
            chains |= set(n.escapes)
            if n.exceptionChain is not None:
                chains.add(n.exceptionChain)
        for chain in chains:
            found: set[SANode] = set()
            for cs in self.call_sites:
                if is_pseudo(chain):
                    found.update(cs.unknown_handlers.get(chain[0], []))
                else:
                    h = cs.handler_for(chain)
                    if h is not None:
                        found.add(h)
            handlers[chain] = found
        harmful: set[ExceptionChain] = set()
        changed = True
        while changed:
            changed = False
            for chain, hs in handlers.items():
                if chain in harmful:
                    continue
                for h in hs:
                    if (h.reachesTargetSameLevel or h.canReturn or self._method_terminates.get(h.method, True)
                            or any(c in harmful for c in h.escapes)):
                        harmful.add(chain)
                        changed = True
                        break
        self.harmful = harmful

    # --- the per-method control-flow graph ----------------------------------------------------

    def _successors(self, n: SANode) -> list[CfgNode]:
        succ: list[CfgNode] = []
        if n.is_call_site():
            callees = n.callees
            if any(e.canReturn for e in callees):
                succ.append(n.resume_site())
            for e in callees:
                for chain in e.escapes:
                    if is_pseudo(chain):
                        succ.extend(n.unknown_handlers.get(chain[0], []))
                        if chain in self.harmful:
                            succ.append(EXIT_EXC)
                        continue
                    handler = n.handler_for(chain)
                    if handler is not None:
                        succ.append(handler)
                    elif chain in self.harmful:
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
        if n.exceptionChain is not None and n.exceptionChain in self.harmful:
            succ.append(EXIT_EXC)
        return succ

    def _postdominators(self, method_nodes: list[SANode]) -> tuple[dict[CfgNode, CfgNode], dict[CfgNode, list[CfgNode]]]:
        """
        Immediate post-dominators within a method (Cooper, Harvey and Kennedy's iterative
        algorithm on the reverse graph), over the nodes that can reach END. Returns (ipdom, succ):
        ipdom maps each such node to its immediate post-dominator, succ every node to its
        successors in the method (virtual nodes included).
        """
        own = set(method_nodes)
        succ: dict[CfgNode, list[CfgNode]] = {}
        for n in method_nodes:
            # Only successors in the same method (or virtual): a call's callee is not, nor is a
            # handler in another frame (those are reached through the call site's own edges).
            succ[n] = [s for s in self._successors(n) if s in own or isinstance(s, str)]
        succ[EXIT] = [END]
        succ[EXIT_EXC] = [END]
        succ[END] = []
        preds: dict[CfgNode, list[CfgNode]] = {n: [] for n in succ}
        for x, ss in succ.items():
            for s in ss:
                preds[s].append(x)
        # Post-order of a DFS from END over predecessors (the reverse graph).
        order: list[CfgNode] = []
        seen: set[CfgNode] = {END}
        stack: list[tuple[CfgNode, Iterator[CfgNode]]] = [(END, iter(preds[END]))]
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
        ipdom: dict[CfgNode, CfgNode] = {END: END}
        rpo = list(reversed(order))
        changed = True
        while changed:
            changed = False
            for x in rpo:
                if x is END:
                    continue
                new: CfgNode | None = None
                for s in succ[x]:
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
                if new is not None and ipdom.get(x) is not new:
                    ipdom[x] = new
                    changed = True
        return ipdom, succ

    # --- the analysis ---------------------------------------------------------------------------

    def compute(self) -> RelevanceStats:
        """Sets irrelevant, summary_ok and region on every branch; with hand-written irrelevance
        facts in the graph (user facts), also checks them and sets irrelevant_assuming (see
        _user_facts)."""
        facts = self._user_facts()
        if facts:
            assuming = self._compute(frozenset(facts))
        result = self._compute(frozenset())
        for b, (irrelevant, region, summary_ok) in result.items():
            b.irrelevant, b.region, b.summary_ok = irrelevant, region, summary_ok
            b.irrelevant_assuming = False
            if facts and not irrelevant and assuming[b][0] and b not in facts:
                # Irrelevant only if the facts hold: dfs may slice it, and counts the facts
                b.irrelevant_assuming = True
                b.region = assuming[b][1]
        self.stats.irrelevant = sum(1 for b in result if b.irrelevant)
        self.stats.irrelevant_assuming = sum(1 for b in result if b.irrelevant_assuming)
        return self.stats

    def _user_facts(self) -> dict[SANode, str]:
        """
        The branches covered by a hand-written irrelevance fact (user facts: @Irrelevant, a
        Swat.beginIrrelevant region; the extractor's userFact), by the fact. A fact covers the nodes
        the extractor marked, and every method all of whose call sites it covers (callees included;
        the executor applies a fact to everything called from its code, so a method that is also
        called from elsewhere only in that context, which this context-insensitive analysis cannot
        tell apart).

        A fact is refuted, and not used, if its code contains a target point, may end the program,
        or calls code that may reach a target point: whether such code runs, which its branches
        decide, matters. Refuted facts go to self.refuted_facts (dfs ignores them where the executor
        applied them), the others to self.user_facts.
        """
        by_fact: dict[str, set[SANode]] = {}
        for n in self.nodes:
            if n.user_fact is not None:
                by_fact.setdefault(n.user_fact, set()).add(n)
        self.user_facts: set[str] = set()
        self.refuted_facts: set[str] = set()
        if not by_fact:
            return {}
        entries: dict[Method, list[SANode]] = {}
        call_sites_into: dict[Method, list[SANode]] = {}
        for cs in self.call_sites:
            for e in cs.callees:
                call_sites_into.setdefault(e.method, []).append(cs)
        covered_branches: dict[SANode, str] = {}
        for fact, marked in by_fact.items():
            covered = set(marked)
            methods: set[Method] = set()
            changed = True
            while changed:
                changed = False
                for m, sites in call_sites_into.items():
                    if m not in methods and m in self.by_method and sites and all(s in covered for s in sites):
                        methods.add(m)
                        covered |= set(self.by_method[m])
                        changed = True
            refuted = False
            for n in covered:
                if n.isTarget or n.isIncomplete or n.terminates or self._method_terminates_at(n):
                    refuted = True
                    break
                if n.is_call_site() and any(e.method not in methods and e.reachesTargetSameLevel for e in n.callees):
                    refuted = True
                    break
            if refuted:
                self.refuted_facts.add(fact)
                continue
            self.user_facts.add(fact)
            for n in covered:
                if n.is_branch():
                    covered_branches.setdefault(n, fact)
        self.stats.user_facts = sorted(self.user_facts)
        self.stats.refuted_facts = sorted(self.refuted_facts)
        return covered_branches

    def _method_terminates_at(self, n: SANode) -> bool:
        """A call site whose callees may end the program."""
        return n.is_call_site() and any(self._terminating_method(e.method) for e in n.callees)

    def _terminating_method(self, m: Method) -> bool:
        if not hasattr(self, "_terminating"):
            callees_of: dict[Method, set[Method]] = {}
            for cs in self.call_sites:
                for e in cs.callees:
                    callees_of.setdefault(cs.method, set()).add(e.method)
            term = {mm: any(x.terminates for x in ns) for mm, ns in self.by_method.items()}
            changed = True
            while changed:
                changed = False
                for caller, called in callees_of.items():
                    if not term.get(caller) and any(term.get(c) for c in called):
                        term[caller] = True
                        changed = True
            self._terminating = term
        return bool(self._terminating.get(m))

    def _compute(self, forced: frozenset[SANode]) -> dict[SANode, tuple[bool, frozenset[SANode] | None, bool]]:
        """
        The relevance analysis proper: (irrelevant, region, summary_ok) per branch. The branches in
        `forced` are taken to be irrelevant (they never become relevant, so what they read does
        not become relevant through them).
        """
        nodes = self.nodes
        self.stats = RelevanceStats(user_facts=self.stats.user_facts, refuted_facts=self.stats.refuted_facts)
        # Which methods may end the program, and which global locations they may write, callees
        # included (least fixpoints over the call graph).
        callees_of: dict[Method, set[Method]] = {m: set() for m in self.by_method}
        for cs in self.call_sites:
            for e in cs.callees:
                if cs.method is not None and e.method is not None:
                    callees_of.setdefault(cs.method, set()).add(e.method)
        terminates: dict[Method, bool] = {m: any(n.terminates for n in ns) for m, ns in self.by_method.items()}
        global_writes: dict[Method, set[Location]] = {m: set() for m in self.by_method}
        for m, ns in self.by_method.items():
            for n in ns:
                for written, _ in n.dataflow:
                    global_writes[m].update(w for w in written if not is_local(w))
        changed = True
        while changed:
            changed = False
            for caller, called in callees_of.items():
                for c in called:
                    if terminates.get(c) and not terminates.get(caller):
                        terminates[caller] = True
                        changed = True
                    extra = global_writes.get(c, set()) - global_writes.setdefault(caller, set())
                    if extra:
                        global_writes[caller] |= extra
                        changed = True
        self._method_terminates = terminates
        self._compute_harmful()

        # The data dependencies: each written location on what it may be computed from.
        deps: dict[Location, set[Location]] = {}
        star_reads: set[Location] = set()

        def depend(written: set[Location], read: set[Location]) -> None:
            if ANY in written:
                star_reads.update(read)
            for w in written:
                deps.setdefault(w, set()).update(read)

        for n in nodes:
            method = n.method
            for written, read in n.dataflow:
                depend(self.scoped(method, written), self.scoped(method, read))
            if n.call is not None and n.is_call_site():
                args = n.call.get("args", [])
                base = n.call.get("base", [])
                result = n.call.get("result")
                for e in n.callees:
                    em = e.method
                    for i, arg in enumerate(args):
                        depend({f"{em}|p:{i}"}, self.scoped(method, arg))
                    depend({f"{em}|p:this"}, self.scoped(method, base))
                    if result is not None:
                        depend(self.scoped(method, [result]), {f"{em}|ret"}
                               | self.scoped(method, n.call.get("resultAddress", [])))

        # Regions.
        regions: dict[SANode, RegionFacts] = {}
        branches = [n for n in nodes if n.is_branch()]
        self.stats.branches = len(branches)
        no_ipdom: set[SANode] = set()
        # Statements that may throw to somewhere that matters (a local handler, or a harmful
        # escape) are implicit branches: whether they throw depends on what they read (a null
        # receiver, a cast's operand, a callee's arguments), so they get a region too, and their
        # reads become relevant when it is. They are no tree branches: nothing flips them.
        throwers: list[SANode] = []
        for m, method_nodes in self.by_method.items():
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
                seen: set[CfgNode] = set()
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
                writes: set[Location] = set()
                reaches_target = False
                term = False
                rbranches: list[SANode] = []
                cmethods: set[Method] = set()
                for x in region_nodes:
                    for written, _ in x.dataflow:
                        writes |= self.scoped(m, written)
                    if x.isTarget or x.isIncomplete:
                        reaches_target = True
                    if x.terminates:
                        term = True
                    if x.is_branch():
                        rbranches.append(x)
                    if x.is_call_site():
                        for e in x.callees:
                            cmethods.add(e.method)
                            writes |= global_writes.get(e.method, set())
                            if e.reachesTargetSameLevel:
                                reaches_target = True
                            if terminates.get(e.method):
                                term = True
                        if x.call is not None and x.call.get("result") is not None:
                            writes |= self.scoped(m, [x.call["result"]])
                regions[b] = RegionFacts(region_nodes, frozenset(writes), reaches_target, term,
                                         tuple(rbranches), frozenset(cmethods))
        self.stats.no_postdominator = sum(1 for b in no_ipdom if b.is_branch())

        # The least fixpoint: relevant locations and relevant branches.
        relevant_locations: set[Location] = set()
        everything = False

        def add(locations: Iterable[Location]) -> None:
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
            if n.isTarget or n.isIncomplete:
                for _, read in n.dataflow:
                    add(self.scoped(n.method, read))
                add(self.scoped(n.method, n.control))
                if n.is_call_site() and n.call is not None:
                    for arg in n.call.get("args", []):
                        add(self.scoped(n.method, arg))
                    add(self.scoped(n.method, n.call.get("base", [])))
            if n.terminates:
                add(self.scoped(n.method, n.control))

        def reads_of(n: SANode) -> set[Location]:
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

        relevant = set(b for b in no_ipdom if b not in forced)
        for b in no_ipdom:
            if b not in forced:
                add(reads_of(b))
        candidates = [b for b in branches + throwers if b in regions]
        callers_of: dict[Method, set[Method]] = {}
        for caller, called in callees_of.items():
            for c in called:
                callers_of.setdefault(c, set()).add(caller)

        def methods_running(relevant: set[SANode]) -> set[Method]:
            """The methods that run a relevant branch, callees included."""
            methods = {b.method for b in relevant if b.is_branch()}
            work = list(methods)
            while work:
                c = work.pop()
                for m in callers_of.get(c, ()):
                    if m not in methods:
                        methods.add(m)
                        work.append(m)
            return methods

        changed = True
        while changed and not everything:
            self.stats.iterations += 1
            changed = False
            relevant_methods = methods_running(relevant)
            for b in candidates:
                if b in relevant or b in forced:
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

        # Branches whose regions change nothing relevant but locals of their own method: a loop head
        # whose loop the executor summarized (closed forms of its locals) while its body also does
        # other things, which must not matter (see dfs and the executor's LoopSummaries).
        relevant_methods = methods_running(relevant)

        result: dict[SANode, tuple[bool, frozenset[SANode] | None, bool]] = {}
        for b in branches:
            irrelevant = not everything and b not in relevant and b in regions
            region = regions[b].nodes if irrelevant else None
            result[b] = (irrelevant, region, False)
            if everything or b not in regions:
                continue
            r = regions[b]
            if (r.reaches_target or r.terminates or ANY in r.writes
                    or any('|' not in w and w in relevant_locations for w in r.writes)
                    or any(x in relevant and x is not b for x in r.nodes)
                    or not r.callee_methods.isdisjoint(relevant_methods)):
                continue
            result[b] = (irrelevant, r.nodes, True)
        return result


def in_region(region_owner: SANode, owner_stack: CallStack, node: SANode, stack: CallStack) -> bool:
    """
    Whether the walk's position (`node` under call stack `stack`) is still inside the region of the
    sliced branch `region_owner`, reached with `owner_stack`: in the region in the owner's own
    frame, or in a frame called from a call site in the region.
    """
    d = 0 if owner_stack is None else owner_stack[2]
    depth = 0 if stack is None else stack[2]
    if depth < d:
        return False
    region = region_owner.region
    assert region is not None, f"{region_owner.id} has no region"
    cell = stack
    inner_site: SANode | None = None
    while cell is not None and cell[2] > d:
        inner_site = cell[0]
        cell = cell[1]
    if cell is not owner_stack:
        return False  # the owner's frame is gone
    if inner_site is None:
        return node in region
    return inner_site in region
