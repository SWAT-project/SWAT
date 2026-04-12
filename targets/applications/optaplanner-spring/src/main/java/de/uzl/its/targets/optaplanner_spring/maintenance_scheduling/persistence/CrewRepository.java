package de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.persistence;


import de.uzl.its.targets.optaplanner_spring.maintenance_scheduling.domain.Crew;

import java.util.ArrayList;
import java.util.List;

public class CrewRepository {

    private List<Crew> crewList = new ArrayList<>();

    private static CrewRepository repo;
    private CrewRepository() {}

    public static CrewRepository getInstance() {
        if (repo == null) {
            repo = new CrewRepository();
        }
        return repo;
    }

    public List<Crew> getCrewList() {
        return crewList;
    }

    public void updateCrewList(List<Crew> crewList) {
        this.crewList = crewList;
    }
}
