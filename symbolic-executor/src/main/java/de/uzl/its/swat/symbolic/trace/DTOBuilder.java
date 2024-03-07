package de.uzl.its.swat.symbolic.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.symbolic.trace.*;
import de.uzl.its.swat.symbolic.trace.dto.BranchDTO;
import de.uzl.its.swat.symbolic.trace.dto.ConstraintDTO;
import de.uzl.its.swat.symbolic.trace.dto.InputDTO;
import de.uzl.its.swat.thread.ThreadHandler;
import org.sosy_lab.java_smt.api.FormulaManager;

import java.util.ArrayList;

import static java.lang.Thread.currentThread;

/**
 * Builds a data transfer object (DTO) from the {@link SymbolicTrace SuymbolicState} for transportation to the Symbolic Explorer.
 * The trace is encoded as a JSON string.
 */
class DTOBuilder {

    /**
     * Encodes the symbolic trace as a JSON string.
     * @param symbolicTrace The symbolic trace to be encoded.
     * @return The symbolic trace encoded as a JSON string.
     */
    protected static String encodeTrace(SymbolicTrace symbolicTrace) {
        try {
            return buildRequestBody(buildTraceDTO(symbolicTrace));
        } catch (JsonProcessingException e) {
            new ErrorHandler().handleException("Unable to encode trace", e);
            return "";
        }
    }

    /**
     * Constructs a data transfer object (DTO) from the {@link SymbolicTrace SuymbolicState}
     *
     * @param symbolicTrace The {@link SymbolicTrace SymbolicState} that contains symbolic
     *     information
     * @return A {@link ConstraintDTO ConstraintDTO} that contains relevant all relevant information
     *     to transfer symbolic traces
     */
    private static ConstraintDTO buildTraceDTO(SymbolicTrace symbolicTrace) {
        ArrayList<InputDTO> inputs = new ArrayList<>();
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

        for (Element el : symbolicTrace.getTrace()) {
            if (el instanceof BranchElement be) {
                String constraint;
                try {
                    constraint =
                            String.valueOf(fmgr.dumpFormula(fmgr.simplify(be.getConstraint())));
                } catch (InterruptedException e) {
                    constraint = String.valueOf(fmgr.dumpFormula(be.getConstraint()));
                }
                trace.add(
                        new BranchDTO(
                                be.getIid(),
                                constraint,
                                be.isBranched()));
            } else if (el instanceof SpecialElement se) {
                trace.add(new BranchDTO(se.getIid(), se.getInst()));
            }
        }
        return new ConstraintDTO(inputs, trace);
    }


    /**
     * Transforms DTO into JSON string for transportation
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
