package de.uzl.its.swat.invoke;

import static de.uzl.its.symbolic.value.reference.ObjectValue.ADDRESS_UNKNOWN;

import de.uzl.its.swat.executionData.SymbolicStateHandler;
import de.uzl.its.swat.interpreters.ValueFactory;
import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.LongValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class StaticInvocation {

    public static Value<?, ?> invokeMethod(
            String owner,
            String name,
            Type[] desc,
            Value<?, ?>[] args,
            SymbolicStateHandler symbolicStateHandler) {
        if (owner.equals("de/uzl/its/swat/Main")) {
            return InternalInvocation.invokeMethod(name, args, desc, symbolicStateHandler);
        } else if (owner.equals("java/lang/String")) {
            return StringInvocation.invokeMethod(name, args, desc, symbolicStateHandler);
        } else if (owner.equals("java/lang/Character")) {
            return CharacterInvocation.invokeMethod(name, args, desc, symbolicStateHandler);
        } else if (owner.equals("java/lang/Integer") && name.equals("valueOf")) {
            if (args[0] instanceof IntValue intValue) {
                return ValueFactory.createIntegerObjectValue(intValue, ADDRESS_UNKNOWN);
            }
        } else if (owner.equals("java/lang/Long") && name.equals("valueOf")) {
            if (args[0] instanceof LongValue longValue) {
                return ValueFactory.createLongObjectValue(longValue, ADDRESS_UNKNOWN);
            }

        } else if (owner.equals("java/lang/invoke/LambdaMetafactory")) {
            throw new RuntimeException("Unexpected case!");
        } else if (owner.equals("java/lang/Math") && name.equals("max") && args.length == 2) {
            if (args[0] instanceof IntValue a && args[1] instanceof IntValue b) {
                return invokeMax(a, b);
            }
            if (args[0] instanceof DoubleValue a && args[1] instanceof DoubleValue b) {
                return invokeMax(a, b);
            }
        } else if (owner.equals("java/lang/Math") && name.equals("min") && args.length == 2) {
            if (args[0] instanceof IntValue a && args[1] instanceof IntValue b) {
                return a.concrete < b.concrete ? a : b;
            }
        }
        return PlaceHolder.instance;
    }

    private static IntValue invokeMax(IntValue a, IntValue b) {
        FormulaManager fmgr = a.context.getFormulaManager();
        BooleanFormula cond = fmgr.getIntegerFormulaManager().greaterOrEquals(a.formula, b.formula);
        NumeralFormula.IntegerFormula res =
                fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
        return new IntValue(a.context, Math.max(a.concrete, b.concrete), res);
    }

    private static DoubleValue invokeMax(DoubleValue a, DoubleValue b) {
        FormulaManager fmgr = a.context.getFormulaManager();
        BooleanFormula cond =
                fmgr.getFloatingPointFormulaManager().greaterOrEquals(a.formula, b.formula);
        FloatingPointFormula res =
                fmgr.getBooleanFormulaManager().ifThenElse(cond, a.formula, b.formula);
        return new DoubleValue(a.context, Math.max(a.concrete, b.concrete), res);
    }
}
