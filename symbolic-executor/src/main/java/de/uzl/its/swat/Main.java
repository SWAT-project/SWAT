package de.uzl.its.swat;

import static java.lang.Thread.currentThread;

import de.uzl.its.swat.common.SystemLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.Transformer;
import de.uzl.its.swat.instrument.TransformerType;
import de.uzl.its.swat.instrument.svcomp.Verifier;
import de.uzl.its.swat.solver.LocalSolver;
import de.uzl.its.swat.symbolic.SymbolicInstructionDispatcher;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Main runtime environment for parsing inputs and writing state files. Holds functions that are
 * added during instrumentation.
 */
public class Main {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Main.class);
    private static final SystemLogger systemLogger;
    private static final Config config = Config.instance();

    static {
        systemLogger = new SystemLogger();
        systemLogger.startBox(60, "Execution of target application started!");
        systemLogger.addToBox("Once a symbolic method is executed, the corresponding");
        systemLogger.addToBox("thread will provide further logging");
        systemLogger.addToBox("");
        systemLogger.addToBox("The following classes are currently instrumented using ");
        systemLogger.addToBox("the following Transformers:");
        if (config.getTransformerType().equals(TransformerType.SV_COMP)) {
            Verifier.retrieveInputs(); // ToDo only conditionally and at a petter place
        }
        var instrumentedClasses = Transformer.getInstrumentedClasses();
        for (var entry : instrumentedClasses.entrySet()) {
            systemLogger.addToBox(entry.getKey());
            for (var transformer : entry.getValue()) {
                systemLogger.addToBox("    => " + transformer.toString());
            }
            logger.info(systemLogger.endBox());
        }
    }

    /**
     * Temporarily disables logging, e.g. while loading a class.
     *
     * <p>Should be enabled again with `reEnableLog()`
     */
    public static void disableSymbolicTracking() {
        ThreadHandler.disableThreadContext(currentThread().getId());
    }

    /**
     * Re-enable the logger, e.g. after a class has been loaded. Before re-enabling
     * `temporaryDisableLog` has to be called.
     */
    public static void enableSymbolicTracking() {
        ThreadHandler.enableThreadContext(currentThread().getId());
    }

    /**
     * Initializes symbolic or coverage only tracking for the current execution. The method call is
     * added during instrumentation
     *
     * @param endpoint The name of the endpoint.
     */
    public static void init(String endpoint) {
        int endpointID;
        if (config.getTransformerType().equals(TransformerType.SV_COMP)) {
            endpointID = 0;
        } else {
            endpointID = endpoint.hashCode();
        }
        ThreadHandler.addThreadContext(currentThread().getId());
        ThreadHandler.setEndpointID(currentThread().getId(), endpointID);
        ThreadHandler.setEndpointName(currentThread().getId(), endpoint);

        systemLogger.fullBox(
                60,
                "Symbolic execution started (Thread: " + Thread.currentThread().getId() + ")",
                new ArrayList<>(
                        List.of(
                                new String[] {
                                    "Execution started (Thread: "
                                            + Thread.currentThread().getId()
                                            + ")",
                                    "Threads (tracked / active): ("
                                            + ThreadHandler.getThreadCount()
                                            + " / "
                                            + Thread.activeCount()
                                            + ")",
                                    "Method: " + endpoint,
                                    "ID: " + endpointID
                                })));
    }

    /**
     * Finishes symbolic tracking and initializes constrain transfer to coordinator. Method call
     * added during instrumentation.
     */
    public static void terminate() {
        ThreadHandler.disableThreadContext(currentThread().getId());

        String[] lines = {
            "Symbolic execution finished (Thread: " + currentThread().getId() + ")",
            "Threads (tracked / active): ("
                    + ThreadHandler.getThreadCount()
                    + " / "
                    + Thread.activeCount()
                    + ")",
        };
        // ToDo release: Add output saying symbex finsished?

        switch (config.getSolverRequest()) {
            case LOCAL:
                LocalSolver.solve();
                break;
            case HTTP:
                ThreadHandler.sendData(currentThread().getId());
                break;
            default:
                throw new RuntimeException("Unknown solver request");
        }

        SymbolicInstructionDispatcher.flush(); // TODO WTF does that do? calls log(null)
        ThreadHandler.removeSolverContext(currentThread().getId());
        ThreadHandler.removeThreadContext(currentThread().getId());
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param intValue The concrete value to be considered for symbolic analysis.
     * @return The original long value, with instrumentation logic potentially applied during
     *     runtime.
     */
    @SuppressWarnings("unused")
    public static int MakeSymbolic(int intValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value: " + intValue);
        return intValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param longValue The concrete long value to be considered for symbolic analysis.
     * @return The original long value, with instrumentation logic potentially applied during
     *     runtime.
     */
    @SuppressWarnings("unused")
    public static long MakeSymbolic(long longValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value: " + longValue);
        return longValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param charValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static char MakeSymbolic(char charValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value: " + charValue);
        return charValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param byteValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static byte MakeSymbolic(byte byteValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value: " + byteValue);
        return byteValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param shortValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static short MakeSymbolic(short shortValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value: " + shortValue);
        return shortValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param boolValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static boolean MakeSymbolic(boolean boolValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + boolValue);
        return boolValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param stringValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static String MakeSymbolic(String stringValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value: " + stringValue);
        return stringValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param floatValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static float MakeSymbolic(float floatValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value: " + floatValue);
        return floatValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param doubleValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static double MakeSymbolic(double doubleValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + doubleValue);
        return doubleValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param longValue The concrete value to be considered for symbolic analysis.
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static Long MakeSymbolic(Long longValue) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param intValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static int MakeSymbolic(int intValue, String namePrefix) {
        logger.info(
                "[DSE] Initializing symbolic tracking for the following value "
                        + intValue
                        + " with identifier "
                        + namePrefix);
        return intValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param longValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static long MakeSymbolic(long longValue, String namePrefix) {
        logger.info(
                "[DSE] Initializing symbolic tracking for the following value "
                        + longValue
                        + " with identifier "
                        + namePrefix);
        return longValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param charValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static char MakeSymbolic(char charValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + charValue);
        return charValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param byteValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static byte MakeSymbolic(byte byteValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + byteValue);
        return byteValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param shortValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static short MakeSymbolic(short shortValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + shortValue);
        return shortValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param boolValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static boolean MakeSymbolic(boolean boolValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + boolValue);
        return boolValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param stringValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static String MakeSymbolic(String stringValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + stringValue);
        return stringValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param floatValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static float MakeSymbolic(float floatValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + floatValue);
        return floatValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param doubleValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static double MakeSymbolic(double doubleValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + doubleValue);
        return doubleValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param longValue The concrete value to be considered for symbolic analysis.
     * @param namePrefix The symbolic identifier of the variable
     * @return The original value, with instrumentation logic potentially applied during runtime.
     */
    @SuppressWarnings("unused")
    public static Long MakeSymbolic(Long longValue, String namePrefix) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param intValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static int MakeSymbolic(int intValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + intValue);
        return intValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param longValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static long MakeSymbolic(long longValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param charValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static char MakeSymbolic(char charValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + charValue);
        return charValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param byteValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static byte MakeSymbolic(byte byteValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + byteValue);
        return byteValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param shortValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static short MakeSymbolic(short shortValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + shortValue);
        return shortValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param boolValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static boolean MakeSymbolic(boolean boolValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + boolValue);
        return boolValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param stringValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static String MakeSymbolic(String stringValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + stringValue);
        return stringValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param floatValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static float MakeSymbolic(float floatValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + floatValue);
        return floatValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param doubleValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static double MakeSymbolic(double doubleValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + doubleValue);
        return doubleValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * @param longValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static Long MakeSymbolic(Long longValue, long idx) {
        logger.info("[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * Stub function, call is added during instrumentation
     *
     * @param arg The value (argument) to check
     * @param identifier The type identifier of the arg
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(Object arg, String identifier, String sink) {}

    /**
     * Stub function, call is added during instrumentation
     *
     * @param arg1 The first value (argument) to check
     * @param arg2 The second value (argument) to check
     * @param identifier The type identifier of the arguments (arg1, arg2)
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(Object arg1, Object arg2, String identifier, String sink) {}

    /**
     * Stub function, call is added during instrumentation
     *
     * @param arg1 The first value (argument) to check
     * @param arg2 The second value (argument) to check
     * @param arg3 The third value (argument) to check
     * @param identifier The type identifier of the arguments (arg1, arg2, arg3)
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(
            Object arg1, Object arg2, Object arg3, String identifier, String sink) {}

    /**
     * Stub function, call is added during instrumentation
     *
     * @param arg1 The first value (argument) to check
     * @param arg2 The second value (argument) to check
     * @param arg3 The third value (argument) to check
     * @param arg4 The fourth value (argument) to check
     * @param identifier The type identifier of the arguments (arg1, arg2, arg3, arg4)
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(
            Object arg1, Object arg2, Object arg3, Object arg4, String identifier, String sink) {}

    /**
     * Stub function, call is added during instrumentation
     *
     * @param arg1 The first value (argument) to check
     * @param arg2 The second value (argument) to check
     * @param arg3 The third value (argument) to check
     * @param arg4 The fourth value (argument) to check
     * @param arg5 The fifth value (argument) to check
     * @param identifier The type identifier of the arguments (arg1, arg2, arg3, arg4, arg5)
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(
            Object arg1,
            Object arg2,
            Object arg3,
            Object arg4,
            Object arg5,
            String identifier,
            String sink) {}

    /**
     * Stub function, call is added during instrumentation
     *
     * @param arg1 The first value (argument) to check
     * @param arg2 The second value (argument) to check
     * @param arg3 The third value (argument) to check
     * @param arg4 The fourth value (argument) to check
     * @param arg5 The fifth value (argument) to check
     * @param arg6 The sixth value (argument) to check
     * @param identifier The type identifier of the arguments (arg1, arg2, arg3, arg4, arg5, arg6)
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(
            Object arg1,
            Object arg2,
            Object arg3,
            Object arg4,
            Object arg5,
            Object arg6,
            String identifier,
            String sink) {}
}
