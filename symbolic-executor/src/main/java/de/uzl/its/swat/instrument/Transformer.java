package de.uzl.its.swat.instrument;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.instruction.InstructionTransformer;
import de.uzl.its.swat.instrument.parameter.ParameterTransformer;
import de.uzl.its.swat.instrument.svcomp.SVCompTransformer;
import de.uzl.its.swat.instrument.symbolicwrapper.SymbolicWrapperTransformer;
import de.uzl.its.swat.thread.ThreadHandler;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import org.slf4j.LoggerFactory;

/** This class runs all the different transformers. */
public abstract class Transformer implements ClassFileTransformer {

    @Getter
    private static final HashMap<String, ArrayList<InternalTransformerType>> instrumentedClasses =
            new HashMap<>();

    private static Instrumentation instrumentation;
    private static final Config config = Config.instance();

    @Getter private static PrintBox printBox;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transformer.class);

    public static void retransform(String cname) {
        cname = cname.substring(1);
        boolean loaded = isLoaded(cname);
        if (loaded) {
            try {
                Class<?> clazz = Class.forName(cname);
                instrumentation.retransformClasses(clazz);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if a class has already been instrumented
     *
     * @param cname the name of the class
     * @return true if the class has been instrumented
     */
    public static boolean isInstrumented(String cname) {
        return instrumentedClasses.containsKey(cname);
    }
    /**
     * Adds a class to the list of instrumented classes
     *
     * @param cname the name of the class
     * @param transformer the transformer that instrumented the class
     */
    public static void addInstrumentedClass(String cname, InternalTransformerType transformer) {
        if (instrumentedClasses.containsKey(cname)) {
            instrumentedClasses.get(cname).add(transformer);
        } else {
            ArrayList<InternalTransformerType> transformers = new ArrayList<>();
            transformers.add(transformer);
            instrumentedClasses.put(cname, transformers);
        }
    }

    /*
     * ToDo: Class.getName() expects package notation with dots '.'
     *  rather than '/', imo this should be ensured by the caller
     */
    public static boolean isLoaded(String cname) {
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (cname.equals(clazz.getName())) return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static void premain(String agentArgs, Instrumentation inst) {
        ThreadHandler.init();
        try {
            instrumentation = inst;
            if (config.getInstrumentationTransformer().equals(TransformerType.NONE)) {
                logger.info(
                        new PrintBox(
                                        60,
                                        "Instrumentation Agent started!",
                                        new ArrayList<>(
                                                List.of(
                                                        "No instrumentation selected!",
                                                        "Please select a Transformer type ",
                                                        "in the config file ",
                                                        "and restart the program.")))
                                .toString());

                return;
            }
            printBox = new PrintBox(60, "Instrumentation Agent started!");
            printBox.addMsg(
                    "Selected Instrumentation Type: " + config.getInstrumentationTransformer());
            printBox.addMsg("Working Directory: " + System.getProperty("user.dir"));
            printBox.addMsg("");

            switch (config.getInstrumentationTransformer()) {
                case SPRING_ENDPOINT, WEB_SERVLET -> {
                    new ErrorHandler()
                            .handleException(
                                    new RuntimeException(
                                            "Spring and WebServlet Instrumentation is not supported"
                                                    + " yet!"));
                }
                case PARAMETER -> inst.addTransformer(new ParameterTransformer());

                case SV_COMP -> inst.addTransformer(new SVCompTransformer());
            }

            // Add transformer that tracks all instructions
            inst.addTransformer(new InstructionTransformer());
            // inst.addTransformer(new LoopDetectionTransformer());
            // Post transformation
            inst.addTransformer(new SymbolicWrapperTransformer());

            if (config.isLoggingClasses()) {
                inst.addTransformer(new ClassSavingTransformer());
            }
            logger.info(printBox.toString());
        } catch (Exception e) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error during Transformer initialization!", e);
        }
    }

    /**
     * Checks if a class should be instrumented. If the instrumentPackages are set, only classes in
     * these packages will be instrumented. If the excludePackages are set, classes in these
     * packages will not be instrumented.
     *
     * @param cname the name of the class
     * @return true if the class should be instrumented, false otherwise
     */
    public static boolean shouldInstrument(String cname) {
        // Special case for SV-Comp
        if (cname.equals("de/uzl/its/swat/instrument/svcomp/Verifier")
                && config.getInstrumentationTransformer().equals(TransformerType.SV_COMP))
            return true;

        boolean shouldInst = true;
        if (config.getInstrumentationIncludePackages() != null) {
            shouldInst = false;
            for (String p : config.getInstrumentationIncludePackages()) {
                shouldInst = shouldInst || cname.startsWith(p);
            }
        } else if (config.getInstrumentationExcludePackages() != null) {
            for (String p : config.getInstrumentationExcludePackages()) {
                shouldInst = shouldInst && !cname.startsWith(p);
            }
        }
        return shouldInst;
    }
}
