---
name: svcomp-run
description: Run the SWAT SV-COMP benchmark harness — parallel scoring runs and single-testcase debug runs — and debug one testcase's verdict via single mode. Use when running/scoring sv-benchmarks locally, reproducing or debugging a single testcase, or interpreting points/verdicts/soundness downgrades. Covers setup, the ./svcomp CLI, config files, log locations, and the per-testcase wall-clock cap.
---

# Running & debugging the SV-COMP harness

The custom local harness is in `targets/sv-comp/scripts/`. Per testcase it compiles the target, runs
SWAT (Java agent + Python explorer over HTTP), scores the verdict, and optionally validates witnesses.
Drive everything through the `./svcomp` wrapper (it selects the venv Python) **from
`targets/sv-comp/scripts/`**. This is the *local/custom* runner; the real competition infra uses a
separate wrapper (`scripts/svcomp-package/run_swat.py`) — don't confuse the two.

## One-time setup

From the repo root, build the agent + native libs (rebuild the jar after ANY executor change — the
harness runs the jar, not the classes):
```bash
./gradlew copyNativeLibs                              # z3 -> libs/java-library-path
./gradlew :symbolic-executor:copyJar                  # -> symbolic-executor/lib/symbolic-executor.jar
./gradlew :targets:sv-comp:WitnessCreator:shadowJar   # only if validating violation witnesses
```
Then in `targets/sv-comp/scripts/`:
```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
./svcomp setup checkout-benchmarks   # SSH clone of SV-Benchmarks (sparse java/) -> ../sv-benchmarks
./svcomp setup checkout-validator    # wit4java, only for witness validation
```

## Run — parallel (scoring)
```bash
./svcomp test run --mode parallel --workers 50
./svcomp test run --categories valid-assert.prp --workers 30   # one property only
```
- Uses `../sv-comp.cfg` (quiet: WARN, no console). Prints per-category points and
  `TOTAL POINTS (ALL CATEGORIES): N`, and saves `results/results_<category>_<timestamp>.json`.
- `./svcomp analyze results` summarizes the latest results file (context losses / failures).
- `./svcomp test list [--stats]` enumerates testcases; `./svcomp test validate-ports` checks ports.

## Run — single (one testcase, debug)
```bash
./svcomp test run --mode single \
  --target "autostub/String_public_java_lang_String_java_lang_String_toLowerCase" [--no-witness]
```
- `--target` is the testcase identifier `<group>/<Name>` (matched by suffix against the testcase path).
- Single mode forces `../swat-debug.cfg` (INFO, console on, shadow-stack + symbolic-execution logging on).
- `--no-witness` skips witness gen/validation (avoids wit4java); fine when you only care about the verdict.

## Debugging a testcase (single mode)
1. Run it single-mode; watch the console. SWAT's own lines are prefixed `[SWAT] -->`.
2. Per-testcase logs land in `logs-debug/<group>/<Name>_<property>/` (`verdict.log`, symbolic-execution
   and shadow-stack logs). Parallel mode instead writes to `logs/`.
3. The exact forked command is logged, e.g.
   `java -Xmx32g -Dconfig.path=…/swat-debug.cfg -Dexplorer.port=<port> -javaagent:…/symbolic-executor.jar
   -Djava.library.path=… -cp <common>:<z3>:<testcase> -ea Main`. Copy it to run SWAT directly (attach a
   debugger, change flags, add `-Dsolver.mode=PRINT` to dump the TraceDTO without the explorer).
4. Markers to grep:
   - `[VERDICT <prop>] == TRUE | FALSE | DONT-KNOW` — explorer verdict (safe / violation / unknown).
   - `Context loss recorded!` / `Found symbolic context loss`, `Found ... precision loss` — soundness
     downgrades (a would-be SAFE becomes UNKNOWN).
   - `Invocation of method X in class Y ... cases context loss` (`InvocationHandler.java`) — an unmodeled
     method hit with symbolic input.
   - `Points: P, Case: <expected> -> <got>` — the scored outcome.

## Reading the result
`Case: <expected_verdict> -> <swat_verdict>`; expected comes from the testcase `.yml`
(`expected_verdict: true` = no violation → TRUE; `false` = violation reachable → FALSE):
- match → `Points: 1` (a violation also needs a validated witness for the point unless `--no-witness`);
- mismatch or unknown → `Points: 0`.
- A `… -> unknown` caused by context/precision loss is **sound** — SWAT declined rather than answered
  wrong; not a bug. Example: the autostub `…toLowerCase` testcase scores `violation -> unknown` because no-arg
  `toLowerCase` is locale-dependent and unmodeled, so its result is concretized + context-loss-flagged.

## Per-testcase wall-clock cap (outside the actual run)
Each testcase's SWAT process is wrapped in `lib/execution.py:run_command_with_timeout` — launched in its
own session (`start_new_session=True`); on `TimeoutExpired` it `os.killpg(…, SIGKILL)`s the whole tree
(JVM + Z3) and records `ExecutionStatus.TIMEOUT` → 0 points, never a wrong verdict. This is entirely
outside the run (SWAT is given no limit; the harness kills it). Default is **120s**; raise it here
for slow targets, or expose it as a `--timeout` option on `./svcomp test`. Do **not** edit
`scripts/svcomp-package/run_swat.py` — that is the separate wrapper the competition infra uses.

## Key files
- `svcomp.py` + `commands/{setup,test,analyze,util}.py` — the click CLI.
- `lib/execution.py` — `target_execution` (one task), `run_parallel`, `run_single_target`,
  `run_command_with_timeout` (the wall-clock cap), scoring + `save_results`.
- `lib/command_gen.py` — builds the per-testcase `java … -javaagent … Main` command.
- `lib/selection.py` — `extract_testcases` (parses the `.yml`s).
- Configs: `../sv-comp.cfg` (parallel), `../swat-debug.cfg` (single/debug) — differ only in logging.
- `../sv-benchmarks/java/<group>/<Name>/` — testcase: `Main.java` + `<Name>.yml` (holds `expected_verdict`).

## Gotchas
- Always run from `targets/sv-comp/scripts/` via `./svcomp` (not `svcomp.py` directly — the wrapper sets
  the venv Python). The older standalone `target_execution.py` (invoked by `run_locally.sh`) is a separate
  path with its own shorter timeout — prefer the `./svcomp test` CLI.
- Witness validation (wit4java) resolves `python3` via PATH and needs extra venv packages
  (`setuptools`, `pyyaml`, `javalang`, `networkx`); prefix with `PATH="$PWD/.venv/bin:$PATH"` or use
  `--no-witness`.
- `solver.mode=HTTP`: the explorer runs as a per-testcase HTTP server on an allocated port; parallel runs
  use many ports at once.
- Rebuild `:symbolic-executor:copyJar` after executor changes, or you'll score the old jar.
