package de.uzl.its.swat.symbolic.processor;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.thread.ThreadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to process instructions and triggers symbolic execution handling through
 * visiting the current instruction.
 */
public class SymbolicInstructionProcessor extends AbstractInstructionProcessor {

    // Special logger used to visualize the shadow state. Useful for debugging and learning
    // purposes.
    private static final Logger logger = LoggerFactory.getLogger("ShadowStateLogger");

    /** Constructs a new SymbolicInstructionProcessor. */
    public SymbolicInstructionProcessor() {}

    /**
     * Initiates the symbolic execution handling by visiting the current instruction. Importantly,
     * the symbolic execution lags behind by one instruction to allow some peeking into the future.
     *
     * @param nextInstruction The instruction that was just executed and should be scheduled to be
     *     executed symbolically in one step.
     */
    @Override
    protected void processInstruction(Instruction nextInstruction) {
        long id = Thread.currentThread().getId();
        Instruction currentInstruction = ThreadHandler.getCurrentInstruction(id);

        SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(id);

        if (currentInstruction == null) {
            ThreadHandler.setCurrentInstruction(id, nextInstruction);
        } else {
            ThreadHandler.setNextInstruction(id, nextInstruction);
            visitor.setNext(ThreadHandler.getNextInstruction(id));
            try {
                logger.info(
                        "--------------------------------------------------------------------------------");
                logger.info("Stack:");
                visitor.getCurrentFrame().printStack();
                logger.info(
                        "--------------------------------------------------------------------------------");
                logger.info("Instruction: " + currentInstruction);
                currentInstruction.accept(visitor);
            } catch (Exception e) {
                new ErrorHandler().handleException("Error visiting Instruction!", e);
            }
            ThreadHandler.setCurrentInstruction(id, ThreadHandler.getNextInstruction(id));
        }
    }
}
