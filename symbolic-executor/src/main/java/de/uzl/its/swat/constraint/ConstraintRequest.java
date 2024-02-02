package de.uzl.its.swat.constraint;

import static java.lang.Thread.currentThread;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uzl.its.dto.BranchDTO;
import de.uzl.its.dto.ConstraintDTO;
import de.uzl.its.dto.InputDTO;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.executionData.SymbolicState;
import de.uzl.its.swat.request.Request;
import de.uzl.its.swat.thread.ThreadHandler;
import de.uzl.its.symbolic.BranchElement;
import de.uzl.its.symbolic.Element;
import de.uzl.its.symbolic.InputElement;
import de.uzl.its.symbolic.SpecialElement;
import java.util.ArrayList;
import org.sosy_lab.java_smt.api.FormulaManager;

/**
 * Sends the constraints recorded during symbolic execution to the symbolic explorer. The location
 * of the symbolic explorer is retrieved from the configuration. The data transfer object (DTO) is
 * build from the {@link SymbolicState SuymbolicState}.
 *
 * @author Nils Loose
 * @version 1.0
 */
public class ConstraintRequest extends Request {
    private static final Config config = Config.instance();

    /**
     * Sends the {@link SymbolicState SuymbolicState} recorded during symbolic execution to the
     * symbolic explorer.
     *
     * @param symbolicState The {@link SymbolicState SymbolicState} recorded during symbolic
     *     execution.
     * @param endpointID The id representing the endpoint that was queried.
     * @param traceID The unique id of the generated trace.
     */
    public static void sendConstraints(SymbolicState symbolicState, int endpointID, int traceID) {

        if (!symbolicState.getTrace().isEmpty() && !symbolicState.getInputs().isEmpty()) {

            String host = config.getCoordinatorHost();
            int port = Integer.parseInt(config.getCoordinatorPort());
            String path = config.getSolverPath();
            try {
                ConstraintDTO constraints = buildDTO(symbolicState);
                send(host, port, path, endpointID, traceID, buildRequestBody(constraints));
            } catch (Exception e) {
                new ErrorHandler().handleException("Unable to send Constraints", e);
            }
        }
    }

    public static String buildRequestBody(Object payload) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    /**
     * Constructs a data transfer object (DTO) from the {@link SymbolicState SuymbolicState}
     *
     * @param symbolicState The {@link SymbolicState SymbolicState} that contains symbolic
     *     information
     * @return A {@link ConstraintDTO ConstraintDTO} that contains relevant all relevant information
     *     to transfer symbolic traces
     */
    private static ConstraintDTO buildDTO(SymbolicState symbolicState) {
        ArrayList<InputDTO> inputs = new ArrayList<>();
        ArrayList<BranchDTO> trace = new ArrayList<>();

        FormulaManager fmgr =
                ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();
        for (InputElement ie : symbolicState.getInputs()) {
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

        for (Element el : symbolicState.getTrace()) {
            if (el instanceof BranchElement be) {
                String constraint;
                try {
                    constraint =
                            String.valueOf(fmgr.dumpFormula(fmgr.simplify(be.getPathConstraint())));
                } catch (InterruptedException e) {
                    constraint = String.valueOf(fmgr.dumpFormula(be.getPathConstraint()));
                }
                trace.add(
                        new BranchDTO(
                                be.getIid(),
                                constraint,
                                be.getBranch(),
                                be.getStackSize(),
                                be.getLocalsSize(),
                                be.getCallSize()));
            } else if (el instanceof SpecialElement se) {
                trace.add(new BranchDTO(se.getIid(), se.getInst()));
            }
        }
        return new ConstraintDTO(inputs, trace);
    }
}
