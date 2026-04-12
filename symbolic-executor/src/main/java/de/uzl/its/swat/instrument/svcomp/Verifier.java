package de.uzl.its.swat.instrument.svcomp;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import java.util.*;

import de.uzl.its.swat.instrument.Intrinsics;
import de.uzl.its.swat.symbolic.invoke.InternalInvocation;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadContext;
import de.uzl.its.swat.thread.ThreadHandler;
import lombok.Getter;
import lombok.Setter;

import static java.lang.Thread.currentThread;
@SuppressWarnings("removal")
public class Verifier {
    private static long nextId = 0;
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    private static final Config config;
    static {
        config = Config.instance();
    }

    public static long getNextId() {
        long id = nextId;
        nextId++;
        return id;
    }

    @Getter @Setter private static HashMap<String, Queue<String>> inputs = new HashMap<>();

    public static void retrieveInputs() {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: retrieving inputs");
        System.getProperties()
                .forEach(
                        (key, value) -> {
                            if (key.toString().startsWith("swat.input.")) {
                                // add the single value to the back of the queue
                                Queue<String> queue =
                                        inputs.getOrDefault(
                                                key.toString().replace("swat.input.", ""),
                                                new LinkedList<>());
                                queue.add((String) value);
                                inputs.put(key.toString().replace("swat.input.", ""), queue);
                                printBox.addMsg(
                                        "Input with name: "
                                                + key.toString().replace("swat.input.", "")
                                                + " and (encoded) value: "
                                                + value
                                                + " registered");
                            }
                        });
        logger.info(printBox.toString());
    }

    public static void assume(boolean condition) {
        logger.info(
                new PrintBox(
                                60,
                                "SV-Comp Verifier: assume",
                                new ArrayList<>(List.of("Assuming condition: " + condition)))
                        .toString());
        if (!condition) {
            Intrinsics.terminate();
            Runtime.getRuntime().halt(0);
        }
    }

    @SuppressWarnings("removal")
    public static boolean nondetBoolean(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetBoolean");
            String prefix = BooleanValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetBoolean with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                boolean b = Boolean.parseBoolean(inputs.get(prefix + newId).remove());
                printBox.addMsg("Returning: " + b);
                logger.info(printBox.toString());
                return b;

            } else {
                boolean rnd =
                        config.isSvcompRandomInputs()
                                ? (new Random()).nextBoolean()
                                : Boolean.valueOf(false);
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return new Boolean(rnd); // Needed to prevent intering
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return false;
        }
    }

    
    public static byte nondetByte(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetByte");
            String prefix = ByteValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetByte with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                // Parse as int first to handle unsigned byte values (0-255) from SMT solver
                int intVal = Integer.parseInt(inputs.get(prefix + newId).remove());
                byte b = (byte) intVal;  // Cast handles conversion from unsigned to signed
                printBox.addMsg("Returning: " + b);
                logger.info(printBox.toString());
                return b;
            } else {
                byte rnd =
                        config.isSvcompRandomInputs()
                                ? (byte) (new Random()).nextInt()
                                : Byte.valueOf((byte) 0);
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return new Byte(rnd); // Needed to prevent intering
            }

        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return 0;
        }
    }

    @SuppressWarnings("removal")
    public static char nondetChar(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetChar");
            String prefix = CharValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetChar with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                String encodedValue = inputs.get(prefix + newId).remove();
                char c = (char) Integer.parseInt(encodedValue);
                printBox.addMsg("Returning: " + c);
                logger.info(printBox.toString());
                return new Character(c); // Needed to prevent intering
            } else {
                char rnd =
                        config.isSvcompRandomInputs()
                                ? (char) (new Random()).nextInt()
                                : Character.valueOf((char) 0);
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return new Character(rnd); // Needed to prevent intering
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return '\u0000';
        }
    }

    @SuppressWarnings("removal")
    public static short nondetShort(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetShort");
            String prefix = ShortValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetShort with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                // Parse as int first to handle unsigned short values (0-65535) from SMT solver
                int intVal = Integer.parseInt(inputs.get(prefix + newId).remove());
                short s = (short) intVal;  // Cast handles conversion from unsigned to signed
                printBox.addMsg("Returning: " + s);
                logger.info(printBox.toString());
                return s;
            } else {
                short rnd =
                        config.isSvcompRandomInputs()
                                ? (short) (new Random()).nextInt()
                                : Short.valueOf((short) 0);
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return new Short(rnd); // Needed to prevent intering
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return 0;
        }
    }

    @SuppressWarnings("removal")
    public static int nondetInt(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetInt");
            String prefix = IntValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetInt with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                // Parse as unsigned to handle values from SMT solver bitvector representation
                long longVal = Long.parseLong(inputs.get(prefix + newId).remove());
                int i = (int) longVal;  // Cast handles conversion from unsigned to signed
                printBox.addMsg("Returning: " + i);
                logger.info(printBox.toString());
                return i;
            } else {
                int rnd = config.isSvcompRandomInputs() ? (new Random()).nextInt() : 0;
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return new Integer(rnd); // Needed to prevent intering
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return 0;
        }
    }

    @SuppressWarnings("removal")
    public static long nondetLong(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetLong");
            String prefix = LongValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetLong with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                // Parse as unsigned to handle values from SMT solver bitvector representation
                long l = Long.parseUnsignedLong(inputs.get(prefix + newId).remove());
                printBox.addMsg("Returning: " + l);
                logger.info(printBox.toString());
                return l;
            } else {
                long rnd = config.isSvcompRandomInputs() ? (new Random()).nextLong() : 0L;
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return new Long(rnd); // Needed to prevent intering
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return 0L;
        }
    }

    public static float nondetFloat(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetFloat");
            String prefix = FloatValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetFloat with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                int enc = Integer.parseUnsignedInt(inputs.get(prefix + newId).remove());
                float f = Float.intBitsToFloat(enc);
                printBox.addMsg("Returning: " + f);
                logger.info(printBox.toString());
                return f;
            } else {
                float rnd = config.isSvcompRandomInputs() ? (new Random()).nextFloat() : 0f;
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return rnd;
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return 0.0f;
        }
    }

    public static double nondetDouble(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetDouble");
            String prefix = DoubleValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetDouble with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                long enc = Long.parseUnsignedLong(inputs.get(prefix + newId).remove());
                double d = Double.longBitsToDouble(enc);
                printBox.addMsg("Returning: " + d);
                logger.info(printBox.toString());
                return d;
            } else {
                double rnd = config.isSvcompRandomInputs() ? (new Random()).nextDouble() : 0d;
                String msg =
                        config.isSvcompRandomInputs()
                                ? "Randomness enabled, using random value"
                                : "Randomness disabled, using fixed value";
                printBox.addMsg(msg);
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return rnd;
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return 0.0d;
        }
    }

    public static String nondetString(long id) {
        try{
            PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetString");
            String prefix = StringValue.getSymbolicPrefix() + "_";
            String requestedName = prefix +  id;
            String newId = String.valueOf(id);
            int occurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId()).getOrDefault(requestedName, 0);
            if (occurrence > 0) {
                newId = id + String.format("%02d", occurrence);
                logger.warn("Multiple calls to nondetString with the same name: {}. Assigning new idx: {}", requestedName, newId);

            }
            printBox.addMsg("Requested ID: " + newId);
            if (inputs.containsKey(prefix + newId) && !inputs.get(prefix + newId).isEmpty()) {
                printBox.addMsg("Predetermined value available!");
                var val = inputs.get(prefix + newId).peek();
                printBox.addMsg("Returning: " + val);
                logger.info(printBox.toString());
                return val;
            } else {
                printBox.addMsg("No predetermined value available");
                String rnd = "";
                if (config.isSvcompRandomInputs()) {
                    printBox.addMsg("Randomness enabled, using random value");
                    Random random = new Random();
                    int size = random.nextInt(100);
                    byte[] bytes = new byte[size];
                    random.nextBytes(bytes);
                    rnd = new String(bytes);
                } else {
                    printBox.addMsg("Randomness disabled, using fixed value");
                    rnd = new String("fixed"); // constructor required for preventing interning
                }
                printBox.addMsg("Returning " + rnd);
                logger.info(printBox.toString());
                return rnd;
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to the target domain
            new ErrorHandler().handleException("Error retrieving value for SV-Comp", t);
            return null;
        }
    }
}
