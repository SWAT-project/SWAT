package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain;

import org.optaplanner.core.api.domain.lookup.PlanningId;

public class Crew {

    @PlanningId
    private Long id;

    private String name;

    // No-arg constructor required for Hibernate
    public Crew() {
    }

    public Crew(String name) {
        this.id = IdState.getNextId();
        this.name = name;
    }

    public Crew(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
