package de.uzl.its.swat.symbolic.trace.dto;

public class InputDTO {
    private String name;
    private Object value;
    private String type;
    private String lowerBound;
    private String upperBound;

    public InputDTO(String name, String value, String type, String lowerBound, String upperBound) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public String toString() {
        return "InputDTO{"
                + "name='"
                + name
                + '\''
                + ", value="
                + value
                + ", type='"
                + type
                + '\''
                + ", lowerBound='"
                + lowerBound
                + '\''
                + ", upperBound='"
                + upperBound
                + '\''
                + '}';
    }

    /** Private default constructor for serialization */
    @SuppressWarnings("unused")
    private InputDTO() {}
}
