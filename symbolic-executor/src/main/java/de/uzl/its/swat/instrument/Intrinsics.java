package de.uzl.its.swat.instrument;

import ch.qos.logback.classic.Logger;
import com.sap.fontus.taintaware.unified.IASString;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.SWATException;
import de.uzl.its.swat.common.exceptions.ThreadAlreadyDisabledException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.solver.LocalSolver;
import de.uzl.its.swat.symbolic.dispatcher.SymbolicInstructionDispatcher;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadHandler;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Function;

import static java.lang.Thread.currentThread;

@SuppressWarnings("unused")
public class Intrinsics {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    private static final Config config = Config.instance();

    @Getter
    @Setter
    private static HashMap<String, Queue<String>> assignments = new HashMap<>();
    private static long uid = 0;

    public static long getNextUid() {
        return uid++;
    }
    public static long peekCurrentUid() {
        return uid;
    }



    public static void unsupported() {
        SWATAssert.enforce(false, "Unsupported functionality triggered");
    }

    /**
     * Initializes symbolic or coverage only tracking for the current execution. The method call is
     * added during instrumentation
     *
     * @param endpoint The name of the endpoint.
     */
    public static void init(String endpoint) {
        try {
            int endpointID;
            if (config.getInstrumentationTransformer().equals(TransformerType.SV_COMP)) {
                endpointID = 0;
            } else {
                endpointID = endpoint.hashCode();
            }
            ThreadHandler.addThreadContext(currentThread().getId(), endpoint, endpointID);

            logger.info(
                    new PrintBox(
                            60,
                            "Symbolic execution started (Thread: "
                                    + Thread.currentThread().getId()
                                    + ")",
                            new ArrayList<>(
                                    List.of(
                                            new String[]{
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
                                            })))
                            .toString());
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error during initialization of symbolic engine: ", t);
        }
    }


    /**
     * Finishes symbolic tracking and initializes constrain transfer to coordinator. Method call
     * added during instrumentation.
     */
    public static void terminate() {
        try {
            Transformer.logMissedClasses();
            ThreadHandler.disableThreadContext(currentThread().getId());
            ThreadHandler.logStats(currentThread().getId());
            ThreadHandler.logStats(-1);
            logger.info(
                    new PrintBox(
                            60,
                            "Symbolic execution finished (Thread: "
                                    + currentThread().getId()
                                    + ")",
                            new ArrayList<>(
                                    List.of(
                                            new String[] {
                                                    "Threads (tracked / active): ("
                                                            + ThreadHandler.getThreadCount()
                                                            + " / "
                                                            + Thread.activeCount()
                                                            + ")",
                                            })))
                            .toString());

            switch (config.getSolverMode()) {
                case LOCAL:
                    if (!ThreadHandler.isThreadContextAborted(currentThread().getId())) {
                        LocalSolver.solve();
                    }
                    break;
                case HTTP:
                    if (!ThreadHandler.isThreadContextAborted(currentThread().getId())) {
                        ThreadHandler.sendData(currentThread().getId());
                    }
                    break;
                case PRINT:
                    if (!ThreadHandler.isThreadContextAborted(currentThread().getId())) {
                        System.out.println(ThreadHandler.getSymbolicVisitor(currentThread().getId()).getSymbolicTraceHandler().getTraceDTO());
                    }
                    break;
                case NONE:
                    break;
                default:
                    throw new SWATException("Unknown solver mode");
            }

            SymbolicInstructionDispatcher.flush(); // TODO WTF does that do? calls log(null) <- I think what this does is removing that last symbolic instruction from the pipeline as symbolic execution is always one behind.
            ThreadHandler.removeSolverContext(currentThread().getId());
            ThreadHandler.removeThreadContext(currentThread().getId());
        } catch (NoThreadContextException | ThreadAlreadyDisabledException e) {
            logger.warn(e.toString());
            logger.warn(
                    "======= Trying to terminate thread context without having started it. This"
                            + " might be expected behavior in case of exceptions thrown before handling"
                            + " of symbolicexecution. =======");
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error terminating symbolic tracking", t);
        }
    }

    public static void retrieveAssignments() {
        PrintBox printBox = new PrintBox(60, "Retrieving assignments");
        System.getProperties().forEach((key, value) -> {
            if (key.toString().startsWith("swat.assignment.")) {
                // Add the single value to the back of the queue.
                String name = key.toString().replace("swat.assignment.", "");
                Queue<String> queue = assignments.getOrDefault(name, new LinkedList<>());
                queue.add((String) value);
                assignments.put(name, queue);
                printBox.addMsg("Assignment with name: " + name + " and (encoded) value: " + value + " registered");
            }
        });
        logger.info(printBox.toString());
    }

    /**
     * Generic method to inject an assignment.
     *
     * It builds the key using the provided symbolic prefix and uid, checks whether an assignment is available,
     * and if so uses the provided parse function to convert the stored string into the desired type.
     *
     * @param uid            Unique identifier for the variable.
     * @param value          The original concrete value.
     * @param symbolicPrefix The symbolic prefix (e.g. obtained via StringValue.getSymbolicPrefix()).
     * @param parseFunction  A function to convert a String into the type T.
     * @param type           The type of the value.
     * @param <T>            The type of the value.
     * @return The assignment provided by the symbolic engine if available, otherwise the original value.
     */
    public static <T> T injectAssignmentGeneric(long uid, T value, String symbolicPrefix,
                                                Function<String, T> parseFunction,
                                                Type type){
        try{
            PrintBox printBox = new PrintBox(60, "Injecting assignment for " + type + " " + uid);
            String prefix = symbolicPrefix + "_";
            String requestedName = prefix + uid;
            String newId = String.valueOf(uid);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = uid + String.format("%02d", occurrence);
                logger.warn("Multiple calls to injectAssignment with the same name: {}. Assigning new idx: {}", requestedName, newId);
            }
            requestedName = prefix + newId;
            printBox.addMsg("Requested assignment: " + requestedName);
            if (assignments.containsKey(requestedName) && !assignments.get(requestedName).isEmpty()) {
                printBox.addMsg("Assignment available!");
                String assignmentStr = assignments.get(prefix + newId).remove();
                T parsedValue = parseFunction.apply(assignmentStr);
                printBox.addMsg("Returning: " + parsedValue);
                logger.info(printBox.toString());
                return parsedValue;
            } else {
                printBox.addMsg("No assignment available, returning original value");
                logger.info(printBox.toString());
                return value;
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error during assignment injection", t);
            return value;
        }
    }

    // Overloads for String and primitive wrapper types.

    public static String injectAssignment(String value, long uid) {
        return injectAssignmentGeneric(uid, value, StringValue.getSymbolicPrefix(),
                s -> s, Type.getType(String.class));
    }

    public static int injectAssignment(int value, long uid) {
        return injectAssignmentGeneric(uid, value, IntValue.getSymbolicPrefix(),
                Integer::parseInt, Type.INT_TYPE);
    }

    public static Integer injectAssignment(Integer value, long uid) {
        return injectAssignmentGeneric(uid, value, IntValue.getSymbolicPrefix(),
                Integer::parseInt, Type.INT_TYPE);
    }

    public static boolean injectAssignment(boolean value, long uid) {
        return injectAssignmentGeneric(uid, value, BooleanValue.getSymbolicPrefix(),
                Boolean::parseBoolean, Type.BOOLEAN_TYPE);
    }

    public static Boolean injectAssignment(Boolean value, long uid) {
        return injectAssignmentGeneric(uid, value, BooleanValue.getSymbolicPrefix(),
                Boolean::parseBoolean, Type.BOOLEAN_TYPE);
    }

    public static byte injectAssignment(byte value, long uid) {
        return injectAssignmentGeneric(uid, value, ByteValue.getSymbolicPrefix(),
                Byte::parseByte, Type.BYTE_TYPE);
    }

    public static Byte injectAssignment(Byte value, long uid) {
        return injectAssignmentGeneric(uid, value, ByteValue.getSymbolicPrefix(),
                Byte::parseByte, Type.BYTE_TYPE);
    }

    public static char injectAssignment(char value, long uid) {
        return injectAssignmentGeneric(uid, value, CharValue.getSymbolicPrefix(),
                s -> s.charAt(0), Type.CHAR_TYPE);
    }

    public static Character injectAssignment(Character value, long uid) {
        return injectAssignmentGeneric(uid, value, CharValue.getSymbolicPrefix(),
                s -> s.charAt(0), Type.CHAR_TYPE);
    }

    public static short injectAssignment(short value, long uid) {
        return injectAssignmentGeneric(uid, value, ShortValue.getSymbolicPrefix(),
                Short::parseShort, Type.SHORT_TYPE);
    }

    public static Short injectAssignment(Short value, long uid) {
        return injectAssignmentGeneric(uid, value, ShortValue.getSymbolicPrefix(),
                Short::parseShort, Type.SHORT_TYPE);
    }

    public static long injectAssignment(long value, long uid) {
        return injectAssignmentGeneric(uid, value, LongValue.getSymbolicPrefix(),
                Long::parseLong, Type.LONG_TYPE);
    }

    public static Long injectAssignment(Long value, long uid) {
        return injectAssignmentGeneric(uid, value, LongValue.getSymbolicPrefix(),
                Long::parseLong, Type.LONG_TYPE);
    }

    public static float injectAssignment(float value, long uid) {
        Function<String, Float> parseFunction = s ->  {
            int enc = Integer.parseUnsignedInt(s);
            return Float.intBitsToFloat(enc);
        };
        return injectAssignmentGeneric(uid, value, FloatValue.getSymbolicPrefix(),
                parseFunction, Type.FLOAT_TYPE);
    }

    public static Float injectAssignment(Float value, long uid) {
        Function<String, Float> parseFunction = s ->  {
            int enc = Integer.parseUnsignedInt(s);
            return Float.intBitsToFloat(enc);
        };
        return injectAssignmentGeneric(uid, value, FloatValue.getSymbolicPrefix(),
                parseFunction, Type.FLOAT_TYPE);
    }

    public static double injectAssignment(double value, long uid) {
        Function<String, Double> parseFunction = s ->  {
            long enc = Long.parseUnsignedLong(s);
            return Double.longBitsToDouble(enc);
        };
        return injectAssignmentGeneric(uid, value, DoubleValue.getSymbolicPrefix(),
                parseFunction, Type.DOUBLE_TYPE);
    }

    public static Double injectAssignment(Double value, long uid) {
        Function<String, Double> parseFunction = s ->  {
            long enc = Long.parseUnsignedLong(s);
            return Double.longBitsToDouble(enc);
        };
        return injectAssignmentGeneric(uid, value, DoubleValue.getSymbolicPrefix(),
                parseFunction, Type.DOUBLE_TYPE);
    }

    public static Object injectAssignment(Object value, long uid) {
        // For now, just return the original value - assignment injection for lists is complex
        logger.info("[DSE] Assignment injection for Object (returning original)");
        return value;
    }
    // List injectAssignment methods
    public static java.util.List injectAssignment(java.util.List value, long uid) {
        // For now, just return the original value - assignment injection for lists is complex
        logger.info("[DSE] Assignment injection for List (returning original): {}", value);
        return value;
    }

    public static java.util.ArrayList injectAssignment(java.util.ArrayList value, long uid) {
        // For now, just return the original value - assignment injection for lists is complex  
        logger.info("[DSE] Assignment injection for ArrayList (returning original): {}", value);
        return value;
    }

    public static java.util.LinkedList injectAssignment(java.util.LinkedList value, long uid) {
        // For now, just return the original value - assignment injection for lists is complex
        logger.info("[DSE] Assignment injection for LinkedList (returning original): {}", value);
        return value;
    }

    // Lift methods simply log that symbolic tracking is being initialized
    public static String liftValue(String value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for String: {}", value);
        return value;
    }

    public static IASString liftValue(IASString value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for String: {}", value);
        return value;
    }

    public static int liftValue(int value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Integer: {}", value);
        return value;
    }

    public static Integer liftValue(Integer value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Integer: {}", value);
        return value;
    }

    public static boolean liftValue(boolean value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Boolean: {}", value);
        return value;
    }

    public static Boolean liftValue(Boolean value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Boolean: {}", value);
        return value;
    }

    public static byte liftValue(byte value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Byte: {}", value);
        return value;
    }

    public static Byte liftValue(Byte value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Byte: {}", value);
        return value;
    }

    public static char liftValue(char value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Character: {}", value);
        return value;
    }

    public static Character liftValue(Character value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Character: {}", value);
        return value;
    }

    public static short liftValue(short value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Short: {}", value);
        return value;
    }

    public static Short liftValue(Short value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Short: {}", value);
        return value;
    }

    public static long liftValue(long value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Long: {}", value);
        return value;
    }

    public static Long liftValue(Long value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Long: {}", value);
        return value;
    }

    public static float liftValue(float value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Float: {}", value);
        return value;
    }


    public static Float liftValue(Float value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Float: {}", value);
        return value;
    }

    public static double liftValue(double value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Double: {}", value);
        return value;
    }

    public static Double liftValue(Double value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Double: {}", value);
        return value;
    }

    // Lift methods simply log that symbolic tracking is being initialized
    public static String liftValue(String value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for String: {}", value);
        return value;
    }

    public static int liftValue(int value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Integer: {}", value);
        return value;
    }

    public static Integer liftValue(Integer value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Integer: {}", value);
        return value;
    }

    public static boolean liftValue(boolean value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Boolean: {}", value);
        return value;
    }

    public static Boolean liftValue(Boolean value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Boolean: {}", value);
        return value;
    }

    public static byte liftValue(byte value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Byte: {}", value);
        return value;
    }

    public static Byte liftValue(Byte value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Byte: {}", value);
        return value;
    }

    public static char liftValue(char value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Character: {}", value);
        return value;
    }

    public static Character liftValue(Character value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Character: {}", value);
        return value;
    }

    public static short liftValue(short value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Short: {}", value);
        return value;
    }

    public static Short liftValue(Short value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Short: {}", value);
        return value;
    }

    public static long liftValue(long value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Long: {}", value);
        return value;
    }

    public static Long liftValue(Long value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Long: {}", value);
        return value;
    }

    public static float liftValue(float value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Float: {}", value);
        return value;
    }

    public static Float liftValue(Float value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Float: {}", value);
        return value;
    }

    public static double liftValue(double value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for Double: {}", value);
        return value;
    }


    public static Object liftValue(Object value, long uid) {
        logger.info("[DSE] (Potentially) initializing symbolic tracking for Object : {}", value );
        return value;
    }
    public static Object liftValue(Object value, String prefix, long uid) {
        logger.info("[DSE] (Potentially) initializing symbolic tracking for Object : {}", value );
        return value;
    }
    // List lifting methods
    public static java.util.List liftValue(java.util.List value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for List with size: {}", value != null ? value.size() : 0);
        return value;
    }

    public static java.util.ArrayList liftValue(java.util.ArrayList value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for ArrayList with size: {}", value != null ? value.size() : 0);
        return value;
    }

    public static java.util.LinkedList liftValue(java.util.LinkedList value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for LinkedList with size: {}", value != null ? value.size() : 0);
        return value;
    }

    public static java.util.List liftValue(java.util.List value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for List with prefix '{}' and size: {}", prefix, value != null ? value.size() : 0);
        return value;
    }

    public static java.util.ArrayList liftValue(java.util.ArrayList value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for ArrayList with prefix '{}' and size: {}", prefix, value != null ? value.size() : 0);
        return value;
    }

    public static java.util.LinkedList liftValue(java.util.LinkedList value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for LinkedList with prefix '{}' and size: {}", prefix, value != null ? value.size() : 0);
        return value;
    }

    // Array lifting methods
    public static int[] liftValue(int[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for int[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    public static long[] liftValue(long[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for long[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    public static boolean[] liftValue(boolean[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for boolean[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    public static byte[] liftValue(byte[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for byte[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    public static char[] liftValue(char[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for char[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    public static short[] liftValue(short[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for short[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    public static float[] liftValue(float[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for float[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    public static double[] liftValue(double[] value, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for double[] with length: {}", value != null ? value.length : 0);
        return value;
    }

    // Array lifting methods with prefix
    public static int[] liftValue(int[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for int[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
    }

    public static long[] liftValue(long[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for long[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
    }

    public static boolean[] liftValue(boolean[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for boolean[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
    }

    public static byte[] liftValue(byte[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for byte[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
    }

    public static char[] liftValue(char[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for char[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
    }

    public static short[] liftValue(short[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for short[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
    }

    public static float[] liftValue(float[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for float[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
    }

    public static double[] liftValue(double[] value, String prefix, long uid) {
        logger.info("[DSE] Initializing symbolic tracking for double[] with prefix '{}' and length: {}", prefix, value != null ? value.length : 0);
        return value;
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
