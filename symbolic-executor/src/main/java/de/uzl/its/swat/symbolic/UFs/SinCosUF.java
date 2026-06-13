package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.sosy_lab.java_smt.api.*;

public class SinCosUF {
    private final FormulaType.FloatingPointType DoubleType = FormulaType.getDoublePrecisionFloatingPointType();
    private final FunctionDeclaration<FloatingPointFormula> sinUF;
    private final FunctionDeclaration<FloatingPointFormula> cosUF;
    private final UFManager ufmgr;
    private final BooleanFormulaManager bmgr;
    private final FloatingPointFormulaManager fmgr;

    private final ArrayList<Double> inputValues;

    public SinCosUF(SolverContext ctx) throws NoThreadContextException {
        SymbolicTraceHandler sth = ThreadHandler.getSymbolicTraceHandler(Thread.currentThread().getId());
        FormulaManager frml_mgr = ctx.getFormulaManager();
        ufmgr = frml_mgr.getUFManager();
        bmgr = frml_mgr.getBooleanFormulaManager();
        fmgr = frml_mgr.getFloatingPointFormulaManager();

        sinUF = ufmgr.declareUF("sin", DoubleType, DoubleType);
        cosUF = ufmgr.declareUF("cos", DoubleType, DoubleType);

        final double PI = Math.PI;
        inputValues = new ArrayList<Double>(Arrays.asList(
                0.0, 0.5 * PI, 1.0 * PI, 1.5 * PI, 2.0 * PI,
                -0.0, -0.5 * PI, -1.0 * PI, -1.5 * PI, -2.0 * PI,
                1.0, 2.0, 3.0, 4.0));
        int initialSize = inputValues.size();
        for (int i = 0; i < initialSize; i++) {
            var out = Math.sin(inputValues.get(i));
            if (!inputValues.contains(out))
                inputValues.add(out);

            out = Math.cos(inputValues.get(i));
            if (!inputValues.contains(out))
                inputValues.add(out);
        }

        for (double concreteIn : inputValues) {
            var symIn = fmgr.makeNumber(concreteIn, DoubleType);
            var symOut = fmgr.makeNumber(Math.sin(concreteIn), DoubleType);
            sth.addConstraint(fmgr.equalWithFPSemantics(ufmgr.callUF(sinUF, symIn), symOut));

            symIn = fmgr.makeNumber(concreteIn, DoubleType);
            symOut = fmgr.makeNumber(Math.cos(concreteIn), DoubleType);
            sth.addConstraint(fmgr.equalWithFPSemantics(ufmgr.callUF(cosUF, symIn), symOut));
        }
    }

    public FloatingPointFormula applySin(FloatingPointFormula symIn) throws NoThreadContextException {
        var symOut = ufmgr.callUF(sinUF, symIn);

        SymbolicTraceHandler sth = ThreadHandler.getSymbolicTraceHandler(Thread.currentThread().getId());
        sth.addConstraint(bmgr.and( // Output is between -1 and 1.
                fmgr.greaterOrEquals(symOut, fmgr.makeNumber(-1.0, DoubleType)),
                fmgr.lessOrEquals(symOut, fmgr.makeNumber(1.0, DoubleType))));

        // Input must be one of the known inputValues, otherwise the solver can just "make up" any result.
        // This loses soundness, but that's okay, because UFs cause precision loss anyway and thus we will never
        // give a SAFE verdict. We can however find some VIOLATIONs this way.
        sth.addConstraint(
                bmgr.or(inputValues.stream().map(v -> fmgr.equalWithFPSemantics(symIn, fmgr.makeNumber(v, DoubleType)))
                        .collect(Collectors.toList())));

        return symOut;
    }

    public FloatingPointFormula applyCos(FloatingPointFormula symIn) throws NoThreadContextException {
        var symOut = ufmgr.callUF(cosUF, symIn);

        SymbolicTraceHandler sth = ThreadHandler.getSymbolicTraceHandler(Thread.currentThread().getId());
        sth.addConstraint(bmgr.and( // Output is between -1 and 1.
                fmgr.greaterOrEquals(symOut, fmgr.makeNumber(-1.0, DoubleType)),
                fmgr.lessOrEquals(symOut, fmgr.makeNumber(1.0, DoubleType))));

        // Input must be one of the known inputValues, otherwise the solver can just "make up" any result.
        // This loses soundness, but that's okay, because UFs cause precision loss anyway and thus we will never
        // give a SAFE verdict. We can however find some VIOLATIONs this way.
        sth.addConstraint(
                bmgr.or(inputValues.stream().map(v -> fmgr.equalWithFPSemantics(symIn, fmgr.makeNumber(v, DoubleType)))
                        .collect(Collectors.toList())));

        return symOut;
    }
}
