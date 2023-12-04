package de.uzl.its.swat.interpreters;

import de.uzl.its.swat.thread.ThreadHandler;
import de.uzl.its.symbolic.value.ValueType;
import de.uzl.its.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.symbolic.value.reference.LambdaObjectValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import de.uzl.its.symbolic.value.reference.StringBuilderValue;
import de.uzl.its.symbolic.value.reference.array.*;
import de.uzl.its.symbolic.value.reference.lang.*;
import de.uzl.its.symbolic.value.reference.util.ListValue;
import java.util.List;
import org.sosy_lab.java_smt.api.SolverContext;

public class ValueFactory {

    public static NumericalValue<?, ?> createNumericalValue(ValueType type, Object concrete) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return switch (type) {
            case intValue -> new IntValue(context, (int) concrete);
            case doubleValue -> new DoubleValue(context, (double) concrete);
            case floatValue -> new FloatValue(context, (float) concrete);
            case longValue -> new LongValue(context, (long) concrete);
            case charValue -> new CharValue(context, (char) concrete);
            case booleanValue -> new BooleanValue(context, (boolean) concrete);
            case shortValue -> new ShortValue(context, (short) concrete);
            case byteValue -> new ByteValue(context, (byte) concrete);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public static AbstractArrayValue<?, ?, ?, ?, ?> createArrayValue(
            ValueType type, IntValue size, int address) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return switch (type) {
            case intValue -> new IntArrayValue(context, size, address);
            case doubleValue -> new DoubleArrayValue(context, size, address);
            case longValue -> new LongArrayValue(context, size, address);
            case charValue -> new CharArrayValue(context, size, address);
            case byteValue -> new ByteArrayValue(context, size, address);
            case floatValue -> new FloatArrayValue(context, size, address);
            case booleanValue -> new BooleanArrayValue(context, size, address);
            case shortValue -> new ShortArrayValue(context, size, address);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public static ObjectArrayValue createObjectArrayValue(String type, IntValue dimensions) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new ObjectArrayValue(context, type, dimensions);
    }

    public static StringValue createStringValue(String concrete, int address) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new StringValue(context, concrete, address);
    }

    public static ObjectValue<?, ?> createObjectValue(Object concrete, int address) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        if (concrete == null) {
            return new ObjectValue<>(context, -1, address);
        }
        if (concrete instanceof String s) {
            return new StringValue(context, s, address);
        } else if (concrete instanceof Long l) {
            return new LongObjectValue(context, new LongValue(context, l), address);
        } else if (concrete instanceof Integer i) {
            return new IntegerObjectValue(context, new IntValue(context, i), address);
        } else if (concrete instanceof Double d) {
            return new DoubleObjectValue(context, new DoubleValue(context, d), address);
        } else if (concrete instanceof Float f) {
            return new FloatObjectValue(context, new FloatValue(context, f), address);
        } else if (concrete instanceof Character c) {
            return new CharacterObjectValue(context, new CharValue(context, c), address);
        } else if (concrete instanceof List<?> list) {
            return new ListValue(context, list);
        }
        throw new RuntimeException("ERROR: Unknown type in getObjectValue");
    }

    public static ObjectValue<?, ?> createObjectValue(int nFields, String className) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return switch (className) {
            case "java.lang.Integer" -> new IntegerObjectValue(context);
            case "java.lang.Long" -> new LongObjectValue(context);
            case "java.lang.StringBuilder" -> new StringBuilderValue(context);
            case "java.util.List", "java.util.ArrayList" -> new ListValue(context);
            default -> new ObjectValue<>(
                    context,
                    className,
                    createNumericalValue(ValueType.intValue, nFields).asIntValue());
        };
    }

    public static LambdaObjectValue getLambdaObjectValue(int address, int parentAddress, int key) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new LambdaObjectValue(context, address, parentAddress, key);
    }

    public static LongObjectValue createLongObjectValue(LongValue longValue, int address) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new LongObjectValue(context, longValue, address);
    }

    public static IntegerObjectValue createIntegerObjectValue(IntValue intValue, int address) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new IntegerObjectValue(context, intValue, address);
    }

    public static ObjectValue<?, ?> createNULLValue() {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new ObjectValue<>(context, 0, 0);
    }
}
