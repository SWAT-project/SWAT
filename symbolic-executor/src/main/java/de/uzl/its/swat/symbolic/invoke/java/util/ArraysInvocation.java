package de.uzl.its.swat.symbolic.invoke.java.util;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.BooleanArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ByteArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.IntArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.LongArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ObjectArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ShortArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BooleanObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ByteObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.LongObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ShortObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.lang.reflect.Array;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Exact models of {@code java.util.Arrays.equals} and {@code java.util.Arrays.hashCode} for arrays
 * of a concrete length whose elements SWAT encodes exactly. Everything else (float/double
 * elements, symbolic lengths, unknown elements, Object[] elements other than String, boxed
 * integers and null, Strings in a hash) returns a PlaceHolder, i.e. stays a missing invocation.
 *
 * <p>The model runs after the concrete call returned; its concrete result is computed from the
 * shadows, so a stale shadow shows up as a divergence at GETVALUE (context loss). Elements without
 * symbolic variables are taken by their concrete value, so a stale constant formula cannot be
 * relied on.
 */
public final class ArraysInvocation {

    /** Longer arrays are not modelled: each element adds a conjunct / a multiplication. */
    static final int MAX_LENGTH = 4096;

    private ArraysInvocation() {}

    public static Value<?, ?> invokeStaticMethod(String name, Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (name.equals("equals") && args.length == 2 && desc.length == 2) {
            return invokeEquals(args[0], args[1], desc[0].getDescriptor(), desc[1].getDescriptor());
        }
        if (name.equals("hashCode") && args.length == 1 && desc.length == 1) {
            return hashCode(args[0], desc[0].getDescriptor());
        }
        return PlaceHolder.instance;
    }

    private static boolean isNull(Value<?, ?> v) {
        return v instanceof ObjectValue<?, ?> o && o.isNullValue();
    }

    private static boolean symbolic(SolverContext ctx, Formula f) {
        return !ctx.getFormulaManager().extractVariables(f).isEmpty();
    }

    /** The concrete length of a primitive array shadow, or -1 if it is not known exactly. */
    private static int length(AbstractArrayValue<?, ?, ?, ?, ?> a) {
        if (a.size == null || a.size.isSymbolic() || a.concrete == null) return -1;
        int n = a.size.concrete;
        if (n < 0 || n > MAX_LENGTH || Array.getLength(a.concrete) != n) return -1;
        return n;
    }

    private static int length(ObjectArrayValue a) {
        if (a.size == null || a.size.isSymbolic() || a.getFields() == null) return -1;
        int n = a.size.concrete;
        if (n < 0 || n > MAX_LENGTH || a.getFields().length != n) return -1;
        return n;
    }

    // ---------------------------------------------------------------- equals

    private static Value<?, ?> invokeEquals(Value<?, ?> a, Value<?, ?> b, String descA, String descB) throws NotImplementedException {
        if (!descA.equals(descB)) return PlaceHolder.instance;
        SolverContext ctx;
        if (a instanceof ObjectValue<?, ?> oa) ctx = oa.context; else return PlaceHolder.instance;
        // a == b: true; a or b null: false (the references are concrete)
        if (isNull(a) || isNull(b)) return new BooleanValue(ctx, isNull(a) && isNull(b));
        if (a == b) return new BooleanValue(ctx, true);
        return switch (descA) {
            case "[I", "[J", "[S", "[B", "[C", "[Z" -> primitiveEquals(ctx, a, b, descA);
            case "[Ljava/lang/Object;" -> objectEquals(ctx, a, b);
            default -> PlaceHolder.instance;
        };
    }

    private static Value<?, ?> primitiveEquals(SolverContext ctx, Value<?, ?> a, Value<?, ?> b, String desc) {
        if (!(a instanceof AbstractArrayValue<?, ?, ?, ?, ?> x) || !(b instanceof AbstractArrayValue<?, ?, ?, ?, ?> y)
                || a.getClass() != b.getClass() || !descMatches(desc, x)) {
            return PlaceHolder.instance;
        }
        int n = length(x);
        int m = length(y);
        if (n < 0 || m < 0) return PlaceHolder.instance;
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        if (n != m) return new BooleanValue(ctx, false);
        boolean result = true;
        BooleanFormula formula = bmgr.makeTrue();
        for (int i = 0; i < n; i++) {
            BooleanValue eq = elementEquals(ctx, element(ctx, x, i), element(ctx, y, i));
            if (eq == null) return PlaceHolder.instance;
            result &= eq.concrete;
            formula = bmgr.and(formula, eq.formula);
        }
        return new BooleanValue(ctx, result, formula);
    }

    /** Element i of a primitive array, as an int (byte/short sign-, char zero-extended), long or boolean. */
    private static Value<?, ?> element(SolverContext ctx, AbstractArrayValue<?, ?, ?, ?, ?> array, int i) {
        IntValue idx = new IntValue(ctx, i);
        if (array instanceof IntArrayValue a) return constantIfConcrete(ctx, a.getElement(idx));
        if (array instanceof ByteArrayValue a) return constantIfConcrete(ctx, a.getElement(idx).asIntValue());
        if (array instanceof ShortArrayValue a) return constantIfConcrete(ctx, a.getElement(idx).asIntValue());
        if (array instanceof CharArrayValue a) return constantIfConcrete(ctx, a.getElement(idx).asIntValue());
        if (array instanceof LongArrayValue a) return constantIfConcrete(ctx, a.getElement(idx));
        if (array instanceof BooleanArrayValue a) return constantIfConcrete(ctx, a.getElement(idx));
        return null;
    }

    private static Value<?, ?> constantIfConcrete(SolverContext ctx, Value<?, ?> v) {
        if (v instanceof IntValue i && !symbolic(ctx, i.formula)) return new IntValue(ctx, i.concrete);
        if (v instanceof LongValue l && !symbolic(ctx, l.formula)) return new LongValue(ctx, l.concrete);
        if (v instanceof BooleanValue z && !symbolic(ctx, z.formula)) return new BooleanValue(ctx, z.concrete);
        return v;
    }

    /** x == y for two values of the same primitive kind (int, long or boolean), else null. */
    private static BooleanValue elementEquals(SolverContext ctx, Value<?, ?> x, Value<?, ?> y) {
        BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        if (x instanceof IntValue i && y instanceof IntValue j) {
            return new BooleanValue(ctx, i.concrete.intValue() == j.concrete.intValue(), bv.equal(i.formula, j.formula));
        }
        if (x instanceof LongValue i && y instanceof LongValue j) {
            return new BooleanValue(ctx, i.concrete.longValue() == j.concrete.longValue(), bv.equal(i.formula, j.formula));
        }
        if (x instanceof BooleanValue i && y instanceof BooleanValue j) {
            return new BooleanValue(ctx, i.concrete.booleanValue() == j.concrete.booleanValue(), bmgr.equivalence(i.formula, j.formula));
        }
        return null;
    }

    /**
     * Arrays.equals(Object[], Object[]): Objects.equals(a[i], b[i]) for every i, i.e. identity,
     * else a non-null a[i] whose equals decides. Exact for null, String and the boxed integral
     * types (their equals is false for another class); any other non-identical element is not
     * modelled.
     */
    private static Value<?, ?> objectEquals(SolverContext ctx, Value<?, ?> a, Value<?, ?> b) throws NotImplementedException {
        if (a.getClass() != ObjectArrayValue.class || b.getClass() != ObjectArrayValue.class) return PlaceHolder.instance;
        ObjectArrayValue x = (ObjectArrayValue) a;
        ObjectArrayValue y = (ObjectArrayValue) b;
        int n = length(x);
        int m = length(y);
        if (n < 0 || m < 0) return PlaceHolder.instance;
        if (n != m) return new BooleanValue(ctx, false);
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        boolean result = true;
        BooleanFormula formula = bmgr.makeTrue();
        for (int i = 0; i < n; i++) {
            Value<?, ?> u = x.getFields()[i];
            Value<?, ?> v = y.getFields()[i];
            BooleanValue eq = objectElementEquals(ctx, u, v);
            if (eq == null) return PlaceHolder.instance;
            result &= eq.concrete;
            formula = bmgr.and(formula, eq.formula);
        }
        return new BooleanValue(ctx, result, formula);
    }

    private static boolean exactlyModelled(Value<?, ?> v) {
        if (v == null) return false;
        Class<?> c = v.getClass();
        if (c == StringValue.class) return true;
        if (c == IntegerObjectValue.class || c == LongObjectValue.class || c == ShortObjectValue.class
                || c == ByteObjectValue.class || c == CharacterObjectValue.class || c == BooleanObjectValue.class) {
            return ((BoxedValue<?>) v).getVal() != null && ((BoxedValue<?>) v).getVal().concrete != null;
        }
        return false;
    }

    private static BooleanValue objectElementEquals(SolverContext ctx, Value<?, ?> u, Value<?, ?> v) throws NotImplementedException {
        if (u == null || v == null || u instanceof PlaceHolder || v instanceof PlaceHolder) return null; // unknown
        boolean uNull = isNull(u);
        boolean vNull = isNull(v);
        if (uNull || vNull) {
            // null equals only null; a non-null a[i] decides by its equals, false for null if exact
            if (!uNull && !exactlyModelled(u)) return null;
            return new BooleanValue(ctx, uNull && vNull);
        }
        if (u == v) return new BooleanValue(ctx, true);
        if (!exactlyModelled(u) || !exactlyModelled(v)) return null;
        if (u.getClass() != v.getClass()) return new BooleanValue(ctx, false); // equals of another class
        if (u instanceof StringValue s && v instanceof StringValue t) {
            // as StringValue's equals model
            if (s.concrete == null || t.concrete == null) return null;
            return new BooleanValue(ctx, s.concrete.equals(t.concrete),
                    ctx.getFormulaManager().getStringFormulaManager().equal(s.formula, t.formula));
        }
        Value<?, ?> p = ((BoxedValue<?>) u).getVal();
        Value<?, ?> q = ((BoxedValue<?>) v).getVal();
        Value<?, ?> pi = asComparable(ctx, p);
        Value<?, ?> qi = asComparable(ctx, q);
        if (pi == null || qi == null) return null;
        return elementEquals(ctx, pi, qi);
    }

    /** A boxed value as int (byte, short, char, int), long or boolean, for equality and hashing. */
    private static Value<?, ?> asComparable(SolverContext ctx, Value<?, ?> p) {
        if (p instanceof IntValue i) return constantIfConcrete(ctx, i);
        if (p instanceof ByteValue i) return constantIfConcrete(ctx, i.asIntValue());
        if (p instanceof ShortValue i) return constantIfConcrete(ctx, i.asIntValue());
        if (p instanceof CharValue i) return constantIfConcrete(ctx, i.asIntValue());
        if (p instanceof LongValue l) return constantIfConcrete(ctx, l);
        if (p instanceof BooleanValue z) return constantIfConcrete(ctx, z);
        return null;
    }

    // ---------------------------------------------------------------- hashCode

    /** Arrays.hashCode: 0 for null, else h = 31 * h + hash(e) from h = 1, in 32-bit arithmetic. */
    private static Value<?, ?> hashCode(Value<?, ?> a, String desc) throws NotImplementedException {
        if (!(a instanceof ObjectValue<?, ?> oa)) return PlaceHolder.instance;
        SolverContext ctx = oa.context;
        if (isNull(a)) return new IntValue(ctx, 0);
        Value<?, ?>[] elements;
        switch (desc) {
            case "[I", "[J", "[S", "[B", "[C", "[Z" -> {
                if (!(a instanceof AbstractArrayValue<?, ?, ?, ?, ?> x)) return PlaceHolder.instance;
                if (!descMatches(desc, x)) return PlaceHolder.instance;
                int n = length(x);
                if (n < 0) return PlaceHolder.instance;
                elements = new Value<?, ?>[n];
                for (int i = 0; i < n; i++) elements[i] = element(ctx, x, i);
            }
            case "[Ljava/lang/Object;" -> {
                if (a.getClass() != ObjectArrayValue.class) return PlaceHolder.instance;
                ObjectArrayValue x = (ObjectArrayValue) a;
                int n = length(x);
                if (n < 0) return PlaceHolder.instance;
                elements = new Value<?, ?>[n];
                for (int i = 0; i < n; i++) {
                    Value<?, ?> e = x.getFields()[i];
                    if (e == null || e instanceof PlaceHolder) return PlaceHolder.instance;
                    if (isNull(e)) {
                        elements[i] = new IntValue(ctx, 0); // null hashes to 0
                    } else if (exactlyModelled(e) && e instanceof BoxedValue<?> boxed) {
                        // Integer/Short/Byte/Character.hashCode() is the (widened) value,
                        // Long.hashCode() and Boolean.hashCode() as below. (No String: its
                        // hashCode is not modelled.)
                        elements[i] = asComparable(ctx, boxed.getVal());
                    } else {
                        return PlaceHolder.instance;
                    }
                }
            }
            default -> {
                return PlaceHolder.instance;
            }
        }
        IntValue h = new IntValue(ctx, 1);
        IntValue thirtyOne = new IntValue(ctx, 31);
        for (Value<?, ?> e : elements) {
            IntValue eh = elementHash(ctx, e);
            if (eh == null) return PlaceHolder.instance;
            h = thirtyOne.IMUL(h).IADD(eh);
        }
        return h;
    }

    private static boolean descMatches(String desc, AbstractArrayValue<?, ?, ?, ?, ?> x) {
        return switch (desc) {
            case "[I" -> x instanceof IntArrayValue;
            case "[J" -> x instanceof LongArrayValue;
            case "[S" -> x instanceof ShortArrayValue;
            case "[B" -> x instanceof ByteArrayValue;
            case "[C" -> x instanceof CharArrayValue;
            case "[Z" -> x instanceof BooleanArrayValue;
            default -> false;
        };
    }

    /** int: the value; long: (int) (v ^ (v >>> 32)); boolean: v ? 1231 : 1237. */
    private static IntValue elementHash(SolverContext ctx, Value<?, ?> e) {
        if (e instanceof IntValue i) return i;
        if (e instanceof LongValue l) return l.LXOR(l.LUSHR(new IntValue(ctx, 32))).L2I();
        if (e instanceof BooleanValue z) {
            BitvectorFormulaManager bv = ctx.getFormulaManager().getBitvectorFormulaManager();
            BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
            BitvectorFormula f = bmgr.ifThenElse(z.formula, bv.makeBitvector(32, 1231), bv.makeBitvector(32, 1237));
            return new IntValue(ctx, z.concrete ? 1231 : 1237, f);
        }
        return null;
    }
}
