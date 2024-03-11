package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.Main;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.config.Config;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

public class Verifier {
    private static long nextId = 0;
    private static final String prefixBoolean = "Z_";
    private static final String prefixChar = "C_";
    private static final String prefixByte = "B_";
    private static final String prefixShort = "S_";
    private static final String prefixInt = "I_";
    private static final String prefixFloat = "F_";
    private static final String prefixLong = "L_";
    private static final String prefixDouble = "D_";
    private static final String prefixString = "Ljava/lang/String_";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Verifier.class);
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
                                                + " and value: "
                                                + value
                                                + " registered");
                            }
                        });
        logger.info(printBox.toString());
    }

    public static void assume(boolean condition) {
        PrintBox printBox = new PrintBox(60);
        logger.info(
                new PrintBox(
                                60,
                                "SV-Comp Verifier: assume",
                                new ArrayList<>(List.of("Assuming condition: " + condition)))
                        .toString());
        if (!condition) {
            Main.terminate();
            Runtime.getRuntime().halt(0);
        }
    }

    public static boolean nondetBoolean(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetDouble");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixBoolean + id) && !inputs.get(prefixBoolean + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            boolean b = Boolean.parseBoolean(inputs.get(prefixBoolean + id).remove());
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
            return rnd;
        }
    }

    public static byte nondetByte(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetByte");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixByte + id) && !inputs.get(prefixByte + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            byte b = Byte.parseByte(inputs.get(prefixByte + id).remove());
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
            return rnd;
        }
    }

    public static char nondetChar(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetChar");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixChar + id) && !inputs.get(prefixChar + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            char c = inputs.get(prefixChar + id).remove().charAt(0);
            printBox.addMsg("Returning: " + c);
            logger.info(printBox.toString());
            return c;
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
            return rnd;
        }
    }

    public static short nondetShort(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetShort");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixShort + id) && !inputs.get(prefixShort + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            short s = Short.parseShort(inputs.get(prefixShort + id).remove());
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
            return rnd;
        }
    }

    public static int nondetInt(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetInt");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixInt + id) && !inputs.get(prefixInt + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            int i = Integer.parseInt(inputs.get(prefixInt + id).remove());
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
            return rnd;
        }
    }

    public static long nondetLong(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetLong");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixLong + id) && !inputs.get(prefixLong + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            long l = Long.parseLong(inputs.get(prefixLong + id).remove());
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
            return rnd;
        }
    }

    public static float nondetFloat(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetFloat");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixFloat + id) && !inputs.get(prefixFloat + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            int enc = Integer.parseUnsignedInt(inputs.get(prefixFloat + id).remove());
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
    }

    public static double nondetDouble(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetDouble");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixDouble + id) && !inputs.get(prefixDouble + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            long enc = Long.parseUnsignedLong(inputs.get(prefixDouble + id).remove());
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
    }

    public static String nondetString(long id) {
        PrintBox printBox = new PrintBox(60, "SV-Comp Verifier: nondetString");
        printBox.addMsg("Requested ID: " + id);
        if (inputs.containsKey(prefixString + id) && !inputs.get(prefixString + id).isEmpty()) {
            printBox.addMsg("Predetermined value available!");
            var val = inputs.get(prefixString + id).peek();
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
                rnd = "fixed";
            }
            printBox.addMsg("Returning " + rnd);
            logger.info(printBox.toString());
            return rnd;
        }
    }
}
