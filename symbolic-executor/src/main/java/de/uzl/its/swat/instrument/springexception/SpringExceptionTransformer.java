package de.uzl.its.swat.instrument.springexception;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.Transformer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
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
public class SpringExceptionTransformer implements ClassFileTransformer {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();


    private static final ThreadLocal<PrintBox> printBox = ThreadLocal.withInitial(() -> new PrintBox(60, "Transformer" + SpringExceptionTransformer.class.getSimpleName()));

    public static PrintBox getPrintBox() {
        return printBox.get();
    }

    private HashMap<String, HashSet<String>> interfaces;

    public SpringExceptionTransformer() {
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
        interfaces = new HashMap<>();
    }

    private boolean shouldInstrument(String cname) {
        return (Util.shouldInstrument(cname)
                || cname.equals(
                        "org/springframework/web/servlet/mvc/method/annotation/ResponseEntityExceptionHandler")
                || cname.equals(
                        "org/springframework/boot/autoconfigure/web/servlet/error/BasicErrorController")
                || cname.equals(
                        "org/springframework/boot/autoconfigure/web/servlet/error/AbstractErrorController")
                || cname.equals("org/springframework/boot/web/servlet/support/ErrorPageFilter"));
        // || cname.startsWith("org/springframework/security/web/authentication"));
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

        // System.out.println("!!!!!!!!!!!!! " + cname);

        if (classBeingRedefined != null || cname == null || !shouldInstrument(cname)) { // TODO only temporary
            return cbuf;
        }
        getPrintBox().addMsg("Class: " + cname);

        try {
            ClassReader cr = new ClassReader(cbuf);
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            cr.accept(cn, 0);
            // List<AnnotationNode> visibleAnnotations = cn.visibleAnnotations;
            // boolean restController = false;
            // if (visibleAnnotations != null) {
            //     for (AnnotationNode an : visibleAnnotations) {
            //         restController = restController || an.desc.contains("Controller");
            //     }
            // }
            // if (!restController) {
            //     return cbuf;
            // }
            ClassWriter cw = new ClassWriter(cr, 0); // For some reason changing this to 0 from
            // ClassWriter.COMPUTE_FRAMES helps??
            ClassVisitor cv = new SpringExceptionClassAdapter(cw, cname, interfaces);
            cr.accept(cv, 0);

            if (Config.instance().isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                SWATAssert.enforce(stringWriter.toString().isEmpty(), "Instrumentation error: {}", stringWriter);
            }

            Transformer.addInstrumentedClass(cname, InternalTransformerType.SPRING_EXCEPTION);
            if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());
            return cw.toByteArray();

        } catch (Exception e) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error while instrumenting class: " + cname, e);
        }
        Transformer.addInstrumentedClass(cname, InternalTransformerType.SPRING_EXCEPTION);
        if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());

        return cbuf;
    }
}
