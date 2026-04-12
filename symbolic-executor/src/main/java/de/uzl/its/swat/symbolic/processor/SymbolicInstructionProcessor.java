package de.uzl.its.swat.symbolic.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.coverage.CoverageType;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.metadata.ClassDepot;
import de.uzl.its.swat.metadata.ClassDepotRuntime;
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor;
import de.uzl.its.swat.symbolic.instruction.*;
import de.uzl.its.swat.symbolic.shadow.ShadowContext;
import de.uzl.its.swat.thread.ThreadHandler;

import java.util.*;
import java.util.stream.Stream;

/**
 * This class is used to process instructions and triggers symbolic execution handling through
 * visiting the current instruction.
 */
public class SymbolicInstructionProcessor extends AbstractInstructionProcessor {

    Config config = Config.instance();
    ClassDepotRuntime classDepot = ClassDepot.getRuntimeInstance();

    /** Constructs a new SymbolicInstructionProcessor. */
    public SymbolicInstructionProcessor() {}

    private boolean isInUnknownSpace = false;
    private boolean isActive = false;
    private boolean isInDelegation = false; // Track when delegation from non-instrumented to instrumented code occurs

    private final Stack<Long> openInvokeIds = new Stack<>();

    /**
     * Returns whether we're currently in a delegation context (non-instrumented → instrumented).
     * This is used to relax certain checks that would otherwise fail during delegation.
     *
     * @return true if currently in delegation context
     */
    public boolean isInDelegation() {
        return isInDelegation;
    }

    private boolean isSpecialInstruction(
            Instruction currentInstruction, String expectedMethodName) {
        return currentInstruction.iid == -1
                || expectedMethodName.equals(config.getInstrumentationPrefix())
                || expectedMethodName.equals("call" + config.getInstrumentationPrefix());
    }

    private boolean isActualClassNameExpectedClassName(
            String actualFull, String actualBase,
            String expectedFull, String expectedBase,
            String actualMethodIdentifier, String expectedMethodIdentifier) {

        // direct match
        if (actualBase.equals(expectedBase)) {
            return true;
        }

        // Check inheritance: actual extends expected (standard polymorphism)
        if (hasAncestor(actualFull, expectedBase)) {
            return true;
        }

        // Check reverse: expected extends actual (less common but valid for some patterns)
        if (hasAncestor(expectedFull, actualBase)) {
            return true;
        }

        // Allow delegation from non-instrumented to instrumented code
        // E.g., java/io/PushbackReader (not instrumented) delegates to IntReader (instrumented)
        // Requirements for valid delegation:
        // 1. Expected class must not be instrumented (standard library)
        // 2. Actual class must be instrumented (user code)
        // 3. Method names must match (e.g., both calling "read")
        //    This is a practical heuristic that works well in practice while being
        //    more restrictive than allowing all non-instrumented → instrumented transitions
        boolean expectedIsInstrumented = Util.shouldInstrument(expectedFull);
        boolean actualIsInstrumented = Util.shouldInstrument(actualFull);


        if(!expectedIsInstrumented && actualIsInstrumented){
            // This serves as a heuristic to detect delegation scenarios
            if(Config.instance().isUseDelegationDetection()) return true;

            // Otherwise we enforce that the method names match exactly

            // Extract method names from identifiers
            // actualMethodIdentifier is in format "className:methodName:descriptor"
            String actualMethodName = Util.extractMethodName(actualMethodIdentifier);
            // expectedMethodIdentifier might just be the plain method name from stack frame
            String expectedMethodName = expectedMethodIdentifier;
            // If it's in the full format, extract it; otherwise use as-is
            if (expectedMethodIdentifier != null && expectedMethodIdentifier.contains(Util.METHOD_DIVIDER)) {
                expectedMethodName = Util.extractMethodName(expectedMethodIdentifier);
            }

            if (actualMethodName != null && actualMethodName.equals(expectedMethodName)) {
                // Non-instrumented code delegating to instrumented code with matching method name
                return true;
            }

        }
        return false;
    }

    /**
     * Detects if we're in a delegation scenario where non-instrumented code
     * is delegating to instrumented code.
     *
     * @param actualFull The actual class name (full)
     * @param expectedFull The expected class name (full)
     * @param actualMethodIdentifier The actual method identifier
     * @param expectedMethodIdentifier The expected method identifier
     * @return true if this is a delegation from non-instrumented to instrumented code
     */
    private boolean isDelegation(
            String actualFull, String expectedFull,
            String actualMethodIdentifier, String expectedMethodIdentifier) {

        boolean expectedIsInstrumented = Util.shouldInstrument(expectedFull);
        boolean actualIsInstrumented = Util.shouldInstrument(actualFull);

        if (!expectedIsInstrumented && actualIsInstrumented) {
            // Extract method names from identifiers
            String actualMethodName = Util.extractMethodName(actualMethodIdentifier);
            String expectedMethodName = expectedMethodIdentifier;
            // If it's in the full format, extract it; otherwise use as-is
            if (expectedMethodIdentifier != null && expectedMethodIdentifier.contains(Util.METHOD_DIVIDER)) {
                expectedMethodName = Util.extractMethodName(expectedMethodIdentifier);
            }

            return actualMethodName != null && actualMethodName.equals(expectedMethodName);
        }

        return false;
    }

    private boolean hasAncestor(String classFull, String targetBase) {
        ClassDepotRuntime classDepot = ClassDepot.getRuntimeInstance();
        // computeSet will only run once per classFull
        Set<String> baseNames = classDepot.getAncestorBaseCache().computeIfAbsent(classFull, key -> {
            List<String> ifaces = classDepot.getInterfacesForClass(key);
            List<String> parents = classDepot.getParentsForClass(key);
            Set<String> bases = new HashSet<>(ifaces.size() + parents.size());
            for (String anc : ifaces)   bases.add(Util.extractBaseCname(anc));
            for (String anc : parents)  bases.add(Util.extractBaseCname(anc));
            return bases;
        });
        return baseNames.contains(targetBase);
    }

    private boolean isActualMethodNameExpectedMethodName(String actualMethodName, String expectedMethodName) {
        return actualMethodName.equals(expectedMethodName);
    }

    /**
     * Initiates the symbolic execution handling by visiting the current instruction. Importantly,
     * the symbolic execution lags behind by one instruction to allow some peeking into the future.
     *
     * @param nextInstruction The instruction that was just executed and should be scheduled to be
     *     executed symbolically in one step.
     */
    @Override
    public void processInstruction(Instruction nextInstruction) {
        try {
            SWATAssert.enforce(!isActive, "Symbolic execution is already active!");
            isActive = true;
            long id = Thread.currentThread().getId();

            // For debugging of large applications
            if(ThreadHandler.getInstructionCount(id) == Config.instance().getLoggingInstructionCount()){
                Logger logger = GlobalLogger.getSymbolicExecutionLogger();
                logger.info("Logging instruction count reached. Enabling detailed logging.");
                ThreadHandler.getShadowStateLogger(id).setLevel(Level.DEBUG);
            }


            // Get the current instruction and the logger

            Instruction currentInstruction = ThreadHandler.getCurrentInstruction(id);
            // Special logger used to visualize the shadow state. Useful for debugging and learning
            Logger logger = ThreadHandler.getShadowStateLogger(id);

            // Coverage - Start
            if (!config.getCoverageType().equals(CoverageType.NONE)
                    && currentInstruction != null
                    && currentInstruction.iid != -1) {
                ThreadHandler.addCoverage(id, currentInstruction.iid);
            }

            // No tracking if only coverage is measured
            if (config.isCoverageOnly()) {
                ThreadHandler.setCurrentInstruction(id, nextInstruction);
                isActive = false;
                return;
            }
            // Coverage - End

            SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(id);

            if (currentInstruction == null) {
                ThreadHandler.setCurrentInstruction(id, nextInstruction);
            } else {
                ThreadHandler.setNextInstruction(id, nextInstruction); // Needs to be moved down?
                visitor.setNextInst(ThreadHandler.getNextInstruction(id));

                String expectedCnameFull = Util.formatClassName(visitor.getStack().getActiveFrame().getClassName());
                String expectedCname = Util.extractBaseCname(expectedCnameFull);
                String expectedMethodName = visitor.getStack().getActiveFrame().getMethodName();
                String expectedDesc =
                        expectedCname
                                + (expectedMethodName.isEmpty() ? "" : ":" + expectedMethodName);

                boolean isSpecialInstruction =
                        isSpecialInstruction(currentInstruction, expectedMethodName);

                int cid = (int) GlobalStateForInstrumentation.extractCid(currentInstruction.iid);
                int mid = (int) GlobalStateForInstrumentation.extractMid(currentInstruction.iid);

                boolean actualClassNameIsExpectedClassName = false;
                boolean actualMethodNameIsExpectedMethodName = false;
                String actualClassNameFull = classDepot.getClassName(cid);
                String actualClassName = Util.extractBaseCname(actualClassNameFull);
                String actualMethodIdentifier = classDepot.getMethodIdentifier(cid, mid);
                String actualMethodName = Util.extractMethodName(actualMethodIdentifier);

                // For expected method identifier, we need to construct it from available info
                // We have the method name but not the full descriptor from the stack frame
                // For now, we'll pass empty string if we can't construct it properly
                String expectedMethodIdentifier = expectedMethodName; // simplified for now

                actualClassNameIsExpectedClassName =
                        isActualClassNameExpectedClassName(
                                actualClassNameFull, actualClassName, expectedCnameFull, expectedCname,
                                actualMethodIdentifier, expectedMethodIdentifier);

                actualMethodNameIsExpectedMethodName =
                        isActualMethodNameExpectedMethodName(actualMethodName, expectedMethodName);

                // Detect and track delegation from non-instrumented to instrumented code
                boolean isDelegation = isDelegation(
                        actualClassNameFull, expectedCnameFull,
                        actualMethodIdentifier, expectedMethodIdentifier);

                boolean isLambdaFrame = expectedCname.equals("LambdaFrame");
                boolean isInitialFrame =
                        expectedCname.equals(ShadowContext.getINITIAL_FRAME_NAME());

                if ((actualClassNameIsExpectedClassName && actualMethodNameIsExpectedMethodName) || isInitialFrame) {
                    if (isInUnknownSpace) {
                        logger.info("Leaving unknown space.");
                        isInUnknownSpace = false;
                    }
                    if (isInDelegation && !isDelegation) {
                        logger.info("Leaving delegation context.");
                        isInDelegation = false;
                    }
                }

                // Track when we enter delegation context
                if (isDelegation && !isInDelegation) {
                    logger.info("Entering delegation context (non-instrumented → instrumented).");
                    isInDelegation = true;
                } else if (isInDelegation && !isDelegation) {
                    logger.info("Leaving delegation context.");
                    isInDelegation = false;
                }

                boolean isActiveClinit = currentInstruction instanceof CLINIT && !isInUnknownSpace;

                boolean closeOpenInvoke = handleInvokeStack(currentInstruction, logger);

                // ToDo: Is handling for nested CLINIT necessary?
                if ((!isInUnknownSpace
                                && (isSpecialInstruction
                                        || isLambdaFrame
                                        || isInitialFrame
                                        || (actualClassNameIsExpectedClassName && actualMethodNameIsExpectedMethodName)
                                        || closeOpenInvoke))
                        || isActiveClinit) {

                    try {
                        logger.info(Util.DIVIDER);
                        logger.info("Stack ({})", expectedDesc);
                        visitor.getStack().getActiveFrame().printStack();
                        logger.info(Util.DIVIDER);
                        logger.info("Instruction: {}", currentInstruction);
                        currentInstruction.accept(visitor);
                    } catch (Exception e) {
                        new ErrorHandler().handleException("Error visiting Instruction " + currentInstruction, e);
                    }
                } else {
                    String unknownSpaceState = !isInUnknownSpace ? "Entering" : "In";
                    isInUnknownSpace = true;
                    logger.info(Util.DIVIDER);
                    logger.info("Stack mismatch! {} unknown space.", unknownSpaceState);
                    logger.info(
                            "Skipping symbolic execution of instruction {} at stack ( Expected: {},"
                                    + " Actual: {})",
                            currentInstruction,
                            expectedCname,
                            actualClassName);
                    logger.info(
                            "( Expected full: {}:{}, Actual full: {}:{}, class name match {}, method name match {} )",
                            expectedCnameFull,
                            expectedMethodName,
                            actualClassNameFull,
                            actualMethodName,
                            actualClassNameIsExpectedClassName,
                            actualMethodNameIsExpectedMethodName);
                }

                ThreadHandler.setCurrentInstruction(id, ThreadHandler.getNextInstruction(id));
            }
        } catch (Throwable t) {
            new ErrorHandler().handleException("Error visiting Instruction!", t);
        } finally {
            long id = Thread.currentThread().getId();
            ThreadHandler.checkThreadContextAbortTimer(id);
        }
        isActive = false;
    }

    private boolean handleInvokeStack(Instruction currentInstruction, Logger logger) {

        boolean closeOpenInvoke = false;
        boolean currentInstructionIsInvokeX =
                currentInstruction instanceof INVOKEDYNAMIC
                        || currentInstruction instanceof INVOKESTATIC
                        || currentInstruction instanceof INVOKEVIRTUAL
                        || currentInstruction instanceof INVOKEINTERFACE
                        || currentInstruction instanceof INVOKESPECIAL
                        || currentInstruction instanceof CLINIT
                        || currentInstruction instanceof SET_FIELD_REFLECTION
                        || currentInstruction instanceof GET_FIELD_REFLECTION;

        boolean currentInstructionIsMethodEndOrException =
                currentInstruction instanceof INVOKEMETHOD_END
                        || currentInstruction instanceof INVOKEMETHOD_EXCEPTION
                        || currentInstruction instanceof INVOKECLINIT_END;

        if (!isInUnknownSpace && currentInstructionIsInvokeX) {
            openInvokeIds.push(((InvokeIdInstruction) currentInstruction).invokeId);
        }

        if (currentInstructionIsMethodEndOrException) {
            if (!openInvokeIds.empty()
                    && openInvokeIds.peek() > 0
                    && openInvokeIds.peek()
                            == ((InvokeIdInstruction) currentInstruction).invokeId) {
                openInvokeIds.pop();
                closeOpenInvoke = true;
                if (isInUnknownSpace) {
                    logger.info("Leaving unknown space.");
                    isInUnknownSpace = false;
                }
            } else if (!openInvokeIds.empty() && !isInUnknownSpace) {
                for (Long invokeId : openInvokeIds.stream().toList()) {
                    SWATAssert.enforce(invokeId != ((InvokeIdInstruction) currentInstruction).invokeId,
                            "Invocation stack is broken!");
                }
            }
        }
        return closeOpenInvoke;
    }
}
