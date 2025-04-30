package witness;

public class WitnessEdge {

    private final WitnessNode source;

    private final WitnessNode dest;

    private final WitnessAssumption witness;

    private final String assumption;

    public WitnessEdge(WitnessNode source, WitnessAssumption witness, String assumption, WitnessNode dest) {
        this.source = source;
        this.dest = dest;
        this.witness = witness;
        this.assumption  = assumption;
    }

    public WitnessNode getSource() {
        return source;
    }

    public WitnessNode getDest() {
        return dest;
    }

    public WitnessAssumption getWitness() {
        return witness;
    }

    public String getAssumption() {
        return assumption;
    }
}