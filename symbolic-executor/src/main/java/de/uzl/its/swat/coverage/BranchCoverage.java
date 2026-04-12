package de.uzl.its.swat.coverage;

import java.util.HashSet;

/** ToDo (Flo): Branch Coverage currently logs global. Move it to ThreadContext */
public final class BranchCoverage {
    public static HashSet<Long> totalBranches = new HashSet<>();
    public static HashSet<Long> visitedBranches = new HashSet<>();

    public static void addBranch(long iid) {
        totalBranches.add(iid);
    }

    public static void addVisitedBranch(long iid) {
        visitedBranches.add(iid);
    }

    public static HashSet<Long> getTotalBranches() {
        return totalBranches;
    }

    public static HashSet<Long> getVisitedBranches() {
        return visitedBranches;
    }
}
