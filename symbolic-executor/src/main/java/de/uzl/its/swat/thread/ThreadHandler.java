package de.uzl.its.swat.thread;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.constraint.ConstraintRequest;
import de.uzl.its.swat.executionData.SymbolicStateHandler;
import de.uzl.its.swat.interpreters.SymbolicInterpreter;
import de.uzl.its.swat.logger.DirectConcolicExecution;
import de.uzl.its.swat.logger.DummyLogger;
import de.uzl.its.swat.logger.Logger;
import de.uzl.its.swat.logger.ObjectInfo;
import de.uzl.its.swat.logger.inst.Instruction;
import de.uzl.its.symbolic.value.Value;
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
    private static final DummyLogger dummyLogger = new DummyLogger();
    private static final DirectConcolicExecution concolicLogger = new DirectConcolicExecution();

    private static final HashMap<Long, ThreadContext> threadContextHashMap = new HashMap<>();

    @SuppressWarnings("unused")
    private static SolverContext solverContext;

    private static final LogFormatter logFormatter = new LogFormatter();
    private static final Config config = Config.instance();

    public static void init() {
        try {
            solverContext =
                    SolverContextFactory.createSolverContext(
                            Configuration.defaultConfiguration(),
                            BasicLogManager.create(Configuration.defaultConfiguration()),
                            ShutdownManager.create().getNotifier(),
                            SolverContextFactory.Solvers.Z3);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addThreadContext(long id) {
        if (!threadContextHashMap.containsKey(id)) {
            System.out.println("Adding context for thread: " + id);
            threadContextHashMap.put(id, new ThreadContext(id));
        } else {
            throw new RuntimeException(
                    "A thread cannot have more than one context ("
                            + threadContextHashMap.size()
                            + " / "
                            + Thread.activeCount()
                            + ")");
        }
    }

    public static void removeThreadContext(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            threadContextHashMap.remove(id);
        } else {
            throw new RuntimeException("The thread " + id + " has no context to remove");
        }
    }

    public static void sendData(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            int endpointID = getEndpointID(id);
            int traceID = ThreadLocalRandom.current().nextInt();
            SymbolicStateHandler symbolicStateHandler =
                    getConcolicInterpreter(id).getSymbolicStateHandler();
            ConstraintRequest.sendConstraints(
                    symbolicStateHandler.getExecutionData(), endpointID, traceID);

        } else {
            throw new RuntimeException("The thread " + id + " has no context");
        }
    }

    public static int getThreadCount() {
        return threadContextHashMap.size();
    }

    public static void removeSolverContext(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            SolverContext solverContext = context.getSolverContext();
            solverContext.close();
        } else {
            throw new RuntimeException("The thread " + id + " has no context!");
        }
    }

    public static SolverContext getSolverContext(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getSolverContext();
        } else {
            throw new RuntimeException("The thread " + id + " has no context!");
        }
    }

    public static Logger getLogger(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.isDisabled() ? dummyLogger : concolicLogger;
        } else {
            return dummyLogger;
        }
    }

    public static void enableThreadContext(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.enable();
        } else {
            throw new RuntimeException(
                    "ThreadContext for thread "
                            + id
                            + " cannot be enabled because thread has no context");
        }
    }

    public static void disableThreadContext(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.disable();
        } else {
            throw new RuntimeException(
                    "ThreadContext for thread "
                            + id
                            + " cannot be disabled because thread has no context");
        }
    }

    public static void setNextInstruction(long id, Instruction next) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setNext(next);
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static Instruction getNextInstruction(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getNext();
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static void setCurrentInstruction(long id, Instruction current) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setCurrent(current);
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static Instruction getCurrentInstruction(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getCurrent();
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static void logInvocation(long id, LogRecord logRecord) {
        if (!config.isInvocationLogging()) return;
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.getInvocationStream().print(logFormatter.format(logRecord));
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static SymbolicInterpreter getConcolicInterpreter(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getSymbolicInterpreter();
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static void setEndpointName(long id, String endpointName) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setEndpointName(endpointName);
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }
    public static String getEndpointName(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getEndpointName();
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static void setEndpointID(long id, int endpointID) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setEndpointID(endpointID);
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static int getEndpointID(long id) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getEndpointID();
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static void setStaticField(
            long id, ObjectInfo oi, int classId, int fieldId, Value<?, ?> value) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            context.setStaticField(oi, classId, fieldId, value);
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }

    public static Value<?, ?> getStaticField(long id, ObjectInfo oi, int classId, int fieldId) {
        ThreadContext context = threadContextHashMap.get(id);
        if (context != null) {
            return context.getStaticField(oi, classId, fieldId);
        } else {
            throw new RuntimeException("No thread context found for thread " + id);
        }
    }
}
