package de.uzl.its.swat.instrument.symbolicwrapper;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.SafeClassWriter;
import de.uzl.its.swat.instrument.Transformer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.LoggerFactory;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class SymbolicWrapperTransformer implements ClassFileTransformer {

    Config config = Config.instance();

    @Getter private static PrintBox printBox;
    private static final org.slf4j.Logger logger =
            LoggerFactory.getLogger(SymbolicWrapperTransformer.class);

    public SymbolicWrapperTransformer() {
        printBox = new PrintBox(60, "Transformer: " + this.getClass().getSimpleName());
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
    }
    /**
     * The implementation of this method may transform the supplied class file and return a new
     * replacement class file. Addition: Adds the SymbolicWrapperClassAdapter for modifying the
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
            byte[] cbuf)
            throws IllegalClassFormatException {
        logger.error("HERE");
        if (classBeingRedefined != null || !Transformer.shouldInstrument(cname)) {
            return cbuf;
        }
        logger.error("HERE1");

        switch (config.getTransformerType()) {
            case SPRING_ENDPOINT,
                    WEB_SERVLET,
                    SV_COMP -> {} // ToDo: will that do nothing or execute the next case?

            default -> {
                if (!cname.equals(config.getSymbolicStartPath())) {
                    logger.error("HERE2");
                    return cbuf;
                }
            }
        }
        logger.error("HERE3");

        printBox.addMsg("Class: " + cname);
        try {
            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw =
                    new SafeClassWriter(
                            cr, loader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new SymbolicWrapperClassAdapter(cw, cname);
            cr.accept(cv, 0);
            Transformer.addInstrumentedClass(cname, InternalTransformerType.SYMBOLIC_WRAPPER);
            if (printBox.isContentPresent()) logger.info(printBox.toString());
            return cw.toByteArray();

        } catch (Exception e) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error while instrumenting class: " + cname, e);
        }
        Transformer.addInstrumentedClass(cname, InternalTransformerType.SYMBOLIC_WRAPPER);
        if (printBox.isContentPresent()) logger.info(printBox.toString());

        return cbuf;
    }
}
