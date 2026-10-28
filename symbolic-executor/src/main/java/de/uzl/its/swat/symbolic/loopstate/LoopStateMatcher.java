package de.uzl.its.swat.symbolic.loopstate;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.symbolic.instruction.LOOP_HEAD;
import de.uzl.its.swat.symbolic.shadow.Frame;
import de.uzl.its.swat.symbolic.shadow.ShadowContext;
import de.uzl.its.swat.symbolic.value.Value;
import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.logging.GlobalLogger;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hashes the program state at a loop head.
 *
 * <p>The state is everything the rest of the execution depends on, apart from the inputs still to
 * be read: the loop head, the locals defined there, the static fields of the initialized
 * application classes, the set of those classes, and the heap reachable from all of these. The
 * hash is only computed when the state is fully known and fully concrete; otherwise there is none:
 *
 * <ul>
 *   <li>the head's method is the only application frame on the stack (the frames below it run no
 *       application code after it returns) and the target runs no other thread;
 *   <li>no shadow value of the head's locals, of another shadow frame, of a static field or of the
 *       shadow heap is symbolic. A concrete state is then the same on every execution that takes
 *       the same branches up to the head, whatever its inputs;
 *   <li>the reachable heap consists of application objects, enum constants of application enums,
 *       arrays and plain {@code Object}s only. Strings, boxed values and library objects are not
 *       hashed, since part of their identity (interning, caches) or content lies outside the
 *       reachable heap.
 * </ul>
 *
 * The heap is encoded as a graph, by the order in which a depth-first walk from the roots meets
 * its objects, so two states have the same encoding iff their reachable heaps are isomorphic with
 * equal contents. Floating-point values are encoded by their bits.
 *
 * <p>Why a branch that follows a repeated state need not be flipped (the explorer side): order the
 * executions by the number of branches, then by their branch outcomes (lexicographically), then by
 * the number of loop-head states they pass. Take a smallest execution that violates the property
 * and suppose it passes, before some branch, a state S that some executed path reached earlier in
 * this order. The executed prefix up to S, followed by the rest of the violating execution after
 * S, is an execution too (the state is the same, and the inputs read afterwards are fresh), it
 * violates the property as well, and it is smaller. So a smallest violating execution never passes
 * a state that was reached earlier in this order, and the search still finds it with those
 * branches left unflipped.
 */
public final class LoopStateMatcher {

    /** Larger reachable heaps are not hashed. */
    private static final int MAX_OBJECTS = 20_000;

    private static final String SWAT_PACKAGE = "de/uzl/its/swat/";

    /**
     * Time one run may spend on hashing. Leaving out a state is always sound (no branch is left
     * out because of it), so once the budget is used up the run reports no more states.
     */
    private static final long BUDGET_NANOS = 1_000_000_000L;

    private static long spentNanos = 0;

    /** The states this run reported. */
    private static final java.util.Set<String> seen = new java.util.HashSet<>();

    /** Whether the run reports no more states: the budget is used up or a state repeated. */
    private static boolean stopped = false;

    private LoopStateMatcher() {}

    /** Thrown when the state cannot be hashed. */
    private static final class Unhashable extends Exception {
        Unhashable() {
            super(null, null, false, false);
        }
    }

    private static final Unhashable UNHASHABLE = new Unhashable();

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    /** Logs why a state is not hashed. */
    private static Unhashable unhashable(String why) {
        logger.debug("[LOOP STATE] Not hashed: {}", why);
        return UNHASHABLE;
    }

    /**
     * Returns the hash of the state at a loop head, or null if it has none (see the class comment).
     *
     * @param inst the probe of the head
     * @param shadow the thread's shadow stack and heap
     * @param shadowStatics the shadow static fields by class name
     * @param threads the number of target threads
     */
    public static String stateOf(
            LOOP_HEAD inst, ShadowContext shadow, Map<String, Value<?, ?>[]> shadowStatics, int threads) {
        if (stopped) {
            return null;
        }
        LoopHeads.Head head = LoopHeads.get(inst.head);
        if (head == null || threads != 1 || inst.locals.length != head.slots().length) {
            logger.debug("[LOOP STATE] Not hashed: unknown head or several threads");
            return null;
        }
        long start = System.nanoTime();
        try {
            checkStack(head);
            checkShadow(head, shadow, shadowStatics);
            String h = hash(head, inst.locals);
            logger.debug("[LOOP STATE] {}: {}", inst.head, h);
            if (!seen.add(h)) {
                // The explorer leaves out everything after a repeat on this path anyway
                stopped = true;
            }
            return h;
        } catch (Unhashable e) {
            return null;
        } finally {
            spentNanos += System.nanoTime() - start;
            if (spentNanos > BUDGET_NANOS) {
                logger.debug("[LOOP STATE] Time budget used up, no more states this run");
                stopped = true;
            }
        }
    }

    /**
     * Notes a method call of the run. A call into code that is not tracked, other than a few pure
     * methods, may read or change state outside the hashed state (a System property, the interrupt
     * flag, the static fields of a class in an excluded package): the run reports no more states.
     * Every state a run reports thus has a prefix without such calls, so the hidden state is the
     * initial one wherever it is reported.
     */
    public static void call(String owner, String name, boolean isStatic) {
        if (!stopped && !isPureCall(owner.replace('.', '/'), name, isStatic)) {
            logger.debug("[LOOP STATE] Call to {}.{}, no more states this run", owner, name);
            stopped = true;
        }
    }

    /** An invokedynamic call site (lambdas, string concatenation) runs library code. */
    public static void invokeDynamic() {
        stopped = true;
    }

    /** Notes an access to a static field: one of an untracked class is state outside the hash. */
    public static void staticField(String owner) {
        if (!stopped && owner != null) {
            String name = owner.replace('.', '/');
            if (!name.startsWith(SWAT_PACKAGE) && !isApplicationClassName(name)) {
                logger.debug("[LOOP STATE] Static field of {}, no more states this run", owner);
                stopped = true;
            }
        }
    }

    private static boolean isPureCall(String owner, String name, boolean isStatic) {
        if (owner.startsWith(SWAT_PACKAGE) || isApplicationClassName(owner)) {
            return true; // tracked code (SWAT's Verifier included)
        }
        switch (owner) {
            case "org/sosy_lab/sv_benchmarks/Verifier", "java/lang/Math", "java/lang/StrictMath":
                return true;
            case "java/lang/Object":
                return name.equals("<init>");
            case "java/lang/Double", "java/lang/Float", "java/lang/Integer", "java/lang/Long":
                // getInteger and getLong read System properties
                return isStatic && !name.equals("getInteger") && !name.equals("getLong");
            default:
                return false;
        }
    }

    /** The head's method must be the only application frame on the stack. */
    private static void checkStack(LoopHeads.Head head) throws Unhashable {
        List<String> app = StackWalker.getInstance().walk(s -> s
                .map(f -> f.getClassName().replace('.', '/'))
                .filter(LoopStateMatcher::isApplicationClassName)
                .limit(2)
                .toList());
        if (app.size() != 1 || !app.get(0).equals(head.owner())) {
            throw unhashable("other application frames on the stack");
        }
    }

    private static void checkShadow(LoopHeads.Head head, ShadowContext shadow,
            Map<String, Value<?, ?>[]> shadowStatics) throws Unhashable {
        Frame active = shadow.getActiveFrame();
        // The entry method runs in the initial frame
        boolean entry = active.getClassName().equals(ShadowContext.getINITIAL_FRAME_NAME())
                && shadow.getFrameStack().size() == 1;
        boolean same = head.owner().equals(active.getClassName()) && head.method().equals(active.getMethodName());
        if (!(entry || same) || !active.getOperandStack().isEmpty()) {
            throw unhashable("shadow frame out of step");
        }
        List<Value<?, ?>> locals = active.getLocals();
        for (int slot : head.slots()) {
            if (slot < locals.size()) {
                requireConcrete(locals.get(slot));
            }
        }
        for (Frame frame : shadow.getFrameStack()) {
            if (frame != active) {
                requireConcrete(frame.getLocals());
                requireConcrete(frame.getOperandStack());
            }
        }
        for (Map.Entry<String, Value<?, ?>[]> e : shadowStatics.entrySet()) {
            if (e.getKey() != null && !e.getKey().startsWith(SWAT_PACKAGE)) {
                requireConcrete(Arrays.asList(e.getValue()));
            }
        }
        requireConcrete(shadow.heapValues());
    }

    private static void requireConcrete(Collection<Value<?, ?>> values) throws Unhashable {
        for (Value<?, ?> v : values) {
            requireConcrete(v);
        }
    }

    private static void requireConcrete(Value<?, ?> v) throws Unhashable {
        if (v == null) {
            return;
        }
        boolean symbolic;
        try {
            symbolic = v.isSymbolic();
        } catch (Throwable t) {
            symbolic = true; // unknown counts as symbolic
        }
        if (symbolic) {
            throw unhashable("symbolic value");
        }
    }

    private static boolean isApplicationClassName(String internalName) {
        return !internalName.startsWith(SWAT_PACKAGE) && Util.shouldInstrument(internalName);
    }

    private static boolean isApplicationClass(Class<?> c) {
        return c.getClassLoader() != null && !c.isHidden() && isApplicationClassName(c.getName().replace('.', '/'));
    }

    private static String hash(LoopHeads.Head head, Object[] locals) throws Unhashable {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw unhashable("no SHA-256");
        }
        try (DataOutputStream out = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), md))) {
            Encoder enc = new Encoder(out);
            out.writeUTF(head.name());
            for (int k = 0; k < locals.length; k++) {
                out.writeInt(head.slots()[k]);
                enc.local(head.kinds()[k], locals[k]);
            }
            String[] classes = ClassInits.snapshot();
            out.writeInt(classes.length);
            for (String name : classes) {
                if (name.startsWith(SWAT_PACKAGE)) {
                    continue;
                }
                out.writeUTF(name);
                Class<?> c = load(name);
                for (Field f : sortedFields(c, true)) {
                    out.writeUTF(f.getName());
                    enc.field(f, null);
                }
            }
        } catch (IOException | ReflectiveOperationException | RuntimeException | LinkageError e) {
            // Includes a class whose initialization failed: reflection on it throws
            // NoClassDefFoundError, and its state cannot be read, so it must stay unhashable.
            throw unhashable("reflection failed");
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("%02x", digest[i]));
        }
        return sb.toString();
    }

    private static Class<?> load(String internalName) throws ClassNotFoundException {
        String name = internalName.replace('/', '.');
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName(name, false, context != null ? context : ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            return Class.forName(name, false, ClassLoader.getSystemClassLoader());
        }
    }

    private static Field[] sortedFields(Class<?> c, boolean statics) {
        Field[] fields = Arrays.stream(c.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) == statics)
                .sorted(Comparator.comparing(Field::getName))
                .toArray(Field[]::new);
        for (Field f : fields) {
            f.setAccessible(true);
        }
        return fields;
    }

    /** Writes values and the heap graph reachable from them. */
    private static final class Encoder {
        private final DataOutputStream out;
        private final IdentityHashMap<Object, Integer> ids = new IdentityHashMap<>();

        Encoder(DataOutputStream out) {
            this.out = out;
        }

        void local(char kind, Object value) throws IOException, Unhashable, IllegalAccessException {
            switch (kind) {
                case LoopHeads.INT -> out.writeInt((Integer) value);
                case LoopHeads.LONG -> out.writeLong((Long) value);
                case LoopHeads.FLOAT -> out.writeInt(Float.floatToRawIntBits((Float) value));
                case LoopHeads.DOUBLE -> out.writeLong(Double.doubleToRawLongBits((Double) value));
                default -> ref(value);
            }
        }

        void field(Field f, Object owner) throws IOException, Unhashable, IllegalAccessException {
            Class<?> t = f.getType();
            if (t == int.class) out.writeInt(f.getInt(owner));
            else if (t == long.class) out.writeLong(f.getLong(owner));
            else if (t == boolean.class) out.writeBoolean(f.getBoolean(owner));
            else if (t == byte.class) out.writeByte(f.getByte(owner));
            else if (t == char.class) out.writeChar(f.getChar(owner));
            else if (t == short.class) out.writeShort(f.getShort(owner));
            else if (t == float.class) out.writeInt(Float.floatToRawIntBits(f.getFloat(owner)));
            else if (t == double.class) out.writeLong(Double.doubleToRawLongBits(f.getDouble(owner)));
            else ref(f.get(owner));
        }

        void ref(Object o) throws IOException, Unhashable, IllegalAccessException {
            if (o == null) {
                out.writeByte('N');
                return;
            }
            Integer id = ids.get(o);
            if (id != null) {
                out.writeByte('R');
                out.writeInt(id);
                return;
            }
            if (ids.size() >= MAX_OBJECTS) {
                throw unhashable("heap too large");
            }
            ids.put(o, ids.size());
            Class<?> c = o.getClass();
            if (c.isArray()) {
                out.writeByte('Y');
                out.writeUTF(c.getName());
                int n = Array.getLength(o);
                out.writeInt(n);
                Class<?> e = c.getComponentType();
                if (o instanceof int[] a) for (int v : a) out.writeInt(v);
                else if (o instanceof long[] a) for (long v : a) out.writeLong(v);
                else if (o instanceof boolean[] a) for (boolean v : a) out.writeBoolean(v);
                else if (o instanceof byte[] a) out.write(a);
                else if (o instanceof char[] a) for (char v : a) out.writeChar(v);
                else if (o instanceof short[] a) for (short v : a) out.writeShort(v);
                else if (o instanceof float[] a) for (float v : a) out.writeInt(Float.floatToRawIntBits(v));
                else if (o instanceof double[] a) for (double v : a) out.writeLong(Double.doubleToRawLongBits(v));
                else if (!e.isPrimitive()) for (Object v : (Object[]) o) ref(v);
                else throw unhashable("unknown array type");
                return;
            }
            if (c == Object.class) {
                out.writeByte('O');
                return;
            }
            if (!isApplicationClass(c)) {
                throw unhashable("non-application object");
            }
            out.writeByte('C');
            out.writeUTF(c.getName());
            for (Class<?> k = c; k != Object.class; k = k.getSuperclass()) {
                if (k == Enum.class) {
                    // name and ordinal are final and fixed by the constant
                    out.writeInt(((Enum<?>) o).ordinal());
                    break;
                }
                if (k == null || !isApplicationClass(k)) {
                    throw unhashable("non-application superclass");
                }
                for (Field f : sortedFields(k, false)) {
                    field(f, o);
                }
            }
        }
    }
}
