package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.SystemLogger;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.Transformer;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class SVCompTransformer implements ClassFileTransformer {

    private final Logger logger;
    private final SystemLogger systemLogger;

    public SVCompTransformer() {
        systemLogger = new SystemLogger();
        logger = systemLogger.getLogger();
        systemLogger.addToBox("Initializing Transformer: " + this.getClass().getSimpleName());
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

        if (classBeingRedefined != null || !Transformer.shouldInstrument(cname)) return cbuf;
        systemLogger.startBox(60, "Transformer: " + "SV-Comp");
        systemLogger.addToBox("Class: " + cname, false);
        try {
            ClassReader cr = new ClassReader(cbuf);
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            cr.accept(cn, ClassReader.EXPAND_FRAMES);

            ClassWriter cw = new ClassWriter(cr, ClassReader.EXPAND_FRAMES);
            ClassVisitor cv = new SVCompClassAdapter(cname, cw);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);

            logger.info(systemLogger.endBox());
            return cw.toByteArray();

        } catch (Exception e) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error while instrumenting class: " + cname, e);
        }
        Transformer.addInstrumentedClass(cname, InternalTransformerType.SV_COMP);
        logger.info(systemLogger.endBox());

        return cbuf;
    }
}
