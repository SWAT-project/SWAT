package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence;

import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.Job;

import java.util.ArrayList;
import java.util.List;

public class JobRepository {

    private List<Job> jobList = new ArrayList<>();

    private static JobRepository repo;

    private JobRepository() {}

    public static JobRepository getInstance() {
        if (repo == null) {
            repo = new JobRepository();
        }
        return repo;
    }

    public List<Job> getJobList() {
        return jobList;
    }

    public void updateJobList(List<Job> jobList) {
        this.jobList = jobList;
    }
}
