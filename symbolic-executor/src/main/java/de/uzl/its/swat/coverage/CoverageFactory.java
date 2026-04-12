package de.uzl.its.swat.coverage;

public class CoverageFactory {

    /**
     * This method produces a Coverage object. If different kinds of Coverage tracer exist it could
     * use a parameter or a config to decide which variant to create.
     *
     * <p>All Coverage subclasses have package scope constructors, such that no class outside the
     * package can create a Coverage object.
     *
     * @return Coverage
     */
    public InstrCoverage getCoverage(CoverageType type) {
        return switch (type) {
            case HASH -> new SimpleHashSetInstrCoverage();
            case TRACE -> new ExecutionTrace();
            case NONE -> null;
        };
    }
}
