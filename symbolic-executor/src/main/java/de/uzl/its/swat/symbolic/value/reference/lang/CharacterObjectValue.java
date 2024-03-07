package de.uzl.its.swat.symbolic.value.reference.lang;

import com.google.common.base.Objects;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public class CharacterObjectValue extends ObjectValue<Object, Object> {

    private CharValue charValue;
    private IntegerFormulaManager imgr;
    private StringFormulaManager smgr;

    public CharacterObjectValue(SolverContext context) {
        super(context, 100, -1);
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.charValue = new CharValue(context, '\u0000');
    }

    public CharacterObjectValue(SolverContext context, CharValue charValue, int address) {
        super(context, 100, address);
        this.charValue = charValue;
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
        if (charValue == null) {
            return context.getFormulaManager().getBooleanFormulaManager().makeBoolean(true);
        }
        return charValue.getBounds(upper);
    }

    public CharValue getCharValue() {
        return charValue;
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
        CharacterObjectValue c = (CharacterObjectValue) o;
        return Objects.equal(charValue, c.charValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(charValue);
    }

    @Override
    public String toString() {
        return "Ljava/lang/Character; @" + Integer.toHexString(address);
    }
}
