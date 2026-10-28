package de.uzl.its.swat.symbolic.loopstate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * The loop heads of the instrumented methods: the targets of backward jumps at which the operand
 * stack is empty. For each the instrumentation records which locals are defined there and of what
 * kind, so that the probe it inserts can pass their values (see {@link #emitProbe}).
 */
public final class LoopHeads {

    /** Value kinds of a local: the JVM computational types. */
    public static final char INT = 'I', LONG = 'J', FLOAT = 'F', DOUBLE = 'D', REFERENCE = 'A';

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

    private LoopHeads() {}

    public static Head get(String name) {
        return heads.get(name);
    }

    /**
     * Finds the loop heads of a method. Returns them keyed by the label the method node replays
     * (LabelNode.getLabel()), or an empty map if the method cannot be analysed.
     */
    public static Map<Label, Head> analyze(String owner, MethodNode mn) {
        Map<Label, Head> result = new IdentityHashMap<>();
        if (mn.instructions.size() == 0 || mn.name.equals("<init>")) {
            return result;
        }
        InsnList insns = mn.instructions;
        List<LabelNode> targets = new ArrayList<>();
        Map<LabelNode, Boolean> seen = new IdentityHashMap<>();
        for (AbstractInsnNode n : insns) {
            List<LabelNode> jumps = new ArrayList<>();
            if (n instanceof JumpInsnNode j) {
                if (j.getOpcode() == Opcodes.JSR) {
                    return result; // subroutines: the analysis below does not apply
                }
                jumps.add(j.label);
            } else if (n instanceof TableSwitchInsnNode t) {
                jumps.add(t.dflt);
                jumps.addAll(t.labels);
            } else if (n instanceof LookupSwitchInsnNode l) {
                jumps.add(l.dflt);
                jumps.addAll(l.labels);
            }
            int at = insns.indexOf(n);
            for (LabelNode target : jumps) {
                if (insns.indexOf(target) < at && seen.put(target, Boolean.TRUE) == null) {
                    targets.add(target);
                }
            }
        }
        if (targets.isEmpty()) {
            return result;
        }
        Frame<BasicValue>[] frames;
        try {
            frames = new Analyzer<>(new BasicInterpreter()).analyze(owner, mn);
        } catch (AnalyzerException | RuntimeException e) {
            return result;
        }
        targets.sort((a, b) -> Integer.compare(insns.indexOf(a), insns.indexOf(b)));
        int ordinal = 0;
        for (LabelNode target : targets) {
            Frame<BasicValue> f = frames[insns.indexOf(target)];
            int position = ordinal++;
            if (f == null || f.getStackSize() != 0) {
                continue; // unreachable, or values on the operand stack the probe cannot see
            }
            List<Integer> slots = new ArrayList<>();
            StringBuilder kinds = new StringBuilder();
            boolean usable = true;
            for (int i = 0; i < f.getLocals(); i++) {
                BasicValue v = f.getLocal(i);
                if (v == null || v == BasicValue.UNINITIALIZED_VALUE) {
                    continue; // never read before it is written again
                }
                if (v == BasicValue.RETURNADDRESS_VALUE) {
                    usable = false;
                    break;
                }
                char kind = switch (v.getType().getSort()) {
                    case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> INT;
                    case Type.LONG -> LONG;
                    case Type.FLOAT -> FLOAT;
                    case Type.DOUBLE -> DOUBLE;
                    case Type.OBJECT, Type.ARRAY -> REFERENCE;
                    default -> 0;
                };
                if (kind == 0) {
                    usable = false;
                    break;
                }
                slots.add(i);
                kinds.append(kind);
            }
            if (!usable) {
                continue;
            }
            String name = owner + "." + mn.name + mn.desc + "#" + position;
            Head head = new Head(name, owner, mn.name,
                    slots.stream().mapToInt(Integer::intValue).toArray(), kinds.toString().toCharArray());
            heads.put(name, head);
            result.put(target.getLabel(), head);
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
        push(mv, head.slots().length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int k = 0; k < head.slots().length; k++) {
            mv.visitInsn(Opcodes.DUP);
            push(mv, k);
            int slot = head.slots()[k];
            switch (head.kinds()[k]) {
                case INT -> box(mv, Opcodes.ILOAD, slot, "java/lang/Integer", "I");
                case LONG -> box(mv, Opcodes.LLOAD, slot, "java/lang/Long", "J");
                case FLOAT -> box(mv, Opcodes.FLOAD, slot, "java/lang/Float", "F");
                case DOUBLE -> box(mv, Opcodes.DLOAD, slot, "java/lang/Double", "D");
                default -> mv.visitVarInsn(Opcodes.ALOAD, slot);
            }
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, dispatcher, "LOOP_HEAD",
                "(JLjava/lang/String;[Ljava/lang/Object;)V", false);
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
