package de.uzl.its.targets.fleet.controller;

import de.uzl.its.targets.fleet.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) { this.analyticsService = analyticsService; }

    @GetMapping("/usage")
    public ResponseEntity<Map<String,Object>> usage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "vehicle") String groupBy) {
        return ResponseEntity.ok(analyticsService.usage(from, to, groupBy));
    }

    @PostMapping("/whatif")
    public ResponseEntity<Map<String,Object>> whatif(@RequestBody Map<String,Object> scenario, @RequestParam(defaultValue = "5") int topN) {
        return ResponseEntity.ok(analyticsService.whatIf(scenario, topN));
    }
}
