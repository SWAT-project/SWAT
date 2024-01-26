package de.uzl.its.swat.thread;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.interpreters.SymbolicInterpreter;
import de.uzl.its.swat.logger.ClassNames;
import de.uzl.its.swat.logger.ObjectInfo;
import de.uzl.its.swat.logger.inst.Instruction;
import de.uzl.its.symbolic.value.Value;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.SolverContext;

public class ThreadContext {
    private final SymbolicInterpreter symbolicInterpreter;
    private Instruction current;
    private Instruction next;
    private PrintStream invocationStream;
    private boolean disabled;
    Config config = Config.instance();
    private final SolverContext context;

    private final HashMap<Integer, Value<?, ?>[]> statics;

    public SolverContext getSolverContext() {
        return context;
    }

    private int endpointID;
    private String endpointName;

    public ThreadContext(long id) {
        this.symbolicInterpreter = new SymbolicInterpreter(ClassNames.getInstance());
        this.statics = new HashMap<>();
        try {
            Configuration SMTConfig = Configuration.defaultConfiguration();
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
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        try {

            this.invocationStream =
                    new PrintStream(
                            new FileOutputStream(
                                    config.getLoggingPath() + "/invocation-" + id + ".log", true));
        } catch (FileNotFoundException e) {
            this.invocationStream = System.out;
        }
    }

    public void enable() {
        if (disabled) {
            disabled = false;
        } else {
            throw new RuntimeException("The thread is already enabled");
        }
    }

    public void disable() {
        if (!disabled) {
            disabled = true;
        } else {
            throw new RuntimeException("The thread is already disabled");
        }
    }

    public Instruction getCurrent() {
        return current;
    }

    public void setCurrent(Instruction current) {
        this.current = current;
    }

    public Instruction getNext() {
        return next;
    }

    public void setNext(Instruction next) {
        this.next = next;
        symbolicInterpreter.setNext(next);
    }

    public PrintStream getInvocationStream() {
        return invocationStream;
    }

    public SymbolicInterpreter getSymbolicInterpreter() {
        return symbolicInterpreter;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public int getEndpointID() {
        return endpointID;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public void setEndpointID(int endpointID) {
        this.endpointID = endpointID;
    }

    public void setStaticField(ObjectInfo oi, int classId, int fieldId, Value<?, ?> value) {
        if (!statics.containsKey(classId)) {
            int nStaticFields = oi.getNStaticFields();
            statics.put(classId, new Value<?, ?>[nStaticFields]);
        }
        statics.get(classId)[fieldId] = value;
    }

    public Value<?, ?> getStaticField(ObjectInfo oi, int classId, int fieldId) {
        if (fieldId == -1) {
            return null;
        }
        if (!statics.containsKey(classId)) {
            int nStaticFields = oi.getNStaticFields();
            statics.put(classId, new Value<?, ?>[nStaticFields]);
        }
        return statics.get(classId)[fieldId];
    }
}
