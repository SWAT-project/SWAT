package de.uzl.its.targets.fleet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stop {
    private String job;
    private java.time.OffsetDateTime eta;
}
