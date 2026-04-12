package de.uzl.its.swat.coverage;

import de.uzl.its.swat.config.Config;
import java.io.PrintStream;
import java.util.HashSet;

public abstract class InstrCoverage {
    public static int numInstructions = 0;
    private static final Config config = Config.instance();

    InstrCoverage() {}

    public abstract void visitInstruction(long instructionId);

    public abstract void printCoverage(PrintStream ps);

    public abstract HashSet<Long> getCoverage();
}
