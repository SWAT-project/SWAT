---
name: jdk-source
description: Fetch the exact source of a JDK method/class from the active JDK's lib/src.zip (version-exact, no network), and audit a java.lang method for SWAT purity-whitelist eligibility. Use when deciding whether a JDK method may be added to PureMethods.WHITELIST (G4 pure-function UF modeling), or when you need to read what a JDK method actually does.
---

# JDK source + purity-whitelist audit

Two jobs: (1) cheaply read JDK source, (2) decide if a method may join `PureMethods.WHITELIST`
(`symbolic-executor/.../symbolic/UFs/PureMethods.java`). Background: `docs/heap-redesign-g4-whitelist-survey.md`.

## Fetching source — `scripts/jmethod.py`

Reads the active JDK's `lib/src.zip` (resolved from `$JAVA_HOME`, else `java -XshowSettings:properties`).
Version-exact, offline.

```bash
python3 .claude/skills/jdk-source/scripts/jmethod.py java.lang.Math                 # index: every method signature + line
python3 .claude/skills/jdk-source/scripts/jmethod.py java.lang.Math floorDiv         # full source of all overloads
python3 .claude/skills/jdk-source/scripts/jmethod.py java.lang.Math floorDiv --callees  # + the methods each overload calls
python3 .claude/skills/jdk-source/scripts/jmethod.py java.lang.Integer --field digits    # a field declaration
```

`<class-fqn>` is dotted (`java.lang.String`). Nested types live in their top-level file. Follow
`--callees` transitively (call the script again on each callee's class) to establish purity through
the call graph.

## Whitelist eligibility — the rubric

A method may be added to `WHITELIST` **iff ALL six hold**. The whitelist is a soundness precondition:
an unsound entry can make the engine model a non-deterministic/side-effecting call as a referentially
transparent UF, which can corrupt a verdict. When unsure, EXCLUDE.

1. **Pure / deterministic / side-effect-free.** Same inputs always give the same output; no observable
   effect. Read the source (transitively via `--callees`): reject any `PUTSTATIC`/`PUTFIELD` to shared
   mutable state, I/O, synchronization-for-effect, or argument mutation. Reading `static final`
   constant tables (e.g. `Integer.digits`) is fine.
2. **No locale / time / random / environment dependence.** Reject no-arg `String.toLowerCase()`/
   `toUpperCase()` (default locale), `String.format`, property/env readers, `Math.random`,
   `System.currentTimeMillis`, etc. NB: `Character.toLowerCase(char)` IS locale-independent (Unicode
   table) and allowed — unlike the `String` no-arg forms.
3. **No identity / interning semantics.** Reject `intern`, identity-hashing, or methods whose result
   identity matters. (Value equality of the result is fine; the UF models the value.)
4. **UNMODELED by SWAT.** If SWAT already models the method, the UF never fires (dead entry). Check the
   matching handler in `symbolic-executor/.../symbolic/invoke/java/lang/<Class>Invocation.java`: the
   method's name must NOT be in its dispatch (it must fall to `default -> PlaceHolder.instance`).
   `String` methods: check `StringInvocation` + the `StringValue` model methods.
5. **Supported return sort.** The return type must be `String` or a primitive (`boolean`, `byte`,
   `short`, `char`, `int`, `long`, `float`, `double`). Object/array/`void`/collection/stream returns
   are NOT supported (`buildPureUF` returns null → falls back to concretization). The UF return sort is
   set by `InvocationHandler#pureUFReturnType`.
6. **Value-typed inputs only.** Every parameter (and, for an instance method, the receiver) must be a
   String or boxed primitive — i.e. captured by a formula (`Util.isValueType`). Reject methods taking
   arrays, `CharSequence`, collections, `Object`, `Function`, etc. (their formula can't capture the
   input). Prefer the STATIC equivalent over a receiver-keyed instance accessor: `ufName` keys only on
   argument descriptors, so two distinct receivers of an instance method (`intValue()`, instance
   `toString()`/`hashCode()`) would collide to one UF term — EXCLUDE those until receiver-keyed UFs exist.

## Entry format

`WHITELIST` keys are `owner + "/" + name + descriptor`, e.g. `java/lang/Math/floorDiv(II)I`. The
descriptor disambiguates overloads. Verify it against the index (`jmethod.py <class>`).

## Verifying an addition

After adding, drive it through the real agent (an L2 `*AgentSpec` calling the method on a `@Symbolic`
input into a branch) and assert: a `pure_<sig>` UF appears in the branch constraints, and
`symbolicContextLoss == false`, `symbolicPrecisionLoss == false`. See `PureFunctionUFAgentSpec` (String)
and `PureFunctionPrimitiveAgentSpec` (primitive). The L0 `PureUFPrimitiveRecoverySpec` pins the
per-sort recovery construction. Use the `swat-test` skill for the harness details.
