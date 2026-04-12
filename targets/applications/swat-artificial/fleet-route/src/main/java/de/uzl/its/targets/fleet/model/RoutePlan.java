package de.uzl.its.targets.fleet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlan {
    private String routeId;
    private double score;
    private List<Assignment> assignments;
}



