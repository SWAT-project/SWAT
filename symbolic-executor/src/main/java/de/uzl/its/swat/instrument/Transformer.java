package de.uzl.its.swat.instrument;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.InstrumentationException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.annotation.AnnotationTransformer;
import de.uzl.its.swat.instrument.classinfo.ClassInfoTransformer;
import de.uzl.its.swat.instrument.classvariables.ClassVariablesTransformer;
import de.uzl.its.swat.instrument.dataendpoint.SpringRepositoryTransformer;
import de.uzl.its.swat.instrument.instruction.InstructionTransformer;
import de.uzl.its.swat.instrument.nocache.NoCacheTransformer;
import de.uzl.its.swat.instrument.parameter.ParameterTransformer;
import de.uzl.its.swat.instrument.refequality.RefEqualityTransformer;
import de.uzl.its.swat.instrument.springendpoint.SpringEndpointTransformer;
import de.uzl.its.swat.instrument.svcomp.SVCompTransformer;
import de.uzl.its.swat.instrument.svcomp.Verifier;
import de.uzl.its.swat.instrument.symbolicwrapper.SymbolicWrapperTransformer;
import de.uzl.its.swat.thread.ThreadHandler;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import org.slf4j.LoggerFactory;

/** This class runs all the different transformers. */
public abstract class Transformer implements ClassFileTransformer {

    static {
        // register the hook as early as possible
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            // this will flush and close all appenders (including your buffered FileAppender)
            lc.stop();
        }));
    }

    @Getter
    private static final HashMap<String, ArrayList<InternalTransformerType>> instrumentedClasses =
            new HashMap<>();

    private static Instrumentation instrumentation;
    private static final Config config = Config.instance();

    private static final List<String> requestBodyClass = new ArrayList<String>();

    @Getter private static PrintBox printBox;
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    public static void addRequestBodyClass(String parameter) {
        requestBodyClass.add(Util.formatClassName(parameter));
    }

    public static boolean containsRequestBodyClass(String parameter) {
        return requestBodyClass.contains(Util.formatClassName(parameter));
    }

    public static void retransform(String cname, boolean isRequestBodyClass) {
        cname = Util.formatClassName(cname);
        boolean loaded = isLoaded(cname);
        if (isRequestBodyClass) {
            addRequestBodyClass(cname);
        }
        if (loaded) {
            try {
                Class<?> clazz = Class.forName(cname.replace("/", "."));
                instrumentation.retransformClasses(clazz);
            } catch (UnmodifiableClassException e) {
                logger.warn("Class " + cname + " is not retransformable.");
            } catch (Throwable e) {
                logger.warn("Error while retransforming class " + cname + ". Reason: " + e.getMessage());
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
        return instrumentedClasses.containsKey(Util.formatClassName(cname));
    }
    /**
     * Adds a class to the list of instrumented classes
     *
     * @param cname the name of the class
     * @param transformer the transformer that instrumented the class
     */
    public static void addInstrumentedClass(String cname, InternalTransformerType transformer) {
        cname = Util.formatClassName(cname);
        if (instrumentedClasses.containsKey(cname)) {
            instrumentedClasses.get(cname).add(transformer);
        } else {
            ArrayList<InternalTransformerType> transformers = new ArrayList<>();
            transformers.add(transformer);
            instrumentedClasses.put(cname, transformers);
        }
    }

    public static boolean isLoaded(String cname) {
        cname = Util.formatClassName(cname);
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (cname.replace("/", ".").equals(clazz.getName())) {
                return true;
            }
        }
        return false;
    }

    public static void logMissedClasses(){
        // Retransform already loaded classes
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            String cname = Util.formatClassName(loadedClass.getName());
            if (instrumentation.isModifiableClass(loadedClass) && Util.shouldInstrument(cname) && !isInstrumented(cname)) {
                logger.warn("YYY: Class {} is already loaded and not instrumented!", loadedClass.getName());
            }

        }
    }

    @SuppressWarnings("unused")
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            ThreadHandler.init();
            ThreadHandler.addThreadContext(-1, "SymbolicThread", -1); // Main thread used during initialization and instrumentation

            if (config.getInstrumentationTransformer().equals(TransformerType.SV_COMP)) {
                Verifier.retrieveInputs(); // ToDo only conditionally and at a petter place
            } else if (config.getInstrumentationTransformer().equals(TransformerType.ANNOTATION)) {
                Intrinsics.retrieveAssignments();
            }


            instrumentation = inst;
            logMissedClasses();
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
            printBox.addMsg("Can Retransform Classes: " + instrumentation.isRetransformClassesSupported());
            printBox.addMsg("Redefine Classes Supported: " + instrumentation.isRedefineClassesSupported());
            printBox.addMsg("Working Directory: " + System.getProperty("user.dir"));
            printBox.addMsg("");

            inst.addTransformer(new ClassInfoTransformer());
            inst.addTransformer(new NoCacheTransformer());
            inst.addTransformer(new RefEqualityTransformer());

            switch (config.getInstrumentationTransformer()) {
                case WEB_SERVLET -> {
                    throw new NotImplementedException("WEB_SERVLET");
                }
                case SPRING_ENDPOINT -> {
                    inst.addTransformer(new SpringEndpointTransformer());
                    // inst.addTransformer(new SpringExceptionTransformer());
                    if (Config.instance().isUseDataEndpointAdapter()) {
                        inst.addTransformer(new SpringRepositoryTransformer());
                    }
                    inst.addTransformer(
                            new ClassVariablesTransformer(), false); // TODO retransform has to be enabled
                }
                case PARAMETER -> {
                    inst.addTransformer(new ParameterTransformer());
                    if (Config.instance().isUseDataEndpointAdapter()) {
                        inst.addTransformer(new SpringRepositoryTransformer());
                    }
                    inst.addTransformer(
                            new ClassVariablesTransformer(), false); // TODO retransform has to be enabled
                }
                case SV_COMP -> {
                    inst.addTransformer(new SVCompTransformer());
                }
                case ANNOTATION -> {
                    inst.addTransformer(new AnnotationTransformer());
                }
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
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to ASM
            new ErrorHandler().handleException(new InstrumentationException(t));
        }
    }
}
