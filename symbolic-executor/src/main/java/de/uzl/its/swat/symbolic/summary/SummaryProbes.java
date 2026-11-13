package de.uzl.its.swat.symbolic.summary;

import de.uzl.its.cfg.loops.LoopSummaries;
import de.uzl.its.cfg.loops.LoopSummaries.MethodPlans;
import de.uzl.its.cfg.loops.LoopSummaries.Plan;
import de.uzl.its.cfg.loops.Positions;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The probes of the loop summaries. Which diamonds and counting loops a method has is static
 * analysis (cfg-extraction's {@link LoopSummaries}, from the loop facts); the instrumentation
 * registers each summarized jump's plan under the jump's iid ({@link #register}), for the
 * {@link Merger} to find when the jump runs, and puts a probe at each join
 * ({@link #emitJoinProbe}).
 */
public final class SummaryProbes {

    private static final Map<Long, Plan> plans = new ConcurrentHashMap<>();

    private SummaryProbes() {}

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

    /** The join ids of a method's plans, keyed by the label the method node replays. */
    public static Map<Label, Integer> joins(MethodNode mn, MethodPlans plans) {
        Map<Label, Integer> joins = new IdentityHashMap<>();
        if (plans.joins().isEmpty()) {
            return joins;
        }
        Map<Integer, LabelNode> labels = Positions.labels(mn.instructions);
        plans.joins().forEach((position, id) -> {
            LabelNode label = labels.get(position);
            if (label == null) {
                throw new IllegalStateException("no label at join position " + position + " of " + mn.name);
            }
            joins.put(label.getLabel(), id);
        });
        return joins;
    }
}
