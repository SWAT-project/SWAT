package de.uzl.its.targets.optaplanner_spring.facility_location_problem.bootstrap;

import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.FacilityLocationProblem;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.Location;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.persistence.FacilityLocationProblemRepository;

public class FacilityLocationDemoDataGenerator {

    private final FacilityLocationProblemRepository repository;

    public FacilityLocationDemoDataGenerator(FacilityLocationProblemRepository repository) {
        this.repository = repository;
    }

    public void generateDemoData(int capacity, int demand, int facilityCount,
                                 int consumerCount, double swcLongitude, double swcLatitude,
                                 double necLongitude, double necLatitude, int averageSetupCost,
                                 int setupCostStandardDeviation) {



        FacilityLocationProblem problem = FacilityLocationDemoDataBuilder.builder()
                .setCapacity(capacity)
                .setDemand(demand)
                .setFacilityCount(facilityCount)
                .setConsumerCount(consumerCount)
                .setSouthWestCorner(new Location(swcLongitude, swcLatitude))
                .setNorthEastCorner(new Location(necLongitude, necLatitude))
                .setAverageSetupCost(averageSetupCost)
                .setSetupCostStandardDeviation(setupCostStandardDeviation)
                .build();
        repository.update(problem);
    }
}
