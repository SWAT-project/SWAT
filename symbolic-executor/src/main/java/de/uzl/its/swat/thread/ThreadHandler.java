package de.uzl.its.swat.thread;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.uzl.its.swat.common.exceptions.*;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.ErrorRecord;
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
import java.io.PrintStream;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
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

    @SuppressWarnings("unused")
    private static SolverContext solverContext;

    private static final LogFormatter logFormatter = new LogFormatter();
    private static final Config config = Config.instance();

    public static void init() throws InvalidConfigurationException {
        solverContext =
                SolverContextFactory.createSolverContext(
                        Configuration.defaultConfiguration(),
                        BasicLogManager.create(Configuration.defaultConfiguration()),
                        ShutdownManager.create().getNotifier(),
                        SolverContextFactory.Solvers.Z3);
    }

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
                ConstraintRequest.sendConstraints(
                        symbolicStateHandler.getTraceDTO(), endpointID, traceID);
            }

        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static int getThreadCount() {
        return threadContextHashMap.size();
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

    public static void recordException(long id, ErrorRecord er) throws NoThreadContextException  {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.recordException(er);
        } else {
            throw new NoThreadContextException(id);
        }
    }

    public static void logStats(long id) throws NoThreadContextException  {
        if (!config.isLoggingStats()) return;
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            PrintStream ps = context.getStatsStream();
            try {
                ps.println(context.getStatsStorage().convertToJson());
            } catch (JsonProcessingException e) {
                logger.warn("Error logging missing invocation context.");
            }
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
