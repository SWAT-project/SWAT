package de.uzl.its.swat;

import static java.lang.Thread.currentThread;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.executionData.SymbolicStateHandler;
import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import de.uzl.its.swat.instrument.DataType;
import de.uzl.its.swat.instrument.Transformer;
import de.uzl.its.swat.instrument.TransformerType;
import de.uzl.its.swat.instrument.svcomp.Verifier;
import de.uzl.its.swat.instrument.symbolicwrapper.SurroundingTryCatchMethodAdapter;
import de.uzl.its.swat.instrument.symbolicwrapper.SymbolicMethodAdapter;
import de.uzl.its.swat.interpreters.SymbolicInterpreter;
import de.uzl.its.swat.invoke.InternalInvocation;
import de.uzl.its.swat.logger.DJVM;
import de.uzl.its.swat.logger.SystemLogger;
import de.uzl.its.swat.thread.ThreadHandler;
import de.uzl.its.symbolic.value.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.objectweb.asm.Type;

/**
 * Main runtime environment for parsing inputs and writing state files. Holds functions that are
 * added during instrumentation.
 */
public class Main {
    private static final Logger logger;
    private static final SystemLogger systemLogger;
    private static final Config config = Config.instance();

    static {
        systemLogger = new SystemLogger();
        logger = systemLogger.getLogger();
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
            systemLogger.endBox();
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

        if (Objects.requireNonNull(config.getSolverRequest()) == Config.SolverRequestImpl.HTTP) {
            ThreadHandler.sendData(currentThread().getId());
        } else {
            throw new RuntimeException("Unknown solver request");
        }

        DJVM.flush(); // TODO WTF does that do? calls log(null)
        ThreadHandler.removeSolverContext(currentThread().getId());
        ThreadHandler.removeThreadContext(currentThread().getId());
    }

    /**
     * Stub function, call is added during instrumentation and handled during symbolic execution.
     *
     * @param intValue The concrete value
     */
    @SuppressWarnings("unused")
    public static int MakeSymbolic(int intValue) {
        return intValue;
    }

    /**
     * A stub function for symbolic value creation. This method is intended to be instrumented
     * during runtime. The actual symbolic processing is performed elsewhere.
     *
     * <p>During instrumentation, calls to this method are added in {@link
     * AbstractMethodAdapter#symbolicLong(int)}. These calls are then caught in the {@link
     * SymbolicInterpreter}, and the symbolic handling is done in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param longValue The concrete long value to be considered for symbolic analysis.
     * @return The original long value, with instrumentation logic potentially applied during
     *     runtime.
     */
    @SuppressWarnings("unused")
    public static long MakeSymbolic(long longValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicChar(int) AbstractMethodAdapter.symbolicChar(int)}, caught in
     * the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param charValue The concrete value
     */
    @SuppressWarnings("unused")
    public static char MakeSymbolic(char charValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + charValue);
        return charValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicByte(int) AbstractMethodAdapter.symbolicByte(int)}, caught in
     * the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param byteValue The concrete value
     */
    @SuppressWarnings("unused")
    public static byte MakeSymbolic(byte byteValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + byteValue);
        return byteValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicShort(int) AbstractMethodAdapter.symbolicShort(int)}, caught in
     * the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param shortValue The concrete value
     */
    @SuppressWarnings("unused")
    public static short MakeSymbolic(short shortValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + shortValue);
        return shortValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicBoolean(int) AbstractMethodAdapter.symbolicBoolean(int)},
     * caught in the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param boolValue The concrete value
     */
    @SuppressWarnings("unused")
    public static boolean MakeSymbolic(boolean boolValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + boolValue);
        return boolValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicString(int) AbstractMethodAdapter.symbolicString(int)}, caught
     * in the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param stringValue The concrete value
     */
    @SuppressWarnings("unused")
    public static String MakeSymbolic(String stringValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + stringValue);
        return stringValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicFloat(int) AbstractMethodAdapter.symbolicFloat(int)}, caught in
     * the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param floatValue The concrete value
     */
    @SuppressWarnings("unused")
    public static float MakeSymbolic(float floatValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + floatValue);
        return floatValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicDouble(int) AbstractMethodAdapter.symbolicDouble(int)}, caught
     * in the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param doubleValue The concrete value
     */
    @SuppressWarnings("unused")
    public static double MakeSymbolic(double doubleValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + doubleValue);
        return doubleValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * AbstractMethodAdapter#symbolicLongObject(int) AbstractMethodAdapter.symbolicLongObject(int)},
     * caught in the {@link SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#makeSymbolic(Value, Type, SymbolicStateHandler)
     * InternalInvocation.makeSymbolic(Value, Type, SymbolicStateHandler)}.
     *
     * @param longValue The concrete value
     */
    @SuppressWarnings("unused")
    public static Long MakeSymbolic(Long longValue) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param intValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static int MakeSymbolic(int intValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + intValue);
        return intValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param longValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static long MakeSymbolic(long longValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param charValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static char MakeSymbolic(char charValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + charValue);
        return charValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param byteValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static byte MakeSymbolic(byte byteValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + byteValue);
        return byteValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param shortValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static short MakeSymbolic(short shortValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + shortValue);
        return shortValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param boolValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static boolean MakeSymbolic(boolean boolValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + boolValue);
        return boolValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param stringValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static String MakeSymbolic(String stringValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + stringValue);
        return stringValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param floatValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static float MakeSymbolic(float floatValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + floatValue);
        return floatValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param doubleValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static double MakeSymbolic(double doubleValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + doubleValue);
        return doubleValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param longValue The concrete value
     * @param namePrefix The symbolic identifier of the variable
     */
    @SuppressWarnings("unused")
    public static Long MakeSymbolic(Long longValue, String namePrefix) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param intValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static int MakeSymbolic(int intValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + intValue);
        return intValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param longValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static long MakeSymbolic(long longValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param charValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static char MakeSymbolic(char charValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + charValue);
        return charValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param byteValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static byte MakeSymbolic(byte byteValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + byteValue);
        return byteValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param shortValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static short MakeSymbolic(short shortValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + shortValue);
        return shortValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param boolValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static boolean MakeSymbolic(boolean boolValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + boolValue);
        return boolValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param stringValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static String MakeSymbolic(String stringValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + stringValue);
        return stringValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param floatValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static float MakeSymbolic(float floatValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + floatValue);
        return floatValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param doubleValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static double MakeSymbolic(double doubleValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + doubleValue);
        return doubleValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * SpringDataMethodAdapter#makeSymbolic(DataType)}, caught in the {@link SymbolicInterpreter
     * SymbolicInterpreter} and handled in ?
     *
     * @param longValue The concrete value
     * @param idx The index of the variable
     */
    @SuppressWarnings("unused")
    public static Long MakeSymbolic(Long longValue, long idx) {
        systemLogger.info(
                "[DSE] Initializing symbolic tracking for the following value " + longValue);
        return longValue;
    }

    /**
     * Stub function, call is added during instrumentation in {@link
     * HttpServletMethodAdapter#checkSink(String, String, String)
     * HttpServletMethodAdapter.checkSink(String, String, String}, caught in the {@link
     * SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#checkSink(Value[]) InternalInvocation.checkSink(Value[])}.
     *
     * @param arg The value (argument) to check
     * @param identifier The type identifier of the arg
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(Object arg, String identifier, String sink) {}

    /**
     * Stub function, call is added during instrumentation in {@link
     * HttpServletMethodAdapter#checkSink(String, String, String)
     * HttpServletMethodAdapter.checkSink(String, String, String}, caught in the {@link
     * SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#checkSink(Value[]) InternalInvocation.checkSink(Value[])}.
     *
     * @param arg1 The first value (argument) to check
     * @param arg2 The second value (argument) to check
     * @param identifier The type identifier of the arguments (arg1, arg2)
     * @param sink The identifier of the sink
     */
    @SuppressWarnings("unused")
    public static void checkSink(Object arg1, Object arg2, String identifier, String sink) {}

    /**
     * Stub function, call is added during instrumentation in {@link
     * HttpServletMethodAdapter#checkSink(String, String, String)
     * HttpServletMethodAdapter.checkSink(String, String, String}, caught in the {@link
     * SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#checkSink(Value[]) InternalInvocation.checkSink(Value[])}.
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
     * Stub function, call is added during instrumentation in {@link
     * HttpServletMethodAdapter#checkSink(String, String, String)
     * HttpServletMethodAdapter.checkSink(String, String, String}, caught in the {@link
     * SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#checkSink(Value[]) InternalInvocation.checkSink(Value[])}.
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
     * Stub function, call is added during instrumentation in {@link
     * HttpServletMethodAdapter#checkSink(String, String, String)
     * HttpServletMethodAdapter.checkSink(String, String, String}, caught in the {@link
     * SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#checkSink(Value[]) InternalInvocation.checkSink(Value[])}.
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
     * Stub function, call is added during instrumentation in {@link
     * HttpServletMethodAdapter#checkSink(String, String, String)
     * HttpServletMethodAdapter.checkSink(String, String, String}, caught in the {@link
     * SymbolicInterpreter SymbolicInterpreter} and handled in {@link
     * InternalInvocation#checkSink(Value[]) InternalInvocation.checkSink(Value[])}.
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
