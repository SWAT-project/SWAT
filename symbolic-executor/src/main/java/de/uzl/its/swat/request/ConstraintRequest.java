package de.uzl.its.swat.request;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.config.Config;

/**
 * Sends the constraints recorded during symbolic execution to the symbolic explorer. The location
 * of the symbolic explorer is retrieved from the configuration. The data transfer object (DTO) is
 * build from the SymbolicTrace.
 */
public class ConstraintRequest extends Request {
    private static final Config config = Config.instance();

    /**
     * Sends the SymbolicState recorded during symbolic execution to the symbolic explorer.
     *
     * @param traceDTO The SymbolicState recorded during symbolic execution encoded as JSON.
     * @param endpointID The id representing the endpoint that was queried.
     * @param traceID The unique id of the generated trace.
     */
    public static void sendConstraints(String traceDTO, int endpointID, int traceID) {

            String host = config.getCoordinatorHost();
            int port = Integer.parseInt(config.getCoordinatorPort());
            String path = config.getSolverPath();
            try {
                send(host, port, path, endpointID, traceID, traceDTO);
            } catch (Exception e) {
                new ErrorHandler().handleException("Unable to send Constraints", e);
            }
        }



}
