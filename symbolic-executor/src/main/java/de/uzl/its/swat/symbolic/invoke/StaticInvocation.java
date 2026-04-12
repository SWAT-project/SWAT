package de.uzl.its.swat.symbolic.invoke;

import org.objectweb.asm.Type;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.invoke.java.lang.BooleanInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.ByteInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.CharacterInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.DoubleInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.FloatInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.IntegerInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.LongInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.MathInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.ShortInvocation;
import de.uzl.its.swat.symbolic.invoke.java.lang.StringInvocation;
import de.uzl.its.swat.symbolic.invoke.java.util.ObjectsInvocation;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;

public final class StaticInvocation {

    public static Value<?, ?> invokeMethod(
            String owner,
            String name,
            Type[] desc,
            Value<?, ?>[] args,
            SymbolicTraceHandler symbolicStateHandler) throws NoThreadContextException, ValueConversionException, NotImplementedException {
        Value<?, ?> ret =
                switch (owner) {
                    case "de/uzl/its/swat/Main" -> InternalInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "de/uzl/its/swat/instrument/Intrinsics" -> InternalInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "java/lang/Boolean" -> BooleanInvocation.invokeStaticMethod(
                            name, args, desc);
                    case "java/lang/Byte" -> ByteInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "java/lang/String" -> StringInvocation.invokeStaticMethod(
                            name, args, desc);
                    case "java/lang/Character" -> CharacterInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "java/lang/Integer" -> IntegerInvocation.invokeStaticMethod(
                            name, args, desc);
                    case "java/lang/Long" -> LongInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "java/lang/Math", "java/lang/StrictMath" -> MathInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "java/lang/Short" -> ShortInvocation.invokeStaticMethod(name, args, desc);
                    case "java/lang/Float" -> FloatInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "java/lang/Double" -> DoubleInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    case "java/util/Objects" -> ObjectsInvocation.invokeStaticMethod(
                            name, args, desc, symbolicStateHandler);
                    default -> PlaceHolder.instance;
                };
        return ret;
    }
}
