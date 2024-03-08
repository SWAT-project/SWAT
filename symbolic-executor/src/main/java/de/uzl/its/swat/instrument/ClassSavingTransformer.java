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

import de.uzl.its.swat.instrument.parameter.ParameterTransformer;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassSavingTransformer implements ClassFileTransformer {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ClassSavingTransformer.class);

    @Getter
    private static PrintBox printBox;

    private final String instDir;
    Config config = Config.instance();

    public ClassSavingTransformer() {
        instDir = config.getInstDir();
        printBox = new PrintBox(60);
        Transformer.getPrintBox().addToBox("Initializing Transformer: " + this.getClass().getSimpleName());
        Transformer.getPrintBox().addToBox("    => Saving to: {cwd}/" + config.getLoggingPath() + instDir);
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
            logger.info(printBox.fullBox(
                    "Transformer: Saving",
                    new ArrayList<>(List.of(new String[] {"Class: " + cname}))));
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
        String fullPath =
                config.getLoggingPath() + "/" + instDir + "/" + cname.replace('.', '/') + ".class";

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
            throw e;
        }
    }
}
