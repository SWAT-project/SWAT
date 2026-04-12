package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.solver;

import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.Job;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.MaintenanceScheduleProblem;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.time.LocalDate;

public class EndDateUpdatingVariableListener implements VariableListener<MaintenanceScheduleProblem, Job> {

    @Override
    public void beforeEntityAdded(ScoreDirector<MaintenanceScheduleProblem> scoreDirector, Job job) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<MaintenanceScheduleProblem> scoreDirector, Job job) {
        updateEndDate(scoreDirector, job);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<MaintenanceScheduleProblem> scoreDirector, Job job) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<MaintenanceScheduleProblem> scoreDirector, Job job) {
        updateEndDate(scoreDirector, job);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<MaintenanceScheduleProblem> scoreDirector, Job job) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<MaintenanceScheduleProblem> scoreDirector, Job job) {
        // Do nothing
    }

    protected void updateEndDate(ScoreDirector<MaintenanceScheduleProblem> scoreDirector, Job job) {
        scoreDirector.beforeVariableChanged(job, "endDate");
        job.setEndDate(calculateEndDate(job.getStartDate(), job.getDurationInDays()));
        scoreDirector.afterVariableChanged(job, "endDate");
    }

    public static LocalDate calculateEndDate(LocalDate startDate, int durationInDays) {
        if (startDate == null) {
            return null;
        } else {
            // Skip weekends. Does not work for holidays.
            // To skip holidays too, cache all working days in scoreDirector.getWorkingSolution().getWorkCalendar().
            // Keep in sync with MaintenanceSchedule.createStartDateList().
            int weekendPadding = 2 * ((durationInDays + (startDate.getDayOfWeek().getValue() - 1)) / 5);
            return startDate.plusDays(durationInDays + weekendPadding);
        }
    }

}
