package de.uzl.its.swat.symbolic.invoke;

import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue;
import org.objectweb.asm.Type;

public class CharacterInvocation {

    public static Value<?, ?> invokeMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicStateHandler) {
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
