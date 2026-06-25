# G2 — Value-type boundary recovery (collapse v1): Problem & Proposed Solution

Phase 2 of the heap redesign. **Status: IMPLEMENTED on `fix/heap-design`** (converged after 1 design-review round + 3 independent post-impl reviews for style/refactoring/correctness). V-1 green at L1 **and** L2 (real agent); V-2/F-1/F-2/V-5/V-6/V-9/O-4/O-5 stay green; 0 pending across heap+processor+agentTest. The design-review resolutions are at the bottom; B2 was settled by reading the explorer (VIOLATION is replay-witnessed, so no downgrade flag is needed). Builds on G1 (`67505c4`).

## Problem (recap, grounded)

After G1 the heap is keyed by the concrete object reference. That makes G1's object cases correct — but it does **not** fix the value-type aliasing bug (V-1), and *cannot*, because the aliasing is semantically faithful at the identity level:

`String.toLowerCase()` on an already-lowercase receiver returns `this`. So the unmodeled call's result *is the receiver object*. Flow today (post-G1):

1. `visitINVOKEVIRTUAL` (toLowerCase, unmodeled) → `InvocationHandler.invoke(...)` returns `PlaceHolder.instance` and records context loss (symbolic receiver) — `SymbolicInstructionVisitor.java:2319+`, `InvocationHandler.java:76-122`.
2. `visitINVOKEMETHOD_END` pushes that placeholder onto the caller stack (`:3406-3413`).
3. `visitGETVALUE_Object` sees the placeholder; `inst.val` is the concrete result = **the receiver object** (this-return); `getFromHeap(inst.val)` returns the **receiver's symbolic `StringValue`** → the result aliases the receiver's formula (`:1265-1294`).

So a value produced by an unmodeled method is re-bound to its receiver's symbolic value. The receiver is a real symbolic input; the result is a different logical value we have no model for — but identity-recovery makes them one. (`invokeToLowerCase` is a `PlaceHolder` stub, `StringValue.java:1125`.)

The right behavior (plan §G2, "collapse v1"): at a **mirrored invoke boundary of an unmodeled, value-returning method**, do **not** identity-recover for value types; produce a concretized (non-symbolic) result and keep the already-recorded context-loss flag.

## Proposed solution (collapse v1)

**Tag the unmodeled value-typed return, and concretize it at recovery instead of identity-recovering.**

1. **New placeholder origin** `PlaceHolder.ValueOrigin.UNMODELED_RETURN` (alongside UNSPECIFIED/DATABASE/GETFIELD/GETSTATIC).
2. **Tag at the single chokepoint — `InvocationHandler.invoke`.** After the existing missing-invocation / context-loss recording runs (which must still observe the plain `PlaceHolder.instance`), if the result is an unmodeled placeholder **and** the method's return type is a value type (`Type.getReturnType(desc)` ∈ `Util.deInternedClasses` = String + 6 boxed wrappers), return `new PlaceHolder(ValueOrigin.UNMODELED_RETURN, …)` instead of `PlaceHolder.instance`. All 5 invoke sites route through here, so this is the only tag site.
3. **Concretize at recovery — `visitGETVALUE_Object`.** When the placeholder's origin is `UNMODELED_RETURN`: skip `getFromHeap` entirely; build a concretized value from `inst.val` via `ValueFactory.createObjectValue(inst.val, inst.address)` (for a String this yields a `StringValue` whose formula is the **constant** `makeString(bytes)` — non-symbolic, so it carries no variables) and push it. Context loss is already flagged (step 2's path is unchanged).
4. **Do NOT overwrite the receiver's heap entry.** Leave `heap[receiverObject] = receiver` intact. This preserves **V-3** (a genuinely symbolic string stored and re-fetched *unchanged* still recovers its formula). The cost: see residual below.

Net: the result no longer carries the receiver's formula (V-1 fixed); it's concrete + context-loss-flagged; V-2/V-3/F-1/F-2 are untouched.

## Why this shape

- **Tag, not type-sniff-at-recovery.** `UNSPECIFIED` placeholders arise from several sources (generic pushes, etc.); only the unmodeled-invoke return should change behavior. A dedicated origin is surgical and mirrors how GETFIELD/GETSTATIC already tag their placeholders (`:1191-1193`, `:1222-1224`).
- **Tag after the context-loss logic** so `InvocationHandler`'s `retValue.equals(PlaceHolder.instance)` check (`:110`) still fires and records the flag; only the *returned* value is swapped.
- **Concretize = createObjectValue with a constant formula** — the existing factory path already produces a non-symbolic value; no new "concretize" primitive needed.

## Scope / non-goals

- **Value types only** (`deInternedClasses`). Unmodeled returns of other reference types keep G1 behavior (identity-recover or create fresh).
- **No UF** (G4b). A pure whitelisted method would later return `UF_m(inputs)` instead of concrete; deferred.
- **No φ-set / branching** (v2). Recovery yields a single concrete value, not a candidate set.
- **No output de-interning** (G3).

## Known residual (documented, deferred to G3)

Because we don't overwrite the heap, the *result* of a this-return is concretized on the stack but the shared object still maps to the receiver. If that result then round-trips through unmodeled memory and re-enters via `getFromHeap`, it re-aliases to the receiver — the V-1-via-round-trip variant. This is the fundamental overloaded-identity problem (result and receiver are one object); only **splitting the identity (G3 output de-interning)** closes it. Overwriting the heap here would close the result's round-trip but break V-3's (the receiver's round-trip), so v1 keeps V-3 and defers the result-round-trip to G3.

## Acceptance tests

Flip green: **V-1 (L1)** `ValueRecoverySpec` (recovered result's vars disjoint from receiver's; concrete correct; context loss flagged) and **V-1 (L2)** `HeapRecoveryV1AgentSpec` (branch on the real `toLowerCase` result does not reference the symbolic input). Remove their `@PendingFeature`.

Stay green: V-2 (new-object — already non-aliasing), F-1/F-2 (flag policy — `InvocationHandler` context-loss path unchanged), V-5/V-6/V-9, O-4/O-5, processor specs, L2 anchor.

## Risks / open questions for review

- **Tag placement vs the context-loss check.** Is returning a tagged placeholder *after* the recording correct, and does anything else compare the return against `PlaceHolder.instance` by identity (`==`) or `equals` and break when it's a tagged instance? (e.g. callers of `InvocationHandler.invoke`, `setReturnValue`, METHOD_END.)
- **Return-type detection.** Is `Type.getReturnType(desc).getClassName()` ∈ `deInternedClasses` the right test? `deInternedClasses` is a private `Set<String>` of `Class.getName()` — need a small accessor (`Util.isValueTypeName(String)` or similar). Boxed return descriptors (`Ljava/lang/Integer;` etc.) map correctly?
- **Concretize correctness.** Does `createObjectValue(inst.val, inst.address)` for a boxed wrapper (Integer/Long/…) also yield a non-symbolic value (no leaked variables)? Any path where `inst.val` is null here?
- **The "don't overwrite" choice** vs V-3: is preserving the receiver entry the right call, and is the result-round-trip residual genuinely G3's to close (not a v1 soundness hole that bites SV-COMP verdicts — note context loss already downgrades the verdict)?
- **Interaction with G1 registration.** After G1, the receiver was registered under its concrete object during its own GETVALUE. Confirm the UNMODELED_RETURN path doesn't need to (and doesn't) touch that entry.
- **Does any modeled method ever return `PlaceHolder.instance`** for a value-typed return (so it'd be wrongly tagged)? (Modeled String ops return real values; verify the stub set.)

## Review round 1 — resolutions

Accepted from review:

- **[B1 + C3, unified] Gate the *concretize* on the concrete `inst.val` type at recovery, not the declared return type at tag time.** Tag **all** unmodeled invoke returns as `UNMODELED_RETURN` (no return-type gating in `InvocationHandler`). In `visitGETVALUE_Object`, the `UNMODELED_RETURN` branch concretizes **iff `Util.isValueType(inst.val)`**, else falls back to the existing G1 recovery (`getFromHeap`/create). This closes both the **Float/Double gap** (`deInternedClasses` is String + 6 wrappers and *excludes* Float/Double — the "6 boxed wrappers" framing was wrong; Java has 8) and the **declared-vs-concrete mismatch** (a method declared `CharSequence`/`Object`/`Number` but concretely returning a String/box now still concretizes) in one move. Non-value-typed unmodeled object returns are unchanged from G1.
- **New `public Util.isValueType(Object)`** = `String || Number || Boolean || Character` (String + all 8 boxed wrappers); `null` ⇒ false. Independent of `deInternedClasses` (which stays as the de-intern/`==` concern and deliberately omits uncached Float/Double — don't widen it and change `==` semantics).
- **[#1 ordering] The tag swap goes strictly after `InvocationHandler.java:108`** (the `equals(PlaceHolder.instance)` context-loss check), with a code comment so a later refactor can't move it ahead and silently kill the flag. `PlaceHolder` has no `equals` override (Object identity), and `Frame.setRet`/the `symbolicInstance`/`i==0` checks don't disturb a tagged instance — verified by review.
- **[#5] Gate is `equals(PlaceHolder.instance)` specifically** (not "any PlaceHolder"): `PlaceHolder.symbolicInstance` (symbolic non-String receivers) and all modeled real results are never tagged. Review confirmed `placeholder ⇒ unmodeled` is safe across the String/boxed stub set.
- **[nits]** Guard `inst.val == null` in the concretize branch (→ fall back); add `UNMODELED_RETURN` to the enum (toString readability optional).

[B2] **downgrade-safety claim corrected.** I overstated it: context loss downgrades **SAFE→UNKNOWN only** (`SVCompDriver.py:304`); VIOLATION is downgraded only by `reference_semantic_change` (`:316`). The base V-1 case is fully sound. The residual (a concretized this-return result round-trips through unmodeled memory, re-enters, re-aliases, then drives a branch) is narrow and closed by G3 (output de-interning splits the identity). Open: in a concolic engine a reported VIOLATION should be a *concretely witnessed* real input replayed to the error — if so, the residual can only change *which inputs we try*, never manufacture a false VIOLATION; SAFE is the only verdict at risk and context loss already downgrades it. To verify during implementation: confirm SWAT's VIOLATION is replay-witnessed; if it is **not**, arm a downgrade (`reference_semantic_change` or equivalent) on the `UNMODELED_RETURN` concretize path.
