# SWAT with static pre-analysis on the applications (HTTP mode)

Status of the applications under `targets/applications/`, what it took to run them with SWAT's
HTTP driver with and without the static pre-analysis (SA), and where SA helps. Work of 2026-10-24/25
on branch `niklas/applications-sa` (both repositories).

## How to run

```bash
cd targets/applications
./http_sa_experiment.py --jar <boot jar> --cfg <swat.cfg> --template 'GET|/path?x=<int_3>' \
    --sa-entry pkg.Controller#method --label name          # one endpoint, with and without SA
./http_sa_campaign.py --search-order coverage --time-limit 180   # the cases below
```

- Each run starts the instrumented application afresh (ports 18080/18078), runs
  `SymbolicExplorer.py -m http`, and keeps `app.log`, `explorer.log` and `http_stats.json` in
  `http-sa-runs/<label>_<sa|nosa>/` (gitignored). The campaign writes `campaign.json`/`.md` there.
- `<type_N>` placeholders must use the executor's uid of the parameter (`symbolic uid N` in the
  application's log; request-body DTO fields get theirs when the class is lifted, e.g. `csvPayload`
  is `String_34`). A body template that is a JSON string (`"<string_0>"`) is sent as the raw body.
- SA: `--sa-path ../cfg-extraction --sa-entry pkg.Class#method -cp <classes>`. The runner builds the
  graph from `BOOT-INF/classes` plus the library jars the executor instruments
  (`includePackages`); with all of Spring's jars the extractor takes more than 5 minutes.
- Inserted bugs are compile-time switches (`-Pbug=<name>`, see the applications' `build.gradle`):
  SA treats every assert reachable from the endpoint as a target, so only the bug under test may be
  in the bytecode.

## Which applications run

| application | status |
|---|---|
| swat-artificial/target-1 | runs. 16 nested-if endpoints; bug inserted in `/e1/depth/5` |
| swat-artificial/personal-finance-engine-v2 | runs (after the executor fixes below). 4 switchable bugs |
| optaplanner-spring | runs. With optaplanner instrumented (its `swat.cfg`) SA is useless: 3 min extraction, 66k nodes, ~7000 target points (optaplanner's own asserts, 327 callbacks reaching them). `swat-app-only.cfg` instruments the application only. 2 switchable bugs |
| oss-fuzz/gson | runs, but explores nothing: the input reaches gson only through `StringReader.read(char[],int,int)`, which SWAT does not model, so all branches are concrete |
| oss-fuzz/spatial4j | runs, but stalls after 3 requests (most flips UNSAT, some 60 s solver timeouts); spatial4j has its own asserts (608 target points), and its endpoint catches `AssertionError` |
| oss-fuzz/java-diff-utils | starts; input is a DTO, not tried further |
| swat-artificial/fleet-route | starts; logic is trivial and DTO/lambda-driven, not used |
| spring-1, restler-test-app, spring-in-memory-db, simple-list-endpoint, modelmapper-test-app, reflection-test-app | start under the agent; small feature tests, not used for SA |
| swat-artificial/finance-advice | does not start: needs an H2 TCP server |
| xss-demo | does not start: needs MySQL (docker-compose) |
| openhospital, petclinic-test, genomenexus, openolat | need Docker, which this user cannot access |
| emb-artificial | no sources |

## What had to be fixed

SWAT (`symbolic-explorer`, `symbolic-executor`):

- **HTTPDriver had no SA.** It now runs the extractor for the endpoint (`--sa-entry`, the method
  the executor starts tracing in) and prunes with the graph as in sv-comp mode; SAFE is downgraded
  on an SA desync or context loss. Violations come from the trace's witness data instead of the
  response text; a server error no longer ends the exploration; `--time-limit`,
  `--stop-on-violation`, `http_stats.json`.
- **Witness data was never reset** between requests of a long-running target.
- **Assertions caught by the target** (spatial4j) were not seen: `AssertionThrowTransformer`
  records an `AssertionError` where it is thrown, as in SV-COMP mode.
- **Null optional parameters** (`@RequestParam(required = false)`, headers) crashed every request:
  the fresh copy of String/boxed parameters threw an NPE. Now `FreshCopy.of*`, uninstrumented so
  that no branch is added that the graph does not have.
- **Static initializers** run in the first request only, so later traces did not fit the tree and
  the search stalled (`tree_mismatch`); an inserted `assert` alone causes this (`$assertionsDisabled`).
  HTTP mode now strips `<clinit>` segments from traces, carrying their stack deltas over.
- **Inlined list lifting** (List parameters) added branches the graph does not have: SA desync.
  They are now reported like SWAT helper branches.
- **A diverged String** in the shadow state halted the application; now context loss.
- **Lone UTF-16 surrogates** in solved strings cannot be sent; such a solution is re-solved with
  sendable strings, and a failing driver no longer leaves a hanging explorer.

cfg-extraction:

- SootUp crashed on a `throw` of a local typed as a primitive or as a class not on the classpath
  (gson, spatial4j).
- **Every `invokedynamic` was incomplete**, so every string concatenation (Java 9+) was a target
  point and nothing could be pruned. String concatenation and lambda creation are now library leaves.
- **Calls on library types missing from the classpath** were incomplete; they are now leaves unless
  an application class overrides the method. A cast to an application type now bounds the receiver.
- Regression sweep over sv-benchmarks: all graphs load, node counts unchanged, incomplete markers
  82 -> 35 (the sweep compiles with a current javac; SWAT's precompiled SV-COMP classes contain no
  `invokedynamic` at all, so SV-COMP graphs are unaffected). `./gradlew test` passes.
  `SWAT/cfg-extraction.ref` pins `v0.2.0`, which has these fixes.

## Results

180 s per run (300 s for the repeated optaplanner runs), stop at the first violation. SA times
include the extractor (about 4 s). Verdict UNKNOWN for bug-free endpoints is a timeout without SA,
and context loss (unmodelled `Map.get`, `String.toLowerCase`, `LocalDate.parse`) with SA.

| case | bug | order | no SA | with SA |
|---|---|---|---|---|
| pfe-import-nobug | none | dfs | 101 requests, timeout | **1 request, 8 s, search exhausted** |
| pfe-import-nobug | none | coverage | 642 requests, timeout | **1 request, 5 s** |
| pfe-filter-nobug | none | dfs | 36 requests, 7.6 s | **1 request, 4.4 s** |
| opta-nobug | none | coverage | 9 requests, timeout (467 solver calls) | **SAFE, 1 request, 4.4 s** |
| opta-generator | generator loop | coverage | **not found in 180 s / 300 s / 300 s** (473-610 solver calls, mostly UNSAT) | **found in 32 s / 32 s / 32 s** (33 solver calls) |
| opta-generator | generator loop | dfs | found, 139 s | found, 143 s |
| opta-validity | input validation | dfs | found, 0.6 s | found, 4.3 s |
| t1-d5 | nested ifs | dfs, coverage | found, 1-2 s (8 requests) | found, 5-8 s (7 requests) |
| pfe-csv-quote | CSV parser | dfs | found, 3 s | found, 9 s |
| pfe-filter | transaction filter | dfs / coverage | found, 4 s | found, 8 s |
| pfe-header | header detection | dfs | not found | not found |
| pfe-header | header detection | coverage | found, 36 s | found, 37 s |
| pfe-merge | merge strategy | dfs, coverage | not found | not found |

No SA desyncs in the final runs, and no case where SA lost a violation that the run without SA
found.

## Findings

- **SA shows absence fast.** On a bug-free endpoint the graph has no target, the first trace is
  pruned entirely and the search ends after one request, where without SA it runs into the time
  limit. The optaplanner endpoint is proven SAFE that way; the personal-finance ones end UNKNOWN
  because of context loss in the executor's models.
- **For finding bugs it helps only where many branches lie past the bug.** With `dfs` the explorer
  flips the earliest open branch first, so a bug ahead of a loop is found quickly anyway, and a bug
  behind a loop that can still reach it (the CSV parser before header detection and merge) starves
  both runs: SA cannot prune that loop. With `coverage` the explorer also tries branches past the
  bug; in optaplanner's data generator these are mostly implicit array and division guards whose
  flips are UNSAT, and SA prunes them (143 s of solver time down to 0.3 s): found in 32 s instead of
  not at all.
- **SA costs about 4 s per endpoint** for the extractor, which dominates when the bug is shallow.
- **Libraries decide whether SA is usable.** Instrumented libraries with their own asserts (gson,
  spatial4j, optaplanner) make most call sites targets via the callback rule, and are slow to
  extract. Analysing the application alone works now (missing library types are leaves), at the
  price that asserts inside the libraries are not targets.
- **Not pruned: branches in callbacks.** Application code that a library calls back (constraint
  lambdas during the optaplanner solve) is masked in the walk and never pruned, even where the
  walk's position shows no target can be reached. Pruning them when the held position is not
  interesting looks sound under the existing callback rule, but changes the core walk; not done.
