package de.uzl.its.symbolic;

import de.uzl.its.symbolic.value.Value;

public final class InputElement extends Element {
    final String name;
    final Value<?, ?> value;

    public InputElement(String name, Value<?, ?> value) {
        this.name = name;
        this.value = value;
    }

    /** Private default constructor for serialization */
    private InputElement() {
        name = "";
        value = null;
    }

    public String getName() {
        return name;
    }

    public Value<?, ?> getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "{\"name\":\""
                + name
                + "\","
                + "\"concrete value\":\""
                + value.getConcrete()
                + "\"}";
    }
}
