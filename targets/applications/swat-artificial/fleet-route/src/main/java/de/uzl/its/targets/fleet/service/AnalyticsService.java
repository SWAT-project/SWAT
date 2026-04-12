package de.uzl.its.targets.fleet.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class AnalyticsService {
    public Map<String,Object> usage(OffsetDateTime from, OffsetDateTime to, String groupBy) {
        return Map.of("from", from, "to", to, "groupBy", groupBy, "buckets", java.util.List.of());
    }

    public Map<String,Object> whatIf(Map<String,Object> scenario, int topN) {
        return Map.of("scenario", scenario, "topN", topN);
    }
}
