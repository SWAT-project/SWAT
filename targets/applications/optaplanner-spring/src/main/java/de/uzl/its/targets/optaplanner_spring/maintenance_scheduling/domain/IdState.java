package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain;

public class IdState {

    private static Long currentId = 0L;

    public static Long getNextId() {
        currentId++;
        return currentId;
    }
}
