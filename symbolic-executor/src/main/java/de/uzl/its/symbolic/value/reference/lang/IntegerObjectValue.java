package de.uzl.its.symbolic.value.reference.lang;

import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import java.util.Arrays;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public final class IntegerObjectValue extends ObjectValue<Object, Object> {
    private IntValue intValue;
    private IntegerFormulaManager imgr;
    private StringFormulaManager smgr;

    public IntValue getIntValue() {
        return intValue;
    }

    public IntegerObjectValue(SolverContext context) {
        super(context, 100, -1);
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.intValue = new IntValue(context, 0);
    }

    public IntegerObjectValue(SolverContext context, IntValue intValue, int address) {
        super(context, 100, address);
        this.intValue = intValue;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
    }

    /**
     * Gets the bound of the primitive type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        if (intValue == null) {
            throw new RuntimeException("ERROR: Cannot create bound for non symbolic value!");
        }
        return intValue.getBounds(upper);
    }

    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        if (intValue == null) {
            throw new RuntimeException("ERROR: Cannot make uninitialized IntValue symbolic!");
        }
        return intValue.MAKE_SYMBOLIC(namePrefix);
    }

    @Override
    public String MAKE_SYMBOLIC() {
        if (intValue == null) {
            throw new RuntimeException("ERROR: Cannot make uninitialized IntValue symbolic!");
        }
        return intValue.MAKE_SYMBOLIC();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof IntegerObjectValue) {
            IntegerObjectValue other = (IntegerObjectValue) o;
            return (intValue.equals(other.intValue));
        } else if (o instanceof IntValue) {
            IntValue otherVal = (IntValue) o;
            return intValue.equals(otherVal);
        } else {
            return false;
        }
    }

    /**
     * @param name
     * @param args
     * @return
     */
    public static Value<?, ?> invokeStaticMethod(
            SolverContext context, String name, Value<?, ?>[] args) {
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        if (name.equals("bitCount")) {
            throw new RuntimeException("'Integer::bitCount' is not implemented yet.");
        } else if (name.equals("compare")) {
            return invokeStaticCompare(context, args, imgr);
        } else if (name.equals("compareUnsigned")) {
            throw new RuntimeException("'Integer::compareUnsigned' is not implemented yet.");
        } else if (name.equals("decode")) {
            return invokeStaticParsePositiveString(context, args, smgr);
        } else if (name.equals("divideUnsigned")) {
            throw new RuntimeException("'Integer::divideUnsigned' is not implemented yet.");
        } else if (name.equals("getInteger")) {
            return invokeStaticGetSystemProperty(context, args);
        } else if (name.equals("highestOneBit")) {
            throw new RuntimeException("'Integer::highestOneBit' is not implemented yet.");
        } else if (name.equals("lowestOneBit")) {
            throw new RuntimeException("'Integer::lowestOneBit' is not implemented yet.");
        } else if (name.equals("max")) {
            return invokeStaticMax(context, args);
        } else if (name.equals("min")) {
            return invokeStaticMin(context, args);
        } else if (name.equals("numberOfLeadingZeros")) {
            throw new RuntimeException("'Integer::numberOfLeadingZeros' is not implemented yet.");
        } else if (name.equals("numberOfTrailingZeros")) {
            throw new RuntimeException("'Integer::numberOfTrailingZeros' is not implemented yet.");
        } else if (name.equals("parseInt")) {
            return invokeStaticParseInt(context, args, smgr);
        } else if (name.equals("parseUnsignedInt")) {
            throw new RuntimeException("'Integer::parseUnsignedInt' is not implemented yet.");
        } else if (name.equals("remainderUnsigned")) {
            return invokeStaticRemainderUnsigned(context, args, imgr);
        } else if (name.equals("reverse")) {
            throw new RuntimeException("'Integer::reverse' is not implemented yet.");
        } else if (name.equals("reverseBytes")) {
            throw new RuntimeException("'Integer::reverseBytes' is not implemented yet.");
        } else if (name.equals("rotateLeft")) {
            throw new RuntimeException("'Integer::rotateLeft' is not implemented yet.");
        } else if (name.equals("rotateRight")) {
            throw new RuntimeException("'Integer::rotateRight' is not implemented yet.");
        } else if (name.equals("signum")) {
            return invokeStaticSignum(context, args, imgr);
        } else if (name.equals("sum")) {
            return invokeStaticSum(context, args, imgr);
        } else if (name.equals("toBinaryString")) {
            throw new RuntimeException("'Integer::toBinaryString' is not implemented yet.");
        } else if (name.equals("toHexString")) {
            throw new RuntimeException("'Integer::toHexString' is not implemented yet.");
        } else if (name.equals("toOctalString")) {
            throw new RuntimeException("'Integer::toOctalString' is not implemented yet.");
        } else if (name.equals("toString")) {
            return invokeStaticToString(context, args, smgr);
        } else if (name.equals("toUnsignedLong")) {
            throw new RuntimeException("'Integer::toUnsignedLong' is not implemented yet.");
        } else if (name.equals("toUnsignedString")) {
            throw new RuntimeException("'Integer::toUnsignedString' is not implemented yet.");
        } else if (name.equals("valueOf")) {
            return invokeStaticValueOf(context, args, smgr);
        }

        return PlaceHolder.instance;
    }

    private static IntegerObjectValue invokeStaticValueOf(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        if (args.length == 1) {
            if (args[0] instanceof IntValue) {
                return new IntegerObjectValue(
                        context,
                        new IntValue(
                                context,
                                ((IntValue) args[0]).concrete,
                                ((IntValue) args[0]).formula),
                        -1);
            } else if (args[0] instanceof StringValue) {
                IntValue intVal = invokeStaticParsePositiveString(context, args, smgr);
                return new IntegerObjectValue(context, intVal, -1);
            } else {
                throw new RuntimeException(
                        "'Integer::toString' called with an unexpected parameter: " + args[0]);
            }
        } else {
            throw new RuntimeException(
                    "'Integer::toString' is not implemented yet for these parameters: "
                            + Arrays.toString(args));
        }
    }

    private static StringValue invokeStaticToString(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        if (args.length == 1) {
            // ToDo: Not sure this is a good idea ...
            IntValue val = (IntValue) args[0];
            if (val.concrete >= 0) {
                return new StringValue(
                        context,
                        Integer.toString(val.concrete),
                        smgr.toStringFormula(val.formula),
                        -1);
            } else {
                throw new RuntimeException(
                        "'Integer::toString' is not implemented yet for these parameter values: "
                                + val.concrete);
            }
        } else {
            throw new RuntimeException(
                    "'Integer::toString' is not implemented yet for these parameters: "
                            + Arrays.toString(args));
        }
    }

    private static IntValue invokeStaticSum(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        IntValue a = (IntValue) args[0];
        IntValue b = (IntValue) args[1];
        return new IntValue(
                context, Integer.sum(a.concrete, b.concrete), imgr.add(a.formula, b.formula));
    }

    private static IntValue invokeStaticSignum(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        IntValue val = (IntValue) args[0];
        NumeralFormula.IntegerFormula zero = imgr.makeNumber(0);
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();

        BooleanFormula isZero = imgr.equal(val.formula, zero);
        BooleanFormula isPositive = imgr.greaterThan(val.formula, zero);
        NumeralFormula.IntegerFormula signum =
                bmgr.ifThenElse(
                        isZero,
                        zero,
                        bmgr.ifThenElse(isPositive, imgr.makeNumber(1), imgr.makeNumber(-1)));

        return new IntValue(context, Integer.signum(val.concrete), signum);
    }

    private static IntValue invokeStaticRemainderUnsigned(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        // ToDo: Not sure this is entirely correct. The documentation says:
        //  "... where each argument and the result is interpreted as an unsigned value."
        //      So what exactly happens with negative values?
        IntValue dividend = (IntValue) args[0];
        IntValue divisor = (IntValue) args[1];
        return new IntValue(
                context,
                Integer.remainderUnsigned(dividend.concrete, divisor.concrete),
                imgr.modulo(dividend.formula, divisor.formula));
    }

    private static IntValue invokeStaticParseInt(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        if (args.length == 1) {
            return invokeStaticParsePositiveString(context, args, smgr);
        } else {
            throw new RuntimeException(
                    "'Integer::parseInt' is not implemented yet for these parameters: "
                            + Arrays.toString(args));
        }
    }

    private static IntValue invokeStaticMin(SolverContext context, Value<?, ?>[] args) {
        IntValue a = (IntValue) args[0];
        IntValue b = (IntValue) args[1];

        FormulaManager fmgr = context.getFormulaManager();
        BooleanFormula cond = fmgr.getIntegerFormulaManager().lessOrEquals(a.formula, b.formula);
        NumeralFormula.IntegerFormula res =
                fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
        return new IntValue(a.context, Integer.min(a.concrete, b.concrete), res);
    }

    private static IntValue invokeStaticMax(SolverContext context, Value<?, ?>[] args) {
        IntValue a = (IntValue) args[0];
        IntValue b = (IntValue) args[1];

        FormulaManager fmgr = context.getFormulaManager();
        BooleanFormula cond = fmgr.getIntegerFormulaManager().greaterOrEquals(a.formula, b.formula);
        NumeralFormula.IntegerFormula res =
                fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
        return new IntValue(a.context, Integer.max(a.concrete, b.concrete), res);
    }

    private static Value invokeStaticGetSystemProperty(SolverContext context, Value<?, ?>[] args) {
        // ToDo: This is a difficult one: We can only get System Property with the concrete
        //  value of the string parameter, so the symbolic information will be mostly lost
        StringValue propertyName = (StringValue) args[0];
        Integer sysPropVal = Integer.getInteger(propertyName.concrete);

        if (args.length == 1) {
            if (sysPropVal != null) {
                return new IntValue(context, Integer.getInteger(propertyName.concrete));
            } else {
                return new ObjectValue(context, 0, 0);
            }
        } else if (args.length == 2) {
            if (sysPropVal != null) {
                return new IntValue(context, sysPropVal);
            } else {
                return (IntValue) args[1];
            }
        }

        return PlaceHolder.instance;
    }

    private static IntValue invokeStaticCompare(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        IntValue arg0 = (IntValue) args[0];
        IntValue arg1 = (IntValue) args[1];

        return new IntValue(
                context, arg0.concrete - arg1.concrete, imgr.subtract(arg0.formula, arg1.formula));
    }

    private static IntValue invokeStaticParsePositiveString(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        // ToDo: Not sure this is a good way to do this ...
        StringValue valueToDecode = (StringValue) args[0];
        if (valueToDecode.concrete.matches("^[+]?[1-9][0-9]*")) {
            return new IntValue(
                    context,
                    Integer.decode(valueToDecode.concrete),
                    smgr.toIntegerFormula(valueToDecode.formula));
        } else {
            throw new RuntimeException(
                    "'Integer::decode' is not implemented for parameter value: "
                            + valueToDecode.concrete);
        }
    }

    /**
     * @param name
     * @param args
     * @return
     */
    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) {

        if (name.equals("<init>")) {
            return invokeInit(args);
        } else if (name.equals("intValue")) {
            // ToDo: Should we just return the reference or make a copy?
            return intValue;
        } else if (name.equals("longValue")) {
            return intValue.asLongValue();
        } else if (name.equals("shortValue")) {
            return intValue.asShortValue();
        } else if (name.equals("byteValue")) {
            return intValue.asByteValue();
        } else if (name.equals("equals")) {
            return invokeEquals(args);
        } else if (name.equals("doubleValue")) {
            return invokeDoubleValue();
        } else if (name.equals("floatValue")) {
            return invokeFloatValue();
        } else if (name.equals("toString")) {
            return invokeToString();
        } else if (name.equals("compareTo")) {
            return invokeCompareTo(args);
        }

        return PlaceHolder.instance;
    }

    private IntValue invokeCompareTo(Value<?, ?>[] args) {
        return new IntValue(
                this.context,
                this.intValue.concrete - ((IntValue) args[0]).concrete,
                this.imgr.subtract(this.intValue.formula, ((IntValue) args[0]).formula));
    }

    private StringValue invokeToString() {
        return new StringValue(
                this.context,
                this.intValue.concrete.toString(),
                this.smgr.toStringFormula(this.intValue.formula),
                -1);
    }

    private FloatValue invokeFloatValue() {
        return new FloatValue(
                context,
                (float) intValue.concrete,
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(this.intValue.formula, true, FloatValue.precision));
    }

    private DoubleValue invokeDoubleValue() {
        return new DoubleValue(
                context,
                (double) intValue.concrete,
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(this.intValue.formula, true, DoubleValue.precision));
    }

    private PlaceHolder invokeInit(Value<?, ?>[] args) {
        if (args[0] instanceof IntValue) {
            this.intValue = (IntValue) args[0];
        } else if (args[0] instanceof StringValue) {
            this.intValue =
                    new IntValue(
                            this.context,
                            Integer.parseInt(((StringValue) args[0]).concrete),
                            this.smgr.toIntegerFormula(((StringValue) args[0]).formula));
        }
        return PlaceHolder.instance;
    }

    private BooleanValue invokeEquals(Value<?, ?>[] args) {
        if (args[0] instanceof IntegerObjectValue) {
            return new BooleanValue(
                    this.context,
                    ((IntegerObjectValue) args[0]).intValue.concrete == this.intValue.concrete,
                    this.imgr.equal(
                            ((IntegerObjectValue) args[0]).intValue.formula,
                            this.intValue.formula));
        } else {
            return new BooleanValue(this.context, false);
        }
    }

    @Override
    public StringValue asStringValue() {
        // ToDo (Nils): Not validated
        return new StringValue(
                this.context,
                String.valueOf(intValue.concrete),
                context.getFormulaManager()
                        .getStringFormulaManager()
                        .toStringFormula(this.intValue.formula),
                -1);
    }

    @Override
    public String toString() {
        String s = "IntegerObjectValue @" + Integer.toHexString(address);
        return s;
    }
}
