package de.uzl.its.targets.fleet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeWindow {
    private OffsetDateTime start;
    private OffsetDateTime end;
}
