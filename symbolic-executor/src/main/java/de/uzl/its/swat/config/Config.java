package de.uzl.its.swat.config;

import de.uzl.its.swat.instrument.TransformerType;
import de.uzl.its.swat.solver.SolverMode;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import lombok.Getter;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * The Config class is responsible for reading the configuration file and providing the application
 * with the configuration options. It is a singleton class, and the instance should be fetched using
 * the static {@link #instance()} method.
 */
public class Config {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Config.class);

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

    // ------------------------------------
    // Logging options
    // ------------------------------------

    /** Directory to store logs and instrumented .class files. */
    @Getter private String loggingDirectory;

    private static final String DEFAULT_LOGGING_DIRECTORY = "logs/";

    /** Sets the logging level. Default: "Level.INFO" */
    @Getter private Level loggingLevel;

    private static final Level DEFAULT_LOGGING_LEVEL = Level.INFO;

    /** Flag to determine if instrumented classes should be written out. */
    @Getter private boolean loggingClasses;

    private static final boolean DEFAULT_LOGGING_CLASSES = false;

    /** Flag for enabling debug mode. */
    @Getter private boolean loggingDebug;

    private static final boolean DEFAULT_LOGGING_DEBUG = false;

    /** Flag for enabling invocation logging. */
    @Getter private boolean loggingInvocations;

    private static final boolean DEFAULT_LOGGING_INVOCATIONS = true;

    /** Length to print symbolic formula. */
    @Getter private int loggingFormulaLength;

    private static final int DEFAULT_LOGGING_FORMULA_LENGTH = 100;

    // ------------------------------------
    // Instrumentation options
    // ------------------------------------

    /**
     * Fully qualified name of the class that receives instruction information from the concrete
     * execution.
     */
    @Getter private String instrumentationDispatcher;

    private static final String DEFAULT_INSTRUMENTATION_DISPATCHER =
            "de.uzl.its.swat.symbolic.SymbolicInstructionDispatcher";

    /** Custom function name for analysis. */
    @Getter private String instrumentationPrefix;

    private static final String DEFAULT_INSTRUMENTATION_PREFIX = "SWAT";

    /** Packages to be instrumented. */
    @Getter private String[] instrumentationIncludePackages;

    private static final String[] DEFAULT_INSTRUMENTATION_INCLUDE_PACKAGES = new String[] {};

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
            };

    /** Fully qualified class name that should be tracked symbolically. Supports basic regex. */
    @Getter private String instrumentationParameterSymbolicClassName;

    /** Method name that should be tracked symbolically. Supports basic regex. */
    @Getter private String instrumentationParameterSymbolicMethodName;

    /**
     * Flag to enable or disable instruction IDs. No default value, as it is derived from the
     * loggingDebug option.
     */
    @Getter private boolean instrumentationInstructionIds;

    /** Type of transformer to be used. */
    @Getter private TransformerType instrumentationTransformer;

    // ------------------------------------
    // Solver options
    // ------------------------------------
    /** Determines solver mode should be used. */
    @Getter private SolverMode solverMode;

    private static final SolverMode DEFAULT_SOLVER_MODE = SolverMode.LOCAL;

    // ------------------------------------
    // General options
    // ------------------------------------
    /** Flag to determine if the application should exit on errors. */
    @Getter private boolean exitOnError;
    private static final boolean DEFAULT_EXIT_ON_ERROR = true;

    // ------------------------------------
    // SV-comp options
    // ------------------------------------

    /** Determines if Verifier randomness is enabled. */
    @Getter private boolean svcompRandomInputs;
    private static final boolean DEFAULT_SVCOMP_RANDOM_INPUTS = false;

    /**
     * Constructor for the configuration class. Loads the properties from the configuration file.
     * Should not be called directly, the configuration instance should be fetched using the static
     * {@link #instance()} method.
     */
    private Config() {
        logger.debug("Initializing configuration.");
        loadProperties();
        readProperties();
    }

    /**
     * Loads the properties from the configuration file. If no file is found, default values are
     * used.
     */
    private void loadProperties() {
        try (FileInputStream input =
                new FileInputStream(System.getProperty("config.path", DEFAULT_CONFIG_FILE))) {
            properties.load(input);
        } catch (IOException ex) {
            logger.warn("Could not find a configuration file, using default values!");
        }
    }

    /**
     * Reads a boolean value from the properties file. If the key is not found, the default value is used
     * @param key The key to read
     * @param defaultValue The default value
     * @return The value read from the properties file or the default value
     */
    private boolean readBoolean(String key, boolean defaultValue) {
        boolean val;
        if (properties.containsKey(key)) {
            val = Boolean.parseBoolean(properties.getProperty(key));
            logger.debug("[Loaded]  " + key + ":" + val);
        } else {
            val = defaultValue;
            logger.debug("[Default] " + key + ":" + val);
        }
        return val;
    }


    /**
     * Reads an int value from the properties file. If the key is not found, the default value is used
     * @param key The key to read
     * @param defaultValue The default value
     * @return The value read from the properties file or the default value
     */
    private int readInt(String key, int defaultValue) {
        int val;
        if (properties.containsKey(key)) {
            val = Integer.parseInt(properties.getProperty(key));
            logger.debug("[Loaded]  " + key + ":" + val);
        } else {
            val = defaultValue;
            logger.debug("[Default] " + key + ":" + val);
        }
        return val;
    }


    /**
     * Reads a string from the properties file. If the key is not found, the default value is used
     * @param key The key to read
     * @param defaultValue The default value
     * @return The value read from the properties file or the default value
     */
    private String readString(String key, String defaultValue) {
        String val;
        if (properties.containsKey(key)) {
            val = properties.getProperty(key);
            logger.debug("[Loaded]  " + key + ":" + val);
        } else {
            val = defaultValue;
            logger.debug("[Default] " + key + ":" + val);
        }
        return val;
    }



    /**
     * Reads a colon separated list from the properties file. If the key is not found, the default value is used
     * @param key The key to read
     * @param defaultValue The default value
     * @return The value read from the properties file or the default value
     */
    private String[] readList(String key, String[] defaultValue) {
        String[] val;
        if (properties.containsKey(key)) {
            val = properties.getProperty(key).split(":");
            logger.debug("[Loaded]  " + key + ":" + Arrays.toString(val));
        } else {
            val = defaultValue;
            logger.debug("[Default] " + key + ":" + Arrays.toString(val));
        }
        return val;
    }

    /** Reads the properties from the configuration file. */
    private void readProperties() {
        // ------------------------------------
        // Explorer Connection options
        // ------------------------------------
        explorerHost = readString("explorer.host", DEFAULT_EXPLORER_HOST);
        explorerPort = readInt("explorer.port", DEFAULT_EXPLORER_PORT);
        explorerTraceURI = readString("explorer.traceURI", DEFAULT_EXPLORER_TRACE_URI);
        // ------------------------------------
        // Logging options
        // ------------------------------------
        loggingDirectory = readString("logging.directory", DEFAULT_LOGGING_DIRECTORY);
        loggingLevel = Level.valueOf(readString("logging.level", DEFAULT_LOGGING_LEVEL.toString()));
        loggingClasses = readBoolean("logging.classes", DEFAULT_LOGGING_CLASSES);
        loggingDebug = readBoolean("logging.debug", DEFAULT_LOGGING_DEBUG);
        loggingInvocations = readBoolean("logging.invocations", DEFAULT_LOGGING_INVOCATIONS);
        loggingFormulaLength = readInt("logging.formulaLength", DEFAULT_LOGGING_FORMULA_LENGTH);

        // ------------------------------------
        // Instrumentation options
        // ------------------------------------
        instrumentationDispatcher =
                readString("instrumentation.dispatcher", DEFAULT_INSTRUMENTATION_DISPATCHER)
                        .replace('.', '/');
        instrumentationPrefix =
                readString("instrumentation.prefix", DEFAULT_INSTRUMENTATION_PREFIX);
        instrumentationIncludePackages =
                readList(
                        "instrumentation.includePackages",
                        DEFAULT_INSTRUMENTATION_INCLUDE_PACKAGES);

        instrumentationExcludePackages =
                readList(
                        "instrumentation.excludePackages",
                        DEFAULT_INSTRUMENTATION_EXCLUDE_PACKAGES);

        instrumentationInstructionIds = readBoolean("instrumentation.instructionIds", loggingDebug);

        instrumentationTransformer =
                TransformerType.valueOf(
                        readString("instrumentation.transformer", TransformerType.NONE.name()));

        String[] instrumentationParameterSymbolicPattern = readString("instrumentation.parameter.symbolicPattern", "").split(":");
        assert instrumentationParameterSymbolicPattern.length == 2;
        instrumentationParameterSymbolicClassName = instrumentationParameterSymbolicPattern[0];
        instrumentationParameterSymbolicMethodName = instrumentationParameterSymbolicPattern[1];

        // ------------------------------------
        // General options
        // ------------------------------------
        exitOnError = readBoolean("exitOnError", DEFAULT_EXIT_ON_ERROR);

        // ------------------------------------
        // Solver options
        // ------------------------------------
        solverMode = SolverMode.valueOf(readString("solver.mode", DEFAULT_SOLVER_MODE.toString()));
        // ------------------------------------
        // SV-Comp options
        // ------------------------------------
        svcompRandomInputs = readBoolean("svcomp.randomInputs", DEFAULT_SVCOMP_RANDOM_INPUTS);

    }

    /**
     * LazyHolder to hold global instance of config object
     */
    private static class LazyHolder {
        /** The global config instance */
        static final Config INSTANCE = new Config();
    }

    /**
     * Instance method. Should be used to obtain config object
     * @return The config instance.
     */
    public static Config instance() {
        return LazyHolder.INSTANCE;
    }
}
