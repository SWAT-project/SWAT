package de.uzl.its.targets.optaplanner_spring.controller;

import de.uzl.its.targets.optaplanner_spring.facility_location_problem.bootstrap.FacilityLocationDemoDataGenerator;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.Consumer;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.Facility;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.domain.FacilityLocationProblem;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.persistence.FacilityLocationProblemRepository;
import de.uzl.its.targets.optaplanner_spring.facility_location_problem.solver.FacilityLocationConstraintProvider;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.bootstrap.MaintenanceSchedulingDemoDataGenerator;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.Job;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.MaintenanceScheduleProblem;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence.CrewRepository;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence.JobRepository;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence.WorkCalendarRepository;
import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.solver.MaintenanceScheduleConstraintProvider;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/optaplanner")
public class OptaplannerEndpoints {

    private boolean checkMaintenanceScheduleProblemValidity(int weekListSize, int crewListSize,
                                                            int nbOfJobAreas, int nbOfJobTargets) {
        return weekListSize > 3 && weekListSize <= 13
                && crewListSize >= 3 && crewListSize <= 36
                && nbOfJobAreas >= 2 && nbOfJobAreas <= 36
                && nbOfJobTargets >= 2 && nbOfJobTargets <= 36;
    }

    @GetMapping("/maintenanceSchedulingProblem")
    public ResponseEntity<String> maintenanceSchedulingProblem(@RequestParam int weekListSize, @RequestParam int crewListSize,
                                                               @RequestParam int nbOfJobAreas, @RequestParam int nbOfJobTargets) {

        if (checkMaintenanceScheduleProblemValidity(weekListSize, crewListSize, nbOfJobAreas, nbOfJobTargets)) {
            try {
                SolverFactory<MaintenanceScheduleProblem> solverFactory = SolverFactory.create(new SolverConfig()
                        .withSolutionClass(MaintenanceScheduleProblem.class)
                        .withEntityClasses(Job.class)
                        .withConstraintProviderClass(MaintenanceScheduleConstraintProvider.class)
                        // The solver runs only for 5 seconds on this small dataset.
                        // It's recommended to run for at least 5 minutes ("5m") otherwise.
                        .withTerminationSpentLimit(Duration.ofSeconds(10)));

                MaintenanceSchedulingDemoDataGenerator generator = new MaintenanceSchedulingDemoDataGenerator();
                generator.generateDemoData(weekListSize, crewListSize, nbOfJobAreas, nbOfJobTargets);
                MaintenanceScheduleProblem maintenanceScheduleProblem = new MaintenanceScheduleProblem(
                        WorkCalendarRepository.getInstance().getWorkCalendar(),
                        CrewRepository.getInstance().getCrewList(),
                        JobRepository.getInstance().getJobList());

                System.out.println(maintenanceScheduleProblem.toString());
                long start = System.currentTimeMillis();
                MaintenanceScheduleProblem problem = solverFactory.buildSolver().solve(maintenanceScheduleProblem);
                long finish = System.currentTimeMillis();
                long timeElapsed = finish - start;
                System.out.println(problem.toString());
                System.out.println("Time: " + timeElapsed);

                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private boolean checkFacilityLocationProblemValidity(int capacity, int demand, int facilityCount,
                                 int consumerCount, double swcLongitude, double swcLatitude,
                                 double necLongitude, double necLatitude, int averageSetupCost,
                                 int setupCostStandardDeviation) {

        return capacity > 0 && capacity < 10000
                && demand > 0 && demand < 10000
                && facilityCount > 0 && facilityCount < 100
                && consumerCount > 0 && consumerCount < 1000
                && swcLongitude >= -90.00 && swcLongitude <= 90
                && swcLatitude >= -90.00 && swcLatitude <= 90
                && necLongitude >= -90.00 && necLongitude <= 90
                && necLatitude >= -90.00 && necLatitude <= 90
                && averageSetupCost > 0 && averageSetupCost < 100000
                && setupCostStandardDeviation > 0 && setupCostStandardDeviation < 100000;
    }

    @GetMapping("/facilityLocationProblem")
    public ResponseEntity<String> facilityLocationProblem(@RequestParam int capacity, @RequestParam int demand,
                                                          @RequestParam int facilityCount, @RequestParam int consumerCount,
                                                          @RequestParam double swcLongitude, @RequestParam double swcLatitude,
                                                          @RequestParam double necLongitude, @RequestParam double necLatitude,
                                                          @RequestParam int averageSetupCost, @RequestParam int setupCostStandardDeviation) {

        if (checkFacilityLocationProblemValidity(capacity, demand, facilityCount,
                consumerCount, swcLongitude, swcLatitude,
                necLongitude, necLatitude, averageSetupCost,
                setupCostStandardDeviation)) {

            try {

                SolverFactory<FacilityLocationProblem> solverFactory = SolverFactory.create(new SolverConfig()
                        .withSolutionClass(FacilityLocationProblem.class)
                        .withEntityClasses(Facility.class, Consumer.class)
                        .withConstraintProviderClass(FacilityLocationConstraintProvider.class)
                        // The solver runs only for 5 seconds on this small dataset.
                        // It's recommended to run for at least 5 minutes ("5m") otherwise.
                        .withTerminationSpentLimit(Duration.ofSeconds(10)));

                FacilityLocationProblemRepository repo = new FacilityLocationProblemRepository();
                FacilityLocationDemoDataGenerator generator = new FacilityLocationDemoDataGenerator(repo);
                generator.generateDemoData(capacity, demand, facilityCount,
                        consumerCount, swcLongitude, swcLatitude,
                        necLongitude, necLatitude, averageSetupCost,
                        setupCostStandardDeviation);
                Optional<FacilityLocationProblem> maybeSolution = repo.solution();
                maybeSolution.ifPresent(facilityLocationProblem -> {
                    System.out.println(maybeSolution.toString());
                    long start = System.currentTimeMillis();
                    FacilityLocationProblem problem = solverFactory.buildSolver().solve(facilityLocationProblem);
                    long finish = System.currentTimeMillis();
                    long timeElapsed = finish - start;
                    System.out.println(problem.toString());
                    System.out.println("Time: " + timeElapsed);
                });

                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
