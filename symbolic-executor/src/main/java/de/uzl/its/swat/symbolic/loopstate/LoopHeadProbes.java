package de.uzl.its.swat.symbolic.loopstate;

import de.uzl.its.cfg.loops.LoopHeads;
import de.uzl.its.cfg.loops.LoopHeads.CallSite;
import de.uzl.its.cfg.loops.LoopHeads.LoopHead;
import de.uzl.its.cfg.loops.Positions;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The probes at the loop heads of the instrumented methods, and at the call sites through which a
 * head's frame can be reached from another application frame. Where the heads and call sites are
 * and which locals are live there is static analysis (cfg-extraction's {@link LoopHeads}, from the
 * loop facts); this class names them, inserts their probes (see {@link #emitProbe},
 * {@link #emitCallProbe}) and looks them up by name when a probe runs.
 */
public final class LoopHeadProbes {

    /** Value kinds of a local: the JVM computational types. */
    public static final char INT = LoopHeads.INT, LONG = LoopHeads.LONG, FLOAT = LoopHeads.FLOAT,
            DOUBLE = LoopHeads.DOUBLE, REFERENCE = LoopHeads.REFERENCE;

    /**
     * A loop head.
     *
     * @param name stable name: owner, method name and descriptor, and the head's position among the
     *     method's heads
     * @param owner internal name of the class
     * @param method name of the method
     * @param slots the local slots defined at the head
     * @param kinds the kind of each slot
     */
    public record Head(String name, String owner, String method, int[] slots, char[] kinds) {}

    private static final Map<String, Head> heads = new ConcurrentHashMap<>();

    /**
     * A call site whose caller state can be hashed while the callee runs.
     *
     * @param name stable name: owner, method name and descriptor, and the site's ordinal among the
     *     method's method calls
     * @param owner internal name of the class
     * @param method name of the method
     * @param slots the local slots live after the call
     * @param kinds the kind of each slot
     */
    public record Call(String name, String owner, String method, int[] slots, char[] kinds) {}

    private static final Map<String, Call> calls = new ConcurrentHashMap<>();

    private LoopHeadProbes() {}

    public static Head get(String name) {
        return heads.get(name);
    }

    public static Call getCall(String name) {
        return calls.get(name);
    }

    /**
     * Names the call sites of a method of {@code owner} and registers them; returns them keyed by
     * their ordinal among the method's method-call instructions.
     */
    public static Map<Integer, Call> bindCalls(String owner, MethodNode mn, List<CallSite> found) {
        Map<Integer, Call> result = new java.util.HashMap<>();
        for (CallSite c : found) {
            String name = owner + "." + mn.name + mn.desc + "@" + c.ordinal();
            Call call = new Call(name, owner, mn.name, c.slots(), c.kinds());
            calls.put(name, call);
            result.put(c.ordinal(), call);
        }
        return result;
    }

    /**
     * Names the heads of a method of {@code owner} and registers them; returns them keyed by the
     * label the method node replays (LabelNode.getLabel()).
     */
    public static Map<Label, Head> bind(String owner, MethodNode mn, List<LoopHead> found) {
        Map<Label, Head> result = new IdentityHashMap<>();
        if (found.isEmpty()) {
            return result;
        }
        Map<Integer, LabelNode> labels = Positions.labels(mn.instructions);
        for (LoopHead h : found) {
            LabelNode label = labels.get(h.at());
            if (label == null) {
                throw new IllegalStateException("no label at loop head position " + h.at() + " of " + owner + "." + mn.name);
            }
            String name = owner + "." + mn.name + mn.desc + "#" + h.ordinal();
            Head head = new Head(name, owner, mn.name, h.slots(), h.kinds());
            heads.put(name, head);
            result.put(label.getLabel(), head);
        }
        return result;
    }

    /**
     * Emits the probe of a loop head: LOOP_HEAD(iid, name, locals), where locals holds the values
     * of the head's locals, primitives boxed.
     */
    public static void emitProbe(MethodVisitor mv, String dispatcher, long iid, Head head) {
        mv.visitLdcInsn(iid);
        mv.visitLdcInsn(head.name());
        pushLocals(mv, head.slots(), head.kinds());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, dispatcher, "LOOP_HEAD",
                "(JLjava/lang/String;[Ljava/lang/Object;)V", false);
    }

    /**
     * Emits the probe of a call site, to come before the call's INVOKE probe: LOOP_CALL(iid, name,
     * invoke, locals), where invoke is the iid of that INVOKE probe and locals holds the values of
     * the locals live after the call, primitives boxed.
     */
    public static void emitCallProbe(MethodVisitor mv, String dispatcher, long iid, Call call, long invoke) {
        mv.visitLdcInsn(iid);
        mv.visitLdcInsn(call.name());
        mv.visitLdcInsn(invoke);
        pushLocals(mv, call.slots(), call.kinds());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, dispatcher, "LOOP_CALL",
                "(JLjava/lang/String;J[Ljava/lang/Object;)V", false);
    }

    /** Pushes an Object[] with the values of the slots, primitives boxed. */
    private static void pushLocals(MethodVisitor mv, int[] slots, char[] kinds) {
        push(mv, slots.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int k = 0; k < slots.length; k++) {
            mv.visitInsn(Opcodes.DUP);
            push(mv, k);
            int slot = slots[k];
            switch (kinds[k]) {
                case INT -> box(mv, Opcodes.ILOAD, slot, "java/lang/Integer", "I");
                case LONG -> box(mv, Opcodes.LLOAD, slot, "java/lang/Long", "J");
                case FLOAT -> box(mv, Opcodes.FLOAD, slot, "java/lang/Float", "F");
                case DOUBLE -> box(mv, Opcodes.DLOAD, slot, "java/lang/Double", "D");
                default -> mv.visitVarInsn(Opcodes.ALOAD, slot);
            }
            mv.visitInsn(Opcodes.AASTORE);
        }
    }

    private static void box(MethodVisitor mv, int load, int slot, String owner, String desc) {
        mv.visitVarInsn(load, slot);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "valueOf", "(" + desc + ")L" + owner + ";", false);
    }

    private static void push(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    /**
     * Emits the call that records the start of a class's initialization, see {@link ClassInits}.
     */
    public static void emitClassInit(MethodVisitor mv, String owner) {
        mv.visitLdcInsn(owner);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ClassInits.class), "started",
                "(Ljava/lang/String;)V", false);
    }
}
