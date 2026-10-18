package de.uzl.its.swat.symbolic.trace;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.StatsStorage;
import de.uzl.its.swat.instrument.BranchKeys;
import de.uzl.its.swat.symbolic.shadow.Frame;
import de.uzl.its.swat.symbolic.shadow.ShadowContext;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;

/**
 * Manages access to the trace of the symbolic execution. This class should be used for all access
 * to the trace package.
 */
public class SymbolicTraceHandler {

    // The symbolic trace storing constraints and inputs
    private final SymbolicTrace symbolicTrace;

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    /** Constructs a new SymbolicTraceHandler. */
    public SymbolicTraceHandler() {
        this.symbolicTrace = new SymbolicTrace();
    }

    /**
     * Checks the result of a branch and adds the corresponding constraint to the symbolic trace.
     *
     * @param result Whether the branch was taken (true) or not (false).
     * @param constraint The constraint of the branch.
     * @param iid The iid of the branch instruction.
     */
    public void checkAndSetBranch(boolean result, BooleanFormula constraint, long iid) throws NoThreadContextException {
        checkAndSetBranch(result, constraint, iid, "");
    }

    /**
     * As {@link #checkAndSetBranch(boolean, BooleanFormula, long)}, for one of several branches an
     * instruction reports: {@code suffix} tells them apart in the branch's key ({@code #<case
     * value>} for a switch case, {@code #<n>} for the n-th dimension MULTIANEWARRAY checks).
     */
    public void checkAndSetBranch(boolean result, BooleanFormula constraint, long iid, String suffix) throws NoThreadContextException {

        long threadId = Thread.currentThread().getId();
        BranchElement current;
        FormulaManager fmgr = ThreadHandler.getSolverContext(threadId).getFormulaManager();
        BooleanFormula pathConstraint =
                result ? constraint : fmgr.getBooleanFormulaManager().not(constraint);

        current = new BranchElement(result, pathConstraint, iid);
        // check if the path constraint contains a free/ symbolic variable

        place(current, iid, suffix);
        symbolicTrace.addTraceElement(current);
    }

    /**
     * Records a branch the executor adds itself to enumerate the values of a symbolic operand, e.g.
     * one step of the search for the element a symbolic array index selects. It is a branch like
     * any other for the explorer, which flips it to try the other values, but the static
     * pre-analysis graph has no counterpart for it.
     *
     * @param result Whether {@code constraint} holds on this execution
     * @param constraint The condition branched on
     * @param iid The iid of the instruction the operand belongs to
     * @param suffix Tells the instruction's branches apart in their keys
     */
    public void addChoiceBranch(boolean result, BooleanFormula constraint, long iid, String suffix) throws NoThreadContextException {
        FormulaManager fmgr = ThreadHandler.getSolverContext(Thread.currentThread().getId()).getFormulaManager();
        BranchElement branch = new BranchElement(result, result ? constraint : fmgr.getBooleanFormulaManager().not(constraint), iid);
        branch.setChoice(true);
        place(branch, iid, suffix);
        symbolicTrace.addTraceElement(branch);
    }

    /**
     * As {@link #checkAndSetBranch(boolean, BooleanFormula, long, String)}, for a branch inside a
     * library call that SWAT models (e.g. the index check of String.charAt). The static pre-analysis
     * graph walks over library calls, so the branch is marked as one in a callback: the explorer's
     * walk of the graph holds its place, as for application code a library calls back.
     */
    public void checkAndSetLibraryBranch(boolean result, BooleanFormula constraint, long iid, String suffix) throws NoThreadContextException {
        BooleanFormulaManager bmgr = ThreadHandler.getSolverContext(Thread.currentThread().getId())
                .getFormulaManager().getBooleanFormulaManager();
        BranchElement branch = new BranchElement(result, result ? constraint : bmgr.not(constraint), iid);
        place(branch, iid, suffix);
        branch.setCallback(true);
        symbolicTrace.addTraceElement(branch);
    }

    /** Records where a branch is: its key and the change of the frame stack since the last branch. */
    private static void place(BranchElement branch, long iid, String suffix) throws NoThreadContextException {
        String key = BranchKeys.keyOf(iid);
        branch.setKey(key == null ? null : key + suffix);
        ShadowContext stack = ThreadHandler.getSymbolicVisitor(Thread.currentThread().getId()).getStack();
        branch.setCallback(stack.inCallback());
        ShadowContext.StackDelta delta = stack.takeStackDelta();
        branch.setPops(delta.popped());
        List<String> pushes = new ArrayList<>(delta.pushed().size());
        for (Frame frame : delta.pushed()) {
            pushes.add(frame.getCallSite() < 0 ? null : BranchKeys.keyOf(frame.getCallSite()));
        }
        branch.setPushes(pushes);
    }

    /**
     * Records a branch without a symbolic constraint, whose outcome is known only concretely. Used
     * where the static pre-analysis graph has a branch that SWAT has no symbolic value for (e.g. the
     * bounds check of an untracked array): the explorer walks that graph in step with the trace, by
     * position, so the branch must be in the trace, but it is never a target for the solver.
     *
     * @param result Whether the branch was taken
     * @param iid The iid of the branching instruction
     */
    public void addConcreteBranch(boolean result, long iid) throws NoThreadContextException {
        BooleanFormulaManager bmgr = ThreadHandler.getSolverContext(Thread.currentThread().getId())
                .getFormulaManager().getBooleanFormulaManager();
        BranchElement branch = new BranchElement(result, bmgr.makeTrue(), iid, true);
        place(branch, iid, "");
        symbolicTrace.addTraceElement(branch);
    }

    /**
     * Adds a special element to the symbolic trace. This is used to catch all unsupported branching
     * behaviour.
     *
     * @param iid The iid of the special instruction.
     * @param inst The instruction of the special instruction.
     */
    public void addSpecialElement(long iid, String inst) {
        symbolicTrace.addTraceElement(new SpecialElement(iid, inst));
    }

    /**
     * Adds a node to the trace to handle unexpected branching. Todo: Specify what branching can
     * occur.
     *
     * @param iid The iid of the instruction.
     * @param inst The type of invocation instruction.
     */
    public void recordInvocation(long iid, String inst) {
        addSpecialElement(iid, inst);
    }
    /**
     * Adds an input to the symbolic trace.
     *
     * @param name The name of the input.
     * @param value The value of the input.
     */
    public void addInput(String name, Value<?, ?> value) {
        logger.info("Input with name: {} and value: {} registered", name, value);
        symbolicTrace.addInputElement(new InputElement(name, value));
        SymbolRegistry.declare(name, SymbolRegistry.Kind.INPUT);
    }

    /**
     * Adds a global constraint to the symbolic trace.
     * Used for UF definitions, invariants, and axioms that should always hold.
     *
     * @param constraint The constraint to add.
     */
    public void addConstraint(BooleanFormula constraint) {
        if (constraint != null) {
            symbolicTrace.addConstraint(constraint);
            logger.debug("Added constraint: {}", constraint);
        } else {
            logger.warn("Attempted to add a null constraint to the symbolic trace.");
        }
    }

    public List<BooleanFormula> getConstraints() {
        return symbolicTrace.getConstraints();
    }

    /**
     * Encodes the symbolic trace as a JSON string.
     *
     * @return The symbolic trace encoded as a JSON string.
     */
    public String getTraceDTO(StatsStorage statsStorage) throws NoThreadContextException, JsonProcessingException, NotImplementedException {
        return DTOBuilder.encodeTrace(symbolicTrace, statsStorage, Thread.currentThread().getId());
    }

    /**
     * Encodes the trace with the solver context of the given thread, for callers running on
     * another thread (the shutdown hook sending the trace of the target's main thread).
     */
    public String getTraceDTO(StatsStorage statsStorage, long threadId) throws NoThreadContextException, JsonProcessingException, NotImplementedException {
        return DTOBuilder.encodeTrace(symbolicTrace, statsStorage, threadId);
    }

    /**
     * Dumps all constraints to the logger. Used for debugging purposes and by the LocalSolver.
     *
     * @param logger The logger to dump the constraints to.
     */
    public void dumpConstraints(Logger logger) {
        logger.info("-------------------- Dumping all constraints --------------------");
        for (Element elem : symbolicTrace.getTrace()) {
            if (elem instanceof BranchElement b) {
                logger.info(b.toString());
            }
        }
        logger.info("-----------------------------------------------------------------");
    }

    /**
     * Returns all imputs as an array list of strings. Used for debugging purposes and by the
     * LocalSolver.
     *
     * @return The inputs as an array list of strings.
     */
    public ArrayList<String> dumpInputs() {
        ArrayList<String> inputs = new ArrayList<>();
        for (InputElement el : symbolicTrace.getInputs()) {
            inputs.add(el.toString());
        }
        return inputs;
    }

    /**
     * Returns the size of the symbolic trace.
     *
     * @return The size of the symbolic trace.
     */
    public int getTraceSize() {
        return symbolicTrace.getTrace().size();
    }

    /**
     * Fetches the branch constraints from the symbolic trace.
     *
     * @return The branch constraints from the symbolic trace.
     */
    public HashMap<Long, BooleanFormula> getBranchConstraints() {
        HashMap<Long, BooleanFormula> constraints = new HashMap<>();
        for (Element elem : symbolicTrace.getTrace()) {
            if (elem instanceof BranchElement b) {
                constraints.put(b.getIid(), b.getConstraint());
            }
        }
        return constraints;
    }

    /**
     * Fetches all path constraints that are required for a specific branch constraints
     *
     * @param iid The id of the branch constraint
     * @return The path constraints
     */
    public HashMap<Long, BooleanFormula> getPathConstraints(long iid) throws IllegalArgumentException {
        HashMap<Long, BooleanFormula> constraints = new HashMap<>();
        for (Element elem : symbolicTrace.getTrace()) {
            if (elem instanceof BranchElement b) {
                if (b.getIid() == iid) {
                    return constraints;
                }
                constraints.put(b.getIid(), b.getConstraint());
            }
        }
        throw new IllegalArgumentException("No branch constraint with id " + iid + " found");
    }

    /**
     * Fetches the input bounds from the symbolic trace. Each symbolic input has two bounds, the
     * lower and the upper bound of the respective datatype.
     *
     * @return The input bounds from the symbolic trace.
     */
    public ArrayList<BooleanFormula> getInputBounds() throws NotImplementedException {
        ArrayList<BooleanFormula> bounds = new ArrayList<>();
        for (InputElement input : symbolicTrace.getInputs()) {
            bounds.add(input.getValue().getBounds(false));
            bounds.add(input.getValue().getBounds(true));
        }
        return bounds;
    }

    /**
     * Records that a symbolic context loss occurred. This happens when a method is invoked with symbolic arguments
     * but is not instrumented.
     */
    public void recordSymbolicContextLoss() {
        symbolicTrace.setSymbolicContextLoss(true);
    }

    /**
     * Records that a symbolic model was only approximate, so an exhausted search proves nothing.
     */
    public void recordSymbolicPrecisionLoss() {
        symbolicTrace.setSymbolicPrecisionLoss(true);
    }

    /**
     * Records that reference equality semantics may have changed. This happens when Objects.equals
     * is called (via refEquals transformation) on strings where at least one was explicitly created
     * with new String() in user code. In such cases, the original reference equality check would
     * return false for different objects, but value equality returns true for equal content.
     */
    public void recordReferenceSemanticChange() {
        logger.warn("Reference semantic change detected: user-de-interned strings compared via Objects.equals");
        symbolicTrace.setReferenceSemanticChange(true);
    }
}
