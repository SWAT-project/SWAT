package de.uzl.its.swat.symbolic.trace;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sosy_lab.java_smt.api.BooleanFormula;
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

        long threadId = Thread.currentThread().getId();
        BranchElement current;
        FormulaManager fmgr = ThreadHandler.getSolverContext(threadId).getFormulaManager();
        BooleanFormula pathConstraint =
                result ? constraint : fmgr.getBooleanFormulaManager().not(constraint);

        current = new BranchElement(result, pathConstraint, iid);
        // check if the path constraint contains a free/ symbolic variable

        symbolicTrace.addTraceElement(current);
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
    }

    /**
     * Adds a UF to the symbolic trace.
     *
     * @param uf The UF to add.
     */
    public void addUF(BooleanFormula uf) {
        if (uf != null) {
            symbolicTrace.addUF(uf);
            logger.debug("Added UF: {}", uf);
        } else {
            logger.warn("Attempted to add a null UF to the symbolic trace.");
        }
    }

    public List<BooleanFormula> getUFs() {
        return symbolicTrace.getUfs();
    }

    /**
     * Encodes the symbolic trace as a JSON string.
     *
     * @return The symbolic trace encoded as a JSON string.
     */
    public String getTraceDTO() throws NoThreadContextException, JsonProcessingException, NotImplementedException {
        return DTOBuilder.encodeTrace(symbolicTrace);
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
