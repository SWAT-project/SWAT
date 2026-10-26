package de.uzl.its.swat.symbolic.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Loop summaries: code whose paths the executor merges into one, so that the explorer has no
 * branch to enumerate there.
 *
 * <p>Two shapes, found in a method's bytecode before it is instrumented:
 *
 * <ul>
 *   <li><b>Diamonds</b>: an {@code if} on ints whose two sides are straight-line int arithmetic on
 *       locals (no calls, fields, arrays, divisions or other jumps) and meet again at a join label,
 *       e.g. {@code if (i < val) count++;} or {@code s = i < val ? s + 2 : s;}. At the {@code if}
 *       the executor computes what both sides do to the locals (and to the value a conditional
 *       expression leaves on the stack), and at the join it gives each of them the formula
 *       {@code ite(condition, jump side, fall-through side)}. Inside a loop this summarizes the
 *       loop: a counter fed by an input-dependent branch becomes a formula of the input.
 *   <li><b>Counting loops</b>: a loop whose head tests {@code i < b} (or {@code i > b}) and whose
 *       body is straight-line int arithmetic that steps {@code i} by +1 (or -1), adds constants to
 *       other locals or sets them to constants, with {@code b} unchanged. At the loop's exit the
 *       executor sets every such local to its closed form in the trip count {@code N = max(b - i0,
 *       0)}, e.g. {@code s0 + 3 * N}. With a strict bound and a step of one, {@code i} cannot
 *       overflow, and bit-vector arithmetic wraps around like Java's ints, so the closed forms
 *       are exact.
 * </ul>
 *
 * Both are exact: every path through the code ends in the merged state, which is a formula of the
 * state before it. The branches that the merged code reports are flagged {@code merged}; the
 * explorer neither flips them nor keeps their constraints, which the merged formulas replace.
 */
public final class LoopSummaries {

    /** An instruction of a side or loop body: opcode, operand (slot or constant), increment. */
    public record Op(int opcode, int operand, int increment) {}

    public sealed interface Plan permits Diamond, CountingLoop {}

    /**
     * A diamond: the ops of the fall-through side and of the jump side, the id of the probe at the
     * join, and how many values the sides leave on the stack (0, or 1 for a conditional expression).
     */
    public record Diamond(List<Op> fallThrough, List<Op> jump, int join, int stackDelta) implements Plan {}

    /**
     * A counting loop with head {@code i < b} (step +1) or {@code i > b} (step -1). {@code bound} is
     * a local slot, or -1 with {@code boundConstant}. {@code exitOnJump}: the head's branch leaves
     * the loop when it jumps. {@code steps}: the locals the body adds a constant to (the induction
     * variable among them), {@code constants}: those it sets to a constant.
     */
    public record CountingLoop(int i, int step, int bound, int boundConstant, boolean exitOnJump,
                               Map<Integer, Integer> steps, Map<Integer, Integer> constants) implements Plan {}

    /** What the instrumentation needs for a method: plans by jump ordinal, probes by join label. */
    public record MethodPlans(Map<Integer, Plan> byJump, Map<Label, Integer> joins) {
        public static final MethodPlans NONE = new MethodPlans(Map.of(), Map.of());
    }

    private static final Map<Long, Plan> plans = new ConcurrentHashMap<>();
    private static final AtomicInteger joinIds = new AtomicInteger();

    private LoopSummaries() {}

    /** Called by the instrumentation with the iid it gave a jump that has a plan. */
    public static void register(long iid, Plan plan) {
        plans.put(iid, plan);
    }

    /** The plan of the jump with this (instrumentation) iid, or null. */
    public static Plan planAt(long iid) {
        return plans.get(iid);
    }

    /** Emits the probe at a join label (see SUMMARY_JOIN). */
    public static void emitJoinProbe(MethodVisitor mv, String dispatcher, long iid, int join) {
        mv.visitLdcInsn(iid);
        mv.visitLdcInsn(join);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, dispatcher, "SUMMARY_JOIN", "(JI)V", false);
    }

    /** Finds the diamonds and counting loops of a method. */
    public static MethodPlans analyze(MethodNode mn) {
        InsnList insns = mn.instructions;
        if (insns.size() == 0) {
            return MethodPlans.NONE;
        }
        // Labels something jumps to (or a handler starts at): code between two of them is entered
        // only at its start.
        Map<LabelNode, Boolean> targets = new IdentityHashMap<>();
        for (AbstractInsnNode n : insns) {
            if (n instanceof JumpInsnNode j) {
                if (j.getOpcode() == Opcodes.JSR) {
                    return MethodPlans.NONE;
                }
                targets.put(j.label, Boolean.TRUE);
            } else if (n instanceof TableSwitchInsnNode t) {
                targets.put(t.dflt, Boolean.TRUE);
                t.labels.forEach(l -> targets.put(l, Boolean.TRUE));
            } else if (n instanceof LookupSwitchInsnNode l) {
                targets.put(l.dflt, Boolean.TRUE);
                l.labels.forEach(x -> targets.put(x, Boolean.TRUE));
            }
        }
        for (TryCatchBlockNode tc : mn.tryCatchBlocks) {
            targets.put(tc.handler, Boolean.TRUE);
        }

        Map<Integer, Plan> byJump = new HashMap<>();
        Map<Label, Integer> joins = new IdentityHashMap<>();
        int ordinal = 0;
        for (AbstractInsnNode n : insns) {
            if (!(n instanceof JumpInsnNode j)) {
                continue;
            }
            int jumpOrdinal = ordinal++;
            if (isIntCondition(j.getOpcode())) {
                Diamond d = diamond(insns, j, targets, joins);
                if (d != null) {
                    byJump.put(jumpOrdinal, d);
                    continue;
                }
            }
            if (j.getOpcode() == Opcodes.GOTO && insns.indexOf(j.label) < insns.indexOf(j)) {
                Map.Entry<JumpInsnNode, CountingLoop> loop = countingLoop(insns, j, targets);
                if (loop != null) {
                    byJump.put(jumpOrdinal(insns, loop.getKey()), loop.getValue());
                }
            }
        }
        return new MethodPlans(byJump, joins);
    }

    private static int jumpOrdinal(InsnList insns, JumpInsnNode target) {
        int ordinal = 0;
        for (AbstractInsnNode n : insns) {
            if (n == target) {
                return ordinal;
            }
            if (n instanceof JumpInsnNode) {
                ordinal++;
            }
        }
        throw new IllegalStateException("jump not in its method");
    }

    private static boolean isIntCondition(int opcode) {
        return opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ICMPLE;
    }

    /** Whether a node is a real instruction (not a label, line number or frame). */
    private static boolean isReal(AbstractInsnNode n) {
        return !(n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode);
    }

    /** The op for an allowed straight-line int instruction, or null. */
    private static Op op(AbstractInsnNode n) {
        int opc = n.getOpcode();
        if (n instanceof VarInsnNode v && (opc == Opcodes.ILOAD || opc == Opcodes.ISTORE)) {
            return new Op(opc, v.var, 0);
        }
        if (n instanceof IincInsnNode inc) {
            return new Op(Opcodes.IINC, inc.var, inc.incr);
        }
        if (opc >= Opcodes.ICONST_M1 && opc <= Opcodes.ICONST_5) {
            return new Op(Opcodes.LDC, opc - Opcodes.ICONST_0, 0);
        }
        if (n instanceof IntInsnNode ii && (opc == Opcodes.BIPUSH || opc == Opcodes.SIPUSH)) {
            return new Op(Opcodes.LDC, ii.operand, 0);
        }
        if (n instanceof LdcInsnNode ldc && ldc.cst instanceof Integer c) {
            return new Op(Opcodes.LDC, c, 0);
        }
        if (n instanceof InsnNode) {
            switch (opc) {
                case Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.INEG, Opcodes.ISHL,
                        Opcodes.ISHR, Opcodes.IUSHR, Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR:
                    return new Op(opc, 0, 0);
                default:
                    return null;
            }
        }
        return null;
    }

    /** How an op changes the stack depth: {pops, pushes}. */
    private static int[] stackEffect(Op op) {
        return switch (op.opcode()) {
            case Opcodes.ILOAD, Opcodes.LDC -> new int[] {0, 1};
            case Opcodes.ISTORE -> new int[] {1, 0};
            case Opcodes.IINC -> new int[] {0, 0};
            case Opcodes.INEG -> new int[] {1, 1};
            default -> new int[] {2, 1};
        };
    }

    /**
     * The ops from {@code from} (exclusive) up to the first real instruction or label that ends
     * the block: returns the ops and the node that ended it, or null if something not allowed
     * comes first (an instruction, or a label something jumps to other than {@code allowedLabel}).
     */
    private static Map.Entry<List<Op>, AbstractInsnNode> block(AbstractInsnNode from,
            Map<LabelNode, Boolean> targets, Set<LabelNode> ends) {
        List<Op> ops = new ArrayList<>();
        for (AbstractInsnNode n = from.getNext(); n != null; n = n.getNext()) {
            if (n instanceof LabelNode l) {
                if (ends.contains(l)) {
                    return Map.entry(ops, n);
                }
                if (targets.containsKey(l)) {
                    return null;
                }
                continue;
            }
            if (!isReal(n)) {
                continue;
            }
            Op op = op(n);
            if (op == null) {
                return Map.entry(ops, n); // ends at a non-arithmetic instruction (e.g. a GOTO)
            }
            ops.add(op);
        }
        return null;
    }

    /** The stack depth at the end of a side, or -1 if it pops below its start. */
    private static int depth(List<Op> ops) {
        int depth = 0;
        for (Op op : ops) {
            int[] e = stackEffect(op);
            depth -= e[0];
            if (depth < 0) {
                return -1;
            }
            depth += e[1];
        }
        return depth;
    }

    private static boolean writes(List<Op> ops) {
        return ops.stream().anyMatch(o -> o.opcode() == Opcodes.ISTORE || o.opcode() == Opcodes.IINC);
    }

    private static Diamond diamond(InsnList insns, JumpInsnNode j, Map<LabelNode, Boolean> targets,
            Map<Label, Integer> joins) {
        LabelNode jumpTarget = j.label;
        if (insns.indexOf(jumpTarget) < insns.indexOf(j)) {
            return null;
        }
        Map.Entry<List<Op>, AbstractInsnNode> fall = block(j, targets, Set.of(jumpTarget));
        if (fall == null) {
            return null;
        }
        List<Op> fallOps = fall.getKey();
        List<Op> jumpOps;
        LabelNode join;
        if (fall.getValue() == jumpTarget) {
            // if (c) { fall-through side }  -- the jump side is empty
            jumpOps = List.of();
            join = jumpTarget;
        } else if (fall.getValue() instanceof JumpInsnNode g && g.getOpcode() == Opcodes.GOTO
                && insns.indexOf(g.label) > insns.indexOf(jumpTarget) && nextLabelIs(g, jumpTarget)) {
            // if (c) { fall-through side } else { jump side }
            join = g.label;
            Map.Entry<List<Op>, AbstractInsnNode> jump = block(jumpTarget, targets, Set.of(join));
            if (jump == null || jump.getValue() != join) {
                return null;
            }
            jumpOps = jump.getKey();
        } else {
            return null;
        }
        int fallDepth = depth(fallOps);
        int jumpDepth = depth(jumpOps);
        if (fallDepth < 0 || fallDepth != jumpDepth || fallDepth > 1) {
            return null;
        }
        if (fallDepth == 0 && !writes(fallOps) && !writes(jumpOps)) {
            return null;
        }
        Integer id = joins.computeIfAbsent(join.getLabel(), l -> joinIds.incrementAndGet());
        return new Diamond(List.copyOf(fallOps), List.copyOf(jumpOps), id, fallDepth);
    }

    /** Whether nothing but non-instructions separates {@code from} and the label {@code label}. */
    private static boolean nextLabelIs(AbstractInsnNode from, LabelNode label) {
        for (AbstractInsnNode n = from.getNext(); n != null; n = n.getNext()) {
            if (n == label) {
                return true;
            }
            if (isReal(n)) {
                return false;
            }
        }
        return false;
    }

    // --- counting loops ---------------------------------------------------------------------------

    /** An abstract int during a loop body: {@code local + add}, or a constant, or unknown. */
    private record Lin(int local, int add, boolean constant) {
        static final Lin UNKNOWN = new Lin(-2, 0, false);

        static Lin of(int local) {
            return new Lin(local, 0, false);
        }

        static Lin constant(int c) {
            return new Lin(-1, c, true);
        }

        Lin plus(int c) {
            return this == UNKNOWN ? UNKNOWN : new Lin(local, add + c, constant);
        }
    }

    /**
     * The counting loop whose back edge is {@code back}, with the head's branch, or null. The head
     * is {@code ILOAD i; (ILOAD b | constant); IF_ICMPxx exit} or {@code ILOAD i; IFxx exit}; the
     * body runs from there to {@code back}, and the exit label follows {@code back}.
     */
    private static Map.Entry<JumpInsnNode, CountingLoop> countingLoop(InsnList insns, JumpInsnNode back,
            Map<LabelNode, Boolean> targets) {
        LabelNode head = back.label;
        List<AbstractInsnNode> h = new ArrayList<>();
        AbstractInsnNode n = head.getNext();
        for (; n != null && h.size() < 3; n = n.getNext()) {
            if (n instanceof LabelNode l && targets.containsKey(l)) {
                return null;
            }
            if (isReal(n)) {
                h.add(n);
                if (n instanceof JumpInsnNode) {
                    break;
                }
            }
        }
        if (h.isEmpty() || !(h.get(h.size() - 1) instanceof JumpInsnNode cond)) {
            return null;
        }
        if (!isIntCondition(cond.getOpcode())) {
            return null;
        }
        LabelNode exit = cond.label;
        if (!nextLabelIs(back, exit)) {
            return null;
        }
        // The head's operands.
        int opcode = cond.getOpcode();
        int i;
        int bound = -1;
        int boundConstant = 0;
        if (opcode <= Opcodes.IFLE) {
            if (h.size() != 2 || !(h.get(0) instanceof VarInsnNode v) || v.getOpcode() != Opcodes.ILOAD) {
                return null;
            }
            i = v.var;
            opcode = opcode - Opcodes.IFEQ + Opcodes.IF_ICMPEQ; // compare with the constant 0
        } else {
            if (h.size() != 3) {
                return null;
            }
            Op a = op(h.get(0));
            Op b = op(h.get(1));
            if (a == null || b == null) {
                return null;
            }
            if (a.opcode() == Opcodes.ILOAD && (b.opcode() == Opcodes.ILOAD || b.opcode() == Opcodes.LDC)) {
                i = a.operand();
                if (b.opcode() == Opcodes.ILOAD) {
                    bound = b.operand();
                } else {
                    boundConstant = b.operand();
                }
            } else if (b.opcode() == Opcodes.ILOAD && a.opcode() == Opcodes.LDC) {
                // constant OP i: mirror the comparison
                i = b.operand();
                boundConstant = a.operand();
                opcode = mirror(opcode);
            } else {
                return null;
            }
        }
        // The head exits (jumps) on i >= b (the loop runs while i < b) or on i <= b (while i > b).
        int step;
        if (opcode == Opcodes.IF_ICMPGE) {
            step = 1;
        } else if (opcode == Opcodes.IF_ICMPLE) {
            step = -1;
        } else {
            return null;
        }
        // The body: straight-line arithmetic up to the back edge.
        Map<Integer, Lin> locals = new HashMap<>();
        List<Lin> stack = new ArrayList<>();
        for (AbstractInsnNode m = cond.getNext(); m != back; m = m.getNext()) {
            if (m == null) {
                return null;
            }
            if (m instanceof LabelNode l) {
                if (targets.containsKey(l)) {
                    return null;
                }
                continue;
            }
            if (!isReal(m)) {
                continue;
            }
            Op op = op(m);
            if (op == null) {
                return null;
            }
            switch (op.opcode()) {
                case Opcodes.ILOAD -> stack.add(locals.getOrDefault(op.operand(), Lin.of(op.operand())));
                case Opcodes.LDC -> stack.add(Lin.constant(op.operand()));
                case Opcodes.ISTORE -> {
                    if (stack.isEmpty()) {
                        return null;
                    }
                    locals.put(op.operand(), stack.remove(stack.size() - 1));
                }
                case Opcodes.IINC -> locals.put(op.operand(),
                        locals.getOrDefault(op.operand(), Lin.of(op.operand())).plus(op.increment()));
                case Opcodes.IADD, Opcodes.ISUB -> {
                    if (stack.size() < 2) {
                        return null;
                    }
                    Lin y = stack.remove(stack.size() - 1);
                    Lin x = stack.remove(stack.size() - 1);
                    Lin r = Lin.UNKNOWN;
                    if (y.constant() && x != Lin.UNKNOWN) {
                        r = x.plus(op.opcode() == Opcodes.IADD ? y.add() : -y.add());
                    } else if (x.constant() && y != Lin.UNKNOWN && !y.constant() && op.opcode() == Opcodes.IADD) {
                        r = y.plus(x.add());
                    }
                    stack.add(r);
                }
                default -> {
                    int[] e = stackEffect(op);
                    if (stack.size() < e[0]) {
                        return null;
                    }
                    for (int k = 0; k < e[0]; k++) {
                        stack.remove(stack.size() - 1);
                    }
                    for (int k = 0; k < e[1]; k++) {
                        stack.add(Lin.UNKNOWN);
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            return null;
        }
        Map<Integer, Integer> steps = new HashMap<>();
        Map<Integer, Integer> constants = new HashMap<>();
        for (Map.Entry<Integer, Lin> e : locals.entrySet()) {
            Lin v = e.getValue();
            if (v.constant()) {
                constants.put(e.getKey(), v.add());
            } else if (v.local() == e.getKey()) {
                if (v.add() != 0) {
                    steps.put(e.getKey(), v.add());
                }
            } else {
                return null; // set to another local, or to something we cannot follow
            }
        }
        if (steps.getOrDefault(i, 0) != step || constants.containsKey(i)) {
            return null;
        }
        if (bound >= 0 && (steps.containsKey(bound) || constants.containsKey(bound) || bound == i)) {
            return null;
        }
        return Map.entry(cond, new CountingLoop(i, step, bound, boundConstant, true,
                Collections.unmodifiableMap(steps), Collections.unmodifiableMap(constants)));
    }

    /** The comparison with its operands swapped: {@code a < b} is {@code b > a}. */
    private static int mirror(int opcode) {
        return switch (opcode) {
            case Opcodes.IF_ICMPLT -> Opcodes.IF_ICMPGT;
            case Opcodes.IF_ICMPGT -> Opcodes.IF_ICMPLT;
            case Opcodes.IF_ICMPLE -> Opcodes.IF_ICMPGE;
            case Opcodes.IF_ICMPGE -> Opcodes.IF_ICMPLE;
            default -> opcode;
        };
    }
}
