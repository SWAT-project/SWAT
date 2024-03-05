package de.uzl.its.symbolic.value.reference.lang;

import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import java.util.Arrays;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public class LongObjectValue extends ObjectValue<Object, Object> {

    private LongValue longValue;
    private IntegerFormulaManager imgr;
    private StringFormulaManager smgr;

    public LongValue getLongValue() {
        return longValue;
    }

    public LongObjectValue(SolverContext context) {
        super(context, 100, -1);
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.longValue = new LongValue(context, 0);
    }

    public LongObjectValue(SolverContext context, LongValue longValue, int address) {
        super(context, 100, address);
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.longValue = longValue;
    }

    /**
     * Compares if an arbitrary object is equal to this instance
     *
     * @param o The other object
     * @return Returns true if the objects are identical or if the other object is a long and has
     *     the same value as this one
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof LongObjectValue other) {
            return (longValue.equals(other.longValue));
        } else if (o instanceof LongValue other) {
            return longValue.equals(other);
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
        } else if (name.equals("getLong")) {
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
        } else if (name.equals("parseLong")) {
            return invokeStaticParseLong(context, args, smgr);
        } else if (name.equals("parseUnsignedLong")) {
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
        } else if (name.equals("toUnsignedString")) {
            throw new RuntimeException("'Integer::toUnsignedString' is not implemented yet.");
        } else if (name.equals("valueOf")) {
            return invokeStaticValueOf(context, args, smgr);
        }

        return PlaceHolder.instance;
    }

    private static LongObjectValue invokeStaticValueOf(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        if (args.length == 1) {
            if (args[0] instanceof LongValue) {
                return new LongObjectValue(
                        context,
                        new LongValue(
                                context,
                                ((LongValue) args[0]).concrete,
                                ((LongValue) args[0]).formula),
                        -1);
            } else if (args[0] instanceof StringValue) {
                LongValue longVal = invokeStaticParsePositiveString(context, args, smgr);
                return new LongObjectValue(context, longVal, -1);
            } else {
                throw new RuntimeException(
                        "'Integer::toString' called with an unexpected parameter: " + args[0]);
            }
        } else {
            throw new RuntimeException(
                    "'Integer::toString' is not implemented yet for these parameters: "
                            + args.toString());
        }
    }

    private static StringValue invokeStaticToString(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        if (args.length == 1) {
            // ToDo: Not sure this is a good idea ...
            LongValue val = (LongValue) args[0];
            if (val.concrete >= 0) {
                return new StringValue(
                        context,
                        Long.toString(val.concrete),
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

    private static LongValue invokeStaticSum(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        LongValue a = (LongValue) args[0];
        LongValue b = (LongValue) args[1];
        return new LongValue(
                context, Long.sum(a.concrete, b.concrete), imgr.add(a.formula, b.formula));
    }

    private static LongValue invokeStaticSignum(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        LongValue val = (LongValue) args[0];
        NumeralFormula.IntegerFormula zero = imgr.makeNumber(0);
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();

        BooleanFormula isZero = imgr.equal(val.formula, zero);
        BooleanFormula isPositive = imgr.greaterThan(val.formula, zero);
        NumeralFormula.IntegerFormula signum =
                bmgr.ifThenElse(
                        isZero,
                        zero,
                        bmgr.ifThenElse(isPositive, imgr.makeNumber(1), imgr.makeNumber(-1)));

        return new LongValue(context, Long.signum(val.concrete), signum);
    }

    private static LongValue invokeStaticRemainderUnsigned(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        // ToDo: Not sure this is entirely correct. The documentation says:
        //  "... where each argument and the result is interpreted as an unsigned value."
        //      So what exactly happens with negative values?
        LongValue dividend = (LongValue) args[0];
        LongValue divisor = (LongValue) args[1];
        return new LongValue(
                context,
                Long.remainderUnsigned(dividend.concrete, divisor.concrete),
                imgr.modulo(dividend.formula, divisor.formula));
    }

    private static LongValue invokeStaticParseLong(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        if (args.length == 1) {
            return invokeStaticParsePositiveString(context, args, smgr);
        } else {
            throw new RuntimeException(
                    "'Integer::parseInt' is not implemented yet for these parameters: "
                            + Arrays.toString(args));
        }
    }

    private static LongValue invokeStaticMin(SolverContext context, Value<?, ?>[] args) {
        LongValue a = (LongValue) args[0];
        LongValue b = (LongValue) args[1];

        FormulaManager fmgr = context.getFormulaManager();
        BooleanFormula cond = fmgr.getIntegerFormulaManager().lessOrEquals(a.formula, b.formula);
        NumeralFormula.IntegerFormula res =
                fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
        return new LongValue(a.context, Long.min(a.concrete, b.concrete), res);
    }

    private static LongValue invokeStaticMax(SolverContext context, Value<?, ?>[] args) {
        LongValue a = (LongValue) args[0];
        LongValue b = (LongValue) args[1];

        FormulaManager fmgr = context.getFormulaManager();
        BooleanFormula cond = fmgr.getIntegerFormulaManager().greaterOrEquals(a.formula, b.formula);
        NumeralFormula.IntegerFormula res =
                fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
        return new LongValue(a.context, Long.max(a.concrete, b.concrete), res);
    }

    private static Value invokeStaticGetSystemProperty(SolverContext context, Value<?, ?>[] args) {
        // ToDo: This is a difficult one: We can only get System Property with the concrete
        //  value of the string parameter, so the symbolic information will be mostly lost
        StringValue propertyName = (StringValue) args[0];
        Long sysPropVal = Long.getLong(propertyName.concrete);

        if (args.length == 1) {
            if (sysPropVal != null) {
                return new LongValue(context, sysPropVal);
            } else {
                return new ObjectValue(context, 0, 0);
            }
        } else if (args.length == 2) {
            if (sysPropVal != null) {
                return new LongValue(context, sysPropVal);
            } else {
                return (LongValue) args[1];
            }
        }

        return PlaceHolder.instance;
    }

    private static LongValue invokeStaticCompare(
            SolverContext context, Value<?, ?>[] args, IntegerFormulaManager imgr) {
        LongValue arg0 = (LongValue) args[0];
        LongValue arg1 = (LongValue) args[1];

        return new LongValue(
                context, arg0.concrete - arg1.concrete, imgr.subtract(arg0.formula, arg1.formula));
    }

    private static LongValue invokeStaticParsePositiveString(
            SolverContext context, Value<?, ?>[] args, StringFormulaManager smgr) {
        // ToDo: Not sure this is a good way to do this ...
        StringValue valueToDecode = (StringValue) args[0];
        if (valueToDecode.concrete.matches("^[+]?[1-9][0-9]*")) {
            return new LongValue(
                    context,
                    Long.decode(valueToDecode.concrete),
                    smgr.toIntegerFormula(valueToDecode.formula));
        } else {
            throw new RuntimeException(
                    "'Integer::decode' is not implemented for parameter value: "
                            + valueToDecode.concrete);
        }
    }

    /**
     * Responsible for invoking the methods present in the java/lang/Long class
     *
     * @param name The Name of the method that should be invoked
     * @param args The methods arguments (if any)
     * @return The resulting Value
     */
    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) {

        if (name.equals("<init>")) {
            return invokeInit(args);
        } else if (name.equals("intValue")) {
            return longValue.asIntValue();
        } else if (name.equals("longValue")) {
            return this.longValue;
        } else if (name.equals("shortValue")) {
            return longValue.asShortValue();
        } else if (name.equals("byteValue")) {
            return longValue.asByteValue();
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

    private LongValue invokeCompareTo(Value<?, ?>[] args) {
        return new LongValue(
                this.context,
                this.longValue.concrete - ((LongValue) args[0]).concrete,
                this.imgr.subtract(this.longValue.formula, ((LongValue) args[0]).formula));
    }

    private StringValue invokeToString() {
        return new StringValue(
                this.context,
                this.longValue.concrete.toString(),
                this.smgr.toStringFormula(this.longValue.formula),
                -1);
    }

    private DoubleValue invokeFloatValue() {
        return new DoubleValue(
                context,
                (double) concrete,
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(this.longValue.formula, true, FloatValue.precision));
    }

    private DoubleValue invokeDoubleValue() {
        return new DoubleValue(
                context,
                (double) concrete,
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(this.longValue.formula, true, DoubleValue.precision));
    }

    private PlaceHolder invokeInit(Value<?, ?>[] args) {
        if (args[0] instanceof LongValue) {
            this.longValue = (LongValue) args[0];
        } else if (args[0] instanceof StringValue) {
            this.longValue =
                    new LongValue(
                            this.context,
                            Integer.parseInt(((StringValue) args[0]).concrete),
                            this.smgr.toIntegerFormula(((StringValue) args[0]).formula));
        }
        return PlaceHolder.instance;
    }

    private BooleanValue invokeEquals(Value<?, ?>[] args) {
        if (args[0] instanceof LongObjectValue) {
            return new BooleanValue(
                    this.context,
                    ((LongObjectValue) args[0]).longValue.concrete == this.longValue.concrete,
                    this.imgr.equal(
                            ((LongObjectValue) args[0]).longValue.formula, this.longValue.formula));
        } else {
            return new BooleanValue(this.context, false);
        }
    }

    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        if (longValue == null) {
            throw new RuntimeException("ERROR: Cannot make uninitialized LongValue symbolic!");
        }
        return longValue.MAKE_SYMBOLIC(namePrefix);
    }

    @Override
    public String MAKE_SYMBOLIC() {
        if (longValue == null) {
            throw new RuntimeException("ERROR: Cannot make uninitialized LongValue symbolic!");
        }
        return longValue.MAKE_SYMBOLIC();
    }

    /**
     * Gets the bound of the primitive type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        if (longValue == null) {
            throw new RuntimeException("ERROR: Cannot create bound for non symbolic value!");
        }
        return longValue.getBounds(upper);
    }

    @Override
    public String toString() {
        return "Ljava/lang/Long; @" + Integer.toHexString(address);
    }
}
