package de.uzl.its.swat.thread;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ThreadAlreadyDisabledException;
import de.uzl.its.swat.common.exceptions.ThreadAlreadyEnabledException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.ErrorRecord;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.common.logging.LoggerUtils;
import de.uzl.its.swat.common.logging.StatsStorage;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.metadata.ClassDepot;
import de.uzl.its.swat.metadata.ClassDepotRuntime;
import de.uzl.its.swat.solver.SolverMode;
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor;
import de.uzl.its.swat.symbolic.UFs.UFHandler;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.symbolic.trace.CoverageTraceHandler;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.SolverContext;

import static java.lang.Thread.currentThread;

public class ThreadContext {
    private final long id;
    @Getter private final Logger shadowStateLogger;

    private SymbolicInstructionVisitor symbolicInstructionVisitor;
    @Getter private final CoverageTraceHandler coverageTraceHandler;
    @Getter private final SymbolicTraceHandler symbolicTraceHandler = new SymbolicTraceHandler();
    @Getter private final UFHandler ufHandler = new UFHandler();
    @Getter @Setter private Instruction current;
    @Getter @Setter private Long instCnt = 0L;
    @Getter private Instruction next;
    @Getter private PrintStream statsStream;
    @Getter private boolean disabled;
    @Getter private boolean aborted;
    Config config = Config.instance();
    private final SolverContext context;

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private final HashMap<Integer, Value<?, ?>[]> statics;
    @Getter private final StatsStorage statsStorage = new StatsStorage();

    private final ClassDepotRuntime classDepot = ClassDepot.getRuntimeInstance();


    @Setter @Getter private int endpointID;
    @Setter @Getter private String endpointName;

    // Tracks if liftValue is called multiple times with the same index.
    @Getter private final HashMap<String, Integer> symbolicIdxOccurrence = new java.util.HashMap<>();
    private HashMap<String, Long> SubUid = new HashMap<>();

    private final AbortTimer timer = new AbortTimer(Config.instance().getAbortTimerValInMS(), TimeUnit.MILLISECONDS);

    public SolverContext getSolverContext() {
        return context;
    }

    public ThreadContext(long id, String endpointName, int endpointID) throws InvalidConfigurationException {

        this.id = id;

        if (Config.instance().isUseAbortTimer()) {
            timer.start();
        }

        Level logLevel =
                Config.instance().isDisableShadowStackLogging()
                        ? Level.OFF
                        : Config.instance().getLoggingLevel();
        this.endpointName = endpointName;
        this.endpointID = endpointID;
        shadowStateLogger =
                Config.instance().isLogShadowStateToConsole()
                        ? logger
                        : LoggerUtils.createAsyncFileLogger(endpointName,
                                null,
                                logLevel);

        this.statics = new HashMap<>();

        Configuration SMTDefaultConfig = Configuration.defaultConfiguration();
        Configuration SMTConfig;

        if (Config.instance().getSolverMode().equals(SolverMode.LOCAL)) {
            ConfigurationBuilder builder = Configuration.builder();
            builder.copyFrom(SMTDefaultConfig);
            builder.setOption("solver", "z3");
            builder.setOption("solver.z3.usePhantomReferences", "true");
            builder.setOption("solver.z3.memoryMaxSize", "1024"); // in MB
            builder.setOption("solver.z3.memoryHighWaterMarkMb", "820"); // in MB
            builder.setOption("solver.z3.timout", "60000");
            SMTConfig = builder.build();
        } else {
            SMTConfig = SMTDefaultConfig;
        }

        logger.info(SMTConfig.asPropertiesString());

        LogManager logger = BasicLogManager.create(SMTConfig);
        ShutdownManager shutdown = ShutdownManager.create();

        // SolverContext is a class wrapping a solver context.
        // Solver can be selected either using an argument or a configuration option
        // inside `config`.
        context =
                SolverContextFactory.createSolverContext(
                        SMTConfig,
                        logger,
                        shutdown.getNotifier(),
                        SolverContextFactory.Solvers.Z3);


        this.coverageTraceHandler = new CoverageTraceHandler();

        try {

            this.statsStream =
                    new PrintStream(
                            new FileOutputStream(
                                    config.getLoggingDirectory() + "/stats_" + id + ".json",
                                    true));
        } catch (FileNotFoundException e) {
            this.statsStream = System.out;
        }
    }

    void checkAbortTimerExpiration() {
        if (timer.isExpired()) {
            aborted = true;
            disabled = true;
            logger.warn("Abort timer expired. Thread context " + id + " is now aborted.");
        }
    }

    void pauseAbortTimer() {
        timer.stop();
    }

    void resumeAbortTimer() {
        timer.restart();
    }

    void resetAndRestartAbortTimer() {
        timer.resetAndStart();
    }

    public void enable() throws ThreadAlreadyEnabledException {
        if (disabled && !aborted) {
            disabled = false;
        } else {
            throw new ThreadAlreadyEnabledException(Thread.currentThread().getId());
        }
    }

    public void disable() throws ThreadAlreadyDisabledException {
        if (!disabled) {
            disabled = true;
        } else if (!aborted) {
            throw new ThreadAlreadyDisabledException(Thread.currentThread().getId());
        }
    }

    public void setNext(Instruction next) {
        this.next = next;
        symbolicInstructionVisitor.setNextInst(next);
    }

    public SymbolicInstructionVisitor getSymbolicInstructionVisitor() throws NoThreadContextException {
        // Lazy initialize
        if (symbolicInstructionVisitor == null) {
            symbolicInstructionVisitor = new SymbolicInstructionVisitor();
        }
        return symbolicInstructionVisitor;
    }
    public void setStaticField(int cIdx, int fIdx, Value<?, ?> value)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        if (!statics.containsKey(cIdx)) {
            String cName = classDepot.getClassName(cIdx);
            int nStaticFields = classDepot.getFieldCountWithReflection(cName, null, true);
            statics.put(cIdx, new Value<?, ?>[nStaticFields]);
        }
        statics.get(cIdx)[fIdx] = value;
    }

    public Value<?, ?> getStaticField(int classIndex, int runtimeFieldIndex)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        SWATAssert.check(runtimeFieldIndex >= 0, "Field ID must be non-negative");
        String cName = classDepot.getClassName(classIndex);
        if (!statics.containsKey(classIndex)) {
            shadowStateLogger.debug("Initializing static fields for class {}", cName);
            logger.debug("Initializing static fields for class {}", cName);

            int nStaticFields = classDepot.getFieldCountWithReflection(cName, null, true);
            statics.put(classIndex, new Value<?, ?>[nStaticFields]);
        }
        Value<?,?> v = statics.get(classIndex)[runtimeFieldIndex];
        if (v == null){
            logger.warn("[{}]: Cannot get static field {} of class {}",
                    ThreadHandler.getCurrentInstruction(currentThread().getId()), runtimeFieldIndex, cName);
            v = PlaceHolder.instance;
        }
        return v;
    }

    public void addCoverage(long iid) {
        coverageTraceHandler.addInstruction(iid);
    }

    public void recordMissingInvocation(InvocationEntry entry) {

        statsStorage.getInvocations().merge(entry, 1, Integer::sum);
    }


    public long getNextSubUid(String symbolicVar) {
        long next = SubUid.getOrDefault(symbolicVar, 0L);
        SubUid.put(symbolicVar, next + 1);
        System.out.println("[DSE] Next sub-uid for " + symbolicVar + " is " + next);
        return next;
    }

    /**
     * Records an exception that occurred during the execution of the symbolic execution. This includes swallowed
     * assertions.
     *
     * @param record The error record
     */
    public void recordException(ErrorRecord record) {
        statsStorage.getErrors().add(record);
    }
}
