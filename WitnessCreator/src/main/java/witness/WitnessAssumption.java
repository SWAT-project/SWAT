package witness;

public class WitnessAssumption {

    private String scope;
    private String value;
    private final String clazz;
    private final int line;

    public WitnessAssumption(String value, String clazz, String scope, int line) {
        this.scope = scope;
        this.value = value;
        this.clazz = clazz;
        this.line = line;
    }

    public String getScope() {
        return scope;
    }

    public String getValue() {
        return value;
    }

    public String getClazz() {
        return clazz;
    }

    public int getLine() {
        return line;
    }

}