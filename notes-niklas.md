> Originally in targets/sv-comp/runs/run_20260822_101238/ (SA-pre_first_try)

More cases were classified as safe
  (changed from safe -> unknown to safe -> safe)

However, there were also many false positives
  (changed from violation -> violation to violation -> safe)
  TODO: investigate why
  this should be fixable this by redoing DFS without SA if SA finds no branches
    it would be better if we can fix the underlying problem though

No new violations where found
  (changed from violation -> unknown to violation -> violation)
  This is bad news, since this was the main goal (stop being distracted by irrelevant branches, find more violations)
  Maybe this depends on the timeout
    Is the ./scripts/svcomp timeout the same as in the competition?
    Did the average solve time decrease? This would align with the goal.
  Maybe there are no cases of this kind (distraction by irrelevant branches)
    Can we test other cases? Whole applications?

How often did SA extraction fail?
  TODO
  But many timed out :(
    In `--mode parallel` the load is higher and extraction can take longer
    Timeout increased to 120s for now


First look at Case: jbmc-regression/ArrayIndexOutOfBoundsException1 changed from violation -> violation to violation -> safe
  SA Graph is broken, unconnected
    ![broken SA Graph](./logs/jbmc-regression/ArrayIndexOutOfBoundsException1_valid-assert/Main_main_interprocedural.png)
    why?
      maybe the try/catch is not handled correctly?
        seems to track with some other cases too
      created isolated test, see /home/niklas/SWAT/cfg-extraction/target/TryCatch.java
        the exceptional path is not handled. missing edge from `throw` to `catch`.
          if violation happens in catch block, we can't find it => false safe verdict.


SootUp has more thorough implicit exception handling than SWAT, e.g. every array access is a potential NullPointerException and ArrayIndexOutOfBoundsException.
SWAT does not actuallly "understand" implicit exceptions for solving, instead phantom branches are added before risky instructions,
e.g. an `if (op2 == 0)` before an IDIV. This turns implicit exceptions into explicit branches. But not all implicit exceptions are
handled this way by SWAT, some are just ignored.
To keep the SA-tree (SootUp) and trace-trees (SWAT) in sync, we might need to treat implicit exceptions differently, based on whether
they are handled by SWAT. For walking the SA-tree during DFS, we should only consider handled exceptions; but for finding interesting paths,
we might want to consider them all, to prevent false SAFE classifications.

Implicit exceptions handled by SWAT (in SymbolicInstructionVisitor):
- ArrayIndexOutOfBoundsException
  - AALOAD
  - AASTORE
  - BALOAD
  - BASTORE
  - CALOAD
  - CASTORE
  - DALOAD
  - DASTORE
  - FALOAD
  - FASTORE
  - IALOAD
  - IASTORE
  - LALOAD
  - LASTORE
  - SALOAD
  - SASTORE
- NegativeArraySizeException
  - NEWARRAY
  - ANEWARRAY
  - MULTIANEWARRAY
- ArithmeticException (divide by zero)
  - IDIV
  - IREM
  - LDIV
  - LREM



## 2nd Try (try-catch fixed?)
Points increased but still negative
- baseline:  601
- 1st try: -2315
- 2nd try:   -83

Way fewer false SAFE verdicts (from 90 down to 20)
- still 20 too many... investigate!
Also more timeouts ("crashes")

Case: securibench/Arrays1 changed from violation -> violation to violation -> unknown
- Also Arrays2, 3, 4, ...
- Somehow, the assert does not appear in the SA at all
  - "assertionPointIds": []
  - thus all, paths are seen as uninteresting
  - assert is hidden behind a virtualinvoke `writer.println()`
    - the virtualinvoke is not expanded in the cfg
    - "<java.io.PrintWriter: void println(java.lang.String)>" is blacklisted for some reason
      - cause: `if (sigStr.contains(blacklistPattern))` matches on "java.lang."
      - idea was to match methods of classes in java.lang, not parameters of type java.lang.String
    - maybe not even possible in the general case to follow all virtualinvokes
    - maybe treat unknown virtualinvokes as 'interesting' (possible assertion point)
- virtual dispatch support added to CFG extractor and new example target in SWAT (`my-example-3/src/VirtualDispatch.java`)
  - used to fail with SA
    - same issue as below: static initializer blocks (CLINIT)
    - now fixed. CLINIT-blocks are ignored when walking the SA tree


### "play it safe" mode where we rerun without SA if we dont find branches
- somehow this performed worse: 23 false SAFE verdicts, up from 20
For example in coral31
- very simple test case: `if (Math.round(x) > 5) { assert false; }`
- mismatch between trace and SA graph
  - seems like SA is missing a branch before the actual `round(x) > 5` check
  - what branch is this?
    - it is inside the class static initializer block, related to initializing `$assertionsDisabled`
    - potentially already handled in SWAT (see CLINIT)
      - static initializer blocks should be visible in trace
      - filter out branches inside static initializer blocks?
  - why did this mismatch only occur now?
    - rerunning without the change produced the same issue
    - is this an intermittend problem? Or caused by some other change?
    - log current git commit on every run for better reproducibility
    - always do two runs to find flaky issues?

Another flaw: (NOW FIXED)
we only rerun without SA if we find NO branches at all.
However, we might find SOME branches which then turn out to be UNSAT or non-symbolic. We should still retry in this case.
Maybe also disable SA for the rest of the run to prevent double-searching every time.
Our retry scope was too low-level (no possible_branches). It should be one level higher (no useful branches inside possible_branches).


# Next things to look into
Benchmarks with no assertion point: securibench Inter6/Refl4 hide their assert in a nested class's <clinit>, which the cfg-extractor never enters.

Datastructures6 violates SAGraph's single-fall-through invariant, because a method inlined once but called twice gets two RETURN edges. The baseline produces the identical violation, so it is unrelated to this change — worth a separate look.
Two ways forward: inline a method once per call site (context-sensitive, bigger graphs), or treat a repeat call to an already-inlined method the same way we treat ambiguous dispatch — no CALL edge, flag the call site.
- additional idea: add a "return address" to each CALL edge and record a call stack inside the symbolic explorer
  - should have the same effect as inlining once per call site, but with less overhead (smaller graphs)


## Is the static pre-analysis too heavy?
Time spent in SA must be made up by faster exploration.
Idea: do virtual dispatch lazily
- First: assume an invokevirtual can reach all application classes that derive from the stated type (add assertion point if necessary)
- When SWAT executes this invokevirtual, remember the exact class that we dispatched to
- update the SA graph with this information
Problem: the receiver type might vary based on preceding branches
- we would need to re-check for every branch
- state explosion and no benefit (we still need to conservatively assume an assertion point)

TODO: Find potential performance optimizations in SA


# 3rd Try (targets/sv-comp/runs/run_20260921_185747/VirtualDispatch_CLINIT-ignored)
Points now 494, almost back at baseline.
Only 5 false SAFE verdicts left.
- all in the `algorithms/` suite
but many timeouts

Weird though, because sum of ``stage_timing`` values seems to have decreased.
Maybe thorough timing comparison is in order:
- confirm timing works correctly
- find all timeouts and increase for trial run
  - timeout for static pre-analysis: 120s (in SVCompDriver)
  - timeout for Z3 solver: None (in SVCompDriver)
  - timeout for symbolic explorer as a whole: 900s (in targets/sv-comp/scripts/lib/execution.py)

