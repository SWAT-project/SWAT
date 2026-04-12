package de.uzl.its.swat.symbolic.value.reference.lang;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import lombok.Getter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public abstract class BoxedValue<V extends NumericalValue> extends ObjectValue<Object, Object> {
    @Getter
    protected V val;

    public BoxedValue(SolverContext context, int address) {
        super(context, address);
    }

    public BoxedValue(SolverContext context, String className, IntValue nFields) {
        super(context, className, nFields);
    }

    /**
     * Converts an integral numeric formula to a string formula, handling negative numbers correctly.
     *
     * Z3's str.from_int only works for non-negative integers (returns empty string for negative values).
     * This method handles the sign manually:
     * - If value < 0: returns "-" + str.from_int(abs(value))
     * - Otherwise: returns str.from_int(value)
     *
     * @param formula The integer formula to convert to string
     * @return StringFormula representing the string representation of the integer
     */
    protected StringFormula integralToStringFormula(NumeralFormula.IntegerFormula formula) {
        BooleanFormulaManager bfm = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager ifm = context.getFormulaManager().getIntegerFormulaManager();
        StringFormulaManager sfm = context.getFormulaManager().getStringFormulaManager();

        NumeralFormula.IntegerFormula zero = ifm.makeNumber(0);
        BooleanFormula isNegative = ifm.lessThan(formula, zero);
        NumeralFormula.IntegerFormula absValue = bfm.ifThenElse(
            isNegative,
            ifm.negate(formula),
            formula
        );

        StringFormula absStr = sfm.toStringFormula(absValue);
        return bfm.ifThenElse(
            isNegative,
            sfm.concat(sfm.makeString("-"), absStr),
            absStr
        );
    }



    @Override
    public boolean isSymbolic() {
        if (val == null) return false;
        return !context.getFormulaManager().extractVariables((Formula) val.formula).isEmpty();
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() {
        if (val == null) return ImmutableSet.of();
        return context.getFormulaManager().extractVariables((Formula) val.formula).keySet();
    }

    @Override
    public String getType() {
        String[] type = val.concrete.getClass().getName().split("\\.");
        return type[type.length - 1];
    }

    @Override
    public String getConcreteEncoded() throws NotImplementedException {
        return val.getConcreteEncoded();
    }

    @Override
    public Object getConcrete() {
        return val.concrete;
    }

}
