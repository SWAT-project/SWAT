package de.uzl.its.swat.instrument.springendpoint;

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

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * An agent provides an implementation of this interface in order to transform class files. The
 * transformation occurs before the class is defined by the JVM.
 */
public class SpringEndpointTransformer implements ClassFileTransformer {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();


    private static final ThreadLocal<PrintBox> printBox = ThreadLocal.withInitial(() -> new PrintBox(60, "Transformer" + SpringEndpointTransformer.class.getSimpleName()));

    public static PrintBox getPrintBox() {
        return printBox.get();
    }

    @Getter private static HashSet<String> instrumentedClasses = new HashSet<>();
    @Getter private static HashSet<String> instrumentedEndpoints = new HashSet<>();

    public SpringEndpointTransformer() {
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
    }

    static boolean isRestEndpointAnnotation(String desc) {
        return desc.equals("Lorg/springframework/web/bind/annotation/RequestMapping;")
                || desc.equals("Lorg/springframework/web/bind/annotation/PostMapping;")
                || desc.equals("Lorg/springframework/web/bind/annotation/DeleteMapping;")
                || desc.equals("Lorg/springframework/web/bind/annotation/GetMapping;")
                || desc.equals("Lorg/springframework/web/bind/annotation/PutMapping;");
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
        getPrintBox().addMsg("Class: " + cname);

        try {
            // parse the class
            ClassReader cr = new ClassReader(cbuf);
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            cr.accept(cn, 0);

            // only controllers
            boolean restController = false;
            if (cn.visibleAnnotations != null) {
                for (AnnotationNode an : cn.visibleAnnotations) {
                    if (an.desc.contains("Controller")) {
                        restController = true;
                        break;
                    }
                }
            }
            if (!restController) {
                return cbuf;
            }


            // 1) scan interfaces for mapping annotations
            Set<String> interfaceEndpoints = new HashSet<>();
            for (String iface : cn.interfaces) {
                // iface is in internal form, e.g. "org/springframework/samples/…/OwnersApi"
                try (InputStream in = loader.getResourceAsStream(iface + ".class")) {
                    if (in == null) continue;
                    ClassReader icr = new ClassReader(in);
                    ClassNode icn = new ClassNode(Opcodes.ASM9);
                    // skip code, we only need annotations
                    icr.accept(icn, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                    for (MethodNode mn : icn.methods) {
                        if (mn.visibleAnnotations != null) {
                            for (AnnotationNode an : mn.visibleAnnotations) {
                                if (isRestEndpointAnnotation(an.desc)) {
                                    interfaceEndpoints.add(mn.name + mn.desc);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // If the interface class cannot be found, we ignore it/ throw an assertion error.
                    // This can happen if the interface is not in the classpath or is not a valid class file.
                    SWATAssert.check(false, "Could not read interface {}: {}", iface, ignored.getMessage());
                }
            }


            ClassWriter cw = new SafeClassWriter(cr, loader, ClassWriter.COMPUTE_FRAMES);

            ClassVisitor cv = new SpringEndpointClassAdapter(cw, cname, interfaceEndpoints);
            cr.accept(cv, 0);

            if (Config.instance().isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                SWATAssert.enforce(stringWriter.toString().isEmpty(), "Instrumentation error: {}", stringWriter);
            }

            Transformer.addInstrumentedClass(cname, InternalTransformerType.SPRING_ENDPOINT);
            if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());
            return cw.toByteArray();

        } catch (Exception e) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error while instrumenting class: " + cname, e);
        }
        Transformer.addInstrumentedClass(cname, InternalTransformerType.SPRING_ENDPOINT);
        if (getPrintBox().isContentPresent()) logger.debug(getPrintBox().toString());

        return cbuf;
    }

    public static void addInstrumentedEndpoint(String name) {
        instrumentedEndpoints.add(name);
    }

    public static void addInstrumentedClass(String cname) {
        instrumentedClasses.add(cname);
    }
}
