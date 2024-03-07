package de.uzl.its.swat.symbolic.trace;

import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaManager;

/**
 * Manages access to the trace of the symbolic execution. This class should be used for all access
 * to the trace package.
 */
public class SymbolicTraceHandler {

    // The symbolic trace storing constraints and inputs
    private final SymbolicTrace symbolicTrace;

    private static final Logger logger = LoggerFactory.getLogger(SymbolicTraceHandler.class);

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
    public void checkAndSetBranch(boolean result, BooleanFormula constraint, int iid) {

        long threadId = Thread.currentThread().getId();
        BranchElement current;
        FormulaManager fmgr = ThreadHandler.getSolverContext(threadId).getFormulaManager();
        BooleanFormula pathConstraint =
                result ? constraint : fmgr.getBooleanFormulaManager().not(constraint);

        current = new BranchElement(result, pathConstraint, iid);
        symbolicTrace.addTraceElement(current);
    }

    /**
     * Adds a special element to the symbolic trace. This is used to catch all unsupported branching
     * behaviour.
     *
     * @param iid The iid of the special instruction.
     * @param inst The instruction of the special instruction.
     */
    public void addSpecialElement(int iid, String inst) {
        symbolicTrace.addTraceElement(new SpecialElement(iid, inst));
    }

    /**
     * Adds an input to the symbolic trace.
     *
     * @param name The name of the input.
     * @param value The value of the input.
     */
    public void addInput(String name, Value<?, ?> value) {
        logger.info("Input with name: " + name + " and value: " + value + "registered");
        symbolicTrace.addInputElement(new InputElement(name, value));
    }

    /**
     * Encodes the symbolic trace as a JSON string.
     *
     * @return The symbolic trace encoded as a JSON string.
     */
    public String getTraceDTO() {
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
     * Dumps all inputs to the logger. Used for debugging purposes and by the LocalSolver.
     *
     * @param logger The logger to dump the inputs to.
     */
    public void dumpInputs(Logger logger) {
        for (InputElement el : symbolicTrace.getInputs()) {
            logger.info(el.toString());
        }
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
    public HashMap<Integer, BooleanFormula> getBranchConstraints() {
        HashMap<Integer, BooleanFormula> constraints = new HashMap<>();
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
    public HashMap<Integer, BooleanFormula> getPathConstraints(int iid)
            throws IllegalArgumentException {
        HashMap<Integer, BooleanFormula> constraints = new HashMap<>();
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
    public ArrayList<BooleanFormula> getInputBounds() {
        ArrayList<BooleanFormula> bounds = new ArrayList<>();
        for (InputElement input : symbolicTrace.getInputs()) {
            bounds.add(input.getValue().getBounds(false));
            bounds.add(input.getValue().getBounds(true));
        }
        return bounds;
    }
}
