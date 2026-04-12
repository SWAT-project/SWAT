package de.uzl.its.swat.instrument.classinfo;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.InstrumentationException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.Transformer;
import de.uzl.its.swat.instrument.common.FieldInfo;
import de.uzl.its.swat.metadata.ClassDepot;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.ClassReader;

public class ClassInfoTransformer implements ClassFileTransformer {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private static final ThreadLocal<PrintBox> printBox = ThreadLocal.withInitial(() -> new PrintBox(60, "Transformer" + ClassInfoTransformer.class.getSimpleName()));

    public static PrintBox getPrintBox() {
        return printBox.get();
    }
    @Override
    public byte[] transform(ClassLoader loader,
                            String cname,
                            Class<?> classBeingRedefined,
                            ProtectionDomain d,
                            byte[] cbuf) {
        try {
        // Only process new classes that should be instrumented.
        if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)) {
            return cbuf;
        }

        getPrintBox().addMsg("Processing class: " + cname);
            ClassReader cr = new ClassReader(cbuf);

            // --- Collect Parent Class Information ---
            List<String> parents = getParents(cname, cr, loader);
            // For each parent, also collect its fields and add to registrations.
            for (String parentName : parents) {
                    ClassReader parentCr = typeInfo(parentName, loader);
                    FieldCollectorVisitor parentFcv = new FieldCollectorVisitor();
                    parentCr.accept(parentFcv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }

            // --- Collect Interface Information ---
            List<String> interfaces = getImplementedInterfaces(cname, cr, loader);
            for (String iface : interfaces) {
                    ClassReader ifaceCr = typeInfo(iface, loader);
                    FieldCollectorVisitor ifaceFcv = new FieldCollectorVisitor();
                    ifaceCr.accept(ifaceFcv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }

            // --- Register Collected Information in ClassDepot ---
            // Register type metadata (parents and interfaces).
            ClassDepot.getInstrumentationInstance().registerTypeInfoForClass(cname, parents, interfaces);
            Transformer.addInstrumentedClass(cname, InternalTransformerType.TYPE_MAP);
            if (getPrintBox().isContentPresent()) {
                logger.debug(getPrintBox().toString());
            }
        } catch (Throwable t) {
            // The ErrorHandler has to be here as this method transfers control to ASM
            new ErrorHandler().handleException(new InstrumentationException(cname, t));
        }
        // Return the original bytecode (this transformer does not modify the class).
        return cbuf;
    }

    /**
     * Utility method to create a ClassReader for the given type.
     */
    private ClassReader typeInfo(final String type, ClassLoader loader) throws IOException {
        String resource = type + ".class";
        if (loader == null) {
            new ErrorHandler().handleException(new InstrumentationException("Cannot create ClassReader for type " + type + " because the class loader is null"));
        }
        InputStream is = loader.getResourceAsStream(resource);
        try (is) {
            if (is == null) {
                throw new IOException("Cannot create ClassReader for type " + type);
            }
            return new ClassReader(is);
        }
    }

    /**
     * Walks the superclass chain and returns the list of parent class names.
     */
    private List<String> getParents(String type, ClassReader cr, ClassLoader loader) throws IOException {
        List<String> parents = new ArrayList<>();
        String currentType = type;
        ClassReader currentCr = cr;
        while (!"java/lang/Object".equals(currentType)) {
            currentType = currentCr.getSuperName();
            parents.add(currentType);
            currentCr = typeInfo(currentType, loader);
        }
        return parents;
    }

    /**
     * Retrieves all interfaces implemented by the class (including inherited ones).
     */
    private List<String> getImplementedInterfaces(String type, ClassReader cr, ClassLoader loader) throws IOException {
        List<String> interfaces = new ArrayList<>();
        String currentType = type;
        ClassReader currentCr = cr;
        while (!"java/lang/Object".equals(currentType)) {
            String[] itfs = currentCr.getInterfaces();
            interfaces.addAll(Arrays.asList(itfs));
            currentType = currentCr.getSuperName();
            currentCr = typeInfo(currentType, loader);
        }
        // Also collect interfaces recursively.
        List<String> allInterfaces = new ArrayList<>(interfaces);
        for (String itf : interfaces) {
            allInterfaces.addAll(getInterfacesRecursively(itf, loader));
        }
        return allInterfaces;
    }

    private List<String> getInterfacesRecursively(String name, ClassLoader loader) throws IOException {
        List<String> result = new ArrayList<>();
            ClassReader cr = typeInfo(name, loader);
            String[] itfs = cr.getInterfaces();
            result.addAll(Arrays.asList(itfs));
            for (String itf : itfs) {
                result.addAll(getInterfacesRecursively(itf, loader));
            }
        return result;
    }
}
