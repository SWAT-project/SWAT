package de.uzl.its.swat.instrument;

import java.util.HashMap;
import java.util.Map;

/** Represents the different data types that can be encountered in method descriptors. */
public enum DataType {
    INTEGER_TYPE("I"),
    BOOLEAN_TYPE("Z"),
    CHAR_TYPE("C"),
    BYTE_TYPE("B"),
    SHORT_TYPE("S"),
    FLOAT_TYPE("F"),
    LONG_TYPE("J"),
    DOUBLE_TYPE("D"),
    VOID_TYPE("V"),
    OBJECT_TYPE("L"),
    ARRAY_TYPE("["),
    METHOD_TYPE("("),
    UNKNOWN_TYPE("UNKNOWN_TYPE"),
    STRING_TYPE("Ljava/lang/String");

    private static final Map<String, DataType> identifierToDataType = new HashMap<>();

    static {
        for (DataType type : values()) {
            identifierToDataType.put(type.identifier, type);
        }
    }

    private final String identifier;

    /**
     * Constructs a new DataType with the given identifier.
     *
     * @param identifier the identifier for this data type
     */
    DataType(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the identifier for this DataType.
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Determines the DataType corresponding to a given identifier.
     *
     * @param identifier the identifier to look up
     * @return the corresponding DataType, or UNKNOWN_TYPE if not found
     */
    public static DataType getDataType(String identifier) {
        identifier = identifier.replace(";", "");

        DataType dataType = identifierToDataType.get(identifier);
        if (dataType != null) {
            return dataType;
        }

        if (identifier.startsWith("L")) {
            return OBJECT_TYPE;
        }

        if (identifier.startsWith("[")) {
            return ARRAY_TYPE;
        }

        if (identifier.startsWith("(")) {
            // ToDo: Can this case ever happen?
            return METHOD_TYPE;
        }

        return UNKNOWN_TYPE;
    }
}
