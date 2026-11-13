package de.uzl.its.swat.thread;

import de.uzl.its.swat.common.Profile;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.uzl.its.swat.common.exceptions.*;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.coverage.CoverageRequest;
import de.uzl.its.swat.coverage.CoverageType;
import de.uzl.its.swat.request.ConstraintRequest;
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor;
import de.uzl.its.swat.symbolic.UFs.UFHandler;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.symbolic.processor.DummyInstructionProcessor;
import de.uzl.its.swat.symbolic.processor.InstructionProcessor;
import de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.api.SolverContext;

class LogFormatter extends SimpleFormatter {
    @Override
    public String format(LogRecord logRecord) {
        return logRecord.getLevel() + ": " + formatMessage(logRecord) + "\n";
    }
}

public final class ThreadHandler {

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    private static final DummyInstructionProcessor DUMMY_INSTRUCTION_PROCESSOR =
            new DummyInstructionProcessor();
    private static final SymbolicInstructionProcessor SYMBOLIC_INSTRUCTION_PROCESSOR =
            new SymbolicInstructionProcessor();

    private static final HashMap<Long, ThreadContext> threadContextHashMap = new HashMap<>();

    private static final LogFormatter logFormatter = new LogFormatter();
    private static final Config config = Config.instance();

    public static boolean hasThreadContext(long id) {
        return threadContextHashMap.containsKey(id);
    }

    /**
     * This method cannot throw exceptions, because it is called directly from the target application
     * @param id the thread id
     */
    public static void addThreadContext(long id, String endpointName, int endpointId) throws DuplicateThreadContextException, InvalidConfigurationException {
        if (!threadContextHashMap.containsKey(id)) {
            logger.info("Adding context for thread: {}", id);
            threadContextHashMap.put(id, new ThreadContext(id, endpointName, endpointId));
        } else {
            throw new DuplicateThreadContextException(id);
        }
    }

    /**
     * Marks the thread's execution as started before its endpoint was entered.
     *
     * @param id the thread id
     */
    public static void setStartedBeforeEndpoint(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setStartedBeforeEndpoint(true);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    /**
     * Enters the endpoint of an execution that was started before it.
     *
     * @param id the thread id
     * @return true if the execution was waiting for its endpoint, false otherwise
     */
    public static boolean enterStartedEndpoint(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null && context.isStartedBeforeEndpoint()) {
            context.setStartedBeforeEndpoint(false);
            return true;
        }
        return false;
    }

    /**
     * Records that an endpoint was entered on a thread whose symbolic execution is already running.
     *
     * @param id the thread id
     */
    public static void enterNestedEndpoint(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setNestedEndpoints(context.getNestedEndpoints() + 1);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    /**
     * Leaves a nested endpoint if one is open.
     *
     * @param id the thread id
     * @return true if a nested endpoint was left, false if the outermost endpoint ends
     */
    public static boolean leaveNestedEndpoint(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null && context.getNestedEndpoints() > 0) {
            context.setNestedEndpoints(context.getNestedEndpoints() - 1);
            return true;
        }
        return false;
    }

    public static void removeThreadContext(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            threadContextHashMap.remove(id);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void checkThreadContextAbortTimer(long id) {
        if (Config.instance().isUseAbortTimer()) {
            ThreadContext context = threadContextHashMap.get(id);
            if (context != null) {
                context.checkAbortTimerExpiration();
            } else {
                logger.error("No thread context found for id: {}. Cannot check abort timer.", id);
            }
        }
    }

    public static boolean isThreadContextAborted(long id) throws NoThreadContextException {
        if (Config.instance().isUseAbortTimer()) {
            ThreadContext context = threadContextHashMap.get(id);
            if (context != null) {
                return context.isAborted();
            } else {
                throw new NoThreadContextException(id);
            }
        } else {
            return false;
        }
    }

    public static Logger getShadowStateLogger(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getShadowStateLogger();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static SymbolicTraceHandler getSymbolicTraceHandler(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getSymbolicTraceHandler();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static UFHandler getUFHandler(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getUfHandler();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static de.uzl.its.swat.common.logging.StatsStorage getStatsStorage(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getStatsStorage();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void sendData(long id) throws NoThreadContextException, JsonProcessingException, NotImplementedException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            int endpointID = getEndpointID(id);
            int traceID = ThreadLocalRandom.current().nextInt();
            SymbolicTraceHandler symbolicStateHandler =
                    getSymbolicVisitor(id).getSymbolicTraceHandler();

            // Todo rework the config options to be more intuitive
            if(!config.getCoverageType().equals(CoverageType.NONE) && !config.isConstraintsOnly()) {
                CoverageRequest.sendCoverage(
                        context.getCoverageTraceHandler().getCoverageDTO(), endpointID, traceID);
                CoverageRequest.sendTotalBranches();

            }
            if(!config.isCoverageOnly()) {
                String traceDTO = symbolicStateHandler.getTraceDTO(context.getStatsStorage(), id);
                Profile.mark("trace.serialized");
                ConstraintRequest.sendConstraints(traceDTO, endpointID, traceID);
                Profile.mark("trace.sent");
            }

        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static int getThreadCount() {
        return threadContextHashMap.size();
    }

    /**
     * The ids of the target threads whose symbolic execution has started and not been terminated
     * (the context of the instrumentation itself, id -1, is left out).
     */
    public static List<Long> getActiveThreadContextIds() {
        List<Long> ids = new ArrayList<>();
        for (Map.Entry<Long, ThreadContext> e : threadContextHashMap.entrySet()) {
            if (e.getKey() != -1 && !e.getValue().isDisabled()) {
                ids.add(e.getKey());
            }
        }
        return ids;
    }

    public static void removeSolverContext(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            SolverContext solverContext = context.getSolverContext();
            solverContext.close();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static SolverContext getSolverContext(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getSolverContext();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static InstructionProcessor getProcessor(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.isDisabled()
                    ? DUMMY_INSTRUCTION_PROCESSOR
                    : SYMBOLIC_INSTRUCTION_PROCESSOR;
        } else {
            return DUMMY_INSTRUCTION_PROCESSOR;
        }
    }

    public static void enableThreadContext(long id) throws NoThreadContextException, ThreadAlreadyEnabledException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.enable();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void disableThreadContext(long id) throws NoThreadContextException, ThreadAlreadyDisabledException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.disable();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void setNextInstruction(long id, Instruction next)  throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setNext(next);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static Instruction getNextInstruction(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getNext();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void setCurrentInstruction(long id, Instruction current) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setCurrent(current);
            context.setInstCnt(context.getInstCnt() + 1);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static Long getInstructionCount(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getInstCnt();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static Instruction getCurrentInstruction(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getCurrent();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void recordMissingInvocation(
            long id, InvocationEntry entry) throws NoThreadContextException  {
        if (!config.isLoggingStats()) return;
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.recordMissingInvocation(entry);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void recordContextLossInvocation(
            long id, InvocationEntry entry) throws NoThreadContextException  {
        if (!config.isLoggingStats()) return;
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.recordContextLossInvocation(entry);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static SymbolicInstructionVisitor getSymbolicVisitor(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getSymbolicInstructionVisitor();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void setEndpointName(long id, String endpointName) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setEndpointName(endpointName);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static String getEndpointName(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getEndpointName();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void setEndpointID(long id, int endpointID) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setEndpointID(endpointID);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static int getEndpointID(long id) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getEndpointID();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void setStaticField(
            long id, int classId, int fieldId, Value<?, ?> value)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setStaticField(classId, fieldId, value);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    /** The shadow values of the static fields, by class: those the thread has accessed so far. */
    public static java.util.Map<String, Value<?, ?>[]> getStaticFields(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getStaticFields();
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static Value<?, ?> getStaticField(long id, int classIndex, int runtimeFieldIndex)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getStaticField(classIndex, runtimeFieldIndex);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void addCoverage(long id, long iid) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.addCoverage(iid);
        } else {
            throw new NoThreadContextException(id);
        }
    }
    public static long getNextSubUid(long id, String symbolicVar) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getNextSubUid(symbolicVar);
        } else {
            throw new NoThreadContextException(id);
        }
    }
    public static HashMap<String, Integer> getSymbolicIdxOccurrence(long id) throws NoThreadContextException {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getSymbolicIdxOccurrence();
        } else {
            throw new NoThreadContextException(id);
        }
    }
}
