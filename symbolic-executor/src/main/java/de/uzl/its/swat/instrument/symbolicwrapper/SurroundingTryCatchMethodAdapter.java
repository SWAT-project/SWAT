package de.uzl.its.swat.instrument.symbolicwrapper;

import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import de.uzl.its.swat.instrument.Intrinsics;
import de.uzl.its.swat.instrument.TryCatchBlock;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A visitor to visit a Java method. This class surrounds the whole function with a try catch blog
 * to add a solve in case of an error.
 */
public class SurroundingTryCatchMethodAdapter extends AbstractMethodAdapter {

    private final TryCatchBlock surroundingTryCatch;
    private boolean addEndOfTryCatch = false;

    private final boolean shouldInstrument;

    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     */
    public SurroundingTryCatchMethodAdapter(
            MethodVisitor mv, String cname, String name, String desc) {
        super(mv, name, desc);

        // Label used for try catch
        this.surroundingTryCatch =
                new TryCatchBlock(new Label(), new Label(), new Label(), new Label(), null);
        this.shouldInstrument = shouldInstrument(cname, name);
    }

    @Override
    public void visitCode() {
        if (shouldInstrument) {
            addBeginOfSurrounding();
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

        if (shouldInstrument && addEndOfTryCatch) {
            addEndOfSurrounding();
        }

        // visit the corresponding instructions
        super.visitMaxs(maxStack, maxLocals);
    }

    private void addBeginOfSurrounding() {
        surroundingTryCatch.visit(mv);
        mv.visitLabel(surroundingTryCatch.start());
        addEndOfTryCatch = true;
    }

    private void addEndOfSurrounding() {

        mv.visitLabel(surroundingTryCatch.end());
        mv.visitJumpInsn(Opcodes.GOTO, surroundingTryCatch.jumpTarget());
        mv.visitLabel(surroundingTryCatch.handler());

        visitInsn(Opcodes.DUP);

        visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);

        // Duplicate exception for instanceof check
        visitInsn(Opcodes.DUP);

        // Check if exception instanceof RuntimeException
        visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/RuntimeException");

        // Create labels for conditional branching
        Label isRuntimeException = new Label();
        Label continueExecution = new Label();

        // If instanceof returns true (non-zero), jump to isRuntimeException label
        visitJumpInsn(Opcodes.IFNE, isRuntimeException);

        // Not a RuntimeException - print checked exception message
        visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;");
        visitLdcInsn("[SWAT] Uncaught top-level checked exception in symbolic execution");
        visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
        visitJumpInsn(Opcodes.GOTO, continueExecution);

        // Is a RuntimeException - print runtime exception message
        visitLabel(isRuntimeException);
        visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;");
        visitLdcInsn("[SWAT] Uncaught top-level RuntimeException in symbolic execution");
        visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);

        visitLabel(continueExecution);
        visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Intrinsics.class), "terminate", "()V", false);
        visitInsn(Opcodes.ATHROW);
        mv.visitLabel(surroundingTryCatch.jumpTarget());
    }
}
