package de.uzl.its.swat.instrument.assertion;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.instrument.Transformer;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Reports every {@code AssertionError} where it is thrown ({@code Witness.recordThrow}), as the
 * SV-COMP transformer does: an assertion fails where it is thrown, even if the target catches the
 * error afterwards (spatial4j's endpoint catches AssertionError, for one). For targets that are
 * not run as SV-COMP tasks, e.g. Spring endpoints, where the explorer reads the violation from the
 * trace's witness data.
 */
public class AssertionThrowTransformer implements ClassFileTransformer {

    public AssertionThrowTransformer() {
        Transformer.getPrintBox()
                .addMsg("Initializing Transformer: " + this.getClass().getSimpleName());
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String cname,
            Class<?> classBeingRedefined,
            ProtectionDomain d,
            byte[] cbuf) {
        if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)) return cbuf;
        try {
            ClassReader cr = new ClassReader(cbuf);
            // The hook keeps the stack the same at every frame, so only the max stack changes.
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cr.accept(
                    new ClassVisitor(Opcodes.ASM9, cw) {
                        @Override
                        public MethodVisitor visitMethod(
                                int access, String name, String desc, String signature, String[] exceptions) {
                            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                            return mv == null || Util.ignoreMethod(name) ? mv : new MethodVisitor(Opcodes.ASM9, mv) {
                                @Override
                                public void visitInsn(int opcode) {
                                    if (opcode == Opcodes.ATHROW) {
                                        super.visitInsn(Opcodes.DUP);
                                        super.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "de/uzl/its/swat/witness/Witness",
                                                "recordThrow",
                                                "(Ljava/lang/Throwable;)V",
                                                false);
                                    }
                                    super.visitInsn(opcode);
                                }
                            };
                        }
                    },
                    0);
            return cw.toByteArray();
        } catch (Exception e) {
            new ErrorHandler().handleException("Error while instrumenting class: " + cname, e);
        }
        return cbuf;
    }
}
