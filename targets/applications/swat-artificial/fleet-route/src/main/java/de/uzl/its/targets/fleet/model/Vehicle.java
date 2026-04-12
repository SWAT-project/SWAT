package de.uzl.its.targets.fleet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    private String vehicleId;
    private double capacityKg;
    private boolean refrigerated;
    private boolean hazardous;
    private Location lastKnownLocation;
}
