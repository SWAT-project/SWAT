package de.uzl.its.swat.coverage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.request.Request;
import de.uzl.its.swat.symbolic.trace.dto.CoverageDTO;

public class CoverageRequest extends Request {

    private static final Config config = Config.instance();

    public static void sendCoverage(String requestBody, int endpointID, int traceID) {
        if (requestBody != null) {

            String host = config.getExplorerHost();
            int port = config.getExplorerPort();
            String path = config.getInstrCoveragePath();
            try {
                send(host, port, path, endpointID, traceID, requestBody);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendTotalBranches() {

        String host = config.getExplorerHost();
        int port = config.getExplorerPort();
        String path = config.getBranchCoveragePath();
        try {
            send(
                    host,
                    port,
                    path,
                    -1,
                    -1,
                    buildBranchCoverageRequestBody(
                            new CoverageDTO(
                                    BranchCoverage.getVisitedBranches().stream().toList(),
                                    BranchCoverage.getTotalBranches().size())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildBranchCoverageRequestBody(Object payload)
            throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }
}
