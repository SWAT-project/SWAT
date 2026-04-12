package de.uzl.its.swat.coverage;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

public class ExecutionTrace extends InstrCoverage {

    private final ArrayList<Long> visitedInstructions = new ArrayList<>();

    ExecutionTrace() {}

    @Override
    public void visitInstruction(long instructionId) {
        visitedInstructions.add(instructionId);
    }

    @Override
    public void printCoverage(PrintStream printStream) {
        for (long iid : visitedInstructions) {
            printStream.println(iid);
        }
    }

    public ArrayList<Long> getVisitedInstructions() {
        return visitedInstructions;
    }

    @Override
    public HashSet<Long> getCoverage() {
        return new HashSet<>(visitedInstructions);
    }
}
