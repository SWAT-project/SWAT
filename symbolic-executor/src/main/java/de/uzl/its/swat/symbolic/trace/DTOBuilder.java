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
import de.uzl.its.swat.common.logging.StatsStorage;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.solver.SolverMode;
import de.uzl.its.swat.coverage.InstrCoverage;
import de.uzl.its.swat.symbolic.trace.dto.*;
import de.uzl.its.swat.thread.ThreadHandler;
import de.uzl.its.swat.witness.Witness;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;

/**
 * Builds a data transfer object (DTO) from the {@link SymbolicTrace SuymbolicState} for
 * transportation to the Symbolic Explorer. The trace is encoded as a JSON string.
 */
class DTOBuilder {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    private static final Pattern INPUT_NAME = Pattern.compile("[A-Z].*_[0-9].*");
    /**
     * Encodes the symbolic trace as a JSON string.
     *
     * @param symbolicTrace The symbolic trace to be encoded.
     * @param statsStorage The per-execution statistics of the thread.
     * @param threadId The thread whose solver context the formulas belong to.
     * @return The symbolic trace encoded as a JSON string.
     */
    protected static String encodeTrace(SymbolicTrace symbolicTrace, StatsStorage statsStorage, long threadId) throws NoThreadContextException, JsonProcessingException, NotImplementedException {
        return buildRequestBody(buildTraceDTO(symbolicTrace, statsStorage, threadId));
    }

    /**
     * Builds the list of missing-invocation DTOs from the per-execution statistics. The full set of
     * missing invocations is the superset; entries present in the context-loss set are flagged so
     * the explorer can compute the context-loss subset without log scraping.
     *
     * @param statsStorage The per-execution statistics accumulator.
     * @return The missing invocations encoded as DTOs.
     */
    private static ArrayList<InvocationDTO> buildMissingInvocations(StatsStorage statsStorage) {
        ArrayList<InvocationDTO> missingInvocations = new ArrayList<>();
        for (Map.Entry<InvocationEntry, Integer> e : statsStorage.getInvocations().entrySet()) {
            InvocationEntry entry = e.getKey();
            missingInvocations.add(
                    new InvocationDTO(
                            entry.owner(),
                            entry.name(),
                            entry.desc(),
                            entry.isInstance(),
                            entry.isSymbolic(),
                            statsStorage.getContextLossInvocations().contains(entry),
                            e.getValue()));
        }
        return missingInvocations;
    }

    /**
     * Constructs a data transfer object (DTO) from the {@link SymbolicTrace SuymbolicState}
     *
     * @param symbolicTrace The {@link SymbolicTrace SymbolicState} that contains symbolic
     *     information
     * @return A {@link TraceDTO ConstraintDTO} that contains relevant all relevant information
     *     to transfer symbolic traces
     */
    private static TraceDTO buildTraceDTO(SymbolicTrace symbolicTrace, StatsStorage statsStorage, long threadId) throws NoThreadContextException, NotImplementedException {
        ArrayList<InputDTO> inputs = new ArrayList<>();
        ArrayList<UFDTO> ufs = new ArrayList<>();
        ArrayList<BranchDTO> trace = new ArrayList<>();

        FormulaManager fmgr =
                ThreadHandler.getSolverContext(threadId).getFormulaManager();
        FormulaDumper dumper = FormulaDumper.of(fmgr);
        for (InputElement ie : symbolicTrace.getInputs()) {
            String lowerBound = dumper.dump(ie.getValue().getBounds(false));
            String upperBound = dumper.dump(ie.getValue().getBounds(true));
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
            String formula = dumper.dump(uf);
            ufs.add(new UFDTO(formula));
        }
        logger.trace("Parsing constraints...");
        // A constraint over anything but the inputs (e.g. an uninterpreted function) means the
        // model is approximate. Input names usually match the pattern below, but String inputs
        // (java/lang/String_0) do not, so they are accepted by name.
        Set<String> inputNames = new HashSet<>();
        for (InputElement ie : symbolicTrace.getInputs()) {
            inputNames.add(ie.getName());
        }
        java.util.function.Predicate<String> isInput =
                name -> inputNames.contains(name) || INPUT_NAME.matcher(name).matches();
        boolean symbolicPrecisionLoss = symbolicTrace.isSymbolicPrecisionLoss();
        for (Element el : symbolicTrace.getTrace()) {
            if (el instanceof BranchElement be) {
                String constraint;
                try {
                    BooleanFormula f = fmgr.simplify(be.getConstraint());
                    // Todo or go back to extracting variables and UFs?
                    if(fmgr.getBooleanFormulaManager().isTrue(f) || fmgr.getBooleanFormulaManager().isFalse(f)){
                        constraint = "(assert true)";
                    } else {
                        constraint = dumper.dump(f);
                        if (!symbolicPrecisionLoss
                                && !fmgr.extractVariablesAndUFs(f).keySet().stream().allMatch(isInput)) {
                            symbolicPrecisionLoss = true;
                        }
                    }
                    //fmgr.extractVariablesAndUFs(f).keySet().forEach(System.out::println);
                    // assert fmgr.extractVariablesAndUFs(f).keySet().stream()
                    //         .allMatch(s -> s.matches("[A-Z].*_[0-9].*")): "[SWAT] UF introduced in: " + constraint;

                } catch (InterruptedException e) {
                    BooleanFormula f = be.getConstraint();
                    logger.warn("Error while simplifying formula", e);
                    constraint = dumper.dump(f);
                    //fmgr.extractVariablesAndUFs(f).keySet().forEach(System.out::println);
                    // assert fmgr.extractVariablesAndUFs(f).keySet().stream()
                    //         .allMatch(s -> s.matches("[A-Z].*_[0-9].*")): "[SWAT] UF introduced in: " + constraint;
                    if (!symbolicPrecisionLoss
                            && !fmgr.extractVariablesAndUFs(f).keySet().stream().allMatch(isInput)) {
                        symbolicPrecisionLoss = true;
                    }
                }
                trace.add(new BranchDTO(be.getIid(), constraint, be.isBranched(), be.isConcreteOnly())
                        .place(be.getKey(), be.getPops(), be.getPushes(), be.isCallback()));
            } else if (el instanceof SpecialElement se) {
                trace.add(new BranchDTO(se.getIid(), se.getInst()));
            }
        }
        return new TraceDTO(inputs, trace, ufs, buildMissingInvocations(statsStorage), Witness.getNondetReturns(), Witness.getViolation(), symbolicTrace.isSymbolicContextLoss(), symbolicPrecisionLoss, symbolicTrace.isReferenceSemanticChange(),
                symbolicTrace.isSymbolicPrecisionLoss());
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
        // Indented only where a person reads it (PRINT); the explorer gets the compact form.
        if (Config.instance().getSolverMode() == SolverMode.PRINT) {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        }
        return objectMapper.writeValueAsString(payload);
    }
}
