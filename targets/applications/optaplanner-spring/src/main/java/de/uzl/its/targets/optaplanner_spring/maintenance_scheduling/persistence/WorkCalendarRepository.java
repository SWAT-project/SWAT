package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence;

import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.WorkCalendar;

public class WorkCalendarRepository {

    private WorkCalendar workCalendar;

    private static WorkCalendarRepository repo;
    private WorkCalendarRepository() {}

    public static WorkCalendarRepository getInstance() {
        if (repo == null) {
            repo = new WorkCalendarRepository();
        }
        return repo;
    }

    public void setWorkCalendar(WorkCalendar workCalendar) {
        this.workCalendar = workCalendar;
    }

    public WorkCalendar getWorkCalendar() {
        return workCalendar;
    }
}

