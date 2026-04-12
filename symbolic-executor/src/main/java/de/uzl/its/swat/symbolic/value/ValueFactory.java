package de.uzl.its.swat.symbolic.value;

import com.sap.fontus.taintaware.unified.IASString;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.*;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.metadata.ClassDepot;
import de.uzl.its.swat.metadata.ClassDepotRuntime;
import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.swat.symbolic.value.reference.LambdaObjectValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.StringBuilderValue;
import de.uzl.its.swat.symbolic.value.reference.array.*;
import de.uzl.its.swat.symbolic.value.reference.lang.*;
import de.uzl.its.swat.thread.ThreadHandler;

import java.util.*;

import org.sosy_lab.java_smt.api.SolverContext;

public class ValueFactory {
    private static final ClassDepotRuntime classDepot = ClassDepot.getRuntimeInstance();

    public static NumericalValue<?, ?> createNumericalValue(ValueType type, Object concrete)
            throws NoThreadContextException, TypeException {
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
            default -> throw new TypeException(type);
        };
    }

    public static AbstractArrayValue<?, ?, ?, ?, ?> createArrayValue(
            ValueType type, IntValue size, int address) throws NoThreadContextException, TypeException {
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
            default -> throw new TypeException(type);
        };
    }

    public static ObjectArrayValue createObjectArrayValue(String type, IntValue dimensions) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new ObjectArrayValue(context, type, dimensions);
    }

    public static IntArrayValue createIntArrayValue(int[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new IntArrayValue(context, concrete, address);
    }

    public static LongArrayValue createLongArrayValue(long[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new LongArrayValue(context, concrete, address);
    }

    public static BooleanArrayValue createBooleanArrayValue(boolean[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new BooleanArrayValue(context, concrete, address);
    }

    public static ByteArrayValue createByteArrayValue(byte[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new ByteArrayValue(context, concrete, address);
    }

    public static CharArrayValue createCharArrayValue(char[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new CharArrayValue(context, concrete, address);
    }

    public static ShortArrayValue createShortArrayValue(short[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new ShortArrayValue(context, concrete, address);
    }

    public static FloatArrayValue createFloatArrayValue(float[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new FloatArrayValue(context, concrete, address);
    }

    public static DoubleArrayValue createDoubleArrayValue(double[] concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new DoubleArrayValue(context, concrete, address);
    }

    public static StringValue createStringValue(String concrete, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new StringValue(context, concrete, address);
    }

    public static ObjectValue<?, ?> createObjectValue(Object concrete, int address)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException, NotImplementedException, TypeException, ValueConversionException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        if (concrete == null) {
            return new ObjectValue<>(context, address);
        } else if (concrete instanceof String s) {
            return new StringValue(context, s, address);
        } else if (concrete instanceof IASString s) {
            return new StringValue(context, s.getString(), address);
        } else if (concrete instanceof Boolean b) {
            return new BooleanObjectValue(context, new BooleanValue(context, b), address);
        } else if (concrete instanceof Byte b) {
            return new ByteObjectValue(context, new ByteValue(context, b), address);
        } else if (concrete instanceof Short s) {
            return new ShortObjectValue(context, new ShortValue(context, s), address);
        } else  if (concrete instanceof Long l) {
            return new LongObjectValue(context, new LongValue(context, l), address);
        } else if (concrete instanceof Integer i) {
            return new IntegerObjectValue(context, new IntValue(context, i), address);
        } else if (concrete instanceof Double d) {
            return new DoubleObjectValue(context, new DoubleValue(context, d), address);
        } else if (concrete instanceof Float f) {
            return new FloatObjectValue(context, new FloatValue(context, f), address);
        } else if (concrete instanceof Character c) {
            return new CharacterObjectValue(context, new CharValue(context, c), address);
        } else if (concrete instanceof int[] arr) {
            return createIntArrayValue(arr, address);
        } else if (concrete instanceof long[] arr) {
            return createLongArrayValue(arr, address);
        } else if (concrete instanceof boolean[] arr) {
            return createBooleanArrayValue(arr, address);
        } else if (concrete instanceof byte[] arr) {
            return createByteArrayValue(arr, address);
        } else if (concrete instanceof char[] arr) {
            return createCharArrayValue(arr, address);
        } else if (concrete instanceof short[] arr) {
            return createShortArrayValue(arr, address);
        } else if (concrete instanceof float[] arr) {
            return createFloatArrayValue(arr, address);
        } else if (concrete instanceof double[] arr) {
            return createDoubleArrayValue(arr, address);
        } else {
            Class<?> cls = concrete.getClass();
            String className = cls.getName();
            int fieldCount = classDepot.getFieldCountWithReflection(className, cls, false);
            return new ObjectValue<>(
                    context,
                    Util.formatClassName(className),
                    createNumericalValue(ValueType.intValue, fieldCount).asIntValue(), address);
        }
        // throw new RuntimeException("ERROR: Unknown type in getObjectValue");
    }

    public static ObjectValue<?, ?> createObjectValue(int nFields, String className) throws NoThreadContextException, NotImplementedException, TypeException, ValueConversionException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        List<String> classInterfacesAndParents =
                ClassDepot.getRuntimeInstance().getInterfacesForClass(className);
        classInterfacesAndParents.addAll(
                ClassDepot.getRuntimeInstance().getParentsForClass(className));

        ObjectValue<?, ?> ret;
        // ToDo, does it make sense, to use the object value constructor with "Fields"?
        if (className.equals(Util.formatClassName(Boolean.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Boolean.class.getName()))) {
            ret = new BooleanObjectValue(context, new BooleanValue(context, false), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(Byte.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Byte.class.getName()))) {
            ret = new ByteObjectValue(context, new ByteValue(context, (byte) 0), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(Integer.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Integer.class.getName()))) {
            ret = new IntegerObjectValue(context, new IntValue(context, 0), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(Short.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Short.class.getName()))) {
            ret = new ShortObjectValue(context, new ShortValue(context, (short) 0), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(Long.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Long.class.getName()))) {
            ret = new LongObjectValue(context, new LongValue(context, 0L), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(Double.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Double.class.getName()))) {
            ret = new DoubleObjectValue(context, new DoubleValue(context, 0.0d), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(Float.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Float.class.getName()))) {
            ret = new FloatObjectValue(context, new FloatValue(context, 0.0f), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(Character.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(Character.class.getName()))) {
            ret = new CharacterObjectValue(context, new CharValue(context, '\u0000'), ObjectValue.ADDRESS_UNKNOWN);
        } else if (className.equals(Util.formatClassName(StringBuilder.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(StringBuilder.class.getName()))) {
            ret = new StringBuilderValue(context, null, ObjectValue.ADDRESS_UNKNOWN); //Todo what is the correct initialization value here?
        } else if (className.equals(Util.formatClassName(String.class.getName()))
                || classInterfacesAndParents.contains(Util.formatClassName(String.class.getName()))) {
            ret = new StringValue(context, "", ObjectValue.ADDRESS_UNKNOWN); // This is not correct but _normally_ the next call should be the <init> function
        } else {
            ret =
                    new ObjectValue<>(
                            context,
                            className,
                            createNumericalValue(ValueType.intValue, nFields).asIntValue());
        }

        return ret;
    }

    public static LambdaObjectValue getLambdaObjectValue(int address, int parentAddress, long key) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new LambdaObjectValue(context, address, parentAddress, key);
    }

    public static LongObjectValue createLongObjectValue(LongValue longValue, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new LongObjectValue(context, longValue, address);
    }

    public static IntegerObjectValue createIntegerObjectValue(IntValue intValue, int address) throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new IntegerObjectValue(context, intValue, address);
    }

    public static ObjectValue<?, ?> createNULLValue() throws NoThreadContextException {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        return new ObjectValue<>(context, ObjectValue.ADDRESS_NULL);
    }
}
