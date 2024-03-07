package de.uzl.its.swat.symbolic.trace;

import de.uzl.its.swat.symbolic.value.Value;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an input element in the symbolic trace.
 */
@Getter
final class InputElement extends Element {
    // The name of the symbolic variable representing the input
    private final String name;
    // The actual value of the input after registering the input
    private final Value<?, ?> value;

    /**
     * Constructs an input element.
     * @param name The name of the symbolic variable representing the input
     * @param value The actual value of the input after registering the input
     */
    InputElement(String name, Value<?, ?> value) {
        this.name = name;
        this.value = value;
    }

    /** Private default constructor for serialization */
    private InputElement() {
        name = "";
        value = null;
    }

    /**
     * Returns a string representation of the input element.
     * @return the representation of the branch element.
     */
    @Override
    public String toString() {
        assert value != null;
        return "[Input] [" + name + ": " + value.getConcrete() + "]";

    }
}
