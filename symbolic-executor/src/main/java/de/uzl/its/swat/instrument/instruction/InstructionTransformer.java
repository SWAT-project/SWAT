package de.uzl.its.swat.instrument.instruction;

import de.uzl.its.swat.Main;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.SafeClassWriter;
import de.uzl.its.swat.instrument.Transformer;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class InstructionTransformer implements ClassFileTransformer {
    private final String instDir;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(InstructionTransformer.class);


    @Getter
    private static PrintBox printBox;
    Config config = Config.instance();

    public InstructionTransformer() {
        printBox = new PrintBox(60);
        Transformer.getPrintBox().addToBox("Initializing Transformer: " + this.getClass().getSimpleName(), false);
        instDir = config.getInstDir();
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

        if (classBeingRedefined != null || !Transformer.shouldInstrument(cname)) return cbuf;
        logger.info("Instrumenting: " + cname);
        printBox.startBox("Transformer: " + "Instruction");
        printBox.addToBox("Class: " + cname, false);
        try {
            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw =
                    new SafeClassWriter(
                            cr, loader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new InstructionClassAdapter(cw, cname);
            try {
                cr.accept(cv, ClassReader.SKIP_FRAMES);
            } catch (Exception e) {
                logger.info("Error: " + e);
                new ErrorHandler()
                        .handleException("[INSTRUCTION TRANSFORMER] Error while instrumenting", e);
            }

            Transformer.addInstrumentedClass(cname, InternalTransformerType.INSTRUCTION);
            logger.info(printBox.endBox());
            return cw.toByteArray();

        } catch (Exception e) {
            new ErrorHandler()
                    .handleException("[INSTRUCTION TRANSFORMER] Error while instrumenting", e);
        }
        Transformer.addInstrumentedClass(cname, InternalTransformerType.INSTRUCTION);
        logger.info(printBox.endBox());
        return cbuf;
    }
}
