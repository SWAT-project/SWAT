# Heap & value tracking in the symbolic executor

This is the reference for how the SWAT symbolic executor (the Java agent under
`symbolic-executor/`) tracks the values and objects a program manipulates. Read it before
changing anything under `symbolic/shadow/`, `symbolic/value/`, `symbolic/invoke/`, `symbolic/UFs/`,
or the `==`/de-interning instrumentation. It describes the system as it is today; it is not a
changelog.

All paths below are relative to
`symbolic-executor/src/main/java/de/uzl/its/swat/` unless noted. Line numbers drift — the
**symbol names** (classes, methods, fields) are the stable anchors; grep for them.

---

## Mental model (read this first)

SWAT runs the real program on the real JVM and, in parallel, maintains a **shadow** interpretation
of it. Every value the program computes has a **shadow value**: the concrete runtime value it
actually holds, plus an optional **symbolic formula** (an SMT term) describing how it depends on the
designated symbolic inputs. Branches on symbolic values are recorded as path constraints; the
explorer later solves them to find new inputs or to prove a property.

The hard part is **objects and library calls**. The JVM shares object identities (interned strings,
cached boxed integers, methods that return `this`), and most library methods are not modeled by
SWAT. The machinery here exists to keep the shadow world faithful across those boundaries:

- a **registry** so each concrete object has exactly one shadow;
- **boundary recovery** so results of unmodeled calls re-enter the shadow world without corrupting
  it;
- **de-intern + provenance** so reference equality (`==`) stays correct once we stop relying on JVM
  interning;
- a **pure-function model** so we keep precision through side-effect-free library calls instead of
  throwing it away;
- **soundness flags** so that whenever the shadow model is knowingly incomplete, a "safe" verdict is
  downgraded rather than trusted.

---

## Vocabulary

- **Shadow value** — a tracked value: a *concrete* component (the observed runtime value) + an
  optional *symbolic formula*. If the formula has no free variables/uninterpreted functions the
  value is effectively concrete.
- **Symbolic input** — a value the user marked with `@Symbolic`; the free variables everything else
  is expressed in terms of.
- **Value type** — a String or a boxed primitive wrapper — an immutable object with value semantics.
  Contrast with a general mutable object.
- **Unmodeled method** — a library method SWAT does not simulate symbolically; it runs for real and
  its result must be *recovered* into the shadow world.
- **Recovery / boundary** — reconciling the shadow stack with reality at a synchronization point
  after an unmodeled call or a field/array read.
- **Uninterpreted function (UF)** — an SMT function symbol with no defining axioms except that equal
  inputs give equal outputs. Used to model pure library calls.

---

## Components

### 1. The canonical shadow registry — one shadow per object

Each tracked concrete object maps to exactly one shadow value. The map is keyed by the **object
itself, compared by reference identity (`==`)** — not by `System.identityHashCode`. Reference
keying is collision-free (two distinct objects stay distinct keys even if their identity hashes
collide) and yields one shadow per object. Keys are held **weakly**, so a plain object's or boxed
wrapper's shadow is evicted once the object is unreachable.

- `symbolic/shadow/JVMHeap.java` — the map: `new MapMaker().weakKeys().makeMap()` in the
  constructor; `put`/`get` (null-guarded); `size`.
- `symbolic/shadow/ShadowContext.java` — per-thread owner; delegators `putToHeap` / `getFromHeap` /
  `heapSize`. One `JVMHeap` is created per `ShadowContext`.
- Read/write sites: `SymbolicInstructionVisitor.visitGETVALUE_Object` (and the primitive mirror).

**String self-pinning caveat.** A `StringValue` stores its own concrete `String`, which is also its
key, so the weak key stays strongly reachable — String-keyed entries do **not** evict until the
whole `ShadowContext` is discarded. To bound this, recovery deliberately does not register
*constant* strings/boxed values in the heap (see §3).

**Invariants**
- At most one shadow value per distinct concrete object.
- No identity-hash collision or reuse can alias two shadows.
- Non-String, non-self-referential entries are GC-evictable.

### 2. What a shadow value is

A shadow value pairs a concrete component with an optional formula whose SMT sort matches the Java
type exactly.

- `symbolic/value/Value.java` — base class; fields `formula` and `concrete`; `isSymbolic()`
  overridden per subtype.
- `symbolic/value/ValueFactory.java` — construction. `createNumericalValue` has a concrete-only
  overload and one that carries an explicit `formula`; `createObjectValue` maps a concrete object to
  the right subtype.
- Sort mapping:
  - `int/long/short/byte/char` → bitvector of width **32/64/16/8/16**
    (`value/primitive/numeric/integral/`).
  - `boolean` → SMT boolean (`BooleanValue`, also in the `integral` package).
  - `float/double` → single/double-precision floating point
    (`value/primitive/numeric/floatingpoint/`).
  - `String` → SMT string (`value/reference/lang/StringValue.java`).
  - Boxed wrappers → `IntegerObjectValue` / `BooleanObjectValue` / … (`value/reference/lang/`).

**Invariant** — a value's formula sort always matches its Java type; `concrete` always holds the
last observed real value.

### 3. Boundary recovery of unmodeled returns

When instrumented code calls a method SWAT does not model, the call runs for real and the executor
pushes a **placeholder** on its shadow stack. The real result is reconciled at the next `GETVALUE`
synchronization point, where the executor learns the concrete value the JVM produced.

The subtle case is a **value-typed return (String or boxed primitive)**. Naively identity-recovering
it is unsound when the method returns `this` (e.g. `String.toLowerCase()` on an already-lowercase
string): the result would be re-bound to the *receiver's* formula. So the rule is: recover the
registered shadow only when the returned object is a **distinct, already-tracked immutable value**
(different from the receiver's own shadow) — e.g. a String retrieved from a `Map`/`List` — otherwise
concretize. This keeps a container round-trip symbolic while still breaking the `this`-return
aliasing. Recovery is limited to immutable value types (their value cannot have drifted since it was
registered) and fires only for values already registered in the heap.

- `symbolic/value/PlaceHolder.java` — the placeholder; `enum ValueOrigin` (notably
  `UNMODELED_RETURN`) tags where a placeholder came from; shared singletons `instance` /
  `symbolicInstance`.
- `symbolic/invoke/InvocationHandler.java` — `invoke`: after the real call, if the result is a
  placeholder and the call is not on `IGNORED_INVOCATIONS`, records a missing invocation and
  re-wraps the result as an `UNMODELED_RETURN` placeholder.
- `SymbolicInstructionVisitor.visitGETVALUE_Object` — the `UNMODELED_RETURN && Util.isValueType(...)`
  branch: models a whitelisted pure result as a UF; else recovers the registered shadow when the
  return is a distinct immutable value (`Util.isImmutableValueType`, the receiver carried on the
  placeholder, a `getFromHeap` hit that is not the receiver's shadow, concrete matches); else
  concretizes via `ValueFactory.createObjectValue`. Non-value results fall through to normal registry
  recovery.
- `SymbolicInstructionVisitor.visitGETVALUE_primitive` — the primitive mirror.

**Invariants**
- An unmodeled value-typed return never aliases the receiver's formula (a `this`-return concretizes).
- A recovered value type is immutable, so its stored shadow still matches the observed value.
- Concrete recovery always adopts the JVM-observed value.

### 4. Out-of-band change detection

At a **primitive** `GETVALUE`, the executor compares the shadow value's `concrete` against what the
JVM actually produced. A mismatch means an *out-of-band change*: either a tracked object was mutated
inside unmodeled code, or the executor desynced internally. A configurable policy decides the
response.

- `symbolic/shadow/ShadowDivergence.java` — the policy enum, two values:
  - `CRASH` (default) — hard-fail via `SWATAssert`; catches executor desync bugs in dev/CI; this is
    the historical behavior.
  - `FLAG` — record a context-loss flag, adopt the observed concrete, and continue. Fully sound;
    recommended for production/SV-COMP runs (no spurious crashes).
- `config/Config.java` — field `shadowDivergence`, default `CRASH`, read from the `shadow.divergence`
  key.
- Decision site: the divergence branch in `SymbolicInstructionVisitor.visitGETVALUE_primitive`. Both
  policies re-adopt the observed concrete afterward.

**Invariants**
- Divergence detection currently covers only the **primitive** `GETVALUE` path.
- `FLAG` is sound: an adopted divergence downgrades a would-be safe verdict (via context loss, §7).

### 5. De-intern + provenance — correct reference equality

The reference-keyed registry (§1) needs distinct objects to have distinct identities. But the JVM
**shares** identities for interned strings, cached boxed wrappers, and `this`-returns, so two
logically-distinct shadow slots could collide on one shared object. To prevent that, value types
entering shadow space (string literals, boxed `valueOf`, and value-typed returns from
un-instrumented callees) are **de-interned**: replaced by a fresh copy with a unique identity.

De-interning would then break `==`, so each fresh copy records the genuine original
("canonical root") it came from, and `==` is resolved by comparing roots — reproducing real Java's
`==` without relying on JVM interning.

- `common/Provenance.java` — weak-keyed map from de-interned copy → canonical root; `record(copy,
  canonical)` (stores the fully-resolved root, keeping chains depth-1); `root(x)` returns the
  canonical or `x` itself.
- `common/UtilInstrumented.java` — `refEquals`: uses value equality
  (`Provenance.root(a) == Provenance.root(b)`) when **either** operand is a de-interned value type
  (the other operand's `root()` is itself), else plain `a == b`.
- `common/Util.java` — `shouldUseValueEquality`, `isDeInternedClass`, and the `deInternedClasses`
  set: **String + Boolean/Byte/Short/Character/Integer/Long**. Float and Double are **not**
  de-interned (they are uncached in real Java, so plain reference equality is already correct).
  `isValueType` is broader (String + all `Number` + Boolean + Character) and drives §3's recovery,
  not `==`.
- `instrument/nocache/NoCacheMethodAdapter.java` — the de-intern instrumentation: rewrites a String
  literal to `new String(...)` + a provenance record; rewrites `<Boxed>.valueOf(prim)` to
  `new <Boxed>(prim)` (`rewriteValueOf`); de-interns value-typed returns from un-instrumented
  callees (`deInternReturn`). The `Boxed` enum is the single source of truth for the six cached
  wrappers; `isDeInternSkippedOwner` excludes SWAT and sv-benchmarks intrinsics.
- `instrument/refequality/RefEqualityMethodAdapter.java` — rewrites `IF_ACMPEQ`/`IF_ACMPNE` bytecode
  to call `UtilInstrumented.refEquals`.

**Invariants**
- No two logically-distinct shadow slots share one interned/cached concrete key.
- `==` on de-interned value types matches real-Java `==`.
- Float/Double keep plain reference equality.

**User-constructed value types compare correctly.** A `new String("x")` (or `new Integer(...)`) is
never given a provenance root — only literals, boxed `valueOf`, and returns from un-instrumented code
are — so it roots to itself and compares unequal to any other instance, matching the real JVM (e.g.
`new String("x") == new String("x")` is `false`, and `new String("x") == "x"` is `false`).

### 6. Pure-function model — keeping precision through side-effect-free calls

Some unmodeled library methods are pure: deterministic and side-effect-free. Rather than concretize
their result (and lose the symbolic dependency), SWAT models each such call as an **axiom-free
uninterpreted function** `pure_<signature>(inputs)`. An axiom-free UF asserts only "equal inputs ⇒
equal outputs", which soundly over-approximates any deterministic function while preserving
relational facts (e.g. two calls on equal arguments return equal results). It fires only when at
least one input is symbolic.

**Whitelist membership is a soundness precondition**: only genuinely pure, deterministic methods may
be listed — otherwise the UF's "equal inputs ⇒ equal outputs" assumption is false.

- `symbolic/UFs/PureMethods.java` — `WHITELIST` (keyed `owner/name+descriptor`; String methods plus
  an audited set from `java.lang`/`java.util`: Math, StrictMath, Character, Integer, Byte, Float,
  Double, Objects). `isWhitelisted`; `ufName` builds `pure_<SimpleClass>_<method>[_<argTypes>]`. The
  `pure_` prefix is the recognizer used by the precision-loss check (§7).
- `symbolic/UFs/PureFunctionUF.java` — per-thread UF registry; `apply(ufName, returnType, args)`
  declares the UF lazily, caches it, and asserts the signature matches on reuse. Axiom-free by
  construction.
- `symbolic/UFs/UFHandler.java` — `getPureFunctionUF` (lazy per-thread accessor).
- `symbolic/invoke/InvocationHandler.java` — `buildPureUF` constructs the result UF; the gate is
  `containsSymbolicArgument && PureMethods.isWhitelisted(...)`. `pureUFReturnType` maps the return
  descriptor to an SMT sort (String; boolean; bitvectors; floating point; else fall back to
  concretize). A successful model also **suppresses the context-loss flag** (a modeled pure call
  loses nothing).
- Recovery: `visitGETVALUE_Object`/`visitGETVALUE_primitive` install the carried UF as the recovered
  value's formula.

**Invariants**
- A whitelisted pure call preserves "equal inputs ⇒ equal outputs" instead of concretizing.
- The UF's return sort matches the recovered value's sort.
- Modeling requires all inputs to be value-typed with formulas; otherwise it falls back to
  concretization.
- Only pure/deterministic methods may be whitelisted.

---

## Lifecycle of a tracked value

1. **Introduced** — a symbolic input (`@Symbolic` parameter) gets a free variable; a literal/boxed
   constant is de-interned (§5) and given a concrete-only shadow; a normal computation produces a
   shadow with a derived formula.
2. **Used** — arithmetic/logic combine formulas; a branch on a symbolic value records a path
   constraint.
3. **Crosses a boundary** — an unmodeled call returns a placeholder (§3); at the next `GETVALUE` the
   result is recovered as either a modeled UF (if whitelisted-pure, §6) or a concrete value
   (otherwise, flagging context loss).
4. **Reference-compared** — `==` is resolved via provenance roots (§5).
5. **Re-synchronized** — at each primitive `GETVALUE`, the shadow concrete is checked against reality
   (§4); a mismatch is crashed or flagged per policy.

---

## Soundness model

Two independent flags mark that the model may be incomplete; either one downgrades a **SAFE** verdict
to **UNKNOWN** (a VIOLATION is not downgraded — it is replay-witnessed).

- **Context loss** — set when SWAT hits an unmodeled method with symbolic input it cannot model at
  all (result discarded), or when an out-of-band divergence is adopted under `FLAG`.
  - Set via `symbolic/trace/SymbolicTraceHandler.java` `recordSymbolicContextLoss` →
    `SymbolicTrace.setSymbolicContextLoss`. Call sites: `InvocationHandler.invoke` and the `FLAG`
    divergence branch.
- **Precision loss** — set when a branch constraint contains a symbol that is neither a designated
  symbolic input, a recovery-named variable, nor a whitelisted `pure_` UF (i.e. a bespoke axiomatized
  UF or an ungrounded variable that could make the constraint unsound).
  - **Computed at trace-build time**, not recorded at runtime: `symbolic/trace/DTOBuilder.java`
    `isPrecisionLoss(...)` walks each branch formula; there is deliberately no runtime recorder.
- Both flags ride on `symbolic/trace/dto/TraceDTO.java` and are emitted by `DTOBuilder`.
- Explorer side: `symbolic-explorer/data/BinaryExecutionTree/Tree.py` (`record_context_loss` /
  `record_precision_loss`), populated by `data/Database.py` `add_trace`; the downgrade happens in
  `driver/SVCompDriver.py` (`SAFE` + either flag ⇒ `UNKNOWN`).

**Invariants**
- SAFE + (context loss OR precision loss) ⇒ UNKNOWN. Never a false SAFE from a knowingly-incomplete
  model.
- A whitelisted `pure_` UF does not trigger precision loss; a bespoke axiomatized UF does.

---

## Extending it & gotchas

- **Adding a pure method to the whitelist.** Add `owner/name+descriptor` to
  `PureMethods.WHITELIST` only after confirming the method is deterministic, side-effect-free, and
  reads no ambient state (locale, time, environment, statics). A wrong entry is unsound. Today a bad
  entry costs only precision (each run emits at most a single self-consistent fact); once cross-run
  accumulation lands (see below) a bad entry can cause a **false SAFE**, so the bar is higher than it
  looks.
- **Instrumenter frame-analysis fragility.** A category-2 parameter (a `double` or `long`) followed
  by a reference parameter, and large mixed-type method bodies, trip an unrelated frame-analysis bug
  in the instrumenter. Keep test targets small and single-typed; see the Javadocs in
  `src/test/resources/targets/StringWhitelistTarget.java` and `WhitelistAuditAgentTarget.java`.
- **`@Symbolic` goes on a method parameter, not a local.** Annotating a local crashes the annotation
  transformer when compiled without `-g`.
- **The legacy `de/uzl/its/value/**` test suite is broken** on an old formula API and fails en
  masse. Scope test runs to the `symbolic.shadow` / `symbolic.processor` / `common` packages (see
  the `swat-test` skill).

---

## Not yet implemented (planned)

Captured here so the plan survives; neither is in the code today.

- **Escape-aware divergence policy.** A third divergence mode (beyond `CRASH`/`FLAG`, §4) that tells
  a *legitimate* out-of-band mutation from a *genuine executor desync*. Intended mechanism: mark a
  tracked object as **escaped** when it is passed as receiver/argument into an unmodeled call
  (marking site in `InvocationHandler.invoke`); at a divergence, if the value came from an escaped
  object → flag + adopt + continue, else → crash. The escaped bit reaches the primitive-`GETVALUE`
  decision via a thread-local set in `visitGETFIELD` and **cleared at the start of every**
  `visitGETVALUE_primitive` (else it leaks and masks real desync). A v1 would cover only the direct
  field-read-of-escaped-object case (array/transitive reads still crash), so `FLAG` stays the
  fully-sound production hatch. A refinement: a *pure* whitelisted call cannot mutate, so it should
  not mark escape.

- **Cross-run accumulation of observed pure-function facts.** The executor already emits, per run,
  one **ground** observed pair `pure_<sig>(constant inputs) == constant output` for a whitelisted
  String-returning pure call (`InvocationHandler.buildPureUF` builds it; `visitGETVALUE_Object`
  asserts it). A single pair per run is sound on its own. What is missing is on the **explorer**: it
  does not yet accumulate these pairs across runs and inject them at solve time. The three required
  explorer changes **must land together**: (1) inject the accumulated per-testcase fact set at the
  solve chokepoint; (2) a contradiction guard so a bad/nondeterministic entry cannot inject two
  contradictory pairs; (3) an UNSAT backstop that downgrades SAFE→UNKNOWN if the accumulated `pure_`
  facts alone are unsatisfiable. Injection without (2)+(3) could turn a bad whitelist entry into a
  false SAFE.
