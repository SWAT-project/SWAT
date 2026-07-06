package de.uzl.its.swat.symbolic.trace;

import static java.lang.Thread.currentThread;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.coverage.InstrCoverage;
import de.uzl.its.swat.symbolic.trace.dto.*;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

/**
 * Builds a data transfer object (DTO) from the {@link SymbolicTrace SuymbolicState} for
 * transportation to the Symbolic Explorer. The trace is encoded as a JSON string.
 */
class DTOBuilder {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    /**
     * Encodes the symbolic trace as a JSON string.
     *
     * @param symbolicTrace The symbolic trace to be encoded.
     * @return The symbolic trace encoded as a JSON string.
     */
    protected static String encodeTrace(SymbolicTrace symbolicTrace) throws NoThreadContextException, JsonProcessingException, NotImplementedException {
        return buildRequestBody(buildTraceDTO(symbolicTrace));
    }

    /**
     * Constructs a data transfer object (DTO) from the {@link SymbolicTrace SuymbolicState}
     *
     * @param symbolicTrace The {@link SymbolicTrace SymbolicState} that contains symbolic
     *     information
     * @return A {@link TraceDTO ConstraintDTO} that contains relevant all relevant information
     *     to transfer symbolic traces
     */
    private static TraceDTO buildTraceDTO(SymbolicTrace symbolicTrace) throws NoThreadContextException, NotImplementedException {
        ArrayList<InputDTO> inputs = new ArrayList<>();
        ArrayList<UFDTO> ufs = new ArrayList<>();
        ArrayList<BranchDTO> trace = new ArrayList<>();
        // The terms of the designated symbolic inputs, used to classify precision loss: a branch
        // variable is "grounded" iff its term is one of these (exact + type-aware, no name pattern).
        Set<Formula> inputTerms = new HashSet<>();

        FormulaManager fmgr =
                ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();
        for (InputElement ie : symbolicTrace.getInputs()) {
            inputTerms.add((Formula) ie.getValue().formula);
            String lowerBound = String.valueOf(fmgr.dumpFormula(ie.getValue().getBounds(false)));
            String upperBound = String.valueOf(fmgr.dumpFormula(ie.getValue().getBounds(true)));
            InputDTO iDto =
                    new InputDTO(
                            ie.getName(),
                            ie.getValue().getConcreteEncoded(),
                            ie.getValue().getType(),
                            lowerBound,
                            upperBound);
            inputs.add(iDto);
        }
        for (BooleanFormula uf : symbolicTrace.getConstraints()) {
            String formula = String.valueOf(fmgr.dumpFormula(uf));
            ufs.add(new UFDTO(formula));
        }
        logger.trace("Parsing constraints...");
        boolean symbolicPrecisionLoss = false;
        for (Element el : symbolicTrace.getTrace()) {
            if (el instanceof BranchElement be) {
                String constraint;
                boolean branchPrecisionLoss = false;
                try {
                    BooleanFormula f = fmgr.simplify(be.getConstraint());
                    if(fmgr.getBooleanFormulaManager().isTrue(f) || fmgr.getBooleanFormulaManager().isFalse(f)){
                        constraint = "(assert true)";
                    } else {
                        constraint = String.valueOf(fmgr.dumpFormula(f));
                        branchPrecisionLoss = isPrecisionLoss(f, fmgr, inputTerms);
                    }
                } catch (InterruptedException e) {
                    BooleanFormula f = be.getConstraint();
                    logger.warn("Error while simplifying formula", e);
                    constraint = String.valueOf(fmgr.dumpFormula(f));
                    branchPrecisionLoss = isPrecisionLoss(f, fmgr, inputTerms);
                }
                // Aggregate precision loss = OR of the per-branch flags. The per-branch flag also
                // travels on the BranchDTO so a future explorer-side, CFG-reachability-aware
                // precision-loss decision can take over with no trace change.
                symbolicPrecisionLoss |= branchPrecisionLoss;
                trace.add(new BranchDTO(be.getIid(), constraint, be.isBranched(), branchPrecisionLoss));
            } else if (el instanceof SpecialElement se) {
                trace.add(new BranchDTO(se.getIid(), se.getInst()));
            }
        }
        return new TraceDTO(inputs, trace, ufs, symbolicTrace.isSymbolicContextLoss(), symbolicPrecisionLoss);
    }

    /**
     * A symbolic-variable name produced by the input-naming convention (primitive prefixes like
     * {@code I_0}). Matches symbolic variables that are NOT designated inputs but are still
     * grounded - notably values re-materialized by GETVALUE heap recovery (which call MAKE_SYMBOLIC
     * with a fresh name, without re-registering an input). Without this pattern such branches would
     * be classified as precision loss - soundly, but unnecessarily.
     */
    private static final java.util.regex.Pattern INPUT_VAR_PATTERN =
            java.util.regex.Pattern.compile("[A-Z].*_[0-9].*");

    /**
     * Whether a branch constraint loses symbolic precision, given the terms of the designated symbolic
     * inputs. It does iff it contains a symbol that is neither (a) a grounded variable - a designated
     * input (its term is in {@code inputTerms}) or a recovery-named variable matching
     * {@link #INPUT_VAR_PATTERN} - nor (b) a whitelisted generic pure UF (name starts with
     * {@code pure_}). Case (b) is precision-preserving: an axiom-free UF over real inputs is a sound
     * over-approximation of any deterministic function, so UNSAT under the free UF implies UNSAT for
     * the real function (SAFE stays sound). Bespoke (axiomatized) UFs and any non-grounded variable
     * do lose precision.
     *
     * <p>The primary input check is exact term identity against the designated inputs (JavaSMT
     * formulas have value-based equals/hashCode), so it is correct for String/array inputs as well as
     * primitives - which a name pattern alone is not. Uses a {@link DefaultFormulaVisitor} so UF
     * symbols and variables are distinguished; it descends into UF arguments so a {@code pure_} UF
     * applied to a non-input variable still loses precision (the nested variable is caught).
     */
    public static boolean isPrecisionLoss(
            BooleanFormula f, FormulaManager fmgr, Set<Formula> inputTerms) {
        AtomicBoolean lossy = new AtomicBoolean(false);
        fmgr.visitRecursively(
                f,
                new DefaultFormulaVisitor<TraversalProcess>() {
                    @Override
                    protected TraversalProcess visitDefault(Formula formula) {
                        return TraversalProcess.CONTINUE;
                    }

                    @Override
                    public TraversalProcess visitFreeVariable(Formula formula, String name) {
                        if (!inputTerms.contains(formula) && !INPUT_VAR_PATTERN.matcher(name).matches()) {
                            lossy.set(true);
                            return TraversalProcess.ABORT;
                        }
                        return TraversalProcess.CONTINUE;
                    }

                    @Override
                    public TraversalProcess visitFunction(
                            Formula formula, List<Formula> args, FunctionDeclaration<?> decl) {
                        // Descend into args regardless so nested non-input variables are caught; only a
                        // bespoke (non-pure_) UF taints the branch by itself.
                        if (decl.getKind() == FunctionDeclarationKind.UF
                                && !decl.getName().startsWith("pure_")) {
                            lossy.set(true);
                            return TraversalProcess.ABORT;
                        }
                        return TraversalProcess.CONTINUE;
                    }
                });
        return lossy.get();
    }

    protected static String encodeCoverage(InstrCoverage instrCoverage) throws JsonProcessingException {
            return buildRequestBody(buildInstrCoverageDTO(instrCoverage));
    }

    private static CoverageDTO buildInstrCoverageDTO(InstrCoverage instrCoverage)
            throws JsonProcessingException {
        HashSet<Long> ids = instrCoverage.getCoverage();
        long totalInstructions = InstrCoverage.numInstructions;
        return buildDTO(ids.stream().toList(), totalInstructions);
    }

    private static CoverageDTO buildDTO(List<Long> ids, long total) {

        return new CoverageDTO(ids, total);
    }

    /**
     * Transforms DTO into JSON string for transportation
     *
     * @param payload The DTO to be transformed
     * @return The JSON string
     * @throws JsonProcessingException If the transformation fails
     */
    private static String buildRequestBody(Object payload) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }
}
