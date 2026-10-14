# Where SA pruning helps: results

Runs of 2026-10-14 on `niklas/dev`, cfg-extractor jar of 2026-09-26 (current with
cfg-extraction `cdc73cb`, whose later change was comments only). Tasks and scripts: see
[README.md](README.md).

## Configurations

`svcomp test run --mode parallel --no-witness --categories valid-assert.prp --benchmark-dir
swat-benchmarks --testcase-timeout-s 300`, 48 workers (32 for the soundness reruns) on 128 cores;
one run per configuration, one after the other:

| config | flags | run |
|---|---|---|
| SA | (default: sequential SA, graph used from round 0) | `run_20261014_162721` |
| no SA | `--no-sa` | `run_20261014_163338` |
| SA + rank | `--sa-rank-distance` | `run_20261014_164316` |
| final `sa-soundness` (incl. narrowing tasks) | SA / `--no-sa` | `run_20261014_172545` / `run_20261014_172627` |
| `real-tools` | SA / `--no-sa` | `run_20261014_171457` / `run_20261014_171958` |

The machine was loaded (load ~100), so one round (a fresh JVM) took 1.3-1.9 s instead of ~1.2 s;
both configurations ran under the same load. Rounds and verdicts are deterministic (the soundness
suite gave identical results twice); wall times are not.

## Summary

Over the 194 family tasks:

| | correct | timeouts | false SAFE | total wall time |
|---|---:|---:|---:|---:|
| no SA | 147 | 37 | 10 | >= 18571 s |
| SA | 174 | 10 | 10 | 7327 s |
| SA + rank | 175 | 9 | 10 | 6938 s |

(The false SAFEs here are `validation-gauntlet/exit` and `virtual-dispatch/array`, wrong in every
configuration: executor bugs E1 and E2 below.)

- **Pruning helps exactly when the dead side of a branch has a subtree to skip.** Then the pruned
  search is constant in n while the unpruned one grows linearly (dispatcher, error-handling,
  virtual-dispatch/local: 16 paths per handler) or exponentially (dead-callees: 3^n). At n = 8
  the SAFE dispatcher needs 117 rounds / 197 s without SA and 5 rounds / 17 s with it; from
  n = 16 on, no-SA times out.
- **It does not help when the dead side is a leaf** (gauntlet `return`/`throw`: the rejecting side
  is the one the first run takes anyway, so pruning saves 2 rounds of 36), **in event loops**
  (dispatcher/`loop`: a handler in round 0 can still reach the assert in round 1), **at ambiguous
  calls** (virtual-dispatch/`factory`: the graph is incomplete at the call, the walker gives up),
  and **when every path reaches the assert** (irrelevant-prefix control: 2^n in both
  configurations, SA only skips the dead sides of `target`'s own guards).
- **Cost:** extraction takes 4-10 s per task (median 5.5 s under load). With ~1.5 s per round SA
  pays off once it saves about four rounds; for tiny tasks it is a net loss of ~5 s.
- **Soundness: SA turns 9 soundness tasks into false SAFE** that are correct (or UNKNOWN) without
  it: static initializers on an input-dependent path, exceptions without an edge in the graph
  (ClassCastException, NullPointerException across frames, exceptions thrown inside the JDK), and
  reflection. Separately, the executor gives false SAFE without any pruning for `System.exit`,
  object-array reads at a symbolic index, and lambda bodies. Details and reproducers below.
- **`--sa-rank-distance` never hurt here** and fixed the VIOLATION cases with long paths
  (dispatcher/loop: 5 rounds for every n instead of 8-36 and a timeout at n = 32;
  irrelevant-prefix: 3 instead of 5-11). It changed no SAFE task and no verdict, apart from the
  n = 32 loop task now solved. This differs from SV-COMP (682 -> 635 points), where the shortest
  path to *some* assert point was often the wrong one; here every task has one assert.
- **Real tools (getopt, opencsv) could not show anything:** both are bound by Z3 String solving
  (single queries of 70-264 s) and by a String encoding bug (E4) that makes runs diverge; neither
  configuration got past round 5 in 300 s.

## Per family

![rounds](report/rounds.png)

![wall time](report/time.png)

SAFE tasks: their search is exhaustive, so they show the whole (pruned) search space. Hollow
markers: timed out, the value is a lower bound. All numbers, also for VIOLATION:
[report/families.md](report/families.md).

| family / variant | rounds no SA -> SA (SAFE) | effect |
|---|---|---|
| dispatcher/switch, ifchain, instance | n=2: 21 -> 5, n=8: 117 -> 5, n>=16: TO -> 5 | all handlers pruned; constant in n |
| dispatcher/loop | TO -> TO | none: event loop |
| validation-gauntlet/report | n=8: 36 -> 10, n=32: 132 -> 34 | the error reports are pruned; the checks still need one round each |
| validation-gauntlet/return, throw | n=32: 36 -> 34 | the dead sides are leaves; ~2 rounds |
| validation-gauntlet/exit | 1 -> 1, false SAFE in both | executor bug E1 |
| error-handling/* (explicit, deep, arith, bounds) | n=32: 36-TO -> 3 | catch paths pruned, also for implicit ArithmeticException/AIOOBE and two frames up |
| dead-callees/suffix, guarded | n=4: 85-TO -> 3; n>=5: TO -> 3 | 3^n dead paths pruned |
| dead-callees/shared | n=4: 87 -> 5; n>=5: TO -> 5 | the context-sensitive walk prunes the dead calls of a helper that is live elsewhere |
| virtual-dispatch/local | n=8: 117 -> 5, n>=16: TO -> 5 | like dispatcher: exact receivers resolve |
| virtual-dispatch/factory | n=8: 117 -> 117 | none: ambiguous call, incomplete graph (correct verdicts) |
| virtual-dispatch/array | 18 -> 16, false SAFE in both | executor bug E2 |
| irrelevant-prefix (control) | n=4: 64 -> 48, n=6: TO -> 192 | exponential in both, as expected |

VIOLATION tasks gain less, and how much depends on where DFS happens to look first: the switch
and instance dispatchers find the target after `n + 4` rounds without SA (the case comparisons
come before the handler bodies in DFS order), the if-chain only after exploring every handler
(same as SAFE). With SA all find it in 3-5 rounds.

## Soundness (`sa-soundness`)

VIOLATION / SAFE result per configuration, with rounds. "unk": UNKNOWN.

| task | VIOLATION no SA | VIOLATION SA | SAFE no SA | SAFE SA |
|---|---|---|---|---|
| anonymous-class | ok/3 | ok/3 | ok/5 | ok/3 |
| anonymous-static-field | ok/3 | ok/3 | ok/5 | ok/4 |
| arith-catch-generic, -local, -up1, -up2 | ok/3 | ok/3 | ok/4-7 | ok/3 |
| bounds-catch-up1, -up2 | ok/3 | ok/3 | unk (E6) | unk (E6) |
| **cce-catch** | ok/3 | **FALSE SAFE/1** | ok/3 | ok/1 |
| **cce-catch-up1** | ok/3 | **FALSE SAFE/1** | ok/3 | ok/1 |
| **clinit-static-call, -new, -field** | ok/3 | **FALSE SAFE/1** | ok/3 | ok/1 |
| **clinit-in-callee** | unk/3 (E7) | **FALSE SAFE/1** | ok/3 | ok/1 |
| clinit-unconditional (control) | ok/2 | ok/2 | ok/4 | ok/2 |
| default-method | ok/3 | ok/3 | ok/5 | ok/3 |
| enum-switch, string-switch | ok/4 | ok/4 | ok/4 | ok/4 |
| explicit-throw-generic-catch (control) | ok/3 | ok/3 | ok/4 | ok/3 |
| finally-assert, finally-exits | ok/3, ok/6 | ok/3, ok/6 | ok/4, ok/7 | ok/4, ok/7 |
| inner-accessor | ok/3 | ok/3 | ok/6 | ok/3 |
| jdk-getmethods, jdk-toarray | ok/3 | ok/3 | ok/3 | ok/3 |
| **library-throw-catch** | ok/3 | **FALSE SAFE/1** | ok/3 | ok/1 |
| **npe-catch-up1** | unk/2 (E5) | **FALSE SAFE/1** | unk/2 | ok/1 |
| npe-catch-local | unk/2 (E5) | unk/2 | unk/2 | unk/2 |
| recursion-depth, recursion-mutual | ok/7, ok/5 | ok/7, ok/5 | ok/11, ok/7 | ok/11, ok/7 |
| ref-eq, ref-ne-chain | ok/3, ok/5 | ok/3, ok/5 | ok/3, ok/5 | ok/3, ok/5 |
| **reflection-noarg** | ok/3 | **FALSE SAFE/1** | ok/5 | ok/1 |
| reflection-invoke, reflection-desync | FALSE SAFE (E3) | FALSE SAFE/1 | ok | ok/1 |
| lambda-predicate, -consumer, -local-noarg | FALSE SAFE (E3) | FALSE SAFE (E3) | ok | ok |
| lambda-static-field, -static-field-noarg | FALSE SAFE (E3) | FALSE SAFE/1 | ok | ok/1 |
| stream-foreach, string-switch-nondet | unk (context loss) | unk | unk | unk |
| synchronized-block, synchronized-throw | ok/4, ok/3 | ok/4, ok/3 | ok/7, ok/5 | ok/5, ok/4 |
| exec-system-exit, -late | FALSE SAFE/1 (E1) | FALSE SAFE/1 | ok/1 | ok/1 |
| exec-object-array-index | FALSE SAFE/3 (E2) | FALSE SAFE/1 | ok/3 | ok/1 |
| exec-int-array-index | unk (E6) | unk | unk | unk |
| exec-string-char | unk (E4) | unk | unk | unk |

Constructs that are handled correctly: implicit ArithmeticException / AIOOBE caught one or two
frames up and as `RuntimeException`, explicit throws caught as a supertype, arrays from JDK calls
(the phantom bounds guard regression), String and enum switches, try/finally duplication,
`refEquals`, recursion at an input-dependent depth, default methods, anonymous and inner classes
(synthetic accessors), `synchronized` (including an exception through the monitor-exit handler),
and `<clinit>` that runs on every path.

## Bugs found

Each has a minimal reproducing task in `java/sa-soundness/` (VIOLATION variant `_false`). None is
fixed here: the SA ones need extractor changes beyond a local fix, the executor ones an executor
rebuild and SV-COMP re-validation.

### SA (extractor / walk): false SAFE only with pruning

- **S1. Static initializer reached on an input-dependent path.** `<clinit>` is not in the graph,
  so an assert inside it is invisible; the graph has no assertion point at all and DFS prunes the
  branch that leads to the first use of the class (static call, `new`, static field read, or
  inside a branch-free callee). Reproducers: `clinit-static-call`, `clinit-new`, `clinit-field`,
  `clinit-in-callee`; control `clinit-unconditional` (first use on every path) is correct.
  Fix direction: treat the first use of an application class whose `<clinit>` can reach an assert
  (or is not analysed) as an assertion point or an incomplete node, or add implicit call edges.
- **S2. Exceptions the graph has no edge for, caught by the application.** The handler is
  unreachable in the graph, so the branch that makes the exception happen is pruned:
  ClassCastException from a `checkcast` (even in the same method: `cce-catch`; one frame up:
  `cce-catch-up1`), NullPointerException one frame up (`npe-catch-up1`; locally there is an
  EXCEPTION edge), and any exception thrown inside the JDK (`library-throw-catch`:
  `Integer.parseInt` -> NumberFormatException, the usual way CLI tools handle bad arguments).
  This is limitation 2 of `INTER_PROCEDURAL.md`, but its consequence is a false SAFE, not only
  lost precision. Fix direction: for each handler that can catch such types (including
  `RuntimeException`, `Exception`, `Throwable`), mark the statements in its try region that can
  throw them (casts, dereferences, library calls) incomplete, or add EXCEPTION/escape edges.
- **S3. Reflection.** `Method.invoke` is a library call, so it gets no CALL edge and is not marked
  incomplete ("only library targets"); the invoked method's assert is unreachable in the graph
  (`reflection-noarg`). Fix direction: mark `Method.invoke`, `Constructor.newInstance`,
  `Class.newInstance` (and `MethodHandle.invoke*`) incomplete.
- **S4 (latent). Lambdas stored in fields.** An interface call whose receiver comes from a field
  falls back to the application subtypes of the interface; lambda classes do not exist statically,
  so a `Runnable`/`IntConsumer` field holding a lambda resolves to "only library targets": no
  edge, not incomplete, no assertion point (`lambda-static-field`, `lambda-static-field-noarg`).
  Currently hidden because the executor misses these violations anyway (E3); it becomes a false
  SAFE once E3 is fixed. Fix direction: treat interface calls on functional interfaces as
  incomplete when the program has lambda bodies.

A cheap stop-gap in SWAT for S1, S3 and S4: do not use a graph that has no assertion point
(`metadata.assertionPointIds` empty) while the program has an assert; all of those reproducers
have such a graph. It does not cover S2.

### Executor: false SAFE or lost results, also without pruning

- **E1. `System.exit` loses the trace.** A run that ends in `System.exit` reports no branches, so
  exploration stops with SAFE after the first round (`exec-system-exit`, also when branches were
  recorded before the exit: `exec-system-exit-late`; `validation-gauntlet/exit`).
- **E2. Object array read at a symbolic index** is not a choice point and is not flagged as
  precision loss, so other elements are never tried: false SAFE (`exec-object-array-index`,
  `virtual-dispatch/array`).
- **E3. Lambda bodies record no symbolic branches,** neither for arguments passed through the
  JDK's lambda forms nor for static fields read inside a no-argument lambda, and this is not
  flagged as context loss: false SAFE (`lambda-*`). The same for arguments passed through
  `Method.invoke` (`reflection-invoke`, `reflection-desync`; `reflection-noarg`, which reads a
  static field instead, works).
- **E4. String inputs have no character range bound.** `StringValue.getBounds` returns `true`,
  and `charAt` is the code point truncated to 16 bits, so Z3 may answer `charAt(0) == '-'` with
  U+1002D; Java then sees a different string and the run diverges ("Target did not explore branch
  ... as expected"); the result is lost (`exec-string-char` gives UNKNOWN for VIOLATION). This
  blocked both real tools. Fix direction: bound input strings to `\u0000-￿` (e.g.
  `str.in_re` with a range) or encode `charAt` without truncation.
- **E5. NullPointerException on `getfield`** halts the executor ("Trying to get a field of NULL"):
  UNKNOWN (`npe-catch-local`). With SA, `npe-catch-up1` turns this UNKNOWN into a false SAFE (S2).
- **E6. "Error retrieving value for SV-Comp (at instruction 4)"** after solving for an int that is
  used as an array index: UNKNOWN (`exec-int-array-index`, `bounds-catch-*_true`).
- **E7. "Invocation stack is broken"** when a `<clinit>` runs inside a callee: UNKNOWN
  (`clinit-in-callee_false` without SA).

### Harness (fixed)

- `svcomp test run --benchmark-dir` crashed for folder names other than `sv-benchmarks`
  (`939933e`).

## Real tools (step 4)

| task | no SA | SA |
|---|---|---|
| `getopt-minitar` (GNU java-getopt 1.0.13, 3 nondet argv strings) | timeout after 4 rounds | timeout after 5 rounds |
| `opencsv-import` (opencsv 2.3 `CSVParser`, 1 nondet line) | timeout after 4 rounds | timeout after 4 rounds |

Both are plain Java and fully analysable (no incomplete nodes, the walk never gave up), and SA
did prune (81 of 324 and 26 of 290 DFS visits had a dead side). But exploration is not the
bottleneck: single Z3 String queries took 264 s (getopt) and 70 s (opencsv), and both runs then
diverged on supplementary code points (E4). Two structural notes for later attempts:

- getopt is driven by a loop in the caller, and the opencsv parser loops over the characters, so
  most branches can reach a post-parse assert in a later iteration: the event-loop shape, with
  pruning limited to the early exits (help, version, usage errors, rejected records).
- Picking argv tokens from a fixed list with `nondetInt` instead avoids String solving, but then
  the tool's own branches are concrete and the only symbolic branches are the token choices,
  which all come before parsing and can all reach the assert: nothing to prune.

Tools in `org/apache`, `com/google` or `com/fasterxml` (commons-cli, commons-csv, Gson, Jackson)
are excluded from instrumentation by `sv-comp.cfg`, and JSON parsers mostly read through a
`java.io.Reader`, which loses the symbolic chars; so they were not tried.

## Recommendations

1. Do not trust SA SAFE verdicts on code with static initializers, reflection, or catch handlers
   for unguarded or JDK exceptions until S1-S3 are fixed; the zero-assertion-point stop-gap covers
   S1 and S3 cheaply. Add the `sa-soundness` suite to the regression runs: it takes about a minute
   per configuration.
2. Pruning pays off for programs whose dead branches lead into substantial code: dispatchers and
   command handlers, error handling and reporting, helpers called only on rejected paths. That is
   the shape of security-sink reachability or crash reproduction at a single site in a
   command-style program. Expect nothing for event loops, all-paths-reach-the-assert programs and
   ambiguous dispatch.
3. Keep `--sa-rank-distance` in mind for single-target tasks: here it was never worse and it found
   deep violations in constant rounds. It is still a poor default for programs with many asserts
   (SV-COMP).
4. Before trying real parsers again, fix E4, and consider `--solver-rlimit` so that slow String
   queries do not stall the whole search.
