package de.uzl.its.swat.config;

import de.uzl.its.swat.instrument.TransformerType;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Getter;

public class Config {
    private static final String DEFAULT_CONFIG_FILE = "../swat.cfg";
    private static final String TRUE = "true";

    /** Implementation of the solver request. */
    @Getter private SolverRequestImpl solverRequest;

    /** Host name of the coordinator service. */
    @Getter private String coordinatorHost;

    /** Port number of the coordinator service. */
    @Getter private String coordinatorPort;

    /** File path for the solver. */
    @Getter private String solverPath;

    /** File path for storing coverage data. */
    @Getter private String coveragePath;

    /** File path for storing total coverage data. */
    @Getter private String coverageTotalPath;

    /** File path for logging. */
    @Getter private String loggingPath;

    /** Flag to use symbolic data endpoint. */
    @Getter private boolean useSymbolicDataEndpoint;

    /** Type of transformer to be used. */
    @Getter private TransformerType transformerType;

    /** Determines if Verifier randomness is enabled. */
    @Getter private boolean verifierRandomnessEnabled;

    /** Flag for verbose logging. */
    @Getter private boolean verbose;

    /** Class name for analysis implementation. */
    @Getter private String analysisClass;

    /** Flag to determine if instrumented classes should be written out. */
    @Getter private boolean writeInstrumentedClasses;

    /** Directory to store instrumented classes. */
    @Getter private String instDir;

    /** Class path for making symbolic values. */
    @Getter private String makeSymbolicClassPath;

    /** Pattern for symbolic function identification. */
    @Getter private String symbolicFunctionPattern;

    /** Start path for symbolic analysis. */
    @Getter private String symbolicStartPath;

    /** Pattern for identifying the starting function in symbolic analysis. */
    @Getter private String symbolicStartFunctionPattern;

    /** Custom function name for analysis. */
    @Getter private String customFunctionName;

    /** Packages to be instrumented. */
    @Getter private String[] instrumentPackages;

    /** Packages to be excluded from instrumentation. */
    @Getter private String[] excludePackages;

    /** Flag to enable or disable instruction IDs. */
    @Getter private boolean instructionIds;

    /** Flag for enabling debug mode. */
    @Getter private boolean debug;

    /** Flag for enabling logging. */
    @Getter private boolean logging;

    /** Flag for enabling invocation logging. */
    @Getter private boolean invocationLogging;

    /** Flag to determine if the application should exit on errors. */
    @Getter private boolean exitOnError;

    private Config() {
        loadProperties();
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream input =
                new FileInputStream(System.getProperty("swat.cfg", DEFAULT_CONFIG_FILE))) {
            properties.load(input);
        } catch (IOException ex) {
            // throw new RuntimeException("Could not find a configuration file!", ex);
            System.err.println("Could not find a configuration file, using default values!");
        }
        solverRequest =
                SolverRequestImpl.valueOf(
                        properties.getProperty("solverRequest", SolverRequestImpl.HTTP.name()));
        coordinatorHost = properties.getProperty("coordinatorHost", "localhost");
        coordinatorPort = properties.getProperty("coordinatorPort", "8079");
        solverPath = properties.getProperty("solverPath", "constraints/submit");
        coveragePath = properties.getProperty("coveragePath", "coverage/submit");
        coverageTotalPath = properties.getProperty("coverageTotalPath", "coverage/total/submit");
        loggingPath = properties.getProperty("loggingPath", "logs/");
        useSymbolicDataEndpoint =
                properties
                        .getProperty("transformer.useSymbolicDataEndpoint", "false")
                        .equals("true");
        transformerType =
                TransformerType.valueOf(
                        properties.getProperty("transformer", TransformerType.NONE.name()));
        verifierRandomnessEnabled =
                properties.getProperty("verifierRandomnessEnabled", "false").equals("true");
        verbose = getBooleanProperty(properties, "verbose");
        analysisClass =
                getClassProperty(properties, "analysisClass", "de.uzl.its.swat.logger.DJVM");
        // Get values for make symbolic
        String symbolicValueFunction = properties.getProperty("symbolicValueFunction", "");
        String[] symbolicValueParts = symbolicValueFunction.split(":", 2);
        makeSymbolicClassPath = symbolicValueParts[0];
        symbolicFunctionPattern = symbolicValueParts.length > 1 ? symbolicValueParts[1] : "";

        String symbolicStartFunction = properties.getProperty("symbolicStartFunction", "");
        String[] symbolicStartParts = symbolicStartFunction.split(":", 2);
        symbolicStartPath = symbolicStartParts[0];
        symbolicStartFunctionPattern =
                processPattern(symbolicStartParts.length > 1 ? symbolicStartParts[1] : "");

        instDir = properties.getProperty("instDir", "instrumented");
        writeInstrumentedClasses =
                properties.getProperty("writeInstrumentedClasses", "false").equals("true");
        customFunctionName =
                properties.getProperty(
                        "customFunctionName", "swatCustomFunctionName_dqjikh32412ds");
        instrumentPackages =
                properties.getProperty("instrumentPackages", null) == null
                        ? null
                        : properties.getProperty("instrumentPackages", null).split(":");

        excludePackages =
                properties.getOrDefault("excludePackages", null) == null
                        ? new String[] {
                            "org/springframework/boot/loader",
                            "de/uzl/its/swat/",
                            "de/uzl/its/symbolic",
                            "de/uzl/its/dto",
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
                        }
                        : properties.getProperty("instrumentPackages", null).split(":");
        debug = properties.getProperty("debug", "false").equals("true");
        exitOnError = properties.getProperty("exitOnError", "true").equals("true");
        instructionIds =
                properties.getProperty("instructionIds", String.valueOf(debug)).equals("true");
        logging = properties.getProperty("logging", "true").equals("true");
        invocationLogging = properties.getProperty("invocationLogging", "true").equals("true");
    }

    private String processPattern(String pattern) {
        return Arrays.stream(pattern.split(","))
                .map(p -> p.replace("*", ".*").replace("?", ".?"))
                .collect(Collectors.joining("|"));
    }

    public boolean exitOnError() {
        return exitOnError;
    }

    private boolean getBooleanProperty(Properties properties, String key) {
        return TRUE.equals(properties.getProperty(key, "false"));
    }

    private String getClassProperty(Properties properties, String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).replace('.', '/');
    }

    private static class LazyHolder {
        static final Config INSTANCE = new Config();
    }

    public static Config instance() {
        return LazyHolder.INSTANCE;
    }
    /** Enum representing the implementations of solver requests. */
    public enum SolverRequestImpl {
        LOCAL,
        HTTP
    }
}
