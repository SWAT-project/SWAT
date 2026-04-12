package org.example.bootstrap;

import de.uzl.its.swat.annotations.Symbolic;
import org.example.domain.FacilityLocationProblem;
import org.example.domain.Location;
import org.example.persistence.FacilityLocationProblemRepository;

public class DemoDataGenerator {

    private final FacilityLocationProblemRepository repository;

    public DemoDataGenerator(FacilityLocationProblemRepository repository) {
        this.repository = repository;
    }

    public void generateDemoData() {

        @Symbolic int capacity = 4500;
        @Symbolic int demand = 900;
        @Symbolic int facilityCount = 30;
        @Symbolic int consumerCount = 60;
        @Symbolic double swcLongitude = 51.44;
        @Symbolic double swcLatitude = -0.16;
        @Symbolic double necLongitude = 51.56;
        @Symbolic double necLatitude = -0.01;
        @Symbolic int averageSetupConst = 50_000;
        @Symbolic int setupCostStandardDeviation = 10_000;

        FacilityLocationProblem problem = DemoDataBuilder.builder()
                .setCapacity(capacity)
                .setDemand(demand)
                .setFacilityCount(facilityCount)
                .setConsumerCount(consumerCount)
                .setSouthWestCorner(new Location(swcLongitude, swcLatitude))
                .setNorthEastCorner(new Location(necLongitude, necLatitude))
                .setAverageSetupCost(averageSetupConst)
                .setSetupCostStandardDeviation(setupCostStandardDeviation)
                .build();
        repository.update(problem);
    }
}
