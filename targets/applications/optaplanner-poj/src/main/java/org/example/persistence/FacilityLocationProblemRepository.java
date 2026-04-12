package org.example.persistence;

import java.util.Optional;

import org.example.domain.FacilityLocationProblem;

public class FacilityLocationProblemRepository {

    private FacilityLocationProblem facilityLocationProblem;

    public Optional<FacilityLocationProblem> solution() {
        return Optional.ofNullable(facilityLocationProblem);
    }

    public void update(FacilityLocationProblem facilityLocationProblem) {
        this.facilityLocationProblem = facilityLocationProblem;
    }
}
