package de.uzl.its.targets.fleet.repository;

import de.uzl.its.targets.fleet.model.Vehicle;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class VehicleRepository {
    private final Map<String, Vehicle> store = new ConcurrentHashMap<>();

    public Vehicle save(Vehicle v) {
        store.put(v.getVehicleId(), v);
        return v;
    }

    public Vehicle findById(String id) { return store.get(id); }
    public Collection<Vehicle> findAll() { return store.values(); }
}
