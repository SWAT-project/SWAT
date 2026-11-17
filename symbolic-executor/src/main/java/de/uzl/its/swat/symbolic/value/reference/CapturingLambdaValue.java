package de.uzl.its.swat.symbolic.value.reference;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * The shadow of a lambda object that {@code LambdaMetafactory} created with captured arguments
 * for an implementation method in instrumented code.
 *
 * <p>The captured values are the object's fields (the generated class stores them as {@code
 * arg$1..arg$n}), so the object is symbolic exactly when a captured value is: passing it to a
 * call SWAT does not model is a context loss, as for any object holding a symbolic value. When an
 * interface call on it runs the implementation method, the body's frame is the captured values
 * followed by the call's arguments (see {@code SymbolicInstructionProcessor.adoptLambdaFrame}).
 */
public class CapturingLambdaValue extends ObjectValue<Object, Object> {

    /** The bootstrap arguments, as instrumented: {@code [samType, implHandle, instantiatedType, ...]}. */
    private static final Pattern HANDLE =
            Pattern.compile("^(\\S+)\\.([^.(\\s]+)(\\([^)\\s]*\\)\\S+) \\((\\d+)( itf)?\\)$");

    public static final int H_INVOKEVIRTUAL = 5;
    public static final int H_INVOKESTATIC = 6;
    public static final int H_INVOKESPECIAL = 7;
    public static final int H_INVOKEINTERFACE = 9;

    /** The interface method the lambda implements. */
    public final String samName;
    public final String implOwner;
    public final String implName;
    public final String implDesc;
    public final int implTag;
    /** The interface method's type as the lambda instantiates it (generic types filled in). */
    public final Type instantiatedType;
    public final Type[] capturedTypes;
    public final Value<?, ?>[] captured;

    private CapturingLambdaValue(SolverContext context, IntValue nFields, String samName,
            String implOwner, String implName, String implDesc, int implTag, Type instantiatedType,
            Type[] capturedTypes, Value<?, ?>[] captured) {
        super(context, null, nFields, ADDRESS_UNKNOWN);
        this.samName = samName;
        this.implOwner = implOwner;
        this.implName = implName;
        this.implDesc = implDesc;
        this.implTag = implTag;
        this.instantiatedType = instantiatedType;
        this.capturedTypes = capturedTypes;
        this.captured = captured;
        Value<?, ?>[] fields = getFields();
        System.arraycopy(captured, 0, fields, 0, captured.length);
    }

    /**
     * The shadow for an {@code invokedynamic} through {@code LambdaMetafactory}, or null if this
     * is not one this class models: nothing captured, an implementation outside instrumented code
     * (e.g. a method reference to a JDK method), a handle kind other than a method call, or
     * bootstrap arguments it cannot read.
     *
     * @param samName the interface method's name (the {@code invokedynamic}'s name)
     * @param indyDesc the {@code invokedynamic}'s descriptor: captured types, then the interface
     * @param bsmArgs the bootstrap arguments as instrumented ({@code Arrays.toString})
     */
    public static CapturingLambdaValue create(SolverContext context, IntValue nFields, String samName,
            String indyDesc, String bsmArgs, Value<?, ?>[] captured) {
        Type[] capturedTypes = Type.getArgumentTypes(indyDesc);
        if (capturedTypes.length == 0 || captured.length != capturedTypes.length
                || nFields.concrete != captured.length || bsmArgs == null
                || !bsmArgs.startsWith("[") || !bsmArgs.endsWith("]")) {
            return null;
        }
        String[] parts = bsmArgs.substring(1, bsmArgs.length() - 1).split(", ");
        if (parts.length < 3) {
            return null;
        }
        Matcher m = HANDLE.matcher(parts[1]);
        if (!m.matches()) {
            return null;
        }
        int tag = Integer.parseInt(m.group(4));
        if (tag != H_INVOKEVIRTUAL && tag != H_INVOKESTATIC && tag != H_INVOKESPECIAL
                && tag != H_INVOKEINTERFACE) {
            return null;
        }
        String owner = m.group(1);
        if (!Util.shouldInstrument(owner)) {
            return null;
        }
        Type instantiated;
        try {
            instantiated = Type.getMethodType(parts[2]);
            Type.getArgumentTypes(m.group(3));
        } catch (RuntimeException e) {
            return null;
        }
        for (Value<?, ?> v : captured) {
            if (v == null) {
                return null;
            }
        }
        return new CapturingLambdaValue(context, nFields, samName, Util.formatClassName(owner),
                m.group(2), m.group(3), tag, instantiated, capturedTypes, captured.clone());
    }

    /**
     * Whether calling {@code samName} on this lambda runs {@code owner.name desc} as LambdaMetafactory
     * would without any adaptation: the implementation's parameters (with the receiver first for
     * an instance method) are exactly the captured types followed by the instantiated interface
     * parameters, and it returns exactly the instantiated return type. Adaptations (unboxing,
     * widening, a dropped return value, a receiver of a subtype) are not modelled.
     */
    public boolean runsExactly(String calledName, String owner, String name, String desc) {
        if (!samName.equals(calledName) || !implOwner.equals(Util.formatClassName(owner))
                || !implName.equals(name) || !implDesc.equals(desc)) {
            return false;
        }
        Type[] implParams = Type.getArgumentTypes(implDesc);
        Type[] instParams = instantiatedType.getArgumentTypes();
        int receiver = implTag == H_INVOKESTATIC ? 0 : 1;
        if (receiver + implParams.length != capturedTypes.length + instParams.length) {
            return false;
        }
        for (int i = 0; i < capturedTypes.length + instParams.length; i++) {
            Type expected = i < capturedTypes.length ? capturedTypes[i] : instParams[i - capturedTypes.length];
            Type actual = i < receiver ? Type.getObjectType(implOwner) : implParams[i - receiver];
            if (!expected.equals(actual)) {
                return false;
            }
        }
        return Type.getReturnType(implDesc).equals(instantiatedType.getReturnType());
    }

    @Override
    public String toString() {
        return "CapturingLambdaValue[" + this.internalID + "] @" + Integer.toHexString(address)
                + " { " + samName + " -> " + implOwner + "." + implName + implDesc + ", captured="
                + captured.length + ", className=" + className + "}";
    }
}
