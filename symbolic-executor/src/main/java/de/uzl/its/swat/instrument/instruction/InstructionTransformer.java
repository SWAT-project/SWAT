package de.uzl.its.swat.instrument.instruction;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.SafeClassWriter;
import de.uzl.its.swat.instrument.Transformer;
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class InstructionTransformer implements ClassFileTransformer {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();


    private static final ThreadLocal<PrintBox> printBox = ThreadLocal.withInitial(() -> new PrintBox(60, "Transformer" + InstructionTransformer.class.getSimpleName()));

    public static PrintBox getPrintBox() {
        return printBox.get();
    }

    public InstructionTransformer() {
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
    }

    /**
     * The implementation of this method may transform the supplied class file and return a new
     * replacement class file. Addition: Adds the InstructionClassAdapter for modifying the
     * classFile
     *
     * @param loader the defining loader of the class to be transformed, may be null if the
     *     bootstrap loader
     * @param cname the name of the class in the internal form of fully qualified class and
     *     interface names as defined in The Java Virtual Machine Specification. For example,
     *     "java/util/List".
     * @param classBeingRedefined if this is triggered by a redefine or retransform, the class being
     *     redefined or retransformed; if this is a class load, null
     * @param d the protection domain of the class being defined or redefined
     * @param cbuf the input byte buffer in class file format - must not be modified
     * @return a well-formed class file buffer (the result of the transform), or null if no
     *     transform is performed.
     */
    @Override
    public byte[] transform(
            ClassLoader loader,
            String cname,
            Class<?> classBeingRedefined,
            ProtectionDomain d,
            byte[] cbuf) {
        try {

            if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)) return cbuf;
            getPrintBox().addMsg("Class: " + cname);
            getPrintBox().setContentPresent(true);

            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw =
                    new SafeClassWriter(
                            cr, loader, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new InstructionClassAdapter(new CheckClassAdapter(cw, true), cname);
            try {
                cr.accept(cv, ClassReader.SKIP_FRAMES);

                if (Config.instance().isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                    SWATAssert.enforce(stringWriter.toString().isEmpty(), "Instrumentation error: {}", stringWriter);
                }
            } catch (Exception e) {
                byte[] partlyTransformedClass = cw.toByteArray();
                saveClass(partlyTransformedClass, cname);
                logger.info("Error: " + e.getMessage());
                for (StackTraceElement ste : Arrays.stream(e.getStackTrace()).toList()) {

                    logger.info("Error: " + ste.toString());
                }
                new ErrorHandler()
                        .handleException("[INSTRUCTION TRANSFORMER] Error while instrumenting", e);
            }

            Transformer.addInstrumentedClass(cname, InternalTransformerType.INSTRUCTION);
            if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());
            return cw.toByteArray();

        } catch (Exception e) {
            new ErrorHandler()
                    .handleException("[INSTRUCTION TRANSFORMER] Error while instrumenting", e);
            System.err.println("HALT!");
            Runtime.getRuntime().halt(3742);
            // new ErrorHandler()
            //         .handleException("[INSTRUCTION TRANSFORMER] Error while instrumenting", e);
        }

        Transformer.addInstrumentedClass(cname, InternalTransformerType.INSTRUCTION);
        if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());
        return cbuf;
    }

    public void saveClass(byte[] transformedClass, String cname) throws Exception {
        String fullPath =
                Config.instance().getLoggingDirectory()
                        + "/"
                        + "instrumented"
                        + "/"
                        + cname.replace('.', '/')
                        + ".class";

        try {
            File file = new File(fullPath);
            File parent = new File(file.getParent());
            if (!parent.exists()) {
                parent.mkdirs();
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(transformedClass);
            }
        } catch (Exception e) {
            new ErrorHandler().handleException(e);
        }
    }
}
