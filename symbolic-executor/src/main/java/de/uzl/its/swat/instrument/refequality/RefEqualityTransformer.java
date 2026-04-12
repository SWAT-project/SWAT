package de.uzl.its.swat.instrument.refequality;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.SafeClassWriter;
import de.uzl.its.swat.instrument.Transformer;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class RefEqualityTransformer implements ClassFileTransformer {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private static final ThreadLocal<PrintBox> printBox = ThreadLocal.withInitial(
            () -> new PrintBox(60, "Transformer" + RefEqualityTransformer.class.getSimpleName()));

    public static PrintBox getPrintBox() {
        return printBox.get();
    }

    public RefEqualityTransformer() {
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
    }

    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> classBeingRedefined,
                            ProtectionDomain d, byte[] cbuf) {

        try {
            if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)) return cbuf;

            // Don't touch UtilInstrumented because it needs to contain reference equality checks
            if(Util.isInstrumentedUtilClass(cname)) {
                return cbuf;
            }
            getPrintBox().addMsg("Class: " + cname);
            getPrintBox().setContentPresent(true);

            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw = new SafeClassWriter(cr, loader, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new RefEqualityClassAdapter(new CheckClassAdapter(cw, true), cname);
            try {
                cr.accept(cv, ClassReader.SKIP_FRAMES);
                if (Config.instance().isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                    SWATAssert.enforce(stringWriter.toString().isEmpty(), "Instrumentation error: {}", stringWriter);
                }
            } catch (Exception e) {
                logger.info("Error: " + e.getMessage());
                for (StackTraceElement ste : Arrays.stream(e.getStackTrace()).toList()) {
                    logger.info("Error: " + ste.toString());
                }
                new ErrorHandler()
                        .handleException("[REFEQUALITY TRANSFORMER] Error while instrumenting class: " + cname, e);
            }

            Transformer.addInstrumentedClass(cname, InternalTransformerType.INSTRUCTION);
            if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());
            return cw.toByteArray();

        } catch (Exception e) {
            new ErrorHandler()
                    .handleException("[REFEQUALITY TRANSFORMER] Error while instrumenting class: " + cname, e);
            System.err.println("HALT!");
            Runtime.getRuntime().halt(3742);
        }

        Transformer.addInstrumentedClass(cname, InternalTransformerType.INSTRUCTION);
        if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());
        return cbuf;
    }
}