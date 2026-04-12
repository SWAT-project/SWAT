package de.uzl.its.targets.fleet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {
    private String vehicle;
    private java.util.List<Stop> stops;
}
