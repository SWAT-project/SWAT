package de.uzl.its.swat.symbolic.trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.coverage.CoverageFactory;
import de.uzl.its.swat.coverage.InstrCoverage;
import lombok.Getter;

@Getter
public class CoverageTraceHandler {

    private final InstrCoverage instrCoverage;
    private static final Config config = Config.instance();

    public CoverageTraceHandler() {
        CoverageFactory coverageFactory = new CoverageFactory();
        this.instrCoverage = coverageFactory.getCoverage(config.getCoverageType());
    }

    public String getCoverageDTO() throws JsonProcessingException {
        return DTOBuilder.encodeCoverage(instrCoverage);
    }

    public void addInstruction(long iid) {
        instrCoverage.visitInstruction(iid);
    }
}
