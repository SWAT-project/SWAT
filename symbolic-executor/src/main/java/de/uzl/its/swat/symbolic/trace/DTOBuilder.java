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

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;

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

        FormulaManager fmgr =
                ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();
        for (InputElement ie : symbolicTrace.getInputs()) {
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
                try {
                    BooleanFormula f = fmgr.simplify(be.getConstraint());
                    // Todo or go back to extracting variables and UFs?
                    if(fmgr.getBooleanFormulaManager().isTrue(f) || fmgr.getBooleanFormulaManager().isFalse(f)){
                        constraint = "(assert true)";
                    } else {
                        constraint = String.valueOf(fmgr.dumpFormula(f));
                        if (!fmgr.extractVariablesAndUFs(f).isEmpty() &&
                            !fmgr.extractVariablesAndUFs(f).keySet().stream().allMatch(s -> s.matches("[A-Z].*_[0-9].*"))) {
                            symbolicPrecisionLoss = true;
                        }
                    }
                    //fmgr.extractVariablesAndUFs(f).keySet().forEach(System.out::println);
                    // assert fmgr.extractVariablesAndUFs(f).keySet().stream()
                    //         .allMatch(s -> s.matches("[A-Z].*_[0-9].*")): "[SWAT] UF introduced in: " + constraint;

                } catch (InterruptedException e) {
                    BooleanFormula f = be.getConstraint();
                    logger.warn("Error while simplifying formula", e);
                    constraint = String.valueOf(fmgr.dumpFormula(f));
                    //fmgr.extractVariablesAndUFs(f).keySet().forEach(System.out::println);
                    // assert fmgr.extractVariablesAndUFs(f).keySet().stream()
                    //         .allMatch(s -> s.matches("[A-Z].*_[0-9].*")): "[SWAT] UF introduced in: " + constraint;
                    if (!fmgr.extractVariablesAndUFs(f).keySet().stream().allMatch(s -> s.matches("[A-Z].*_[0-9].*"))) {
                        symbolicPrecisionLoss = true;
                    }
                }
                trace.add(new BranchDTO(be.getIid(), constraint, be.isBranched()));
            } else if (el instanceof SpecialElement se) {
                trace.add(new BranchDTO(se.getIid(), se.getInst()));
            }
        }
        return new TraceDTO(inputs, trace, ufs, symbolicTrace.isSymbolicContextLoss(), symbolicPrecisionLoss, symbolicTrace.isReferenceSemanticChange());
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
