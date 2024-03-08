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
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: retrieving inputs");
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
                                printBox.addToBox(
                                        "Input with name: "
                                                + key.toString().replace("swat.input.", "")
                                                + " and value: "
                                                + value
                                                + " registered");
                            }
                        });
        logger.info(printBox.endBox());
    }

    public static void assume(boolean condition) {
        PrintBox printBox = new PrintBox(60);
        logger.info(
                printBox.fullBox(
                        "SV-Comp Verifier: assume",
                        new ArrayList<>(List.of("Assuming condition: " + condition))));
        if (!condition) {
            Main.terminate();
            Runtime.getRuntime().halt(0);
        }
    }

    public static boolean nondetBoolean(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetDouble");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixBoolean + id) && !inputs.get(prefixBoolean + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            boolean b = Boolean.parseBoolean(inputs.get(prefixBoolean + id).remove());
            printBox.addToBox("Returning: " + b);
            logger.info(printBox.endBox());
            return b;

        } else {
            boolean rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (new Random()).nextBoolean()
                            : Boolean.valueOf(false);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static byte nondetByte(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetByte");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixByte + id) && !inputs.get(prefixByte + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            byte b = Byte.parseByte(inputs.get(prefixByte + id).remove());
            printBox.addToBox("Returning: " + b);
            logger.info(printBox.endBox());
            return b;
        } else {
            byte rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (byte) (new Random()).nextInt()
                            : Byte.valueOf((byte) 0);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static char nondetChar(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetChar");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixChar + id) && !inputs.get(prefixChar + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            char c = inputs.get(prefixChar + id).remove().charAt(0);
            printBox.addToBox("Returning: " + c);
            logger.info(printBox.endBox());
            return c;
        } else {
            char rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (char) (new Random()).nextInt()
                            : Character.valueOf((char) 0);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static short nondetShort(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetShort");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixShort + id) && !inputs.get(prefixShort + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            short s = Short.parseShort(inputs.get(prefixShort + id).remove());
            printBox.addToBox("Returning: " + s);
            logger.info(printBox.endBox());
            return s;
        } else {
            short rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (short) (new Random()).nextInt()
                            : Short.valueOf((short) 0);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static int nondetInt(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetInt");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixInt + id) && !inputs.get(prefixInt + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            int i = Integer.parseInt(inputs.get(prefixInt + id).remove());
            printBox.addToBox("Returning: " + i);
            logger.info(printBox.endBox());
            return i;
        } else {
            int rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (new Random()).nextInt()
                            : Integer.valueOf(0);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static long nondetLong(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetLong");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixLong + id) && !inputs.get(prefixLong + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            long l = Long.parseLong(inputs.get(prefixLong + id).remove());
            printBox.addToBox("Returning: " + l);
            logger.info(printBox.endBox());
            return l;
        } else {
            long rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (new Random()).nextLong()
                            : Long.valueOf(0L);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static float nondetFloat(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetFloat");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixFloat + id) && !inputs.get(prefixFloat + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            int enc = Integer.parseUnsignedInt(inputs.get(prefixFloat + id).remove());
            float f = Float.intBitsToFloat(enc);
            printBox.addToBox("Returning: " + f);
            logger.info(printBox.endBox());
            return f;
        } else {
            float rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (new Random()).nextFloat()
                            : Float.valueOf(0f);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static double nondetDouble(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetDouble");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixDouble + id) && !inputs.get(prefixDouble + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            long enc = Long.parseUnsignedLong(inputs.get(prefixDouble + id).remove());
            double d = Double.longBitsToDouble(enc);
            printBox.addToBox("Returning: " + d);
            logger.info(printBox.endBox());
            return d;
        } else {
            double rnd =
                    config.isVerifierRandomnessEnabled()
                            ? (new Random()).nextDouble()
                            : Double.valueOf(0d);
            String msg =
                    config.isVerifierRandomnessEnabled()
                            ? "Randomness enabled, using random value"
                            : "Randomness disabled, using fixed value";
            printBox.addToBox(msg);
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }

    public static String nondetString(long id) {
        PrintBox printBox = new PrintBox(60);
        printBox.startBox("SV-Comp Verifier: nondetString");
        printBox.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixString + id) && !inputs.get(prefixString + id).isEmpty()) {
            printBox.addToBox("Predetermined value available!");
            var val = inputs.get(prefixString + id).peek();
            printBox.addToBox("Returning: " + val);
            logger.info(printBox.endBox());
            return val;
        } else {
            printBox.addToBox("No predetermined value available");
            String rnd = "";
            if (config.isVerifierRandomnessEnabled()) {
                printBox.addToBox("Randomness enabled, using random value");
                Random random = new Random();
                int size = random.nextInt(100);
                byte[] bytes = new byte[size];
                random.nextBytes(bytes);
                rnd = new String(bytes);
            } else {
                printBox.addToBox("Randomness disabled, using fixed value");
                rnd = new String("fixed");
            }
            printBox.addToBox("Returning " + rnd);
            logger.info(printBox.endBox());
            return rnd;
        }
    }
}
