package de.uzl.its.swat.invoke;

import de.uzl.its.swat.executionData.SymbolicStateHandler;
import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.array.CharArrayValue;
import org.objectweb.asm.Type;

public class StringInvocation {

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
            return switch (desc[0].getDescriptor()) {
                case "I" -> args[0].asIntValue().asStringValue();
                case "F" -> args[0].asFloatValue().asStringValue();
                case "D" -> args[0].asDoubleValue().asStringValue();
                case "J" -> args[0].asLongValue().asStringValue();
                case "C" -> args[0].asCharValue().asStringValue();
                case "[C" -> args[0].asObjectValue()
                        .asArrayValue()
                        .asCharArrayValue()
                        .asStringValue();
                case "Z" -> args[0].asBooleanValue().asStringValue();
                default -> PlaceHolder.instance;
            };
        } else if (args.length == 3) {
            return invokeValueOf(
                    args[0].asObjectValue().asArrayValue().asCharArrayValue(),
                    args[1].asIntValue(),
                    args[2].asIntValue());
        } else {
            return PlaceHolder.instance;
        }
    }

    private static Value<?, ?> invokeValueOf(CharArrayValue data, IntValue offset, IntValue count) {
        return PlaceHolder.instance;
    }
}
