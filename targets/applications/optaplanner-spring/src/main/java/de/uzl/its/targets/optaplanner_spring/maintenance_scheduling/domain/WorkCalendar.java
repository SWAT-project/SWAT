package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain;

import java.time.LocalDate;

public class WorkCalendar {

    private Long id;

    private LocalDate fromDate; // Inclusive
    private LocalDate toDate; // Exclusive

    // No-arg constructor required for Hibernate
    public WorkCalendar() {
        this.id = IdState.getNextId();
    }

    public WorkCalendar(LocalDate fromDate, LocalDate toDate) {
        this.id = IdState.getNextId();
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    @Override
    public String toString() {
        return fromDate + " - " + toDate;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public Long getId() {
        return id;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

}
