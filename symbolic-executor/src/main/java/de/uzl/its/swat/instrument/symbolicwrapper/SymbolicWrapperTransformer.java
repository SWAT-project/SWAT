package de.uzl.its.swat.instrument.symbolicwrapper;

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
import de.uzl.its.swat.instrument.springendpoint.SpringEndpointTransformer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class SymbolicWrapperTransformer implements ClassFileTransformer {

    Config config = Config.instance();


    private static final ThreadLocal<PrintBox> printBox = ThreadLocal.withInitial(() -> new PrintBox(60, "Transformer" + SymbolicWrapperTransformer.class.getSimpleName()));

    public static PrintBox getPrintBox() {
        return printBox.get();
    }

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    public SymbolicWrapperTransformer() {
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
        try {
        if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)) {
            return cbuf;
        }

        switch (config.getInstrumentationTransformer()) {
            case SPRING_ENDPOINT -> {
                if (!SpringEndpointTransformer.getInstrumentedClasses().contains(cname)) {
                    return cbuf;
                }
            }
            case WEB_SERVLET -> new ErrorHandler()
                    .handleException(
                            new RuntimeException(
                                    "Servlet Endpoint Instrumentation is not supported for symbolic"
                                            + " execution"));
            case SV_COMP, NONE -> {}
            case ANNOTATION -> {
                if (!cname.equals(config.getInstrumentationAnnotationSymbolicClassName()) &&
                        config.getInstrumentationAnnotationSymbolicClassName() != null)
                    return cbuf;
            }
            case PARAMETER -> {
                if (!Util.isSymbolicClass(cname))
                    return cbuf;
            }
        }

        getPrintBox().addMsg("Class: " + cname);
            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw = new SafeClassWriter(cr, loader, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new SymbolicWrapperClassAdapter(cw, cname);
            cr.accept(cv, 0);

            if (Config.instance().isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                SWATAssert.enforce(stringWriter.toString().isEmpty(), "Instrumentation error: {}", stringWriter);
            }

            Transformer.addInstrumentedClass(cname, InternalTransformerType.SYMBOLIC_WRAPPER);
            if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());
            return cw.toByteArray();

        } catch (Throwable t) {
            new ErrorHandler().handleException("Error while instrumenting class: " + cname, t);
        }

        return cbuf;
    }
}
