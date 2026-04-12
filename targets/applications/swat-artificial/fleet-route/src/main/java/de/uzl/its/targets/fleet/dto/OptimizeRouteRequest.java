package de.uzl.its.targets.fleet.dto;

import de.uzl.its.targets.fleet.model.Job;
import lombok.Data;

import java.util.List;

@Data
public class OptimizeRouteRequest {
    private List<Job> jobs;
    private java.util.List<String> vehicles;
    private String objective; // min_time|min_cost|balanced
    private Constraints constraints;

    @Data
    public static class Constraints {
        private double maxWorkHours;
        private java.util.List<Object> avoidZones; // simplified
    }
}
