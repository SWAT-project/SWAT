package de.uzl.its.targets.optaplanner_spring.facility_location_problem.solver;

import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.Consumer;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.Facility;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.FacilityLocationConstraintConfiguration;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sumLong;

public class FacilityLocationConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                facilityCapacity(constraintFactory),
                setupCost(constraintFactory),
                distanceFromFacility(constraintFactory)
        };
    }

    Constraint facilityCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Consumer.class)
                .groupBy(Consumer::getFacility, sumLong(Consumer::getDemand))
                .filter((facility, demand) -> demand > facility.getCapacity())
                .penalizeConfigurableLong((facility, demand) -> demand - facility.getCapacity())
                .asConstraint(FacilityLocationConstraintConfiguration.FACILITY_CAPACITY);
    }

    Constraint setupCost(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Consumer.class)
                .groupBy(Consumer::getFacility)
                .penalizeConfigurableLong(Facility::getSetupCost)
                .asConstraint(FacilityLocationConstraintConfiguration.FACILITY_SETUP_COST);
    }

    Constraint distanceFromFacility(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Consumer.class)
                .filter(Consumer::isAssigned)
                .penalizeConfigurableLong(Consumer::distanceFromFacility)
                .asConstraint(FacilityLocationConstraintConfiguration.DISTANCE_FROM_FACILITY);
    }
}