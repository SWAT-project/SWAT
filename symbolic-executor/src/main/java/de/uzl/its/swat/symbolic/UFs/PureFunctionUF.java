package de.uzl.its.swat.symbolic.UFs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.UFManager;

/**
 * Per-thread registry of the generic uninterpreted functions that model whitelisted pure JDK methods
 * (G4). One UF symbol per signature (named via {@link PureMethods#ufName}), declared lazily and
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
}
