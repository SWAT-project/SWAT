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

    /**
     * An instruction of a side: opcode, operand (slot or constant), increment; or, with {@code
     * nested}, a diamond inside the side, whose opcode is that of its {@code if}.
     */
    public record Op(int opcode, int operand, int increment, Diamond nested) {
        public Op(int opcode, int operand, int increment) {
            this(opcode, operand, increment, null);
        }
    }

    public sealed interface Plan permits Diamond, CountingLoop, BackEdge {}

    /**
     * A diamond: the ops of the fall-through side and of the jump side, the id of the probe at the
     * join, and how many values the sides leave on the stack (0, or 1 for a conditional expression).
     */
    public record Diamond(List<Op> fallThrough, List<Op> jump, int join, int stackDelta) implements Plan {}

    /**
     * A counting loop with head {@code i < b} (step +1) or {@code i > b} (step -1), or with
     * {@code inclusive}, {@code i <= b} or {@code i >= b}. {@code bound} is
     * a local slot, or -1 with {@code boundConstant}. {@code exitOnJump}: the head's branch leaves
     * the loop when it jumps. {@code steps}: the locals the body adds a constant to (the induction
     * variable among them), {@code constants}: those it sets to a constant.
     */
    public record CountingLoop(int i, int step, int bound, int boundConstant, boolean exitOnJump,
                               Map<Integer, Integer> steps, Map<Integer, Integer> constants,
                               boolean pure, boolean inclusive) implements Plan {}

    /**
     * The back edge of a counting loop: tells the executor that the loop's head is reached again
     * in the same activation (rather than entered anew, e.g. after an exception left the loop).
     */
    public record BackEdge(CountingLoop loop) implements Plan {}

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
        return analyze("java/lang/Object", mn);
    }

    /**
     * Finds the diamonds and counting loops of a method of {@code owner} whose merged values stay
     * symbolic (see TaintCheck).
     */
    public static MethodPlans analyze(String owner, MethodNode mn) {
        InsnList insns = mn.instructions;
        if (insns.size() == 0) {
            return MethodPlans.NONE;
        }
        TaintCheck taint = null;
        org.objectweb.asm.tree.analysis.Frame<org.objectweb.asm.tree.analysis.BasicValue>[] frames = null;
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
        // Who jumps to each label (a switch or a handler counts as a source no diamond owns).
        Map<LabelNode, List<AbstractInsnNode>> sources = new IdentityHashMap<>();
        for (AbstractInsnNode n : insns) {
            if (n instanceof JumpInsnNode j) {
                sources.computeIfAbsent(j.label, k -> new ArrayList<>()).add(n);
            } else if (n instanceof TableSwitchInsnNode t) {
                sources.computeIfAbsent(t.dflt, k -> new ArrayList<>()).add(n);
                t.labels.forEach(l -> sources.computeIfAbsent(l, k -> new ArrayList<>()).add(n));
            } else if (n instanceof LookupSwitchInsnNode l) {
                sources.computeIfAbsent(l.dflt, k -> new ArrayList<>()).add(n);
                l.labels.forEach(x -> sources.computeIfAbsent(x, k -> new ArrayList<>()).add(n));
            }
        }
        for (TryCatchBlockNode tc : mn.tryCatchBlocks) {
            sources.computeIfAbsent(tc.handler, k -> new ArrayList<>()).add(tc.handler);
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
                Map<Label, Integer> joinCandidate = new IdentityHashMap<>();
                Diamond d = diamond(insns, j, targets, joinCandidate, joins, sources);
                if (d != null) {
                    if (taint == null && (taint = TaintCheck.of(owner, mn)) == null) {
                        return MethodPlans.NONE;
                    }
                    LabelNode join = joinLabel(insns, joinCandidate.keySet().iterator().next());
                    java.util.Set<Integer> slots = new java.util.HashSet<>(written(d.fallThrough()));
                    slots.addAll(written(d.jump()));
                    if (!taint.leaks(join, slots, d.stackDelta() == 1)) {
                        joins.putAll(joinCandidate);
                        byJump.put(jumpOrdinal, d);
                    }
                    continue;
                }
            }
            if (j.getOpcode() == Opcodes.GOTO && insns.indexOf(j.label) < insns.indexOf(j)) {
                if (frames == null) {
                    frames = basicFrames(owner, mn);
                }
                Map.Entry<JumpInsnNode, CountingLoop> loop = frames == null ? null : countingLoop(insns, j, targets, frames);
                if (loop != null) {
                    if (taint == null && (taint = TaintCheck.of(owner, mn)) == null) {
                        return MethodPlans.NONE;
                    }
                    CountingLoop l = loop.getValue();
                    java.util.Set<Integer> slots = new java.util.HashSet<>(l.steps().keySet());
                    slots.addAll(l.constants().keySet());
                    if (!taint.leaks(loop.getKey().label, slots, false)) {
                        byJump.put(jumpOrdinal(insns, loop.getKey()), l);
                        byJump.put(jumpOrdinal, new BackEdge(l));
                    }
                }
            }
        }
        return new MethodPlans(byJump, joins);
    }

    private static java.util.Set<Integer> written(List<Op> ops) {
        java.util.Set<Integer> slots = new java.util.HashSet<>();
        for (Op op : ops) {
            if (op.nested() != null) {
                slots.addAll(written(op.nested().fallThrough()));
                slots.addAll(written(op.nested().jump()));
            } else if (op.opcode() == Opcodes.ISTORE || op.opcode() == Opcodes.IINC) {
                slots.add(op.operand());
            }
        }
        return slots;
    }

    private static LabelNode joinLabel(InsnList insns, Label label) {
        for (AbstractInsnNode n : insns) {
            if (n instanceof LabelNode l && l.getLabel() == label) {
                return l;
            }
        }
        throw new IllegalStateException("join label not in its method");
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
    static Op op(AbstractInsnNode n) {
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
        if (op.nested() != null) {
            return new int[] {op.opcode() <= Opcodes.IFLE ? 1 : 2, op.nested().stackDelta()};
        }
        return switch (op.opcode()) {
            case Opcodes.ILOAD, Opcodes.LDC -> new int[] {0, 1};
            case Opcodes.ISTORE -> new int[] {1, 0};
            case Opcodes.IINC -> new int[] {0, 0};
            case Opcodes.INEG -> new int[] {1, 1};
            default -> new int[] {2, 1};
        };
    }

    /**
     * The ops from {@code from} (exclusive) up to the first label in {@code ends} or the first
     * instruction that is not one: returns the ops and the node that ended the block, or null if a
     * label something else jumps to comes first. An {@code if} whose sides are blocks again and
     * meet within this one becomes a nested op; a division is allowed by a nonzero constant only
     * (it cannot throw then).
     */
    private static Map.Entry<List<Op>, AbstractInsnNode> block(AbstractInsnNode from,
            Map<LabelNode, Boolean> targets, Set<LabelNode> ends, Map<LabelNode, List<AbstractInsnNode>> sources) {
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
            if (n instanceof JumpInsnNode k && isIntCondition(k.getOpcode()) && sources != null) {
                Parsed inner = parse(k, targets, sources);
                if (inner == null) {
                    return Map.entry(ops, n);
                }
                // Control enters the rest of this block only from the inner diamond's own jumps.
                for (AbstractInsnNode src : sources.getOrDefault(inner.join(), List.of())) {
                    if (src != k && src != inner.gotoNode() && !ends.contains(inner.join())) {
                        return null;
                    }
                }
                ops.add(new Op(k.getOpcode(), 0, 0, new Diamond(inner.fall(), inner.jump(), -1, inner.depth())));
                if (ends.contains(inner.join())) {
                    return Map.entry(ops, inner.join());
                }
                n = inner.join();
                continue;
            }
            int opc = n.getOpcode();
            if ((opc == Opcodes.IDIV || opc == Opcodes.IREM) && !ops.isEmpty()
                    && ops.get(ops.size() - 1).opcode() == Opcodes.LDC && ops.get(ops.size() - 1).nested() == null
                    && ops.get(ops.size() - 1).operand() != 0) {
                ops.add(new Op(opc, 0, 0));
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
        return !written(ops).isEmpty();
    }

    /** A diamond's sides, its join, the GOTO that ends its fall-through side (if any), its depth. */
    private record Parsed(List<Op> fall, List<Op> jump, LabelNode join, AbstractInsnNode gotoNode, int depth) {}

    /** The diamond of the {@code if} {@code j}, or null. */
    private static Parsed parse(JumpInsnNode j, Map<LabelNode, Boolean> targets,
            Map<LabelNode, List<AbstractInsnNode>> sources) {
        LabelNode jumpTarget = j.label;
        if (!isBefore(j, jumpTarget)) {
            return null;
        }
        Map.Entry<List<Op>, AbstractInsnNode> fall = block(j, targets, Set.of(jumpTarget), sources);
        if (fall == null) {
            return null;
        }
        List<Op> fallOps = fall.getKey();
        List<Op> jumpOps;
        LabelNode join;
        AbstractInsnNode gotoNode = null;
        if (fall.getValue() == jumpTarget) {
            // if (c) { fall-through side }  -- the jump side is empty
            jumpOps = List.of();
            join = jumpTarget;
        } else if (fall.getValue() instanceof JumpInsnNode g && g.getOpcode() == Opcodes.GOTO
                && isBefore(jumpTarget, g.label) && nextLabelIs(g, jumpTarget)) {
            // if (c) { fall-through side } else { jump side }
            join = g.label;
            gotoNode = g;
            Map.Entry<List<Op>, AbstractInsnNode> jump = block(jumpTarget, targets, Set.of(join), sources);
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
        // The jump target is entered only by this if (the fall-through side may also end there)
        for (AbstractInsnNode src : sources.getOrDefault(jumpTarget, List.of())) {
            if (src != j && join != jumpTarget) {
                return null;
            }
        }
        return new Parsed(List.copyOf(fallOps), List.copyOf(jumpOps), join, gotoNode, fallDepth);
    }

    /** Whether {@code a} comes before {@code b} in their method. */
    private static boolean isBefore(AbstractInsnNode a, AbstractInsnNode b) {
        for (AbstractInsnNode n = a.getNext(); n != null; n = n.getNext()) {
            if (n == b) {
                return true;
            }
        }
        return false;
    }

    private static Diamond diamond(InsnList insns, JumpInsnNode j, Map<LabelNode, Boolean> targets,
            Map<Label, Integer> joins, Map<Label, Integer> existingJoins,
            Map<LabelNode, List<AbstractInsnNode>> sources) {
        Parsed p = parse(j, targets, sources);
        if (p == null) {
            return null;
        }
        if (p.depth() == 0 && !writes(p.fall()) && !writes(p.jump())) {
            return null;
        }
        LabelNode join = p.join();
        // Diamonds that meet at the same label share its probe
        Integer id = existingJoins.containsKey(join.getLabel()) ? existingJoins.get(join.getLabel())
                : Integer.valueOf(joinIds.incrementAndGet());
        joins.put(join.getLabel(), id);
        return new Diamond(p.fall(), p.jump(), id, p.depth());
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

    /**
     * The counting loop whose back edge is {@code back}, with the head's branch, or null. The head
     * is {@code ILOAD i; (ILOAD b | constant); IF_ICMPxx exit} or {@code ILOAD i; IFxx exit}; the
     * body runs from there to {@code back}, and the exit label follows {@code back}.
     */
    private static Map.Entry<JumpInsnNode, CountingLoop> countingLoop(InsnList insns, JumpInsnNode back,
            Map<LabelNode, Boolean> targets,
            org.objectweb.asm.tree.analysis.Frame<org.objectweb.asm.tree.analysis.BasicValue>[] frames) {
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
        boolean inclusive = false;
        if (opcode == Opcodes.IF_ICMPGE) {
            step = 1;
        } else if (opcode == Opcodes.IF_ICMPLE) {
            step = -1;
        } else if (opcode == Opcodes.IF_ICMPGT) {
            // runs while i <= b: never ends for b = 2^31 - 1, where nothing after it runs either,
            // so closed forms that are wrong only there add no execution that exists
            step = 1;
            inclusive = true;
        } else if (opcode == Opcodes.IF_ICMPLT) {
            step = -1; // while i >= b, likewise
            inclusive = true;
        } else {
            return null;
        }
        // The body: straight-line code up to the back edge. Calls cannot change the method's locals,
        // so whatever else the body does (calls, fields, arrays), the closed forms of its int locals
        // stay exact; such a loop is impure, and the explorer only uses its summary if that other
        // code cannot matter to the targets (see BranchElement.mergedConditional).
        BodyAnalysis body = BodyAnalysis.run(insns, cond, back, targets, frames);
        if (body == null) {
            return null;
        }
        Map<Integer, Integer> steps = body.steps();
        Map<Integer, Integer> constants = body.constants();
        if (steps.getOrDefault(i, 0) != step || constants.containsKey(i)) {
            return null;
        }
        if (bound >= 0 && (steps.containsKey(bound) || constants.containsKey(bound) || bound == i)) {
            return null;
        }
        return Map.entry(cond, new CountingLoop(i, step, bound, boundConstant, true,
                Collections.unmodifiableMap(steps), Collections.unmodifiableMap(constants), body.pure(), inclusive));
    }

    /** The method's frames (types of locals and stack values), or null if it cannot be analysed. */
    private static org.objectweb.asm.tree.analysis.Frame<org.objectweb.asm.tree.analysis.BasicValue>[] basicFrames(
            String owner, MethodNode mn) {
        try {
            return new org.objectweb.asm.tree.analysis.Analyzer<>(
                    new org.objectweb.asm.tree.analysis.BasicInterpreter()).analyze(owner, mn);
        } catch (Exception e) {
            return null;
        }
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
