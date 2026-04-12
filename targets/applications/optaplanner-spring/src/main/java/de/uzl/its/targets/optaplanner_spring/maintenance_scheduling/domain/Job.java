package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain;

import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.solver.EndDateUpdatingVariableListener;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import java.time.LocalDate;
import java.util.Set;

@PlanningEntity
public class Job {

    private Long id;

    private String name;
    private int durationInDays;
    private LocalDate readyDate; // Inclusive
    private LocalDate dueDate; // Exclusive
    private LocalDate idealEndDate; // Exclusive

    private Set<String> tagSet;

    @PlanningVariable
    private Crew crew;
    // Follows the TimeGrain Design Pattern
    @PlanningVariable
    private LocalDate startDate; // Inclusive
    @ShadowVariable(variableListenerClass = EndDateUpdatingVariableListener.class, sourceVariableName = "startDate")
    private LocalDate endDate; // Exclusive

    // No-arg constructor required for Hibernate and OptaPlanner
    public Job() {
    }

    public Job(String name, int durationInDays, LocalDate readyDate, LocalDate dueDate, LocalDate idealEndDate, Set<String> tagSet) {
        this.id = IdState.getNextId();
        this.name = name;
        this.durationInDays = durationInDays;
        this.readyDate = readyDate;
        this.dueDate = dueDate;
        this.idealEndDate = idealEndDate;
        this.tagSet = tagSet;
    }

    public Job(Long id, String name, int durationInDays, LocalDate readyDate, LocalDate dueDate, LocalDate idealEndDate, Set<String> tagSet,
            Crew crew, LocalDate startDate) {
        this.id = id;
        this.name = name;
        this.durationInDays = durationInDays;
        this.readyDate = readyDate;
        this.dueDate = dueDate;
        this.idealEndDate = idealEndDate;
        this.tagSet = tagSet;
        this.crew = crew;
        this.startDate = startDate;
        this.endDate = EndDateUpdatingVariableListener.calculateEndDate(startDate, durationInDays);
    }

    @Override
    public String toString() {
        return name + "(" + id + ")";
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    @PlanningId
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDurationInDays() {
        return durationInDays;
    }

    public LocalDate getReadyDate() {
        return readyDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getIdealEndDate() {
        return idealEndDate;
    }

    public Set<String> getTagSet() {
        return tagSet;
    }

    public Crew getCrew() {
        return crew;
    }

    public void setCrew(Crew crew) {
        this.crew = crew;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
