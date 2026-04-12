package de.uzl.its.swat.instrument.annotation;

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
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * A {@code ClassFileTransformer} that instruments classes to support symbolic execution
 * based on the {@code @Symbolic} annotation.
 * <p>
 * This transformer uses the {@link AnnotationClassAdapter} to modify classes whose internal
 * name matches the instrumentation criteria (as determined by {@link Util#shouldInstrument(String)}).
 * Instrumented classes are logged and added to the list of instrumented classes.
 * </p>
 */
public class AnnotationTransformer implements ClassFileTransformer {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    private final Config config = Config.instance();


     private static final ThreadLocal<PrintBox> printBox = ThreadLocal.withInitial(() -> new PrintBox(60, "Transformer" + AnnotationTransformer.class.getSimpleName()));

     public static PrintBox getPrintBox() {
     return printBox.get();
     }

    /**
     * Constructs a new {@code AnnotationTransformer} and initializes logging.
     */
    public AnnotationTransformer() {
        Transformer.getPrintBox().addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
    }

    /**
     * Transforms the supplied class file if it matches the target for annotation instrumentation.
     * <p>
     * The transformer uses {@link AnnotationClassAdapter} to modify the class bytecode. It also
     * verifies the correctness of the modified class if configured to do so.
     * </p>
     *
     * @param loader               the defining loader of the class to be transformed, may be null
     * @param cname                the internal name of the class
     * @param classBeingRedefined  if this is triggered by a redefine or retransform, the class being redefined; null otherwise
     * @param domain               the protection domain of the class
     * @param cbuf                 the input byte buffer in class file format
     * @return a well-formed class file buffer (the result of the transform), or the original buffer if no transformation is performed
     */
    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> classBeingRedefined,
                            ProtectionDomain domain, byte[] cbuf) {

        try {
        // Skip redefinitions and classes that should not be instrumented.
        if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)) {
            return cbuf;
        }
        getPrintBox().addMsg("Class: " + cname);
            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw = new SafeClassWriter(cr, loader, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new AnnotationClassAdapter(cw);
            cr.accept(cv, 0);

            if (config.isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                SWATAssert.enforce(stringWriter.toString().isEmpty(),
                        "Instrumentation error: {}", stringWriter);
            }
            Transformer.addInstrumentedClass(cname, InternalTransformerType.ANNOTATION);
            if (getPrintBox().isContentPresent()) {
                logger.debug(getPrintBox().toString());
            }
            return cw.toByteArray();
        } catch (Exception e) {
            // The ErrorHandler has to be here as this method transfers control to ASM
            new ErrorHandler().handleException("Error while instrumenting class: " + cname, e);
        }
        // Return the original bytecode (this transformer does not modify the class).
        return cbuf;
    }
}
