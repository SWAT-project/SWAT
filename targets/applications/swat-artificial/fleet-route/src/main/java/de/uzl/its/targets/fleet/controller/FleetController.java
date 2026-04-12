package de.uzl.its.targets.fleet.controller;

import de.uzl.its.targets.fleet.dto.RegisterVehicleRequest;
import de.uzl.its.targets.fleet.model.Vehicle;
import de.uzl.its.targets.fleet.service.FleetService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;

@RestController
@RequestMapping("/fleet")
@Validated
public class FleetController {
    private final FleetService fleetService;

    public FleetController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @PostMapping("/register")
    public ResponseEntity<Vehicle> register(@RequestBody @Valid RegisterVehicleRequest req) {
        Vehicle v = new Vehicle(req.getVehicleId(), req.getCapacityKg(), req.isRefrigerated(), req.isHazardous(), req.getLastKnownLocation());
        return ResponseEntity.ok(fleetService.register(v));
    }

    @GetMapping("/health")
    public ResponseEntity<Collection<Vehicle>> health(@RequestParam(required = false) String region, @RequestParam(required = false) Double minBattery) {
        return ResponseEntity.ok(fleetService.list(region, minBattery));
    }
}
