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
 * be read: the loop head, the locals live there, the application frames below the head's (each
 * frame's call site, where it continues after the call returns, and its locals live after that
 * call), the static fields of the initialized application classes, the set of those classes, and
 * the heap reachable from all of these. The hash is only computed when the state is fully known and
 * fully concrete; otherwise there is none:
 *
 * <ul>
 *   <li>the application frames on the stack are contiguous (no library frame between two of them:
 *       library code would continue with state that is not hashed), each frame below the head's
 *       was entered through a call site with loop facts (its LOOP_CALL probe recorded the caller's
 *       live locals; a call site with values on the operand stack below its arguments has none),
 *       and the target runs no other thread. The frames below the lowest application frame run no
 *       application code after it returns;
 *   <li>no shadow value of the head's locals, of the callers' live locals, of the other shadow
 *       frames, of a static field or of the shadow heap is symbolic. A concrete state is then the
 *       same on every execution that takes the same branches up to the head, whatever its inputs;
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

    /**
     * The state of a caller at a call site with loop facts, recorded on its shadow frame by the
     * site's LOOP_CALL probe.
     *
     * @param site the call site
     * @param invoke the iid of the call's INVOKE: the call site of the frame the call pushed
     * @param locals the values of the site's live locals, in the order of its slots
     */
    public record PendingCall(LoopHeadProbes.Call site, long invoke, Object[] locals) {}

    /**
     * Records the caller's state at a call site with loop facts on the caller's shadow frame, which
     * is still the active one (the probe comes before the call's INVOKE).
     */
    public static void enterCall(de.uzl.its.swat.symbolic.instruction.LOOP_CALL inst, ShadowContext shadow) {
        if (stopped || shadow.getFrameStack().isEmpty()) {
            return;
        }
        LoopHeadProbes.Call site = LoopHeadProbes.getCall(inst.site);
        shadow.getActiveFrame().setLoopCall(site == null || inst.locals.length != site.slots().length
                ? null : new PendingCall(site, inst.invoke, inst.locals));
    }

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
        LoopHeadProbes.Head head = LoopHeadProbes.get(inst.head);
        if (head == null || threads != 1 || inst.locals.length != head.slots().length) {
            logger.debug("[LOOP STATE] Not hashed: unknown head or several threads");
            return null;
        }
        long start = System.nanoTime();
        try {
            List<StackWalker.StackFrame> app = applicationFrames();
            List<PendingCall> callers = app.size() == 1 ? List.of() : callers(head, app, shadow);
            if (!app.get(0).getClassName().replace('.', '/').equals(head.owner())
                    || !app.get(0).getMethodName().equals(head.method())) {
                throw unhashable("other application frames on the stack");
            }
            checkShadow(head, shadow, shadowStatics, callers);
            String h = hash(head, inst.locals, callers);
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
            case "java/lang/Class":
                // The static initializer of every class with an assert calls it (javac's
                // $assertionsDisabled). It reads the JVM's assertion setting, the same in every
                // run, and the result lands in a static field of the class, which is hashed.
                return name.equals("desiredAssertionStatus");
            case "java/lang/Double", "java/lang/Float", "java/lang/Integer", "java/lang/Long":
                // getInteger and getLong read System properties
                return isStatic && !name.equals("getInteger") && !name.equals("getLong");
            default:
                return false;
        }
    }

    /**
     * The application frames on the stack, the head's first. They must be contiguous: a library
     * frame between two of them (a callback) continues with state that is not hashed. SWAT's own
     * frames (the probe's) are skipped; hidden frames (lambda forms) are not shown.
     */
    private static List<StackWalker.StackFrame> applicationFrames() throws Unhashable {
        List<StackWalker.StackFrame> frames = StackWalker.getInstance().walk(s -> s.toList());
        List<StackWalker.StackFrame> app = new java.util.ArrayList<>();
        boolean ended = false;
        for (StackWalker.StackFrame f : frames) {
            String name = f.getClassName().replace('.', '/');
            if (name.startsWith(SWAT_PACKAGE)) {
                continue;
            }
            if (isApplicationClassName(name)) {
                if (ended) {
                    throw unhashable("library frame between application frames");
                }
                app.add(f);
            } else if (!app.isEmpty()) {
                ended = true;
            }
        }
        if (app.isEmpty()) {
            throw unhashable("no application frame");
        }
        return app;
    }

    /**
     * The callers' states of a head reached in a callee, from the head's caller down: one per
     * application frame below the head's. The shadow stack must hold exactly the application
     * frames (the lowest one is the initial frame when it is the entry method), and each frame below
     * the top must have recorded, at the call site that pushed the frame above it, its live locals.
     */
    private static List<PendingCall> callers(LoopHeadProbes.Head head, List<StackWalker.StackFrame> app,
            ShadowContext shadow) throws Unhashable {
        List<Frame> stack = shadow.getFrameStack();
        int n = app.size();
        if (stack.size() != n) {
            throw unhashable("shadow stack out of step with the application frames");
        }
        List<PendingCall> callers = new java.util.ArrayList<>(n - 1);
        for (int k = 1; k < n; k++) {
            Frame above = stack.get(n - k);
            Frame frame = stack.get(n - k - 1);
            StackWalker.StackFrame real = app.get(k);
            PendingCall call = frame.getLoopCall();
            String owner = real.getClassName().replace('.', '/');
            boolean entry = k == n - 1 && frame.getClassName().equals(ShadowContext.getINITIAL_FRAME_NAME());
            if (call == null || call.invoke() != above.getCallSite() || above.getCallSite() < 0
                    || !call.site().owner().equals(owner) || !call.site().method().equals(real.getMethodName())
                    || !(entry || (owner.equals(frame.getClassName()) && real.getMethodName().equals(frame.getMethodName())))) {
                throw unhashable("caller without a recorded call site");
            }
            callers.add(call);
        }
        return callers;
    }

    private static void checkShadow(LoopHeadProbes.Head head, ShadowContext shadow,
            Map<String, Value<?, ?>[]> shadowStatics, List<PendingCall> callers) throws Unhashable {
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
        List<Frame> stack = shadow.getFrameStack();
        for (int k = 0; k < callers.size(); k++) {
            // A caller's dead locals cannot influence the rest of the execution
            Frame frame = stack.get(stack.size() - 2 - k);
            List<Value<?, ?>> callerLocals = frame.getLocals();
            for (int slot : callers.get(k).site().slots()) {
                if (slot < callerLocals.size()) {
                    requireConcrete(callerLocals.get(slot));
                }
            }
            requireConcrete(frame.getOperandStack());
        }
        for (int k = 0; k < stack.size() - 1 - callers.size(); k++) {
            Frame frame = stack.get(k);
            requireConcrete(frame.getLocals());
            requireConcrete(frame.getOperandStack());
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

    private static String hash(LoopHeadProbes.Head head, Object[] locals, List<PendingCall> callers) throws Unhashable {
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
            // Where each caller continues and what it still reads (none for a head in the entry
            // method, whose hash is as before)
            for (PendingCall call : callers) {
                out.writeUTF(call.site().name());
                for (int k = 0; k < call.locals().length; k++) {
                    out.writeInt(call.site().slots()[k]);
                    enc.local(call.site().kinds()[k], call.locals()[k]);
                }
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
                case LoopHeadProbes.INT -> out.writeInt((Integer) value);
                case LoopHeadProbes.LONG -> out.writeLong((Long) value);
                case LoopHeadProbes.FLOAT -> out.writeInt(Float.floatToRawIntBits((Float) value));
                case LoopHeadProbes.DOUBLE -> out.writeLong(Double.doubleToRawLongBits((Double) value));
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
