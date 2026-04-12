package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.thread.ThreadHandler;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.sosy_lab.java_smt.api.*;

import static java.lang.Thread.currentThread;

public class ToLowerCaseUF {
    private final FunctionDeclaration<NumeralFormula.IntegerFormula> toLowerUF;
    private final UFManager ufmgr;
    private final IntegerFormulaManager imgr;
    private final BooleanFormulaManager bmgr;

    public ToLowerCaseUF(SolverContext ctx) throws NoThreadContextException {
        SymbolicTraceHandler sth = ThreadHandler.getSymbolicTraceHandler(Thread.currentThread().getId());
        FormulaManager fmgr = ctx.getFormulaManager();
        ufmgr = fmgr.getUFManager();
        imgr = fmgr.getIntegerFormulaManager();
        bmgr = fmgr.getBooleanFormulaManager();

        toLowerUF = ufmgr.declareUF("toLowerCase", FormulaType.IntegerType, FormulaType.IntegerType);

        // A..Z -> a..z
        for (int c = 'A'; c <= 'Z'; c++) {
            NumeralFormula.IntegerFormula in = imgr.makeNumber(c);
            NumeralFormula.IntegerFormula out = imgr.makeNumber(c + 32);
            sth.addUF(imgr.equal(ufmgr.callUF(toLowerUF, in), out));
        }
        // identity for printable ASCII excluding A..Z
        for (int c = 32; c <= 126; c++) {
            if (c < 'A' || c > 'Z') {
                NumeralFormula.IntegerFormula in = imgr.makeNumber(c);
                sth.addUF(imgr.equal(ufmgr.callUF(toLowerUF, in), in));
            }
        }
        // No definition outside printable ASCII (keeps it partial there on purpose)
    }

    /** Apply toLower with an optional guard that asserts ASCII bounds only when relevant. */
    public NumeralFormula.IntegerFormula applyToLower(
            NumeralFormula.IntegerFormula charCode,
            @Nullable BooleanFormula guard
    ) throws NoThreadContextException {
        if (guard != null) {
            SymbolicTraceHandler sth = ThreadHandler.getSymbolicTraceHandler(Thread.currentThread().getId());
            BooleanFormula inAscii = bmgr.and(
                    imgr.greaterOrEquals(charCode, imgr.makeNumber(32)),
                    imgr.lessOrEquals(charCode, imgr.makeNumber(126))
            );
            sth.addUF(bmgr.implication(guard, inAscii));
        }
        return ufmgr.callUF(toLowerUF, charCode);
    }
}
