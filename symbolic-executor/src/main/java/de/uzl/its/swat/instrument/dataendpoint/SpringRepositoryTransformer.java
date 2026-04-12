package de.uzl.its.swat.instrument.dataendpoint;

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class SpringRepositoryTransformer implements ClassFileTransformer {

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    @Getter private static PrintBox printBox;

    public SpringRepositoryTransformer() {
        printBox = new PrintBox(60, "Transformer: " + "DataEndpoint");
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
    }

    /**
     * The implementation of this method may transform the supplied class file and return a new
     * replacement class file. Addition: Adds the SpringEndpointClassAdapter for modifying the
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

        if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)) {
            return cbuf;
        }

        printBox.addMsg("Class: " + cname);
        printBox.setContentPresent(true);

        try {
            ClassReader cr = new ClassReader(cbuf);
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            cr.accept(cn, ClassReader.EXPAND_FRAMES);

            ClassWriter cw = new SafeClassWriter(cr, loader,
                            ClassWriter.COMPUTE_FRAMES); // ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new SpringRepositoryClassAdapter(cw);

            cr.accept(cv, ClassReader.EXPAND_FRAMES);

            if (Config.instance().isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                SWATAssert.enforce(stringWriter.toString().isEmpty(), "Instrumentation error: " + stringWriter);
            }

            Transformer.addInstrumentedClass(cname, InternalTransformerType.SPRING_DATA);
            if (printBox.isContentPresent()) {
                logger.debug(printBox.toString());
            }
            return cw.toByteArray();
        } catch (Throwable t) {

            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error while instrumenting class: " + cname, t);
        }

        Transformer.addInstrumentedClass(cname, InternalTransformerType.SPRING_DATA);
        if (printBox.isContentPresent()) {
            logger.debug(printBox.toString());
        }

        return cbuf;
    }
}
