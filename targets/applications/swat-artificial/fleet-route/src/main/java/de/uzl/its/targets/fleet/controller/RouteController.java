package de.uzl.its.targets.fleet.controller;

import de.uzl.its.targets.fleet.dto.OptimizeRouteRequest;
import de.uzl.its.targets.fleet.model.RoutePlan;
import de.uzl.its.targets.fleet.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/route")
public class RouteController {
    private final RouteService routeService;

    public RouteController(RouteService routeService) { this.routeService = routeService; }

    @PostMapping("/optimize")
    public ResponseEntity<RoutePlan> optimize(@RequestBody OptimizeRouteRequest req,
                                              @RequestParam(defaultValue = "10") int timeoutSec,
                                              @RequestParam(defaultValue = "greedy") String solver) {
        return ResponseEntity.ok(routeService.optimize(req, timeoutSec, solver));
    }

    @GetMapping("/simulation/{routeId}")
    public ResponseEntity<RoutePlan> simulation(@PathVariable String routeId, @RequestParam(defaultValue = "1.0") double speedFactor,
                                                 @RequestParam(defaultValue = "true") boolean includeDelays) {
        RoutePlan p = routeService.get(routeId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PostMapping("/adjust/{routeId}")
    public ResponseEntity<RoutePlan> adjust(@PathVariable String routeId, @RequestBody(required = false) Object body) {
        RoutePlan p = routeService.get(routeId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }
}
