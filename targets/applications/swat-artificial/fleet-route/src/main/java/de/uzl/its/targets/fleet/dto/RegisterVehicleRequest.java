package de.uzl.its.targets.fleet.dto;

import de.uzl.its.targets.fleet.model.Location;
import lombok.Data;

@Data
public class RegisterVehicleRequest {
    private String vehicleId;
    private double capacityKg;
    private boolean refrigerated;
    private boolean hazardous;
    private Location lastKnownLocation;
}
