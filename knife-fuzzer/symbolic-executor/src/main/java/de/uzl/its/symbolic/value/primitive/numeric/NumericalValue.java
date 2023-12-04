package de.uzl.its.symbolic.value.primitive.numeric;

import de.uzl.its.symbolic.value.Value;
import org.sosy_lab.java_smt.api.*;

/**
 * @param <T> The type of Formula used by this Value
 * @param <K> The class wrapper around the primitive datatype this value represents
 */
public abstract class NumericalValue<T extends Formula, K> extends Value<T, K> {

    /** Java-smt formula manager for handling integral formulas */
    protected IntegerFormulaManager imgr;

    /**
     * Models int overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapInteger(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Integer.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Integer.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Integer.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Integer.MIN_VALUE)));
    }

    /**
     * Models long overflow in constraints
     *
     * @param i The formula to wrapLong
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapLong(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Long.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Long.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Long.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Long.MIN_VALUE)));
    }

    /**
     * Models short overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapShort(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Short.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Short.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Short.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Short.MIN_VALUE)));
    }

    /**
     * Models short overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapByte(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Byte.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Byte.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Byte.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Byte.MIN_VALUE)));
    }

    /**
     * Models short overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapCharacter(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Character.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Character.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Character.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Character.MIN_VALUE)));
    }

    @Override
    public String toString() {
        return "NumericalValue{" + "formula=" + formula + ", concrete=" + concrete + '}';
    }
}
