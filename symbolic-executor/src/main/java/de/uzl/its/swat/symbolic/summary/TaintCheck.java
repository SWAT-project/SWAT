package de.uzl.its.swat.symbolic.summary;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

/**
 * Whether merged values stay where the executor keeps them symbolic.
 *
 * <p>A merged branch's constraint is dropped: its outcome lives on only in the merged values'
 * formulas. That is exact as long as the executor tracks those values symbolically wherever they
 * go. Where it concretizes them -- an argument to library code (SWAT's {@code Verifier.assume}
 * only looks at the concrete boolean, a {@code assume(n > 0)} being exactly such a merged
 * conditional expression), a value stored to the heap or an array, an array index or size, a
 * return value -- the dropped constraint would have been the only record, and a solution could
 * violate it unnoticed. So a summary is only applied if, from its join (or the loop's exit) on,
 * every value computed from a merged one is only used in int arithmetic, locals, comparisons and
 * switches: a forward taint analysis over the method.
 */
final class TaintCheck {

    /** A value: its basic type, and whether it is computed from a merged value. */
    private static final class TV extends BasicValue {
        final boolean tainted;

        TV(Type type, boolean tainted) {
            super(type);
            this.tainted = tainted;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TV t && t.tainted == tainted && super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 2 + (tainted ? 1 : 0);
        }
    }

    /** BasicInterpreter's types plus taint; records whether a tainted value reaches a sink. */
    private static final class Taint extends Interpreter<TV> {
        private final BasicInterpreter basic = new BasicInterpreter();
        boolean leaked;

        Taint() {
            super(Opcodes.ASM9);
        }

        private static TV of(BasicValue v, boolean tainted) {
            return v == null ? null : new TV(v.getType(), tainted);
        }

        @Override
        public TV newValue(Type type) {
            BasicValue v = basic.newValue(type);
            return v == null ? null : (v == BasicValue.UNINITIALIZED_VALUE ? new TV(null, false) : of(v, false));
        }

        @Override
        public TV newOperation(AbstractInsnNode insn) throws AnalyzerException {
            return of(basic.newOperation(insn), false);
        }

        @Override
        public TV copyOperation(AbstractInsnNode insn, TV value) throws AnalyzerException {
            return of(basic.copyOperation(insn, value), value.tainted);
        }

        @Override
        public TV unaryOperation(AbstractInsnNode insn, TV value) throws AnalyzerException {
            if (value.tainted) {
                switch (insn.getOpcode()) {
                    case Opcodes.NEWARRAY, Opcodes.ANEWARRAY, Opcodes.PUTSTATIC, Opcodes.ATHROW,
                            Opcodes.MONITORENTER, Opcodes.MONITOREXIT -> leaked = true;
                    default -> { }
                }
            }
            return of(basic.unaryOperation(insn, value), value.tainted);
        }

        @Override
        public TV binaryOperation(AbstractInsnNode insn, TV v1, TV v2) throws AnalyzerException {
            int op = insn.getOpcode();
            if ((op >= Opcodes.IALOAD && op <= Opcodes.SALOAD && (v1.tainted || v2.tainted))
                    || (op == Opcodes.PUTFIELD && (v1.tainted || v2.tainted))) {
                leaked = true;
            }
            return of(basic.binaryOperation(insn, v1, v2), v1.tainted || v2.tainted);
        }

        @Override
        public TV ternaryOperation(AbstractInsnNode insn, TV v1, TV v2, TV v3) throws AnalyzerException {
            if (v1.tainted || v2.tainted || v3.tainted) {
                leaked = true; // an array store
            }
            return of(basic.ternaryOperation(insn, v1, v2, v3), false);
        }

        @Override
        public TV naryOperation(AbstractInsnNode insn, List<? extends TV> values) throws AnalyzerException {
            if (values.stream().anyMatch(v -> v.tainted)) {
                leaked = true; // a call (or a multi-dimensional allocation)
            }
            return of(basic.naryOperation(insn, values), false);
        }

        @Override
        public void returnOperation(AbstractInsnNode insn, TV value, TV expected) throws AnalyzerException {
            if (value.tainted) {
                leaked = true;
            }
        }

        @Override
        public TV merge(TV v, TV w) {
            boolean tainted = v.tainted || w.tainted;
            Type type = java.util.Objects.equals(v.getType(), w.getType()) ? v.getType() : null;
            if (java.util.Objects.equals(type, v.getType()) && v.tainted == tainted) {
                return v;
            }
            return new TV(type, tainted);
        }
    }

    private final MethodNode mn;
    private final Frame<TV>[] frames;

    private TaintCheck(MethodNode mn, Frame<TV>[] frames) {
        this.mn = mn;
        this.frames = frames;
    }

    /** The check for a method, or null if the method cannot be analysed (then nothing is merged). */
    static TaintCheck of(String owner, MethodNode mn) {
        try {
            Frame<TV>[] frames = new Analyzer<>(new Taint()).analyze(owner, mn);
            return new TaintCheck(mn, frames);
        } catch (AnalyzerException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Whether, with {@code slots} (and with {@code top}, the operand stack's top value) merged at
     * {@code start}, a value computed from them may reach a place the executor concretizes.
     */
    boolean leaks(LabelNode start, Set<Integer> slots, boolean top) {
        InsnList insns = mn.instructions;
        int at = insns.indexOf(start);
        Frame<TV> f0 = frames[at];
        if (f0 == null) {
            return false; // unreachable
        }
        Frame<TV> init = new Frame<>(f0);
        for (int slot : slots) {
            if (slot >= init.getLocals()) {
                return true;
            }
            TV v = init.getLocal(slot);
            init.setLocal(slot, new TV(v == null ? null : v.getType(), true));
        }
        if (top) {
            if (init.getStackSize() == 0) {
                return true;
            }
            TV v = init.pop();
            init.push(new TV(v.getType(), true));
        }
        Taint taint = new Taint();
        Map<AbstractInsnNode, Frame<TV>> in = new IdentityHashMap<>();
        Deque<AbstractInsnNode> work = new ArrayDeque<>();
        in.put(start, init);
        work.add(start);
        int steps = 0;
        while (!work.isEmpty()) {
            if (++steps > 200_000) {
                return true; // give up: no summary
            }
            AbstractInsnNode n = work.poll();
            Frame<TV> f = new Frame<>(in.get(n));
            int op = n.getOpcode();
            try {
                if (op >= 0) {
                    f.execute(n, taint);
                }
            } catch (AnalyzerException | RuntimeException e) {
                return true;
            }
            if (taint.leaked) {
                return true;
            }
            for (AbstractInsnNode s : successors(insns, n)) {
                flow(in, work, s, f, taint);
            }
            // Exceptions: a handler sees the locals with the exception on the stack.
            int idx = insns.indexOf(n);
            for (TryCatchBlockNode tc : mn.tryCatchBlocks) {
                if (insns.indexOf(tc.start) <= idx && idx < insns.indexOf(tc.end)) {
                    Frame<TV> h = new Frame<>(in.get(n));
                    h.clearStack();
                    h.push(new TV(Type.getObjectType(tc.type == null ? "java/lang/Throwable" : tc.type), false));
                    flow(in, work, tc.handler, h, taint);
                }
            }
        }
        return false;
    }

    private static void flow(Map<AbstractInsnNode, Frame<TV>> in, Deque<AbstractInsnNode> work,
                             AbstractInsnNode s, Frame<TV> f, Taint taint) {
        Frame<TV> old = in.get(s);
        if (old == null) {
            in.put(s, new Frame<>(f));
            work.add(s);
            return;
        }
        try {
            if (old.merge(f, taint)) {
                work.add(s);
            }
        } catch (AnalyzerException | RuntimeException e) {
            taint.leaked = true;
        }
    }

    private static List<AbstractInsnNode> successors(InsnList insns, AbstractInsnNode n) {
        int op = n.getOpcode();
        java.util.ArrayList<AbstractInsnNode> s = new java.util.ArrayList<>();
        if (n instanceof JumpInsnNode j) {
            s.add(j.label);
            if (op != Opcodes.GOTO) {
                s.add(n.getNext());
            }
        } else if (n instanceof TableSwitchInsnNode t) {
            s.add(t.dflt);
            s.addAll(t.labels);
        } else if (n instanceof LookupSwitchInsnNode l) {
            s.add(l.dflt);
            s.addAll(l.labels);
        } else if (!(op >= Opcodes.IRETURN && op <= Opcodes.RETURN) && op != Opcodes.ATHROW) {
            s.add(n.getNext());
        }
        s.removeIf(x -> x == null);
        return s;
    }
}
