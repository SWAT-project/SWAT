package de.uzl.its.swat.symbolic.summary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

/**
 * What one iteration of a counting loop's body does to the method's locals: for every local it
 * writes, whether the new value is the old one plus a constant (a step), or a constant. The body
 * runs once through ASM's frame machinery over abstract values {@code local + c}, {@code c} or
 * unknown, so it may contain any straight-line code; a local written with anything else (a field,
 * an array element, a call's result, another local) rules the loop out.
 *
 * @param pure whether the body is int arithmetic on locals only (see LoopSummaries.op)
 */
record BodyAnalysis(Map<Integer, Integer> steps, Map<Integer, Integer> constants, boolean pure) {

    /** An abstract value: {@code local + add}, the constant {@code add}, or unknown. */
    private static final class Lin extends BasicValue {
        static final int CONSTANT = -1;
        static final int UNKNOWN = -2;
        final int local;
        final int add;

        Lin(Type type, int local, int add) {
            super(type);
            this.local = local;
            this.add = add;
        }

        static Lin unknown(BasicValue v) {
            return v == null ? null : new Lin(v.getType(), UNKNOWN, 0);
        }

        boolean known() {
            return local != UNKNOWN;
        }

        Lin plus(int c) {
            return known() ? new Lin(Type.INT_TYPE, local, add + c) : unknown(BasicValue.INT_VALUE);
        }
    }

    private static final class LinInterpreter extends Interpreter<Lin> {
        private final BasicInterpreter basic = new BasicInterpreter();

        LinInterpreter() {
            super(Opcodes.ASM9);
        }

        @Override
        public Lin newValue(Type type) {
            BasicValue v = basic.newValue(type);
            return v == null ? null : new Lin(v.getType(), Lin.UNKNOWN, 0);
        }

        @Override
        public Lin newOperation(AbstractInsnNode insn) throws AnalyzerException {
            int op = insn.getOpcode();
            if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
                return new Lin(Type.INT_TYPE, Lin.CONSTANT, op - Opcodes.ICONST_0);
            }
            if (insn instanceof IntInsnNode ii && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) {
                return new Lin(Type.INT_TYPE, Lin.CONSTANT, ii.operand);
            }
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer c) {
                return new Lin(Type.INT_TYPE, Lin.CONSTANT, c);
            }
            return Lin.unknown(basic.newOperation(insn));
        }

        @Override
        public Lin copyOperation(AbstractInsnNode insn, Lin value) {
            return value; // the same value: a local loaded and stored back is unchanged
        }

        @Override
        public Lin unaryOperation(AbstractInsnNode insn, Lin value) throws AnalyzerException {
            if (insn instanceof IincInsnNode inc) {
                return value.plus(inc.incr);
            }
            return Lin.unknown(basic.unaryOperation(insn, value));
        }

        @Override
        public Lin binaryOperation(AbstractInsnNode insn, Lin v1, Lin v2) throws AnalyzerException {
            int op = insn.getOpcode();
            if (op == Opcodes.IADD || op == Opcodes.ISUB) {
                if (v2.local == Lin.CONSTANT && v1.known()) {
                    return v1.plus(op == Opcodes.IADD ? v2.add : -v2.add);
                }
                if (op == Opcodes.IADD && v1.local == Lin.CONSTANT && v2.known()) {
                    return v2.plus(v1.add);
                }
            }
            return Lin.unknown(basic.binaryOperation(insn, v1, v2));
        }

        @Override
        public Lin ternaryOperation(AbstractInsnNode insn, Lin v1, Lin v2, Lin v3) throws AnalyzerException {
            return Lin.unknown(basic.ternaryOperation(insn, v1, v2, v3));
        }

        @Override
        public Lin naryOperation(AbstractInsnNode insn, List<? extends Lin> values) throws AnalyzerException {
            return Lin.unknown(basic.naryOperation(insn, values));
        }

        @Override
        public void returnOperation(AbstractInsnNode insn, Lin value, Lin expected) {
        }

        @Override
        public Lin merge(Lin v, Lin w) {
            return v; // straight-line code: never merged
        }
    }

    /**
     * Analyses the body from after {@code cond} (the loop's head branch) up to {@code back} (its back
     * edge), or returns null if it is not straight-line code or writes a local we cannot follow.
     */
    static BodyAnalysis run(InsnList insns, JumpInsnNode cond, JumpInsnNode back, Map<LabelNode, Boolean> targets,
                            Frame<BasicValue>[] frames) {
        AbstractInsnNode first = cond.getNext();
        Frame<BasicValue> start = frames[insns.indexOf(first)];
        if (start == null || start.getStackSize() != 0) {
            return null;
        }
        Frame<Lin> f = new Frame<>(start.getLocals(), Math.max(start.getMaxStackSize(), 1));
        Lin[] initial = new Lin[start.getLocals()];
        for (int k = 0; k < start.getLocals(); k++) {
            BasicValue v = start.getLocal(k);
            Lin l = v == BasicValue.INT_VALUE ? new Lin(Type.INT_TYPE, k, 0)
                    : new Lin(v == null ? null : v.getType(), Lin.UNKNOWN, 0);
            initial[k] = l;
            f.setLocal(k, l);
        }
        LinInterpreter interpreter = new LinInterpreter();
        boolean pure = true;
        for (AbstractInsnNode n = first; n != back; n = n.getNext()) {
            if (n == null) {
                return null;
            }
            if (n instanceof LabelNode l) {
                if (targets.containsKey(l)) {
                    return null;
                }
                continue;
            }
            if (n instanceof LineNumberNode || n instanceof FrameNode) {
                continue;
            }
            int op = n.getOpcode();
            if (n instanceof JumpInsnNode || n instanceof TableSwitchInsnNode || n instanceof LookupSwitchInsnNode
                    || (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) || op == Opcodes.ATHROW
                    || op == Opcodes.RET) {
                return null;
            }
            pure &= LoopSummaries.op(n) != null;
            try {
                f.execute(n, interpreter);
            } catch (AnalyzerException | RuntimeException e) {
                return null;
            }
        }
        if (f.getStackSize() != 0) {
            return null;
        }
        Map<Integer, Integer> steps = new HashMap<>();
        Map<Integer, Integer> constants = new HashMap<>();
        for (int k = 0; k < start.getLocals(); k++) {
            Lin end = f.getLocal(k);
            if (end == initial[k]) {
                continue;
            }
            if (start.getLocal(k) == BasicValue.UNINITIALIZED_VALUE) {
                // Not defined at the head, which is also the way out: a temporary of the body,
                // which nothing after the loop reads without writing it first.
                continue;
            }
            if (initial[k].local != k) {
                return null; // a non-int local (or an int one that is not defined at the head) written
            }
            if (end.local == k) {
                if (end.add != 0) {
                    steps.put(k, end.add);
                }
            } else if (end.local == Lin.CONSTANT) {
                constants.put(k, end.add);
            } else {
                return null;
            }
        }
        return new BodyAnalysis(steps, constants, pure);
    }
}
