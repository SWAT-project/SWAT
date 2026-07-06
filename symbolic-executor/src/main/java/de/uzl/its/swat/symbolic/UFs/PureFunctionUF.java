package de.uzl.its.swat.symbolic.UFs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.UFManager;

/**
 * Per-thread registry of the generic uninterpreted functions that model whitelisted pure JDK methods.
 * One UF symbol per signature (named via {@link PureMethods#ufName}), declared lazily and
 * cached. The UF is <b>axiom-free</b>: applying it asserts only equal-inputs =&gt; equal-outputs
 * (referential transparency), a sound over-approximation of any deterministic function. Observed
 * concrete input-&gt;output pairs are added separately (as constraints) to tighten it across runs.
 *
 * <p>Contrast with the bespoke UFs ({@link ToLowerCaseUF} etc.), which ship hand axioms and live
 * inside symbolic models; this registry ships none.
 */
public class PureFunctionUF {
    private final UFManager ufmgr;
    private final FormulaManager fmgr;
    private final Map<String, FunctionDeclaration<?>> declarations = new HashMap<>();

    public PureFunctionUF(SolverContext ctx) {
        fmgr = ctx.getFormulaManager();
        ufmgr = fmgr.getUFManager();
    }

    /**
     * Build the UF application {@code ufName(args)} of the given return type. The argument sorts are
     * derived from the actual {@code args} formulas (not the descriptor) so they match the values'
     * sorts. The declaration is cached per name; on reuse the signature is asserted to match.
     */
    public Formula apply(String ufName, FormulaType<?> returnType, List<Formula> args) {
        List<FormulaType<?>> argTypes =
                args.stream().map(fmgr::getFormulaType).collect(Collectors.toList());
        FunctionDeclaration<?> decl = declarations.get(ufName);
        if (decl == null) {
            decl = ufmgr.declareUF(ufName, returnType, argTypes);
            declarations.put(ufName, decl);
        } else {
            assert decl.getType().equals(returnType) && decl.getArgumentTypes().equals(argTypes)
                    : "Generic UF signature mismatch for " + ufName;
        }
        return ufmgr.callUF(decl, args);
    }

    /**
     * A constant formula of {@code type} holding the concrete {@code value}, for the ground (observed)
     * side of a pure-UF pair. Supports the value-type sorts: boolean, bitvector (any width, from a
     * {@link Character} or {@link Number}), floating point (single/double), and String.
     */
    public static Formula constant(FormulaManager fmgr, FormulaType<?> type, Object value) {
        if (type.isBooleanType()) {
            return fmgr.getBooleanFormulaManager().makeBoolean((Boolean) value);
        }
        if (type.isBitvectorType()) {
            int width = ((FormulaType.BitvectorType) type).getSize();
            long bits = (value instanceof Character c) ? (char) c : ((Number) value).longValue();
            return fmgr.getBitvectorFormulaManager().makeBitvector(width, bits);
        }
        if (type.isFloatingPointType()) {
            return fmgr.getFloatingPointFormulaManager()
                    .makeNumber(((Number) value).doubleValue(), (FormulaType.FloatingPointType) type);
        }
        if (type.isStringType()) {
            return fmgr.getStringFormulaManager().makeString((String) value);
        }
        throw new IllegalArgumentException("Unsupported observed-pair type: " + type);
    }

    /**
     * The equality {@code uf == constant(value)} in the theory of {@code uf}'s sort, used to record an
     * observed (input -&gt; output) ground pair for a pure UF.
     */
    public static BooleanFormula equalConstant(FormulaManager fmgr, Formula uf, Object value) {
        FormulaType<?> type = fmgr.getFormulaType(uf);
        Formula c = constant(fmgr, type, value);
        if (type.isBooleanType()) {
            return fmgr.getBooleanFormulaManager().equivalence((BooleanFormula) uf, (BooleanFormula) c);
        }
        if (type.isBitvectorType()) {
            return fmgr.getBitvectorFormulaManager().equal((BitvectorFormula) uf, (BitvectorFormula) c);
        }
        if (type.isFloatingPointType()) {
            // Pin the observed output with structural '=' (assignment: NaN == NaN, +0 != -0), NOT
            // fp.eq. IEEE fp.eq is false for any NaN operand, so an observed pair whose output is NaN
            // (e.g. Math.log/sqrt of an out-of-domain input) would encode as an unsatisfiable term
            // that poisons the whole path constraint, making the solver report a reachable branch as
            // UNSAT (a spurious SAFE). '=' pins the exact observed bit-value soundly, NaN included.
            return fmgr.getFloatingPointFormulaManager()
                    .assignment((FloatingPointFormula) uf, (FloatingPointFormula) c);
        }
        if (type.isStringType()) {
            return fmgr.getStringFormulaManager().equal((StringFormula) uf, (StringFormula) c);
        }
        throw new IllegalArgumentException("Unsupported observed-pair type: " + type);
    }
}
