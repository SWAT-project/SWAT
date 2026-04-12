package de.uzl.its.targets.optaplanner_spring.facility_location_problem.persistence;


import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.FacilityLocationProblem;

import java.util.Optional;

public class FacilityLocationProblemRepository {

    private FacilityLocationProblem facilityLocationProblem;

    public Optional<FacilityLocationProblem> solution() {
        return Optional.ofNullable(facilityLocationProblem);
    }

    public void update(FacilityLocationProblem facilityLocationProblem) {
        this.facilityLocationProblem = facilityLocationProblem;
    }
}
