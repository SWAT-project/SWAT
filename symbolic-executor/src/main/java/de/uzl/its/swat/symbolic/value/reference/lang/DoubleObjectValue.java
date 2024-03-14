package de.uzl.its.swat.symbolic.value.reference.lang;

import com.google.common.base.Objects;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class DoubleObjectValue extends ObjectValue<Object, Object> {

    private DoubleValue doubleValue;
    private FloatingPointFormulaManager fpmgr;
    private StringFormulaManager smgr;

    public DoubleObjectValue(SolverContext context) {
        super(context, 100, -1);
        this.fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.doubleValue = new DoubleValue(context, 0);
    }

    public DoubleObjectValue(SolverContext context, DoubleValue floatValue, int address) {
        super(context, 100, address);
        this.doubleValue = floatValue;
        this.fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
    }

    public DoubleValue getDoubleValue() {
        return doubleValue;
    }

    /**
     * Gets the bound of the primitive type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        if (doubleValue == null) {
            throw new RuntimeException("ERROR: Cannot create bound for non symbolic value!");
        }
        return doubleValue.getBounds(upper);
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
        DoubleObjectValue that = (DoubleObjectValue) o;
        return Objects.equal(doubleValue, that.doubleValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(doubleValue);
    }

    @Override
    public String toString() {
        return "Ljava/lang/Double; @" + Integer.toHexString(address);
    }
}
