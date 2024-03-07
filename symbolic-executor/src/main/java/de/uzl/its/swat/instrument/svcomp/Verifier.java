package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.Main;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.common.SystemLogger;
import java.util.*;
import org.slf4j.Logger;
import lombok.Getter;
import lombok.Setter;

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

    private static final Logger logger;
    private static final SystemLogger systemLogger;
    private static final Config config;

    static {
        systemLogger = new SystemLogger();
        logger = systemLogger.getLogger();
        config = Config.instance();
    }

    public static long getNextId() {
        long id = nextId;
        nextId++;
        return id;
    }

    @Getter @Setter private static HashMap<String, Queue<String>> inputs = new HashMap<>();

    public static void retrieveInputs() {
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
                                systemLogger.addToBox(
                                        "Input with name: "
                                                + key.toString().replace("swat.input.", "")
                                                + " and value: "
                                                + value
                                                + " registered");
                            }
                        });
    }

    public static void assume(boolean condition) {
        systemLogger.fullBox(
                60,
                "SV-Comp Verifier: assume",
                new ArrayList<>(List.of("Assuming condition: " + condition)));
        if (!condition) {
            Main.terminate();
            Runtime.getRuntime().halt(0);
        }
    }

    public static boolean nondetBoolean(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetDouble");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixBoolean + id) && !inputs.get(prefixBoolean + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            boolean b = Boolean.parseBoolean(inputs.get(prefixBoolean + id).remove());
            systemLogger.addToBox("Returning: " + b);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static byte nondetByte(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetByte");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixByte + id) && !inputs.get(prefixByte + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            byte b = Byte.parseByte(inputs.get(prefixByte + id).remove());
            systemLogger.addToBox("Returning: " + b);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static char nondetChar(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetChar");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixChar + id) && !inputs.get(prefixChar + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            char c = inputs.get(prefixChar + id).remove().charAt(0);
            systemLogger.addToBox("Returning: " + c);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static short nondetShort(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetShort");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixShort + id) && !inputs.get(prefixShort + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            short s = Short.parseShort(inputs.get(prefixShort + id).remove());
            systemLogger.addToBox("Returning: " + s);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static int nondetInt(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetInt");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixInt + id) && !inputs.get(prefixInt + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            int i = Integer.parseInt(inputs.get(prefixInt + id).remove());
            systemLogger.addToBox("Returning: " + i);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static long nondetLong(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetLong");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixLong + id) && !inputs.get(prefixLong + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            long l = Long.parseLong(inputs.get(prefixLong + id).remove());
            systemLogger.addToBox("Returning: " + l);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static float nondetFloat(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetFloat");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixFloat + id) && !inputs.get(prefixFloat + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            int enc = Integer.parseUnsignedInt(inputs.get(prefixFloat + id).remove());
            float f = Float.intBitsToFloat(enc);
            systemLogger.addToBox("Returning: " + f);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static double nondetDouble(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetDouble");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixDouble + id) && !inputs.get(prefixDouble + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            long enc = Long.parseUnsignedLong(inputs.get(prefixDouble + id).remove());
            double d = Double.longBitsToDouble(enc);
            systemLogger.addToBox("Returning: " + d);
            logger.info(systemLogger.endBox());
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
            systemLogger.addToBox(msg);
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }

    public static String nondetString(long id) {
        systemLogger.startBox(60, "SV-Comp Verifier: nondetString");
        systemLogger.addToBox("Requested ID: " + id);
        if (inputs.containsKey(prefixString + id) && !inputs.get(prefixString + id).isEmpty()) {
            systemLogger.addToBox("Predetermined value available!");
            var val = inputs.get(prefixString + id).peek();
            systemLogger.addToBox("Returning: " + val);
            logger.info(systemLogger.endBox());
            return val;
        } else {
            systemLogger.addToBox("No predetermined value available");
            String rnd = "";
            if (config.isVerifierRandomnessEnabled()) {
                systemLogger.addToBox("Randomness enabled, using random value");
                Random random = new Random();
                int size = random.nextInt(100);
                byte[] bytes = new byte[size];
                random.nextBytes(bytes);
                rnd = new String(bytes);
            } else {
                systemLogger.addToBox("Randomness disabled, using fixed value");
                rnd = new String("fixed");
            }
            systemLogger.addToBox("Returning " + rnd);
            logger.info(systemLogger.endBox());
            return rnd;
        }
    }
}
