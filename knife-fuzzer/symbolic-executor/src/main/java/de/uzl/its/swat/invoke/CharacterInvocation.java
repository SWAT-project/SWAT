package de.uzl.its.swat.invoke;

import de.uzl.its.swat.executionData.SymbolicStateHandler;
import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import de.uzl.its.symbolic.value.reference.lang.CharacterObjectValue;
import org.objectweb.asm.Type;

public class CharacterInvocation {

    public static Value<?, ?> invokeMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicStateHandler symbolicStateHandler) {
        return switch (name) {
            case "valueOf" -> invokeValueOf(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args, Type[] desc) {
        if (args.length == 1) {
            CharValue c = args[0].asCharValue();
            return new CharacterObjectValue(c.context, c, ObjectValue.ADDRESS_UNKNOWN);
        } else {
            return PlaceHolder.instance;
        }
    }
}
