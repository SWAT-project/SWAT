package de.uzl.its.swat.symbolic.value.reference.lang;

import com.google.common.base.Objects;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class FloatObjectValue extends ObjectValue<Object, Object> {

    private FloatValue floatValue;
    private FloatingPointFormulaManager fpmgr;
    private StringFormulaManager smgr;

    public FloatObjectValue(SolverContext context) {
        super(context, 100, -1);
        this.fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.floatValue = new FloatValue(context, 0);
    }

    public FloatObjectValue(SolverContext context, FloatValue floatValue, int address) {
        super(context, 100, address);
        this.floatValue = floatValue;
        this.fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
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
        if (floatValue == null) {
            return context.getFormulaManager().getBooleanFormulaManager().makeBoolean(true);
        }
        return floatValue.getBounds(upper);
    }

    public FloatValue getFloatValue() {
        return floatValue;
    }

    public static Value<?, ?> invokeStaticMethod(
            SolverContext context, String name, Value<?, ?>[] args) {
        return PlaceHolder.instance;
    }

    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) {
        return PlaceHolder.instance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FloatObjectValue that = (FloatObjectValue) o;
        return Objects.equal(floatValue, that.floatValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(floatValue);
    }

    @Override
    public String toString() {
        return "Ljava/lang/Float; @" + Integer.toHexString(address);
    }
}
