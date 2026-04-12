package de.uzl.its.targets.fleet.service;

import de.uzl.its.targets.fleet.model.Vehicle;
import de.uzl.its.targets.fleet.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class FleetService {
    private final VehicleRepository vehicleRepository;

    public FleetService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle register(Vehicle v) {
        if (v.getCapacityKg() < 0) throw new IllegalArgumentException("capacity must be >= 0");
        return vehicleRepository.save(v);
    }

    public Collection<Vehicle> list(String region, Double minBattery) {
        return vehicleRepository.findAll();
    }
}
