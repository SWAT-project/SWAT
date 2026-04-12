package de.uzl.its.swat.symbolic.trace.dto;

import java.util.List;

public class CoverageDTO {

    public List<Long> ids;
    public long total;

    public CoverageDTO(List<Long> ids, long total) {
        this.ids = ids;
        this.total = total;
    }

    public CoverageDTO() {}
}
