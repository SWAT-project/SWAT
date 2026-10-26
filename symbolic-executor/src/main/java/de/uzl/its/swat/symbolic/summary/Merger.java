package de.uzl.its.swat.symbolic.summary;

import de.uzl.its.swat.symbolic.shadow.Frame;
import de.uzl.its.swat.symbolic.shadow.ShadowContext;
import de.uzl.its.swat.symbolic.summary.LoopSummaries.CountingLoop;
import de.uzl.its.swat.symbolic.summary.LoopSummaries.Diamond;
import de.uzl.its.swat.symbolic.summary.LoopSummaries.Op;
import de.uzl.its.swat.symbolic.trace.BranchElement;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Applies loop summaries (see {@link LoopSummaries}) while the executor runs: at a summarized
 * branch it records what to merge, at the join (a diamond) or the loop's exit (a counting loop) it
 * gives the locals their merged formulas. Their concrete values are never changed: the merged
 * value's concrete part is the one the execution computed, which is checked; if it differs, the
 * summary is dropped and its branches are reported as ordinary ones.
 */
public final class Merger {

    private static final int BITS = 32;

    /** A diamond waiting for its join: the merged locals and stack value, and its branch. */
    private record PendingJoin(Map<Integer, IntValue> locals, IntValue top, BranchElement branch) {}

    /** A counting loop being executed: its values at entry, and the branches its head reported. */
    private static final class ActiveLoop {
        final IntValue i0;
        final IntValue bound;
        final Map<Integer, IntValue> entry = new HashMap<>();
        final List<BranchElement> branches = new ArrayList<>();
        final boolean summarize;

        ActiveLoop(IntValue i0, IntValue bound, boolean summarize) {
            this.i0 = i0;
            this.bound = bound;
            this.summarize = summarize;
        }
    }

    private final ShadowContext stack;

    public Merger(ShadowContext stack) {
        this.stack = stack;
    }

    /**
     * Called for every int branch after it was recorded: {@code jumpCondition} holds iff the branch
     * jumps, {@code jumped} says whether it did.
     */
    public void atBranch(long instIid, boolean jumped, BooleanFormula jumpCondition, BranchElement branch,
                         SolverContext context) {
        if (branch == null) {
            return;
        }
        LoopSummaries.Plan plan = LoopSummaries.planAt(instIid);
        if (plan instanceof Diamond d) {
            diamond(d, jumped, jumpCondition, branch, context);
        } else if (plan instanceof CountingLoop l) {
            loopHead(instIid, l, jumped, branch, context);
        }
    }

    // --- diamonds -------------------------------------------------------------------------------

    private void diamond(Diamond d, boolean jumped, BooleanFormula jumpCondition, BranchElement branch,
                         SolverContext context) {
        if (context.getFormulaManager().extractVariables(jumpCondition).isEmpty()) {
            return; // a concrete branch: there is nothing to merge
        }
        Frame frame = stack.getActiveFrame();
        Side fall = simulate(d.fallThrough(), frame, context);
        Side jump = simulate(d.jump(), frame, context);
        if (fall == null || jump == null) {
            return;
        }
        BitvectorFormulaManager bv = context.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bm = context.getFormulaManager().getBooleanFormulaManager();
        Map<Integer, IntValue> merged = new LinkedHashMap<>();
        java.util.Set<Integer> slots = new java.util.TreeSet<>(fall.written);
        slots.addAll(jump.written);
        for (int slot : slots) {
            IntValue j = jump.locals.get(slot);
            IntValue f = fall.locals.get(slot);
            if (j == null || f == null) {
                return;
            }
            IntValue taken = jumped ? j : f;
            merged.put(slot, new IntValue(context, taken.concrete, bm.ifThenElse(jumpCondition, j.formula, f.formula)));
        }
        IntValue top = null;
        if (d.stackDelta() == 1) {
            IntValue j = jump.top;
            IntValue f = fall.top;
            top = new IntValue(context, (jumped ? j : f).concrete, bm.ifThenElse(jumpCondition, j.formula, f.formula));
        }
        frame.summaryJoins().put(d.join(), new PendingJoin(merged, top, branch));
        branch.setMerged(true);
    }

    /** What a side does: the locals it leaves (read or written), those it wrote, its stack top. */
    private record Side(Map<Integer, IntValue> locals, java.util.Set<Integer> written, IntValue top) {}

    /** Runs a side's ops on the frame's current values, without changing the frame. */
    private Side simulate(List<Op> ops, Frame frame, SolverContext context) {
        Map<Integer, IntValue> locals = new HashMap<>();
        java.util.Set<Integer> written = new java.util.HashSet<>();
        List<IntValue> st = new ArrayList<>();
        BitvectorFormulaManager bv = context.getFormulaManager().getBitvectorFormulaManager();
        try {
            for (Op op : ops) {
                switch (op.opcode()) {
                    case Opcodes.ILOAD -> {
                        IntValue v = local(locals, frame, op.operand());
                        if (v == null) {
                            return null;
                        }
                        st.add(v);
                    }
                    case Opcodes.ISTORE -> {
                        locals.put(op.operand(), st.remove(st.size() - 1));
                        written.add(op.operand());
                    }
                    case Opcodes.IINC -> {
                        IntValue v = local(locals, frame, op.operand());
                        if (v == null) {
                            return null;
                        }
                        locals.put(op.operand(), v.IINC(op.increment()));
                        written.add(op.operand());
                    }
                    case Opcodes.LDC -> st.add(new IntValue(context, op.operand()));
                    case Opcodes.INEG -> st.add(st.remove(st.size() - 1).INEG());
                    default -> {
                        IntValue y = st.remove(st.size() - 1);
                        IntValue x = st.remove(st.size() - 1);
                        st.add(switch (op.opcode()) {
                            case Opcodes.IADD -> x.IADD(y);
                            case Opcodes.ISUB -> x.ISUB(y);
                            case Opcodes.IMUL -> x.IMUL(y);
                            case Opcodes.ISHL -> x.ISHL(y);
                            case Opcodes.ISHR -> x.ISHR(y);
                            case Opcodes.IUSHR -> x.IUSHR(y);
                            case Opcodes.IAND -> x.IAND(y);
                            case Opcodes.IOR -> x.IOR(y);
                            case Opcodes.IXOR -> x.IXOR(y);
                            default -> throw new IllegalStateException("op " + op.opcode());
                        });
                    }
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        // The other side's written locals need this side's value too: its current one.
        for (int slot : new java.util.ArrayList<>(locals.keySet())) {
            if (!written.contains(slot)) {
                locals.remove(slot);
            }
        }
        return new Side(new LocalsView(locals, frame), written, st.isEmpty() ? null : st.get(st.size() - 1));
    }

    /** A side's locals: those it wrote, else the frame's current ones. */
    private static final class LocalsView extends HashMap<Integer, IntValue> {
        private final Frame frame;

        LocalsView(Map<Integer, IntValue> written, Frame frame) {
            super(written);
            this.frame = frame;
        }

        @Override
        public IntValue get(Object key) {
            IntValue v = super.get(key);
            return v != null ? v : asInt(frame, (Integer) key);
        }
    }

    private static IntValue local(Map<Integer, IntValue> locals, Frame frame, int slot) {
        IntValue v = locals.get(slot);
        return v != null ? v : asInt(frame, slot);
    }

    /** The frame's local as an int value, or null if it is not one (a placeholder, a boolean). */
    private static IntValue asInt(Frame frame, int slot) {
        try {
            Value<?, ?> v = frame.getLocal(slot);
            return v != null && v.getClass() == IntValue.class ? (IntValue) v : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** At a join probe: gives the locals (and the stack top) of the pending diamond their merge. */
    public void atJoin(int join) {
        Frame frame = stack.getActiveFrame();
        if (frame == null || frame.summaryJoinsIfAny() == null) {
            return;
        }
        Object p = frame.summaryJoinsIfAny().remove(join);
        if (!(p instanceof PendingJoin pending)) {
            return;
        }
        // The execution must have computed what the summary says, or the summary is wrong here.
        for (Map.Entry<Integer, IntValue> e : pending.locals().entrySet()) {
            IntValue current = asInt(frame, e.getKey());
            if (current == null || current.concrete.intValue() != e.getValue().concrete.intValue()) {
                pending.branch().setMerged(false);
                return;
            }
        }
        IntValue top = null;
        if (pending.top() != null) {
            Value<?, ?> t = frame.peek();
            if (t == null || t.getClass() != IntValue.class || ((IntValue) t).concrete.intValue() != pending.top().concrete.intValue()) {
                pending.branch().setMerged(false);
                return;
            }
            top = pending.top();
        }
        for (Map.Entry<Integer, IntValue> e : pending.locals().entrySet()) {
            frame.setLocal(e.getKey(), e.getValue());
        }
        if (top != null) {
            try {
                frame.pop();
                frame.push(top);
            } catch (Exception e) {
                pending.branch().setMerged(false);
            }
        }
    }

    // --- counting loops --------------------------------------------------------------------------

    private void loopHead(long instIid, CountingLoop l, boolean jumped, BranchElement branch, SolverContext context) {
        Frame frame = stack.getActiveFrame();
        Map<Long, Object> loops = frame.summaryLoops();
        ActiveLoop a = (ActiveLoop) loops.get(instIid);
        if (a == null) {
            // Entering the loop: its head is reached for the first time since the loop was left.
            IntValue i0 = asInt(frame, l.i());
            IntValue b = l.bound() >= 0 ? asInt(frame, l.bound()) : new IntValue(context, l.boundConstant());
            boolean summarize = i0 != null && b != null && (i0.isSymbolic() || b.isSymbolic());
            a = new ActiveLoop(i0, b, summarize);
            if (summarize) {
                for (int slot : l.steps().keySet()) {
                    IntValue v = asInt(frame, slot);
                    if (v == null) {
                        a = new ActiveLoop(i0, b, false);
                        break;
                    }
                    a.entry.put(slot, v);
                }
            }
            if (a.summarize) {
                for (int slot : l.constants().keySet()) {
                    IntValue v = asInt(frame, slot);
                    if (v == null) {
                        a = new ActiveLoop(i0, b, false);
                        break;
                    }
                    a.entry.put(slot, v);
                }
            }
            loops.put(instIid, a);
        }
        if (a.summarize) {
            a.branches.add(branch);
            branch.setMerged(true);
        }
        if (jumped != l.exitOnJump()) {
            return; // another iteration
        }
        loops.remove(instIid);
        if (!a.summarize) {
            return;
        }
        // The loop ran N = max(b - i0, 0) times (or i0 - b for a step of -1).
        BitvectorFormulaManager bv = context.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bm = context.getFormulaManager().getBooleanFormulaManager();
        BitvectorFormula i0 = a.i0.formula;
        BitvectorFormula b = a.bound.formula;
        BooleanFormula runs = l.step() > 0 ? bv.lessThan(i0, b, true) : bv.greaterThan(i0, b, true);
        BitvectorFormula zero = bv.makeBitvector(BITS, 0);
        BitvectorFormula n = bm.ifThenElse(runs, l.step() > 0 ? bv.subtract(b, i0) : bv.subtract(i0, b), zero);
        int ci0 = a.i0.concrete;
        int cb = a.bound.concrete;
        boolean cruns = l.step() > 0 ? ci0 < cb : ci0 > cb;
        int cn = cruns ? (l.step() > 0 ? cb - ci0 : ci0 - cb) : 0;
        Map<Integer, IntValue> merged = new HashMap<>();
        for (Map.Entry<Integer, Integer> s : l.steps().entrySet()) {
            int slot = s.getKey();
            IntValue x0 = a.entry.get(slot);
            if (slot == l.i()) {
                merged.put(slot, new IntValue(context, cruns ? cb : ci0, bm.ifThenElse(runs, b, i0)));
            } else {
                int d = s.getValue();
                merged.put(slot, new IntValue(context, x0.concrete + d * cn,
                        bv.add(x0.formula, bv.multiply(bv.makeBitvector(BITS, d), n))));
            }
        }
        for (Map.Entry<Integer, Integer> c : l.constants().entrySet()) {
            IntValue y0 = a.entry.get(c.getKey());
            merged.put(c.getKey(), new IntValue(context, cruns ? c.getValue() : y0.concrete,
                    bm.ifThenElse(runs, bv.makeBitvector(BITS, c.getValue()), y0.formula)));
        }
        for (Map.Entry<Integer, IntValue> e : merged.entrySet()) {
            IntValue current = asInt(frame, e.getKey());
            if (current == null || current.concrete.intValue() != e.getValue().concrete.intValue()) {
                a.branches.forEach(x -> x.setMerged(false));
                return;
            }
        }
        merged.forEach(frame::setLocal);
    }
}
