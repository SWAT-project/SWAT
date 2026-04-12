package de.uzl.its.swat.instrument;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

public class ClassSavingTransformer implements ClassFileTransformer {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    @Getter private static PrintBox printBox;

    // The relative path where instrumented classes are written to
    private final String DIR;
    Config config = Config.instance();

    public ClassSavingTransformer() {
        DIR = config.getLoggingDirectory() + "/" + "instrumented" + "/";
        printBox = new PrintBox(60);
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
        Transformer.getPrintBox().addMsg("    => Saving to: {cwd}/" + DIR);
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String cname,
            Class<?> classBeingRedefined,
            ProtectionDomain d,
            byte[] cbuf) {

        ClassWriter cw = null;
        try {
            // ToDo (Flo): checking for classBeingRedefined != null probably does not make sense
            if (!Transformer.isInstrumented(cname)) {
                return cbuf;
            }

            ClassReader cr = new ClassReader(cbuf);
            cw =
                    new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {};
            cr.accept(cv, 0);

            if (Config.instance().isUseCheckClassAdapter() && Util.useCheckClassAdapterForClass(cname)) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, printWriter);
                SWATAssert.enforce(stringWriter.toString().isEmpty(), "Instrumentation error: {}", stringWriter);
            }

            byte[] transformedClass = cw.toByteArray();
            logger.debug(
                    new PrintBox(
                                    60,
                                    "Transformer: Saving",
                                    new ArrayList<>(List.of(new String[] {"Class: " + cname})))
                            .toString());

            Transformer.addInstrumentedClass(cname, InternalTransformerType.SAVING);
            saveClass(transformedClass, cname);

            return transformedClass;
        } catch (Exception e) {
            try {
                if (cw != null) {
                    byte[] transformedClass = cw.toByteArray();
                    saveClass(transformedClass, cname);
                }
            } catch (Throwable t) {
                logger.warn("Error while trying to write incomplete class file in exception block.");
            }

            // The ErrorHandler has to be here as this method transfers control to ASM
            new ErrorHandler().handleException("Error while instrumenting class: " + cname, e);
        }
        return cbuf;
    }

    public void saveClass(byte[] transformedClass, String cname) throws IOException {
        String fullPath = DIR + cname.replace('.', '/') + ".class";

        File file = new File(fullPath);
        File parent = new File(file.getParent());
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(transformedClass);
        }
    }
}
