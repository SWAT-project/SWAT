package de.uzl.its.swat.symbolic.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.StackOverflowGuard;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.coverage.CoverageType;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.metadata.ClassDepot;
import de.uzl.its.swat.metadata.ClassDepotRuntime;
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor;
import de.uzl.its.swat.symbolic.instruction.*;
import de.uzl.its.swat.symbolic.shadow.Frame;
import de.uzl.its.swat.symbolic.shadow.ShadowContext;
import de.uzl.its.swat.symbolic.value.Value;
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
    /** The open invokes that are static initializers (CLINIT). */
    private final Set<Long> clinitInvokeIds = new HashSet<>();
    /**
     * The open invokes of a constructor called from within a constructor. The instrumentation
     * cannot wrap them in a try/catch (the receiver may be the uninitialized this), so an exception
     * leaving them emits no INVOKEMETHOD_EXCEPTION.
     */
    private final Set<Long> unwrappedInitInvokeIds = new HashSet<>();
    /** Set by the probe the instrumentation places before such a constructor call. */
    private boolean nextInvokeIsUnwrappedInit = false;
    /** The value of that probe (see InstructionMethodAdapter.visitMethodInsn). */
    private static final int UNWRAPPED_INIT_PROBE = 420;

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
        // After a stack overflow the shadow state is no longer reliable: stop tracking
        if (StackOverflowGuard.trackingLost | StackOverflowGuard.targetOverflowed) {
            return;
        }
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

                closeClinitsLeftByException(visitor.getStack(), currentInstruction, logger);

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

                if (!isInUnknownSpace && !isInitialFrame && !isLambdaFrame
                        && !(actualClassNameIsExpectedClassName && actualMethodNameIsExpectedMethodName)
                        && adoptLambdaFrame(visitor.getStack(), expectedCnameFull,
                                actualClassNameFull, actualMethodIdentifier, logger)) {
                    actualClassNameIsExpectedClassName = true;
                    actualMethodNameIsExpectedMethodName = true;
                }

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

                if (isDelegation && !hasAncestor(actualClassNameFull, expectedCname)) {
                    // Library code called back into application code, which runs in the library
                    // call's frame: its branches are not in the static pre-analysis graph. (An
                    // application class implementing the frame's library type was called by the
                    // application itself through that type, e.g. Runnable.run(): the graph has it.)
                    visitor.getStack().markActiveFrameCallback();
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

                boolean closeOpenInvoke = handleInvokeStack(currentInstruction, visitor.getStack(), logger);

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
                    if (!isInUnknownSpace) {
                        // Instrumented code is running that no tracked invocation led to (e.g. a
                        // callback from library code). Its instructions are skipped, so whatever
                        // it branches on or writes is lost to the symbolic state.
                        recordUnknownSpaceContextLoss(id, expectedCnameFull, expectedMethodName,
                                actualMethodIdentifier, logger);
                    }
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
        } catch (StackOverflowError e) {
            // Only field writes here: the stack is still exhausted. The error is swallowed, so the
            // target continues as it would without SWAT; the trace is marked as context loss.
            StackOverflowGuard.trackingLost = true;
        } catch (Throwable t) {
            if (StackOverflowGuard.findStackOverflow(t) != null) {
                StackOverflowGuard.trackingLost = true;
            } else {
                new ErrorHandler().handleException("Error visiting Instruction!", t);
            }
        } finally {
            isActive = false;
        }
        if (!(StackOverflowGuard.trackingLost | StackOverflowGuard.targetOverflowed)) {
            ThreadHandler.checkThreadContextAbortTimer(Thread.currentThread().getId());
        }
    }

    /**
     * Lets a lambda body run as the interface method it implements.
     *
     * <p>Calling an interface method on a lambda goes through a class the JVM generates at run
     * time, which is not instrumented, into the synthetic {@code lambda$...} method that holds the
     * body. The frame pushed for the call is named after the interface method, so without this the
     * body would not match it and be skipped. For a lambda that captures nothing (a static
     * {@code lambda$...} method taking exactly the interface method's arguments) the generated
     * class only forwards the arguments, so the frame is replaced by one for the body with the
     * same arguments. Capturing lambdas are left alone: their captured values are not on the
     * frame.
     *
     * @return whether the frame was replaced
     */
    private boolean adoptLambdaFrame(ShadowContext stack, String expectedClassFull, String actualClassFull,
            String actualMethodIdentifier, Logger logger) {
        if (Util.shouldInstrument(expectedClassFull) || !Util.shouldInstrument(actualClassFull)) {
            return false;
        }
        String[] parts = actualMethodIdentifier.split(Util.METHOD_DIVIDER);
        if (parts.length != 3 || !parts[1].startsWith("lambda$")) {
            return false;
        }
        String name = parts[1];
        String desc = parts[2];
        if (!isStaticMethod(actualClassFull, name, desc)) {
            return false;
        }
        Frame expected = stack.getActiveFrame();
        List<Value<?, ?>> locals = expected.getLocals();
        int argSlots = 0;
        for (org.objectweb.asm.Type t : org.objectweb.asm.Type.getArgumentTypes(desc)) {
            argSlots += t.getSize();
        }
        // The interface call's frame holds the lambda object, then the arguments.
        if (locals.size() - 1 != argSlots
                || org.objectweb.asm.Type.getReturnType(desc).getSize() != expected.nReturnWords) {
            return false;
        }
        Frame body = new Frame(actualClassFull, name, expected.nReturnWords);
        body.setCallSite(expected.getCallSite());
        for (Value<?, ?> local : locals.subList(1, locals.size())) {
            body.addLocal(local);
        }
        stack.popFrame();
        stack.pushFrame(body);
        logger.info("Running lambda body {} as {}:{}", actualMethodIdentifier, expectedClassFull,
                expected.getMethodName());
        return true;
    }

    private static boolean isStaticMethod(String classFull, String name, String desc) {
        try {
            Class<?> cls = Class.forName(classFull.replace('/', '.'), false, ClassLoader.getSystemClassLoader());
            for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals(name) && org.objectweb.asm.Type.getMethodDescriptor(m).equals(desc)) {
                    return java.lang.reflect.Modifier.isStatic(m.getModifiers());
                }
            }
        } catch (Throwable t) {
            // Not resolvable here: do not adopt.
        }
        return false;
    }

    private void recordUnknownSpaceContextLoss(long threadId, String expectedClassFull, String expectedMethodName,
            String actualMethodIdentifier, Logger logger) {
        try {
            logger.warn("Untracked instrumented code ({}) runs inside {}:{}: recording symbolic context loss",
                    actualMethodIdentifier, expectedClassFull, expectedMethodName);
            ThreadHandler.getSymbolicTraceHandler(threadId).recordSymbolicContextLoss();
            ThreadHandler.recordContextLossInvocation(threadId, new InvocationEntry(
                    expectedClassFull, expectedMethodName, "", true, -1, true));
        } catch (Throwable t) {
            new ErrorHandler().handleException("Error recording context loss", t);
        }
    }

    /**
     * Closes static initializers that an exception left, once an instruction of the frame below
     * them runs. unwindAbandonedInitializers only catches this when an invoke below ends; an
     * initializer triggered by a field access or {@code new} has none, and the next instruction
     * already belongs to the handler of the frame below. Without this, that handler would run as
     * untracked code inside the initializer's frame.
     */
    private void closeClinitsLeftByException(ShadowContext stack, Instruction inst, Logger logger) {
        if (isInUnknownSpace || inst.iid == -1) {
            return;
        }
        int cid = (int) GlobalStateForInstrumentation.extractCid(inst.iid);
        int mid = (int) GlobalStateForInstrumentation.extractMid(inst.iid);
        String actualClass = Util.formatClassName(classDepot.getClassName(cid));
        String actualMethod = Util.extractMethodName(classDepot.getMethodIdentifier(cid, mid));
        Stack<Frame> frames = stack.getFrameStack();
        while (frames.size() > 1 && stack.getActiveFrame().getMethodName().equals("<clinit>")
                && !openInvokeIds.empty() && clinitInvokeIds.contains(openInvokeIds.peek())) {
            Frame below = frames.get(frames.size() - 2);
            // main runs in the initial frame, which matches any instruction (see process())
            boolean belowIsInitial = Util.extractBaseCname(Util.formatClassName(below.getClassName()))
                    .equals(ShadowContext.getINITIAL_FRAME_NAME());
            if (!belowIsInitial && (!Util.formatClassName(below.getClassName()).equals(actualClass)
                    || !below.getMethodName().equals(actualMethod))) {
                return;
            }
            if (belowIsInitial && actualMethod.equals("<clinit>")) {
                return; // still inside an initializer
            }
            long clinit = openInvokeIds.pop();
            clinitInvokeIds.remove(clinit);
            stack.popFrame();
            logger.info("Closing static initializer (invoke {}) left by an exception", clinit);
        }
    }

    /**
     * A static initializer left by an exception emits no INVOKECLINIT_END, and a constructor
     * called from within a constructor left by an exception emits no INVOKEMETHOD_EXCEPTION, so
     * their invokes and shadow frames stay open. When the end (or exception) of an invoke below
     * them arrives, they are certainly over: close them first, as the JVM did when it unwound them.
     */
    private void unwindAbandonedInitializers(long invokeId, ShadowContext stack, Logger logger) {
        if (openInvokeIds.empty() || openInvokeIds.peek() == invokeId || !openInvokeIds.contains(invokeId)) {
            return;
        }
        List<Long> above = openInvokeIds.subList(openInvokeIds.lastIndexOf(invokeId) + 1, openInvokeIds.size());
        for (long id : above) {
            if (!clinitInvokeIds.contains(id) && !unwrappedInitInvokeIds.contains(id)) {
                return; // not only initializers: leave it to the consistency check below
            }
        }
        while (openInvokeIds.peek() != invokeId) {
            long open = openInvokeIds.pop();
            String method = clinitInvokeIds.remove(open) ? "<clinit>" : "<init>";
            unwrappedInitInvokeIds.remove(open);
            if (stack.getActiveFrame().getMethodName().equals(method)) {
                stack.popFrame();
            }
            logger.info("Closing initializer {} (invoke {}) left by an exception", method, open);
        }
    }

    private boolean handleInvokeStack(Instruction currentInstruction, ShadowContext stack, Logger logger) {

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

        if (currentInstruction instanceof SPECIAL special && special.i == UNWRAPPED_INIT_PROBE) {
            nextInvokeIsUnwrappedInit = !isInUnknownSpace;
        }

        if (!isInUnknownSpace && currentInstructionIsInvokeX) {
            openInvokeIds.push(((InvokeIdInstruction) currentInstruction).invokeId);
            if (currentInstruction instanceof CLINIT) {
                clinitInvokeIds.add(((InvokeIdInstruction) currentInstruction).invokeId);
            }
            if (nextInvokeIsUnwrappedInit && currentInstruction instanceof INVOKESPECIAL) {
                unwrappedInitInvokeIds.add(((InvokeIdInstruction) currentInstruction).invokeId);
            }
        }
        if (currentInstructionIsInvokeX) {
            nextInvokeIsUnwrappedInit = false;
        }

        if (currentInstructionIsMethodEndOrException) {
            unwindAbandonedInitializers(((InvokeIdInstruction) currentInstruction).invokeId, stack, logger);
            if (!openInvokeIds.empty()
                    && openInvokeIds.peek() > 0
                    && openInvokeIds.peek()
                            == ((InvokeIdInstruction) currentInstruction).invokeId) {
                unwrappedInitInvokeIds.remove(openInvokeIds.pop());
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
