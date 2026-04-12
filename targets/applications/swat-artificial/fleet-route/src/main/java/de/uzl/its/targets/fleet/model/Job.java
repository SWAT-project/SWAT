package de.uzl.its.targets.fleet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private String id;
    private Location pickup;
    private Location dropoff;
    private double sizeKg;
    private TimeWindow timeWindow;
}
