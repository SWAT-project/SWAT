package de.uzl.its.swat.executionData;

import de.uzl.its.swat.thread.ThreadHandler;
import de.uzl.its.symbolic.BranchElement;
import de.uzl.its.symbolic.InputElement;
import de.uzl.its.symbolic.SpecialElement;
import de.uzl.its.symbolic.value.Value;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaManager;

public class SymbolicStateHandler {

    private final SymbolicState symbolicState;

    public SymbolicStateHandler() {
        this.symbolicState = new SymbolicState();
    }

    public void checkAndSetBranch(boolean result, BooleanFormula constraint, int iid) {

        long threadId = Thread.currentThread().getId();
        BranchElement current;
        FormulaManager fmgr = ThreadHandler.getSolverContext(threadId).getFormulaManager();
        BooleanFormula pathConstraint =
                result ? constraint : fmgr.getBooleanFormulaManager().not(constraint);

        current = new BranchElement(result, pathConstraint, iid);
        current.setStackSize(
                ThreadHandler.getConcolicInterpreter(threadId).getCurrentFrame().getStack().size());
        current.setLocalsSize(
                ThreadHandler.getConcolicInterpreter(threadId)
                        .getCurrentFrame()
                        .getLocals()
                        .size());
        current.setCallSize(ThreadHandler.getConcolicInterpreter(threadId).getStack().size());
        symbolicState.addTraceElement(current);
    }

    public void addSpecialElement(int iid, String inst) {
        symbolicState.addTraceElement(new SpecialElement(iid, inst));
    }

    public void addInput(String name, Value<?, ?> value) {
        System.out.println("Input with name: " + name + " and value: " + value + "registered");
        symbolicState.addInputElement(new InputElement(name, value));
    }

    public SymbolicState getExecutionData() {
        return symbolicState;
    }
}
