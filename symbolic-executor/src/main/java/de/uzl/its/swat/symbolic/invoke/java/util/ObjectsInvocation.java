package de.uzl.its.swat.symbolic.invoke.java.util;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue;
import org.objectweb.asm.Type;

public class ObjectsInvocation {

    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] desc,
            SymbolicTraceHandler symbolicStateHandler) throws NotImplementedException, ValueConversionException, NoThreadContextException {
        return switch (name) {
            case "equals" -> invokeEquals(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    // return (a == b) || (a != null && a.equals(b));
    private static Value<?, ?> invokeEquals(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException, NoThreadContextException {
        SWATAssert.enforce(args.length == 2, "Wrong argument counts");
        ObjectValue<?,?> a = args[0].asObjectValue();
        ObjectValue<?,?> b = args[1].asObjectValue();

        if(a.getAddress() == ObjectValue.ADDRESS_NULL || b.getAddress() == ObjectValue.ADDRESS_NULL) {
            return PlaceHolder.instance;
        }
        return a.invokeMethod("equals", null, new Value[]{b} );
    }
}
