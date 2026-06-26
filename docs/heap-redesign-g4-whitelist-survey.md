# G4 purity whitelist — java.lang survey (audited backlog)

Audited classification of `java.lang` methods for the G4 generic-UF whitelist (`UFs/PureMethods.WHITELIST`).
The whitelist is **soundness-load-bearing** (G4 step-1 fix B skips context-loss for whitelisted calls, and
step-2's cross-run injection would let a wrong entry cause a false SAFE), so the bar is: **pure +
deterministic + side-effect-free + identical across JVM runs/platforms**. Surveyed by 5 parallel agents
(String / Integer+Long+Short+Byte / Float+Double / Boolean+Character / Math+StrictMath).

## What is active now (committed to PureMethods.WHITELIST)

Only **String-returning, UNMODELED** methods are active (v1 materializes String returns; whitelisting a
method SWAT already models is inert — the UF never fires). All instance methods on `String` (the proven
receiver shape):

```
java/lang/String/trim()Ljava/lang/String;            (G4 step 1)
java/lang/String/strip()Ljava/lang/String;           (G4 step 1)
java/lang/String/stripLeading()Ljava/lang/String;    no-arg, same shape as trim
java/lang/String/stripTrailing()Ljava/lang/String;   no-arg
java/lang/String/substring(I)Ljava/lang/String;      arg-taking (mixed-sort UF: String,int)
java/lang/String/substring(II)Ljava/lang/String;     arg-taking
java/lang/String/repeat(I)Ljava/lang/String;         arg-taking
java/lang/String/replace(CC)Ljava/lang/String;       arg-taking (String,char,char)
java/lang/String/indent(I)Ljava/lang/String;         arg-taking
```
Verified unmodeled (stubs returning `PlaceHolder.instance` in `StringValue`). Tested: no-arg shape by
`PureFunctionUFSpec` (trim) + L2 `TrimTarget`; arg-taking shape by L2 `SubstringTarget`.

## Backlog — verified-effective, NOT yet added (needs test-harness support)

These are sound + unmodeled + String-returning, but introduce shapes the current L1 fixture / test
harness doesn't yet drive (static invoke; the executor handles them, but we add nothing untested to a
soundness-load-bearing list):
- **Cross-class static String returns (UNMODELED — only `valueOf` is modeled in their Invocation handlers):**
  `java/lang/Float/toString(F)`, `Float/toHexString(F)`, `java/lang/Double/toString(D)`,
  `Double/toHexString(D)`, `java/lang/Character/toString(C)`, `Character/toString(I)`. Add once a
  static-invoke L1/L2 test exists. (FP `toString` is spec-deterministic; Character case mapping is
  locale-INDEPENDENT, unlike String.)

## Inert — do NOT add (already modeled → the UF never fires)

The boxed integral/boolean `toString`-family is modeled in `IntegerInvocation`/`LongInvocation`/
`ShortInvocation`/`ByteInvocation`/`BooleanInvocation` (cases: `toString`, `toHexString`,
`toBinaryString`, `toOctalString`, `toUnsignedString`, `valueOf`). Whitelisting them is inert.
(`String.concat(String)` single-arg is also modeled.) The SV-COMP autostub corpus exercises
`Integer.toHexString`/`toBinaryString` etc. — already handled by these models, no UF needed.

## Future tier — pure but NON-String return (activate when UF materialization extends beyond String)

Large vetted set (sound to model as UFs once non-String return categories are materialized):
- **Integer/Long/Short/Byte:** parse*/valueOf/decode (String→num, deterministic), bit ops (bitCount,
  highestOneBit, numberOfLeadingZeros, reverse, rotate*, reverseBytes), compare/compareUnsigned,
  divideUnsigned/remainderUnsigned, sum/max/min, toUnsignedInt/Long, static hashCode(prim).
- **Float/Double:** parseFloat/parseDouble, isNaN/isInfinite/isFinite, compare, sum/max/min, static
  hashCode, the CANONICALIZING bit ops (`floatToIntBits`/`doubleToLongBits`, `intBitsToFloat`/
  `longBitsToDouble`).
- **Boolean:** parseBoolean, compare, logicalAnd/Or/Xor, static hashCode.
- **Character:** the large predicate/conversion set (isDigit/isLetter/isWhitespace/getNumericValue/
  digit/forDigit/toUpperCase(char)/toLowerCase(char)/toTitleCase, codePoint helpers) — all
  locale-independent + deterministic.
- **Math (exactly-specified only):** sqrt, fma, ceil/floor/rint/round/IEEEremainder, toRadians/toDegrees,
  all integer arithmetic (*Exact, floorDiv/floorMod, abs, min/max), bit helpers (copySign, ulp, signum,
  getExponent, nextAfter/Up/Down, scalb).
- **StrictMath (ALL math, incl. transcendentals):** bit-for-bit reproducible by spec — sin/cos/tan/exp/
  log/pow/cbrt/hypot/sinh/... plus everything Math has.

Instance accessors on wrappers (`intValue()`, `doubleValue()`, instance `toString()`/`hashCode()`,
`compareTo(boxed)`) are pure-on-value but receiver-keyed — `ufName` keys on arg descriptors only, so two
distinct boxed receivers would collide to one UF term unless the receiver is incorporated. EXCLUDE until
receiver-keyed UFs exist; prefer the static equivalents (`hashCode(I)`, static `toString(I)`, `compare(II)`).

## EXCLUDE — soundness traps (never whitelist)

- **Locale-dependent:** `String.toLowerCase()`/`toUpperCase()` no-arg (default locale; Turkish-I). The
  `(Locale)` overloads are deterministic given the locale arg, but the Locale arg is a non-value-typed
  object so the UF wouldn't fire anyway.
- **Environment/property readers (look pure, are not):** `Integer.getInteger`, `Long.getLong`,
  `Boolean.getBoolean` — read `System.getProperty`.
- **Nondeterministic:** `Math.random()` AND `StrictMath.random()` (the "Strict" prefix does NOT rescue it).
- **Cross-JVM nondeterministic:** `Math` transcendentals (sin/cos/tan/exp/log/pow/atan2/sinh/...) — only
  within-ulp, not bit-identical across platforms; use the StrictMath twins. The raw-bit variants
  `Float.floatToRawIntBits`/`Double.doubleToRawLongBits` (NaN payload varies).
- **Identity / arg-mutating / arbitrary-code / fresh-object:** `String.intern()`; `String.getChars`,
  `Character.toChars(I[CI)` (mutate an arg); `String.format`/`valueOf(Object)`/`join(Iterable)`/
  `transform` (default locale or invoke arbitrary `toString`/`Function`); array/stream returners
  (`toCharArray`, `getBytes*`, `split*`, `chars/codePoints/lines`); `equals(Object)` (consumes runtime
  type + a foreign reference); constructors (`<init>` allocate identity); `describeConstable`/
  `resolveConstantDesc` (Optional/reflection). `String.replaceAll/replaceFirst` excluded for v1 ($-group/
  escape replacement semantics a plain UF won't capture — revisit if regex is modeled).

Per-class survey counts (INCLUDE/EXCLUDE) and full rationale are in the agents' transcripts; this file is
the actionable distillation.
