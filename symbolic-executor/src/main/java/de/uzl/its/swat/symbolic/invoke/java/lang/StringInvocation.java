package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.objectweb.asm.Type;

public class StringInvocation {

    public static Value<?, ?> invokeStaticMethod(String name, Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "valueOf" -> invokeValueOf(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    private static Value<?, ?> invokeValueOf(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
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
                case "Ljava/lang/Object;" -> invokeValueOfObject(args[0]);
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

    /**
     * String.valueOf(Object) is obj.toString() for a non-null obj, which is modelled for strings
     * and boxed primitives (a boxed toString that is not modelled stays a PlaceHolder).
     */
    private static Value<?, ?> invokeValueOfObject(Value<?, ?> obj) throws NotImplementedException, ValueConversionException {
        if (obj instanceof StringValue str) return str;
        if (obj instanceof BoxedValue<?> boxed) return boxed.invokeMethod("toString", new Type[0], new Value<?, ?>[0]);
        return PlaceHolder.instance;
    }

    private static Value<?, ?> invokeValueOf(CharArrayValue data, IntValue offset, IntValue count) {
        return PlaceHolder.instance;
    }
}
