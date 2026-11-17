package de.uzl.its.swat.symbolic.value.reference;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.invoke.java.lang.StringCallBranches.Branch;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BooleanObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ByteObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.LongObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ShortObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;

/**
 * The exact entries of a {@code HashMap}, {@code LinkedHashMap}, {@code ConcurrentHashMap}, {@code
 * HashSet} or {@code LinkedHashSet} that SWAT saw created and has modelled every call on since: the key shadows
 * (symbolic or concrete) in insertion order, each with the shadow of its value (maps).
 *
 * <p>A lookup is an equality chain: the key is compared with the stored keys in order, as {@code
 * key.equals(k_j)}, up to the first one it equals. Each comparison is a branch of the library call
 * ({@link Branch}), which holds or not as the concrete values say. The stored keys are pairwise
 * different (concretely, and by the branches of their insertions on the path), so the chain fixes
 * which entry the JVM found, and the solver takes the other sides to reach the other entries.
 *
 * <p>Only keys whose {@code equals} is exact and free of side effects are compared: {@code null},
 * String, and Integer, Long, Short, Byte, Character and Boolean, where two keys of different
 * classes are never equal ({@link #supported}). Anything else ends the exact record, see {@code
 * CollectionInvocation}.
 *
 * <p>The order of the entries is the iteration order of a linked container only; that of a plain
 * hash container depends on the keys' hash codes, which are not modelled ({@link #orderUnknown}).
 */
public final class HashEntries {

    /** Lookups in larger containers are not modelled: each is a chain of that many branches. */
    public static final int MAX_ENTRIES = 64;

    /** A key with the value stored under it ({@code null} in a set). */
    public static final class Entry {
        public final Value<?, ?> key;
        public Value<?, ?> value;

        Entry(Value<?, ?> key, Value<?, ?> value) {
            this.key = key;
            this.value = value;
        }
    }

    /** Whether iteration follows insertion order (LinkedHashMap, LinkedHashSet). */
    public final boolean linked;

    /** For a linked container: whether {@link #entries} is in iteration order. */
    private boolean orderKnown = true;

    /**
     * Whether a symbolic key was stored since the container was last empty by {@code clear()}.
     * It may have shaped the table (which bin a key is in, a bin's order once it is a tree) for
     * the keys that came after it, also when it is removed again.
     */
    private boolean symbolicSinceClear = false;

    private final List<Entry> entries = new ArrayList<>();

    public HashEntries(boolean linked) {
        this.linked = linked;
    }

    public int size() {
        return entries.size();
    }

    public List<Entry> entries() {
        return entries;
    }

    public void clear() {
        entries.clear();
        orderKnown = true;
        symbolicSinceClear = false;
    }

    public void append(Value<?, ?> key, Value<?, ?> value) throws NotImplementedException {
        entries.add(new Entry(key, value));
        symbolicSinceClear |= key.isSymbolic();
    }

    public Entry remove(int index) {
        return entries.remove(index);
    }

    /** The entries were added in an order that is not the iteration order. */
    public void forgetOrder() {
        orderKnown = false;
    }

    /** The entries were added in an order that may depend on a symbolic key (a copy in hash order). */
    public void taintOrder() {
        symbolicSinceClear = true;
    }

    /**
     * Whether the iteration order may depend on a symbolic key in a way the path does not fix:
     * two or more entries in a container that does not iterate in the order recorded here and
     * that has held a symbolic key since it was last cleared.
     */
    public boolean orderUnknown() {
        return entries.size() >= 2 && (!linked || !orderKnown) && symbolicSinceClear;
    }

    /** Whether {@link #entries} is the iteration order whatever the keys are. */
    public boolean iteratesInOrder() {
        return entries.size() < 2 || (linked && orderKnown);
    }

    /**
     * The index of the entry {@code key} equals, or -1 if there is none, comparing with the first
     * {@code limit} entries. The comparisons made are added to {@code branches}.
     *
     * @return the index, -1 for no match, or -2 if a comparison is not supported
     */
    public int find(Value<?, ?> key, int limit, List<Branch> branches) {
        for (int i = 0; i < limit && i < entries.size(); i++) {
            Branch eq = equal(key, entries.get(i).key);
            if (eq == null) {
                return -2;
            }
            branches.add(eq);
            if (eq.holds()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Whether the value can be a key of an exact record: {@code null}, a String or a boxed integral
     * value (or Boolean) with a known concrete value. Float and Double are left out (their equals
     * compares bits, not values), as is every other class (its equals is unknown), and a shadow
     * whose value is not known to be the object's ({@link ObjectValue#valueUnknown}): the chain
     * compares shadows, so a wrong one would put a wrong constraint on the path.
     */
    public static boolean supported(Value<?, ?> key) {
        if (key instanceof StringValue s) {
            return !s.valueUnknown && s.concrete != null && s.formula != null && !s.isNullValue();
        }
        if (key instanceof BoxedValue<?> b) {
            return !b.valueUnknown && boxClass(b) != null && b.getVal() != null && b.getVal().concrete != null
                    && b.getVal().formula != null && !b.isNullValue();
        }
        return isNull(key);
    }

    private static boolean isNull(Value<?, ?> key) {
        return key != null && key.getClass() == ObjectValue.class && ((ObjectValue<?, ?>) key).isNullValue();
    }

    private static Class<?> boxClass(BoxedValue<?> b) {
        Class<?> c = b.getClass();
        return c == IntegerObjectValue.class || c == LongObjectValue.class || c == ShortObjectValue.class
                || c == ByteObjectValue.class || c == CharacterObjectValue.class || c == BooleanObjectValue.class
                ? c : null;
    }

    /**
     * {@code a.equals(b)} for two supported keys (also when one is null, as HashMap compares
     * them): the condition, and whether it holds concretely. {@code null} if the keys cannot be
     * compared after all.
     */
    public static Branch equal(Value<?, ?> a, Value<?, ?> b) {
        if (!supported(a) || !supported(b)) {
            return null;
        }
        SolverContext context = ((ObjectValue<?, ?>) a).context;
        FormulaManager fmgr = context.getFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        if (isNull(a) || isNull(b)) {
            boolean both = isNull(a) && isNull(b);
            return new Branch(bmgr.makeBoolean(both), both, false);
        }
        if (a instanceof StringValue x && b instanceof StringValue y) {
            return new Branch(fmgr.getStringFormulaManager().equal((StringFormula) x.formula, (StringFormula) y.formula),
                    x.concrete.equals(y.concrete), false);
        }
        if (a instanceof BoxedValue<?> x && b instanceof BoxedValue<?> y && a.getClass() == b.getClass()) {
            NumericalValue<?, ?> u = x.getVal();
            NumericalValue<?, ?> v = y.getVal();
            boolean holds = u.concrete.equals(v.concrete);
            if (a.getClass() == BooleanObjectValue.class) {
                if (!(u.formula instanceof BooleanFormula f) || !(v.formula instanceof BooleanFormula g)) {
                    return null;
                }
                return new Branch(bmgr.equivalence(f, g), holds, false);
            }
            if (!(u.formula instanceof BitvectorFormula f) || !(v.formula instanceof BitvectorFormula g)) {
                return null;
            }
            BitvectorFormulaManager bvmgr = fmgr.getBitvectorFormulaManager();
            if (bvmgr.getLength(f) != bvmgr.getLength(g)) {
                return null;
            }
            return new Branch(bvmgr.equal(f, g), holds, false);
        }
        // Keys of different classes (a String and an Integer, an Integer and a Long): never equal
        return new Branch(bmgr.makeFalse(), false, false);
    }
}
