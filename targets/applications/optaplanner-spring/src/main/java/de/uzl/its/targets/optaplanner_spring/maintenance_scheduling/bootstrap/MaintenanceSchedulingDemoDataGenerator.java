package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.bootstrap;


import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.Crew;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.Job;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.WorkCalendar;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence.CrewRepository;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence.JobRepository;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence.WorkCalendarRepository;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.solver.EndDateUpdatingVariableListener;

import java.time.LocalDate;
import java.util.*;

public class MaintenanceSchedulingDemoDataGenerator {

    private final int[] durationInDaysVariation = {1, 4, 9, 2, 3, 8, 8, 7, 3, 1};
    private int durationInDaysVariationIt = 0;

    private final int[] readyDueBetweenWorkdaysVariation = {2, 4, 2, 3, 6, 8, 7, 1, 4, 2};
    private int readyDueBetweenWorkdaysVariationIt = 0;

    private final int[] readyWorkdayOffsetVariation = {8, 2, 2, 1, 7, 2, 4, 1, 5, 9};
    private int readyWorkdayOffsetVariationIt = 0;

    private final int[] readyIdealEndBetweenWorkdaysVariation = {3, 2, 2, 1, 3, 2, 1, 1, 2, 1};
    private int readyIdealEndBetweenWorkdaysVariationIt = 0;

    WorkCalendarRepository workCalendarRepository = WorkCalendarRepository.getInstance();
    CrewRepository crewRepository = CrewRepository.getInstance();
    JobRepository jobRepository = JobRepository.getInstance();

    public void generateDemoData(int weekListSize, int crewListSize,
                                 int nbOfJobAreas, int nbOfJobTargets) {

        List<Crew> crewList = new ArrayList<>();
        for (int i = 0; i < crewListSize; i++) {
            crewList.add(new Crew("Crew" + i));
        }

        crewRepository.updateCrewList(crewList);

        LocalDate fromDate = LocalDate.of(2020, 12, 12);
        // int weekListSize = 8;
        LocalDate toDate = fromDate.plusWeeks(weekListSize);

        workCalendarRepository.setWorkCalendar(new WorkCalendar(fromDate, toDate));

        int workdayTotal = weekListSize * 5;

        // final String[] JOB_AREA_NAMES = {
        //         "Downtown", "Uptown", "Park", "Airport", "Bay", "Hill", "Forest", "Station", "Hospital",
        //         "Harbor", "Market", "Fort", "Beach", "Garden", "River", "Springs", "Tower", "Mountain"};
        String[] JOB_AREA_NAMES = new String[nbOfJobAreas];
        for (int i = 0; i < nbOfJobAreas; i++) {
            JOB_AREA_NAMES[i] = "JobArea" + i;
        }
        // final String[] JOB_TARGET_NAMES = {"Street", "Bridge", "Tunnel", "Highway", "Boulevard", "Avenue",
        //         "Square", "Plaza"};
        String[] JOB_TARGET_NAMES = new String[nbOfJobTargets];
        for (int i = 0; i < nbOfJobTargets; i++) {
            JOB_TARGET_NAMES[i] = "JobTarget" + i;
        }

        List<Job> jobList = new ArrayList<>();
        int jobListSize = weekListSize * crewList.size() * 3 / 5;
        int jobAreaTargetLimit = Math.min(JOB_TARGET_NAMES.length, crewList.size() * 2);

        for (int i = 0; i < jobListSize; i++) {

            String jobArea = JOB_AREA_NAMES[(i / jobAreaTargetLimit) % JOB_AREA_NAMES.length];
            String jobTarget = JOB_TARGET_NAMES[i % jobAreaTargetLimit];
            // 1 day to 2 workweeks (1 workweek on average)
            int durationInDays = 1 + durationInDaysVariation[durationInDaysVariationIt] % 10;
            durationInDaysVariationIt = (durationInDaysVariationIt + 1) % durationInDaysVariation.length;

            int readyDueBetweenWorkdays = durationInDays + 5 // at least 5 days of flexibility
                    + (readyDueBetweenWorkdaysVariation[readyDueBetweenWorkdaysVariationIt] % (workdayTotal - (durationInDays + 5)));
            readyDueBetweenWorkdaysVariationIt = (readyDueBetweenWorkdaysVariationIt + 1) % readyDueBetweenWorkdaysVariation.length;

            int readyWorkdayOffset = readyWorkdayOffsetVariation[readyWorkdayOffsetVariationIt] % (workdayTotal - readyDueBetweenWorkdays + 1);
            readyWorkdayOffsetVariationIt = (readyWorkdayOffsetVariationIt + 1) % readyWorkdayOffsetVariation.length;

            int readyIdealEndBetweenWorkdays = readyDueBetweenWorkdays - 1 - (readyIdealEndBetweenWorkdaysVariation[readyIdealEndBetweenWorkdaysVariationIt] % 4);
            readyIdealEndBetweenWorkdaysVariationIt = (readyIdealEndBetweenWorkdaysVariationIt  + 1) % readyIdealEndBetweenWorkdaysVariation.length;

            LocalDate readyDate = EndDateUpdatingVariableListener.calculateEndDate(fromDate, readyWorkdayOffset);
            LocalDate dueDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, readyDueBetweenWorkdays);
            LocalDate idealEndDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, readyIdealEndBetweenWorkdays);

            Set<String> tagSet;
            if (i % 10 == 3) {
                tagSet = Set.of(jobArea, "Subway");
            } else {
                tagSet = Set.of(jobArea);
            }

            jobList.add(new Job(jobArea + " " + jobTarget, durationInDays, readyDate, dueDate, idealEndDate, tagSet));
        }

        jobRepository.updateJobList(jobList);
    }

}
