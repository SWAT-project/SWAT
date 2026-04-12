package org.example.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.example.domain.Consumer;
import org.example.domain.Facility;
import org.example.domain.FacilityLocationProblem;
import org.example.domain.Location;

public class DemoDataBuilder {

    private static final AtomicLong sequence = new AtomicLong();

    private long capacity;
    private long demand;
    private int facilityCount;
    private int consumerCount;
    private long averageSetupCost;
    private long setupCostStandardDeviation;
    private Location southWestCorner;
    private Location northEastCorner;

    private DemoDataBuilder() {
    }

    public static DemoDataBuilder builder() {
        return new DemoDataBuilder();
    }

    public DemoDataBuilder setCapacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public DemoDataBuilder setDemand(long demand) {
        this.demand = demand;
        return this;
    }

    public DemoDataBuilder setFacilityCount(int facilityCount) {
        this.facilityCount = facilityCount;
        return this;
    }

    public DemoDataBuilder setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
        return this;
    }

    public DemoDataBuilder setAverageSetupCost(long averageSetupCost) {
        this.averageSetupCost = averageSetupCost;
        return this;
    }

    public DemoDataBuilder setSetupCostStandardDeviation(long setupCostStandardDeviation) {
        this.setupCostStandardDeviation = setupCostStandardDeviation;
        return this;
    }

    public DemoDataBuilder setSouthWestCorner(Location southWestCorner) {
        this.southWestCorner = southWestCorner;
        return this;
    }

    public DemoDataBuilder setNorthEastCorner(Location northEastCorner) {
        this.northEastCorner = northEastCorner;
        return this;
    }

    public FacilityLocationProblem build() {
        if (demand < 1) {
            throw new IllegalStateException("Demand (" + demand + ") must be greater than zero.");
        }
        if (capacity < 1) {
            throw new IllegalStateException("Capacity (" + capacity + ") must be greater than zero.");
        }
        if (facilityCount < 1) {
            throw new IllegalStateException("Number of facilities (" + facilityCount + ") must be greater than zero.");
        }
        if (consumerCount < 1) {
            throw new IllegalStateException("Number of consumers (" + consumerCount + ") must be greater than zero.");
        }
        if (demand > capacity) {
            throw new IllegalStateException("Overconstrained problem not supported. The total capacity ("
                    + capacity + ") must be greater than or equal to the total demand (" + demand + ").");
        }

        // TODO SW<NE
        /*
        Random random = new Random(0);
        PrimitiveIterator.OfDouble latitudes = random.doubles(southWestCorner.latitude, northEastCorner.latitude)
                .iterator();
        PrimitiveIterator.OfDouble longitudes = random.doubles(southWestCorner.longitude, northEastCorner.longitude)
                .iterator();
        Supplier<Location> locationSupplier = () -> new Location(latitudes.nextDouble(), longitudes.nextDouble());
        List<Facility> facilities = Stream.generate(locationSupplier)
                .map(location -> new Facility(
                        sequence.incrementAndGet(),
                        location,
                        averageSetupCost + (long) (setupCostStandardDeviation * random.nextGaussian()),
                        capacity / facilityCount))
                .limit(facilityCount)
                .collect(Collectors.toList());
        List<Consumer> consumers = Stream.generate(locationSupplier)
                .map(location -> new Consumer(
                        sequence.incrementAndGet(),
                        location,
                        demand / consumerCount))
                .limit(consumerCount)
                .collect(Collectors.toList());

        return new FacilityLocationProblem(facilities, consumers, southWestCorner, northEastCorner);
        */

        List<Integer> costFactors = new ArrayList<>();
        int[] baseFactors = {
                -7, 3, 10, -5, 8, -2, 6, -9, 4, -10,
                2, -8, 9, -3, 7, -6, 5, -4, 10, -1,
                6, -7, 8, -2, 3, -9, 4, -5, 9, -8,
                7, -10, 6, -3, 5, -7, 2, -6, 8, -4
        };
        for (int f : baseFactors) {
            costFactors.add(f);
        }

        double latStartF = southWestCorner.latitude;
        double latEndF   = northEastCorner.latitude;
        double lonStartF = southWestCorner.longitude;
        double lonEndF   = northEastCorner.longitude;

        List<Double> facilityLatitudes  = new ArrayList<>();
        List<Double> facilityLongitudes = new ArrayList<>();

        if (facilityCount == 1) {
            facilityLatitudes.add((latStartF + latEndF) / 2.0);
            facilityLongitudes.add((lonStartF + lonEndF) / 2.0);
        } else {
            double latStepF = (latEndF - latStartF) / (facilityCount - 1);
            double lonStepF = (lonEndF - lonStartF) / (facilityCount - 1);
            for (int i = 0; i < facilityCount; i++) {
                facilityLatitudes.add(latStartF + i * latStepF);
                facilityLongitudes.add(lonStartF + i * lonStepF);
            }
        }

        double latStartC = southWestCorner.latitude;
        double latEndC   = northEastCorner.latitude;
        double lonStartC = southWestCorner.longitude;
        double lonEndC   = northEastCorner.longitude;

        List<Double> consumerLatitudes  = new ArrayList<>();
        List<Double> consumerLongitudes = new ArrayList<>();

        if (consumerCount == 1) {
            consumerLatitudes.add((latStartC + latEndC) / 2.0 + (latEndC - latStartC) / 5.0);
            consumerLongitudes.add((lonStartC + lonEndC) / 2.0 +  (lonEndC - lonStartC) / 5.0);
        } else {
            double latStepC = (latEndC - latStartC) / (consumerCount - 1);
            double lonStepC = (lonEndC - lonStartC) / (consumerCount - 1);
            int sign = 1;
            for (int i = 0; i < consumerCount; i++) {
                consumerLatitudes.add(latStartC + i * latStepC + (sign * (latEndC - latStartC) / (2 * (consumerCount - 1))));
                consumerLongitudes.add(lonStartC + i * lonStepC + (-sign * (latEndC - latStartC) / (3 * (consumerCount - 1))));
                sign *= -1;
            }
        }

        List<Facility> facilities = new ArrayList<>();
        long perFacilityCapacity = capacity / facilityCount;

        for (int i = 0; i < facilityCount; i++) {
            long id = sequence.incrementAndGet();
            long setupCost = averageSetupCost + (costFactors.get(i % costFactors.size()) * (averageSetupCost / 10));
            Location location = new Location(facilityLatitudes.get(i), facilityLongitudes.get(i));

            facilities.add(new Facility(
                    id,
                    location,
                    setupCost,
                    perFacilityCapacity
            ));
        }

        List<Consumer> consumers = new ArrayList<>();
        long perConsumerDemand = consumerCount > 0 ? (demand / consumerCount) : 0;

        for (int i = 0; i < consumerCount; i++) {
            long id = sequence.incrementAndGet();
            Location location = new Location(consumerLatitudes.get(i), consumerLongitudes.get(i));

            consumers.add(new Consumer(
                    id,
                    location,
                    perConsumerDemand
            ));
        }

        return new FacilityLocationProblem(facilities, consumers, southWestCorner, northEastCorner);
    }
}
