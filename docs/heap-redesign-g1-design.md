# G1 — Canonical Registry + Faithful Key: Problem & Proposed Solution

Phase 1 of the heap redesign. **Status: IMPLEMENTED on `fix/heap-design`** (converged after 1 independent review round — see "Review round 1 — resolutions" below). Acceptance green at all levels: O-4 + O-5 green (L0), V-2/F-1/F-2 stay green, V-1 stays pending (correct — that's G2), L2 anchor green through the real agent. No half-migrated API (int heap key fully replaced by the concrete-reference key; `int address` retained only for NULL/de-intern/delegation/debug). Implementation summary at the end of this doc.

## Problem (recap, grounded)

The shadow heap is a *recovery cache* — `JVMHeap` = `HashMap<Integer,Value>` keyed by `System.identityHashCode` (`shadow/JVMHeap.java`), consulted only when a reference re-enters from unmodeled code as a `PlaceHolder` (`SymbolicInstructionVisitor.visitGETVALUE_Object` `getFromHeap(inst.address)`; also `visitLDC_Object`, delegation). Frames hold *direct* `ObjectValue` references, so in-flow aliasing works by shared pointers; the heap is the fallback to re-correlate a bare reference with its shadow.

Four confirmed bugs (audit):

- **(a) duplicate shadow per concrete object** — `visitNEW`/`visitLDC_String` create+push without registering; canonical registration is incidental (only if a `GETVALUE_Object` later fires while the object is stack-top). Distinct wrappers for one object → divergent field maps.
- **(c) collision + no eviction** — 31-bit `identityHashCode` key in a never-evicted `HashMap`: distinct live objects collide (second `put` overwrites); dead-object hashes get reused.
- **(d) unregistered objects** — as (a): `NEW`/arrays/`LDC_String` register only incidentally.
- **`==` correctness** — `ObjectValue.IF_ACMPEQ` is `bfm.makeBoolean(this == o2)` (`ObjectValue.java:153`), i.e. *wrapper* Java-identity. This is correct **iff** there is exactly one wrapper per concrete object — which (a)/(d) violate. So duplicate wrappers make real `==` semantics wrong.

Root cause: the correlation key (`identityHashCode`) is not faithful (collisions; not unique), and there is no canonicalization (find-or-create) guaranteeing one shadow per concrete object.

## Proposed solution

**Make the heap a canonical, faithful-key registry; keep frames holding direct references (cached pointers); canonicalize at the boundary sites only.** No full per-access indirection (the GETVALUE path is hot; full indirection would add a lookup per object touch — non-goal per the plan).

1. **Faithful key = the concrete object reference.** Replace the int-`identityHashCode`-keyed map with an identity map keyed by the actual concrete `Object` (reference equality, not hash). Use a weak-keyed identity map so dead objects evict — Guava `new MapMaker().weakKeys().makeMap()` (Guava already a dependency; `weakKeys()` switches the map to `==` key comparison and weak references). This fixes (c): distinct objects never collide (reference identity); dead entries are GC'd (eviction → fixes the O-6 reuse hazard).

2. **Canonicalize (find-or-create) at every introduce/recover site.** One shadow per concrete object:
   - `visitGETVALUE_Object` already carries the concrete object as `inst.val` (`AbstractInstructionProcessor.GETVALUE_Object` passes `v`). Recovery becomes `registry.get(inst.val)`; on miss, create the shadow and `registry.put(inst.val, shadow)`. The existing `ADDRESS_UNKNOWN` adopt-peek branch becomes the canonical register-on-first-sight for `NEW`/array results (they already flow through a `GETVALUE_Object`).
   - **`visitLDC_Object` currently drops the object** — `AbstractInstructionProcessor.LDC(long,Object)` builds `new LDC_Object(iid, identityHashCode(c))` (`:35`). Thread the object through (`LDC_Object(iid, identityHashCode, c)`) so it can canonicalize by reference like the others. (`LDC_String` already carries the string.)
   - delegation path (`visitGETVALUE_Object` `:1374`): key by the delegated object reference too.

3. **`register-on-create`.** `NEW`/`*NEWARRAY`/`LDC_String`/`LDC_Object` results are canonicalized at their immediately-following `GETVALUE_Object` (which has the concrete ref). No object is left unregistered before it can re-enter. (Fixes (a)/(d).)

4. **Keep `IF_ACMPEQ = this == o2` unchanged.** Once canonicalization guarantees one wrapper per concrete object, wrapper-identity *is* concrete-identity, so `this == o2` is correct **and collision-free** (it never consults the 31-bit hash). We deliberately do **not** switch `IF_ACMPEQ` to compare `address`, because `address` stays `identityHashCode` and address-comparison would re-introduce collisions.

5. **`address` keeps its current meaning** (`identityHashCode`, with `0`=null, `-1`=unknown) — used only for the NULL check (`IFNULL`/`IFNONNULL`), de-interned-string compare (`ObjectsInvocation`), and debug labels. It is **not** the equality key. (Optional later cleanup: give each canonical shadow a unique id and retire `address`-as-hash; out of scope for G1.)

6. **Inspection seam unchanged in contract.** `ShadowContext.heapSize()/heapLookup/heapEntries` and `JVMHeap.size()/values()` keep their semantics (one entry per canonical identity; `heapSize()` = distinct identities). The registry implements the same view, so the O-tests' assertions are stable.

This fixes (a), (c), (d), and the `==` bug. It does **not** fix V-1 (value-type this-return aliasing) — under a faithful key, `registry.get(this-returned-object)` still returns the receiver's shadow *because it is genuinely the same object*; G2's value-type policy is what stops identity-recovery for immutables. G1 before G2 is correct ordering.

## Acceptance tests — and a contradiction to resolve

The intended acceptance reds are **O-4** ("two wrappers for one identity compare equal") and **O-5-heap** ("colliding-hash objects don't merge → `heapSize()==2`"). As currently written (L0, `ObjectIdentitySpec`), they construct `ObjectValue`s **directly** at a chosen `address`, with no concrete object behind them. That creates a real contradiction:

- **O-4-same** (red): `a=objectAt(0x2000)`, `b=objectAt(0x2000)` → asserts `IF_ACMPEQ` **valid** (equal).
- **O-5-distinct** (green guard): `a=objectAt(0x5000)`, `b=objectAt(0x5000)` → asserts `IF_ACMPEQ` **unsatisfiable** (unequal).

These are the *same construction* (two distinct wrappers sharing an address) with **opposite** expectations. No `IF_ACMPEQ` — wrapper-based or address-based — can satisfy both: wrapper-identity makes both unequal (O-4 fails, current state), address-equality makes both equal (O-5 breaks). The L0 manual construction cannot encode the real distinction ("same concrete object" vs "distinct objects, colliding hash"), because that distinction lives in the concrete reference, which L0 doesn't have.

**Proposed resolution:** the canonicalization behavior must be exercised *through the registry by concrete reference*, not by manual wrapper construction:

- **O-4** → obtain the two references by canonicalizing the **same** concrete object twice (`registry.findOrCreate(obj)` → returns the *same* wrapper) → `IF_ACMPEQ` true. (Keep `this==o2`.)
- **O-5** → canonicalize **two distinct** concrete objects (distinct refs, even with a colliding `identityHashCode`) → two distinct wrappers, `heapSize()==2`, and `IF_ACMPEQ` false.

This makes O-4 and O-5 consistent and tests the actual fix. The truest home for these is L1/L2 (real re-entry of real objects); at minimum the L0 specs must be rewritten to drive the registry's find-or-create with concrete references rather than fabricated wrappers at fixed addresses. (The handoff already anticipated rewriting O-5's `when:` block; this extends that to O-4 and explains why.)

## Risks / open questions for review

- **Weak identity map semantics & perf.** Guava `weakKeys()` map at introduce/recover (not per-access). Is the added lookup acceptable on the hot GETVALUE path? Any concern with weak-ref GC timing causing a live-but-evicted miss (then a fresh shadow is created — sound but loses field state)?
- **Threading the concrete ref to `LDC_Object`** (and delegation): correct, and are there other heap-consult sites that only have the int?
- **`address` left as `identityHashCode`**: any remaining code that *relies* on `address` for identity/equality (beyond NULL/de-intern/debug) that would still be collision-exposed?
- **NULL handling** under reference keying: `inst.val == null` ⇒ NULL value (don't register). Confirmed sufficient?
- **Is keeping `this==o2` (vs a unique-id) the right call for G1**, deferring the unique-id cleanup — or does leaving `address`=hash leave a latent trap?
- **Test-rework correctness:** is the O-4/O-5 resolution above sound, and does it actually exercise canonicalization rather than tautology?

## Review round 1 — resolutions

Accepted from review:

- **[B1] Declare Guava explicitly.** Guava is currently only a *transitive* leak (build.gradle:35-47 declares no guava; `ObjectValue.java:3` imports `ImmutableSet` only because java-smt/z3 jars drag it in). Add an explicit `implementation` on guava (catalog entry) as a prerequisite before using `MapMaker`; this also de-risks the existing `ImmutableSet`.
- **[B2] Don't tautologize the L0 tests; move behavioral acceptance to L1.** Resolution refined: **keep `IF_ACMPEQ = this == o2`** unchanged, so the existing green L0 guards stay green and unchanged — "O-4 distinct objects" (different addresses) and "O-5 distinct objects, colliding hash" (two distinct wrappers → `this==o2` false → unequal). The two **red** behavioral cases move to **L1** (real `GETVALUE_Object` recovery by reference): O-4 = re-enter the *same* concrete object twice → registry returns the *same* wrapper → `this==o2` true; O-5 = re-enter two *distinct* concrete objects (even colliding hash) → distinct wrappers → `heapSize()==2` and unequal. At L0 keep only a *non-circular structural* assertion (e.g. distinct keys ⇒ distinct entries that does not pre-assume find-or-create); do not rewrite the green guards into registry-tautologies (would lose their infra-break protection). The contradiction the doc named is real (O-4-same `0x2000/0x2000` expect-equal vs O-5-distinct `0x5000/0x5000` expect-unequal are the same construction with opposite expectations), and "keep `this==o2` + move the reds to L1" is what dissolves it — manual same-address wrappers can't encode same-object vs distinct-colliding.
- **[C1] Weaken the `==`-correctness claim to the accurate version.** Not "canonicalization guarantees one wrapper everywhere." Accurate: for plain reference objects `this==o2` is correct because **either** both operands are in-flow shared pointers (`visitIF_ACMPEQ` pops them straight off the stack, no registry), **or** a recovered operand was canonicalized at the boundary. Strings don't use this path at all (`StringValue.IF_ACMPEQ:81` uses formula equality). **Residual gap (documented, acceptable for G1):** a `NEW`/array result that escapes to unmodeled code *before its first `GETVALUE_Object`* re-enters unregistered → a second wrapper. Registration happens at the first `GETVALUE_Object` (where the concrete ref exists; the ref is not available at `visitNEW`), so "register-on-create" = "register at first GETVALUE." This narrows but does not fully close bug (a); note it.
- **[C2] `LDC_Object` threading is feasible and a bonus fix.** The instrumentation already pushes the constant for the dispatch (`InstructionMethodAdapter.visitLdcInsn`), so `LDC(long,Object)` receives the real object — threading it is pure plumbing. Bonus: today an LDC'd object becomes a bare addressed `ObjectValue` with `null` concrete (`visitLDC_Object` → `createObjectValue(null, inst.c)`); threading the ref fixes that too. Caveat: guard the `Class`/`MethodHandle`/`MethodType` constant case (reflection branch in `ValueFactory`).
- **[C4] Finish the delegation path** (`visitGETVALUE_Object` ~:1374): key by the delegated object reference too. Note (pre-existing, not G1's to fix): delegation detection uses mutable flags on the shared static processor singleton.
- **[C5] `address`-as-`identityHashCode` collision is NOT debug-only.** `ObjectsInvocation.invokeEquals` decides the `referenceSemanticChange` flag via `getAddress() != getAddress()` — a real soundness signal keyed on the 31-bit hash, so collisions can under-flag it. G1 leaves this (de-interning is G3); record it as a known residual to retire when `address` becomes a unique id.
- **Nits:** `equals()` NPEs when `fields==null` (latent, no callers — add a guard if touched); `getConcrete()` returns `address` for `ObjectValue` (no identity-comparison callers — inert); `visitIF_ACMPEQ` records the branch twice (pre-existing — flag to test authors so oracle counts aren't confused).

Pushed back (reviewer error to confirm):

- **[C3] Weak-key eviction does NOT cause live-object precision loss.** A `weakKeys()` entry is cleared only when the key (the concrete object) is **unreachable from GC roots** — i.e. dead. While the program holds the object anywhere (reachable), the weak key is retained regardless of whether the shadow side can "see" it; GC reachability is a property of the program's strong refs, not the shadow's visibility. So there is **no** "live-but-evicted" hazard: eviction happens exactly when the object can never re-enter again (which is precisely O-6's desired behavior). Therefore weak `IdentityHashMap`/`MapMaker().weakKeys()` is **sound and precise for live objects AND fixes O-6** — no collision/precision trade, contrary to the round-1 note. (Injected-id/JVMTI remain future options, but are not needed to avoid an eviction problem that doesn't exist.) Reviewer to confirm this reasoning.

Confirmed by review (no change): G1 correctly does NOT fix V-1 (a `this`-returned genuinely-same object recovering the receiver's shadow is correct *identity* behavior; G2's value-type policy is what stops it). Reference-keying does not disturb V-1/V-2/F-1/F-2 or the de-interned-string formula-equality path. One heap per thread (per-visitor `ShadowContext`), so the registry needs no extra locking.
