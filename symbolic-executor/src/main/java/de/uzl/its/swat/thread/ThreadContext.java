package de.uzl.its.swat.thread;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor;
import de.uzl.its.swat.symbolic.ClassNames;
import de.uzl.its.swat.symbolic.ObjectInfo;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.symbolic.value.Value;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.SolverContext;

public class ThreadContext {
    @Getter
    private final SymbolicInstructionVisitor symbolicInstructionVisitor;
    @Getter
    @Setter
    private Instruction current;
    @Getter
    private Instruction next;
    @Getter
    private PrintStream invocationStream;
    @Getter
    private boolean disabled;
    Config config = Config.instance();
    private final SolverContext context;

    private final HashMap<Integer, Value<?, ?>[]> statics;

    public SolverContext getSolverContext() {
        return context;
    }

    @Setter
    @Getter
    private int endpointID;
    @Setter
    @Getter
    private String endpointName;

    public ThreadContext(long id) {
        this.symbolicInstructionVisitor = new SymbolicInstructionVisitor(ClassNames.getInstance());
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

    public void setNext(Instruction next) {
        this.next = next;
        symbolicInstructionVisitor.setNext(next);
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
