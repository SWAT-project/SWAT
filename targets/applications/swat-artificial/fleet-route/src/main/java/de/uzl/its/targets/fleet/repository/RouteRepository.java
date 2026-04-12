package de.uzl.its.targets.fleet.repository;

import de.uzl.its.targets.fleet.model.RoutePlan;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RouteRepository {
    private final Map<String, RoutePlan> store = new ConcurrentHashMap<>();

    public RoutePlan save(RoutePlan r) {
        store.put(r.getRouteId(), r);
        return r;
    }

    public RoutePlan findById(String id) { return store.get(id); }
}
