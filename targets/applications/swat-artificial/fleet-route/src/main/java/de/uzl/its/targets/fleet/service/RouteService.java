package de.uzl.its.targets.fleet.service;

import de.uzl.its.targets.fleet.dto.OptimizeRouteRequest;
import de.uzl.its.targets.fleet.model.Assignment;
import de.uzl.its.targets.fleet.model.Job;
import de.uzl.its.targets.fleet.model.RoutePlan;
import de.uzl.its.targets.fleet.model.Stop;
import de.uzl.its.targets.fleet.repository.RouteRepository;
import de.uzl.its.targets.fleet.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class RouteService {
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final AtomicInteger idSeq = new AtomicInteger(1);

    public RouteService(RouteRepository routeRepository, VehicleRepository vehicleRepository) {
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public RoutePlan optimize(OptimizeRouteRequest req, int timeoutSec, String solver) {
        List<Job> jobs = new ArrayList<>(req.getJobs());
        jobs.sort(Comparator.comparing(j -> j.getTimeWindow().getStart()));

        Map<String, List<Job>> assign = new HashMap<>();
        for (String v : req.getVehicles()) assign.put(v, new ArrayList<>());

        Map<String, Double> used = req.getVehicles().stream().collect(Collectors.toMap(v -> v, v -> 0.0));

        for (Job job : jobs) {
            Optional<String> vehicleOpt = req.getVehicles().stream()
                    .filter(v -> {
                        var veh = vehicleRepository.findById(v);
                        double cap = (veh != null) ? veh.getCapacityKg() : Double.MAX_VALUE;
                        return used.get(v) + job.getSizeKg() <= cap;
                    })
                    .findFirst();
            if (vehicleOpt.isPresent()) {
                String v = vehicleOpt.get();
                assign.get(v).add(job);
                used.put(v, used.get(v) + job.getSizeKg());
            } else {
                assign.computeIfAbsent("UNASSIGNED", k -> new ArrayList<>()).add(job);
            }
        }

        List<Assignment> assignments = new ArrayList<>();
        OffsetDateTime base = OffsetDateTime.now();

        for (var e : assign.entrySet()) {
            List<Stop> stops = new ArrayList<>();
            int i = 1;
            for (Job j : e.getValue()) {
                stops.add(new Stop(j.getId(), base.plusMinutes(30 * i)));
                i++;
            }
            assignments.add(new Assignment(e.getKey(), stops));
        }

        String routeId = "r-" + idSeq.getAndIncrement();
        RoutePlan plan = new RoutePlan(routeId, 0.0, assignments);
        routeRepository.save(plan);
        return plan;
    }

    public RoutePlan get(String routeId) { return routeRepository.findById(routeId); }
}
