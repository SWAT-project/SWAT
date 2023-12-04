package de.uzl.its.swat.instrument.parameter;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.SafeClassWriter;
import de.uzl.its.swat.instrument.Transformer;
import de.uzl.its.swat.logger.SystemLogger;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class ParameterTransformer implements ClassFileTransformer {
    Logger logger;
    SystemLogger systemLogger;
    Config config = Config.instance();

    public ParameterTransformer() {
        systemLogger = new SystemLogger();
        logger = systemLogger.getLogger();
        systemLogger.addToBox("Initializing Transformer: " + this.getClass().getSimpleName());
    }
    /**
     * The implementation of this method may transform the supplied class file and return a new
     * replacement class file. Addition: Adds the ParameterClassAdapter for modifying the classFile
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
            byte[] cbuf)
            throws IllegalClassFormatException {

        if (classBeingRedefined != null || !cname.equals(config.getMakeSymbolicClassPath())) {
            return cbuf;
        }
        systemLogger.startBox(60, "Transformer: " + "Parameter");
        systemLogger.addToBox("Class: " + cname, false);
        try {

            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw =
                    new SafeClassWriter(
                            cr, loader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new ParameterClassAdapter(cw, cname);

            cr.accept(cv, 0);
            Transformer.addInstrumentedClass(cname, InternalTransformerType.PARAMETER);
            systemLogger.endBox();
            return cw.toByteArray();

        } catch (Exception e) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error while instrumenting class: " + cname, e);
        }
        Transformer.addInstrumentedClass(cname, InternalTransformerType.PARAMETER);
        systemLogger.endBox();
        return cbuf;
    }
}
