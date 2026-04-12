package de.uzl.its.swat.coverage;

import java.io.PrintStream;
import java.util.HashSet;

public class SimpleHashSetInstrCoverage extends InstrCoverage {

    private final HashSet<Long> visitedInstructions = new HashSet<>();

    SimpleHashSetInstrCoverage() {}

    @Override
    public void visitInstruction(long instructionId) {
        visitedInstructions.add(instructionId);
    }

    @Override
    public void printCoverage(PrintStream printStream) {
        printStream.println("Visited Instructions: " + visitedInstructions.size());
    }

    @Override
    public HashSet<Long> getCoverage() {
        return visitedInstructions;
    }
}
