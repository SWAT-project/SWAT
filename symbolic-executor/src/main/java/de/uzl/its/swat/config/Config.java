package de.uzl.its.swat.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.logging.LoggerUtils;
import de.uzl.its.swat.coverage.CoverageType;
import de.uzl.its.swat.instrument.TransformerType;
import de.uzl.its.swat.solver.SolverMode;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import lombok.Getter;
import lombok.Setter;

/**
 * The Config class is responsible for reading the configuration file and providing the application
 * with the configuration options. It is a singleton class, and the instance should be fetched using
 * the static {@link #instance()} method.
 */
public class Config {

    private static final Properties properties = new Properties();

    private static final String DEFAULT_CONFIG_FILE = "../swat.cfg";

    // ------------------------------------
    // Explorer Connection options
    // ------------------------------------

    /** Host name of the symbolic explorer. */
    @Getter private String explorerHost;
    private static final String DEFAULT_EXPLORER_HOST = "localhost";

    /** Port number of the symbolic explorer. */
    @Getter private int explorerPort;
    private static final int DEFAULT_EXPLORER_PORT = 8078;

    /** URI where the symbolic explorer receives trace information. */
    @Getter private String explorerTraceURI;
    private static final String DEFAULT_EXPLORER_TRACE_URI = "constraints/submit";

    @Getter private String branchCoveragePath;
    @Getter private String instrCoveragePath;
    public static final String DEFAULT_BRANCH_COVERAGE_SUBMIT = "branch-coverage/submit";
    public static final String DEFAULT_INSTR_COVERAGE_SUBMIT = "instr-coverage/submit";

    // ------------------------------------
    // Logging options
    // ------------------------------------

    /** Directory to store logs and instrumented .class files. */
    @Getter private String loggingDirectory;
    private static final String DEFAULT_LOGGING_DIRECTORY = "logs";

    /** Flag to determine if instrumented classes should be written out. */
    @Getter private boolean loggingClasses;
    private static final boolean DEFAULT_LOGGING_CLASSES = false;

    /** Flag for enabling debug mode. */
    @Getter private boolean loggingDebug;
    private static final boolean DEFAULT_LOGGING_DEBUG = false;

    /** Flag for disabling shadow stack logging. */
    @Getter private boolean disableShadowStackLogging;
    private static final boolean DEFAULT_DISABLE_SHADOW_STACK_LOGGING = false;

    /** Flag for disabling symbolic execution logging. */
    @Getter private boolean disableSymbolicExecutionLogging;
    private static final boolean DEFAULT_DISABLE_SYMBOLIC_EXECUTION_LOGGING = false;

    /** Logging level. */
    @Getter private Level loggingLevel;
    private static final String DEFAULT_LOGGING_LEVEL = "info";

    @Getter private boolean logToConsole;
    private static final boolean DEFAULT_LOG_TO_CONSOLE = true;

    @Getter@Setter
    private boolean logShadowStateToConsole;
    private static final boolean DEFAULT_LOG_SHADOW_STATE_TO_CONSOLE = false;

    /** Flag for enabling stats logging. */
    @Getter private boolean loggingStats;
    private static final boolean DEFAULT_LOGGING_STATS = true;

    /** Length to print symbolic formula. */
    @Getter private int loggingFormulaLength;
    private static final int DEFAULT_LOGGING_FORMULA_LENGTH = 100;

    /** Instruction count after which to enable detailed logging for debugging large applications. */
    @Getter private int loggingInstructionCount;
    private static final int DEFAULT_LOGGING_INSTRUCTION_COUNT = Integer.MAX_VALUE;

    // ------------------------------------
    // Instrumentation options
    // ------------------------------------

    @Getter private boolean useDataEndpointAdapter;

    private static final boolean DEFAULT_USE_DATA_ENDPOINT_ADAPTER = false;

    /**
     * For debugging and analysis purposes.
     * https://asm.ow2.io/javadoc/org/objectweb/asm/util/CheckClassAdapter.html
     * WARNING: Does not work with all targets, for example verifying a spring target fails due to missing classes
     */
    @Getter private boolean useCheckClassAdapter;
    private static final boolean DEFAULT_USE_CHECK_CLASS_ADAPTER = false;

    @Getter private String checkClassAdapterClass;
    private static final String DEFAULT_CHECK_CLASS_ADAPTER_CLASS = "*";

    /**
     * Fully qualified name of the class that receives instruction information from the concrete execution.
     */
    @Getter private String instrumentationDispatcher;
    private static final String DEFAULT_INSTRUMENTATION_DISPATCHER =
            "de.uzl.its.swat.symbolic.dispatcher.SymbolicInstructionDispatcher";

    /** Custom function name for analysis. */
    @Getter private String instrumentationPrefix;
    private static final String DEFAULT_INSTRUMENTATION_PREFIX = "SWAT";

    /** Packages to be instrumented. */
    @Getter private String[] instrumentationScope;
    private static final String[] DEFAULT_INSTRUMENTATION_SCOPE = new String[] {};

    /** Packages to be instrumented. */
    @Getter private String[] symbolicScope;
    private static final String[] DEFAULT_SYMBOLIC_SCOPE = new String[] {};


    /** Packages to be excluded from instrumentation. */
    @Getter private String[] instrumentationExcludePackages;
    private static final String[] DEFAULT_INSTRUMENTATION_EXCLUDE_PACKAGES =
            new String[] {
                    "org/springframework/boot/loader",
                    "de/uzl/its/swat/",
                    "de/uzl/its/symbolic",
                    "de/uzl/its/swat/symbolic/trace/dto",
                    "com/intellij/",
                    "com/fasterxml/",
                    "sun/",
                    "jdk/",
                    "org/h2/",
                    "org/sosy_lab",
                    "java/",
                    "javax/",
                    "com/sun/",
                    "org/apache/",
                    "com/microsoft",
                    "com/google",
                    "org/w3c",
                    "org/jcp",
                    "org/objectweb/asm",
                    "apple/security",
                    "org/apache",
                    "org/slf4j",
                    "shadow",
                    "org/modelmapper/internal/bytebuddy"
            };

    /** Fully qualified class name that should be tracked symbolically. Supports basic regex. */
    @Getter private ArrayList<String> instrumentationParameterSymbolicClassNames = new ArrayList<>();

    /** Method name that should be tracked symbolically. Supports basic regex. */
    @Getter private ArrayList<String> instrumentationParameterSymbolicMethodNames = new ArrayList<>();


    /** Fully qualified class name that should be tracked symbolically. Supports basic regex. */
    @Getter private String instrumentationAnnotationSymbolicClassName;

    /** Method name that should be tracked symbolically. Supports basic regex. */
    @Getter private String instrumentationAnnotationSymbolicMethodName;

    /** Type of transformer to be used. */
    @Getter private TransformerType instrumentationTransformer;

    // ------------------------------------
    // Solver options
    // ------------------------------------
    /** Determines which solver mode should be used. */
    @Getter private SolverMode solverMode;
    private static final SolverMode DEFAULT_SOLVER_MODE = SolverMode.LOCAL;

    // ------------------------------------
    // General options
    // ------------------------------------

    /** Flag to determine if (custom) assertions should be used. */
    @Getter private boolean useAssertions;
    private static final boolean DEFAULT_USE_ASSERTIONS = true;

    @Getter private boolean coverageOnly;
    @Getter private CoverageType coverageType;
    @Getter private boolean constraintsOnly;

    @Getter private boolean useAbortTimer;
    private static final boolean DEFAULT_USE_ABORT_TIMER = false;
    @Getter private int abortTimerValInMS;
    private static final int DEFAULT_ABORT_TIMER_VAL_IN_MS = 20_000;

    // ------------------------------------
    // SV-comp options
    // ------------------------------------
    /** Determines if verifier randomness is enabled. */
    @Getter private boolean svcompRandomInputs;
    private static final boolean DEFAULT_SVCOMP_RANDOM_INPUTS = false;

    private Logger logger;

    // ------------------------------------
    // Fontus options
    // ------------------------------------
    @Getter private boolean useStringInterning;
    private static final boolean DEFAULT_STRING_INTERNING = true;
    @Getter private String stringProxy;
    private static final String DEFAULT_STRING_PROXY = "Ljava/lang/String;";



    /**
     * Constructor for the configuration class. Loads the properties from the configuration file.
     * Should not be called directly, the configuration instance should be fetched using the static
     * {@link #instance()} method.
     */
    private Config() {
        logger =
                LoggerUtils.createGlobalLogger(
                        "config", DEFAULT_LOGGING_DIRECTORY, true, Level.INFO);
        logger.info("Initializing configuration.");
        loadProperties();
        readProperties();
    }

    /**
     * Loads the properties from the configuration file. If no file is found, default values are used.
     */
    private void loadProperties() {
        String configFile = System.getProperty("config.path", DEFAULT_CONFIG_FILE);
        try (FileInputStream input =
                     new FileInputStream(configFile)) {
            properties.load(input);
        } catch (IOException ex) {
            logger.warn("Could not find a configuration file at {}, using default values!", configFile);
        }
    }

    /**
     * Reads a boolean value from the command line or properties file. If the key is not found, the default value is used.
     *
     * @param key The key to read.
     * @param defaultValue The default value.
     * @return The boolean value.
     */
    private boolean readBoolean(String key, boolean defaultValue) {
        String cmdLineValue = System.getProperty(key);
        if (cmdLineValue != null) {
            boolean val = Boolean.parseBoolean(cmdLineValue);
            logger.info("[CommandLine] " + key + ":" + val);
            return val;
        } else if (properties.containsKey(key)) {
            boolean val = Boolean.parseBoolean(properties.getProperty(key));
            logger.info("[Loaded]  " + key + ":" + val);
            return val;
        } else {
            logger.info("[Default] " + key + ":" + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads an int value from the command line or properties file. If the key is not found, the default value is used.
     *
     * @param key The key to read.
     * @param defaultValue The default value.
     * @return The int value.
     */
    private int readInt(String key, int defaultValue) {
        String cmdLineValue = System.getProperty(key);
        if (cmdLineValue != null) {
            try {
                int val = Integer.parseInt(cmdLineValue);
                logger.info("[CommandLine] " + key + ":" + val);
                return val;
            } catch (NumberFormatException e) {
                logger.warn("Invalid int format for command line argument " + key + ": " + cmdLineValue + ". Using default or config file value.");
            }
        }
        if (properties.containsKey(key)) {
            try {
                int val = Integer.parseInt(properties.getProperty(key));
                logger.info("[Loaded]  " + key + ":" + val);
                return val;
            } catch (NumberFormatException e) {
                logger.warn("Invalid int format in properties for key " + key + ": " + properties.getProperty(key) + ". Using default.");
            }
        }
        logger.info("[Default] " + key + ":" + defaultValue);
        return defaultValue;
    }

    /**
     * Reads a string value from the command line or properties file. If the key is not found, the default value is used.
     *
     * @param key The key to read.
     * @param defaultValue The default value.
     * @return The string value.
     */
    private String readString(String key, String defaultValue) {
        String cmdLineValue = System.getProperty(key);
        if (cmdLineValue != null) {
            logger.info("[CommandLine] " + key + ":" + cmdLineValue);
            return cmdLineValue;
        } else if (properties.containsKey(key)) {
            String val = properties.getProperty(key);
            logger.info("[Loaded]  " + key + ":" + val);
            return val;
        } else {
            logger.info("[Default] " + key + ":" + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads a colon separated list from the command line or properties file. If the key is not found, the default value is used.
     *
     * @param key The key to read.
     * @param defaultValue The default value.
     * @return The list as a String array.
     */
    private String[] readList(String key, String[] defaultValue) {
        String cmdLineValue = System.getProperty(key);
        if (cmdLineValue != null) {
            String[] val = cmdLineValue.split(":");
            logger.info("[CommandLine] " + key + ":" + Arrays.toString(val));
            return val;
        } else if (properties.containsKey(key)) {
            String[] val = properties.getProperty(key).split(":");
            logger.info("[Loaded]  " + key + ":" + Arrays.toString(val));
            return val;
        } else {
            logger.info("[Default] " + key + ":" + Arrays.toString(defaultValue));
            return defaultValue;
        }
    }

    /** Reads the properties from the configuration file, command line, or defaults. */
    private void readProperties() {
        // ------------------------------------
        // Logging options
        // ------------------------------------
        loggingDirectory = readString("logging.directory", DEFAULT_LOGGING_DIRECTORY);
        loggingClasses = readBoolean("logging.classes", DEFAULT_LOGGING_CLASSES);
        loggingDebug = readBoolean("logging.debug", DEFAULT_LOGGING_DEBUG);
        loggingLevel = mapToLoggingLevel(readString("logging.level", DEFAULT_LOGGING_LEVEL));
        disableShadowStackLogging = readBoolean("logging.disableShadowStackLogging", DEFAULT_DISABLE_SHADOW_STACK_LOGGING);
        disableSymbolicExecutionLogging = readBoolean("logging.disableSymbolicExecutionLogging", DEFAULT_DISABLE_SYMBOLIC_EXECUTION_LOGGING);
        logToConsole = readBoolean("logging.toConsole", DEFAULT_LOG_TO_CONSOLE);
        logShadowStateToConsole = readBoolean("logging.shadowStateToConsole", DEFAULT_LOG_SHADOW_STATE_TO_CONSOLE);
        loggingStats = readBoolean("logging.statistics", DEFAULT_LOGGING_STATS);
        loggingFormulaLength = readInt("logging.formulaLength", DEFAULT_LOGGING_FORMULA_LENGTH);
        loggingInstructionCount = readInt("logging.instructionCount", DEFAULT_LOGGING_INSTRUCTION_COUNT);

        // ------------------------------------
        // Explorer Connection options
        // ------------------------------------
        explorerHost = readString("explorer.host", DEFAULT_EXPLORER_HOST);
        explorerPort = readInt("explorer.port", DEFAULT_EXPLORER_PORT);
        explorerTraceURI = readString("explorer.traceURI", DEFAULT_EXPLORER_TRACE_URI);
        branchCoveragePath = readString("explorer.branchCoveragePath", DEFAULT_BRANCH_COVERAGE_SUBMIT);
        instrCoveragePath = readString("explorer.instrCoveragePath", DEFAULT_INSTR_COVERAGE_SUBMIT);

        // ------------------------------------
        // Instrumentation options
        // ------------------------------------
        useDataEndpointAdapter = readBoolean("instrumentation.useDataEndpointAdapter", DEFAULT_USE_DATA_ENDPOINT_ADAPTER);
        useCheckClassAdapter = readBoolean("instrumentation.checkClassAdapter", DEFAULT_USE_CHECK_CLASS_ADAPTER);
        checkClassAdapterClass = readString("instrumentation.checkClassAdapterClass", DEFAULT_CHECK_CLASS_ADAPTER_CLASS);
        instrumentationDispatcher = readString("instrumentation.dispatcher", DEFAULT_INSTRUMENTATION_DISPATCHER)
                .replace('.', '/');
        instrumentationPrefix = readString("instrumentation.prefix", DEFAULT_INSTRUMENTATION_PREFIX);
        instrumentationScope = readList("instrumentation.includePackages", DEFAULT_INSTRUMENTATION_SCOPE);
        symbolicScope = readList("instrumentation.symbolicPackages", DEFAULT_SYMBOLIC_SCOPE);
        instrumentationExcludePackages = readList("instrumentation.excludePackages", DEFAULT_INSTRUMENTATION_EXCLUDE_PACKAGES);
        instrumentationTransformer = TransformerType.valueOf(
                readString("instrumentation.transformer", TransformerType.ANNOTATION.name()));
        extractSymbolicParameterPattern();


        String instrumentationAnnotationSymbolicPatternString = readString("instrumentation.annotation.symbolicPattern", "");
        String[] instrumentationAnnotationSymbolicPattern = instrumentationAnnotationSymbolicPatternString.split(":");
        if (instrumentationAnnotationSymbolicPattern.length != 2) {
            if (!instrumentationAnnotationSymbolicPatternString.isEmpty())
                logger.warn("No symbolic pattern (using *:main) or invalid pattern provided: instrumentation.annotation.symbolicPattern: {}", instrumentationAnnotationSymbolicPatternString);
        } else {
            instrumentationAnnotationSymbolicClassName = instrumentationAnnotationSymbolicPattern[0];
            instrumentationAnnotationSymbolicMethodName = instrumentationAnnotationSymbolicPattern[1];
        }

        // ------------------------------------
        // General options
        // ------------------------------------
        useAssertions = readBoolean("useAssertions", DEFAULT_USE_ASSERTIONS);
        coverageType = CoverageType.valueOf(readString("coverageType", CoverageType.NONE.name()).toUpperCase());
        coverageOnly = readBoolean("coverageOnly", false);
        constraintsOnly = readBoolean("constraintsOnly", false);
        useAbortTimer = readBoolean("useAbortTimer", DEFAULT_USE_ABORT_TIMER);
        abortTimerValInMS = readInt("abortTimerValInMS", DEFAULT_ABORT_TIMER_VAL_IN_MS);

        // ------------------------------------
        // Solver options
        // ------------------------------------
        solverMode = SolverMode.valueOf(readString("solver.mode", DEFAULT_SOLVER_MODE.toString()));

        // ------------------------------------
        // SV-Comp options
        // ------------------------------------
        svcompRandomInputs = readBoolean("svcomp.randomInputs", DEFAULT_SVCOMP_RANDOM_INPUTS);

        // ------------------------------------
        // Fontus options
        // ------------------------------------
        useStringInterning = readBoolean("useStringInterning", DEFAULT_STRING_INTERNING);
        stringProxy = readString("stringProxy", DEFAULT_STRING_PROXY);
    }

    private void extractSymbolicParameterPattern() {
        String val = readString("instrumentation.parameter.symbolicPattern", "");

        String[] entries = val.split(",");
        for (String entry : entries) {
            String[] pattern = entry.split(":");
            if (pattern.length != 2) {
                if (!entry.isEmpty())
                    logger.warn("Invalid instrumentation.parameter.symbolicPattern entry: {}", entry);
            } else {
                instrumentationParameterSymbolicClassNames.add(pattern[0]);
                instrumentationParameterSymbolicMethodNames.add(pattern[1]);
            }
        }
    }

    private Level mapToLoggingLevel(String s) {
        Map<String, Level> loggingLevelMap = new HashMap<>();
        loggingLevelMap.put("warn", Level.WARN);
        loggingLevelMap.put("info", Level.INFO);
        loggingLevelMap.put("debug", Level.DEBUG);
        loggingLevelMap.put("error", Level.ERROR);
        loggingLevelMap.put("trace", Level.TRACE);
        loggingLevelMap.put("off", Level.OFF);

        return loggingLevelMap.getOrDefault(s.toLowerCase(), loggingLevelMap.get(DEFAULT_LOGGING_LEVEL));
    }

    /** LazyHolder to hold global instance of config object */
    private static class LazyHolder {
        /** The global config instance */
        static final Config INSTANCE = new Config();
    }

    /**
     * Returns the singleton instance of the configuration.
     *
     * @return The config instance.
     */
    public static Config instance() {
        return LazyHolder.INSTANCE;
    }
}
