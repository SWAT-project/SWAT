package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.thread.ThreadHandler;
import org.sosy_lab.java_smt.api.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.currentThread;
public class EqualsIgnoreCaseUF {
    private final FunctionDeclaration<BooleanFormula> equalsIgnoreCaseUF;
    private final SolverContext ctx;
    private final UFManager ufmgr;
    private final StringFormulaManager smgr;
    private final BooleanFormulaManager bmgr;
    private final IntegerFormulaManager imgr;
    private final ToLowerCaseUF toLowerUF;

    // Global cap for unrolling; tune as needed.
    private static final int MAX_K = 50;

    public EqualsIgnoreCaseUF(SolverContext ctx) throws NoThreadContextException {
        this.ctx = ctx;
        FormulaManager fmgr = ctx.getFormulaManager();
        this.ufmgr = fmgr.getUFManager();
        this.smgr = fmgr.getStringFormulaManager();
        this.bmgr = fmgr.getBooleanFormulaManager();
        this.imgr = fmgr.getIntegerFormulaManager();
        this.toLowerUF = new ToLowerCaseUF(ctx);

        this.equalsIgnoreCaseUF = ufmgr.declareUF(
                "equalsIgnoreCase",
                FormulaType.BooleanType,
                FormulaType.StringType,
                FormulaType.StringType
        );
    }

    public BooleanFormula getUFCall(StringFormula s1, StringFormula s2) {
        return ufmgr.callUF(equalsIgnoreCaseUF, s1, s2);
    }

    /**
     * Create the (always-added) constraints that define equalsIgnoreCase for this call.
     * No branching on the concrete result is needed anymore.
     *
     * @param s1         symbolic term of the receiver
     * @param s2         symbolic term of the argument
     * @param concrete1  runtime concrete value of s1 (used to choose tighter guards)
     * @param concrete2  runtime concrete value of s2
     * @param s1Symbolic whether s1 carries variables (your isSymbolic())
     * @param s2Symbolic whether s2 carries variables
     */
    public BooleanFormula createEqualsIgnoreCaseConstraints(
            StringFormula s1, StringFormula s2,
            String concrete1, String concrete2,
            boolean s1Symbolic, boolean s2Symbolic
    ) throws NoThreadContextException {
        // UF call and lengths
        BooleanFormula uf = getUFCall(s1, s2);
        NumeralFormula.IntegerFormula len1 = smgr.length(s1);
        NumeralFormula.IntegerFormula len2 = smgr.length(s2);

        // Per-index guarded equalities up to K
        final int K;
        if (!s1Symbolic && !s2Symbolic) {
            // Both concrete → caller can short-circuit; keep small K just in case
            K = Math.min(MAX_K, Math.max(concrete1.length(), concrete2.length()));
        } else if (!s1Symbolic) { // s1 concrete
            K = Math.min(MAX_K, concrete1.length());
        } else if (!s2Symbolic) { // s2 concrete
            K = Math.min(MAX_K, concrete2.length());
        } else {
            K = MAX_K;
        }

        List<BooleanFormula> perIndex = new ArrayList<>(K);
        for (int i = 0; i < K; i++) {
            NumeralFormula.IntegerFormula iF = imgr.makeNumber(i);
            BooleanFormula inBoth = bmgr.and(
                    imgr.lessThan(iF, len1),
                    imgr.lessThan(iF, len2)
            );
            StringFormula c1 = smgr.charAt(s1, iF);
            StringFormula c2 = smgr.charAt(s2, iF);
            NumeralFormula.IntegerFormula code1 = smgr.toCodePoint(c1); // -1 if out-of-range
            NumeralFormula.IntegerFormula code2 = smgr.toCodePoint(c2);
            NumeralFormula.IntegerFormula low1 = toLowerUF.applyToLower(code1, inBoth);
            NumeralFormula.IntegerFormula low2 = toLowerUF.applyToLower(code2, inBoth);
            perIndex.add(bmgr.implication(inBoth, imgr.equal(low1, low2)));
        }

        BooleanFormula body = bmgr.and(
                imgr.equal(len1, len2),
                bmgr.and(perIndex)
        );

        // Algebraic axioms (safe & useful)
        BooleanFormula reflexive = getUFCall(s1, s1);                                // equalsIgnoreCase(s,s)
        BooleanFormula symmetric = bmgr.equivalence(uf, getUFCall(s2, s1));          // symmetry
        BooleanFormula lenMismatchImpliesFalse =
                bmgr.implication(bmgr.not(imgr.equal(len1, len2)), bmgr.not(uf));    // different lengths → false

        // Always safe direction: if equal ignoring case, then the first K positions must match
        BooleanFormula ufImpliesBody = bmgr.implication(uf, body);

        // Reverse direction guarded by a length regime where unrolling is complete
        BooleanFormula reverseGuard;
        if (!s1Symbolic && s2Symbolic) {
            // s1 is concrete of length L → activate equivalence exactly at L
            int L = concrete1.length();
            reverseGuard = bmgr.and(
                    imgr.equal(len1, imgr.makeNumber(L)),
                    imgr.equal(len2, imgr.makeNumber(L))
            );
        } else if (s1Symbolic && !s2Symbolic) {
            int L = concrete2.length();
            reverseGuard = bmgr.and(
                    imgr.equal(len1, imgr.makeNumber(L)),
                    imgr.equal(len2, imgr.makeNumber(L))
            );
        } else {
            // both symbolic (or both concrete path kept) → guard by len≤K
            reverseGuard = bmgr.and(
                    imgr.equal(len1, len2),
                    imgr.lessOrEquals(len1, imgr.makeNumber(K))
            );
        }
        BooleanFormula bodyImpliesUf = bmgr.implication(bmgr.and(reverseGuard, body), uf);

        // Bundle all constraints to add to your "UF" trace channel
        return bmgr.and(reflexive, symmetric, lenMismatchImpliesFalse, ufImpliesBody, bodyImpliesUf);
    }
}
