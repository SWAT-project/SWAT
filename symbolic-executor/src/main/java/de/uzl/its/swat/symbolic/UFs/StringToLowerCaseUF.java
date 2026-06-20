package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.sosy_lab.java_smt.api.*;

public class StringToLowerCaseUF {
    private final CharToLowerCaseUF charUF;
    private final FunctionDeclaration<StringFormula> strUF;
    private final UFManager ufmgr;
    private final BooleanFormulaManager bmgr;
    private final IntegerFormulaManager imgr;
    private final StringFormulaManager smgr;

    // Global cap for unrolling; tune as needed.
    private static final int MAX_K = 50;

    public StringToLowerCaseUF(SolverContext ctx) throws NoThreadContextException {
        FormulaManager frml_mgr = ctx.getFormulaManager();
        ufmgr = frml_mgr.getUFManager();
        bmgr = frml_mgr.getBooleanFormulaManager();
        imgr = frml_mgr.getIntegerFormulaManager();
        smgr = frml_mgr.getStringFormulaManager();

        strUF = ufmgr.declareUF("str_toLowerCase", FormulaType.StringType, FormulaType.StringType);

        charUF = ThreadHandler.getUFHandler(Thread.currentThread().getId()).getCharToLowerCaseUF();
    }

    public StringFormula applyToLowerCase(StringFormula symIn) throws NoThreadContextException {
        var symOut = ufmgr.callUF(strUF, symIn);

        var lenIn = smgr.length(symIn);
        var lenOut = smgr.length(symOut);

        final int K = MAX_K;

        SymbolicTraceHandler sth = ThreadHandler.getSymbolicTraceHandler(Thread.currentThread().getId());
        sth.addConstraint(imgr.equal(lenIn, lenOut));

        for (int i = 0; i < K; i++) {
            var iF = imgr.makeNumber(i);
            sth.addConstraint(imgr.equal(
                    charUF.applyToLower(
                            smgr.toCodePoint(smgr.charAt(symIn, iF)), null),
                    smgr.toCodePoint(smgr.charAt(symOut, iF))));
        }

        return symOut;
    }
}
