package de.uzl.its.swat.instrument;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.config.Config;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassSavingTransformer implements ClassFileTransformer {
    private static final Logger logger = LoggerFactory.getLogger(ClassSavingTransformer.class);

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

        if (!Transformer.isInstrumented(cname)) {
            return cbuf;
        }

        try {
            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw =
                    new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {};
            cr.accept(cv, 0);

            byte[] transformedClass = cw.toByteArray();
            logger.info(
                    new PrintBox(
                                    60,
                                    "Transformer: Saving",
                                    new ArrayList<>(List.of(new String[] {"Class: " + cname})))
                            .toString());
            Transformer.addInstrumentedClass(cname, InternalTransformerType.SAVING);
            saveClass(transformedClass, cname);

            return transformedClass;
        } catch (Exception e) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.handleException("Error while instrumenting class: " + cname, e);
        }
        Transformer.addInstrumentedClass(cname, InternalTransformerType.SAVING);
        return cbuf;
    }

    public void saveClass(byte[] transformedClass, String cname) throws Exception {
        String fullPath = DIR + cname.replace('.', '/') + ".class";

        try {
            File file = new File(fullPath);
            File parent = new File(file.getParent());
            if (!parent.exists()) {
                parent.mkdirs();
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(transformedClass);
            }
        } catch (Exception e) {
            new ErrorHandler().handleException(e);
        }
    }
}
