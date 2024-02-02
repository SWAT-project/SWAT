package de.uzl.its.swat.instrument.symbolicwrapper;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A visitor to visit a Java method This class surrounds the whole function with a try catch blog to
 * ad a solve in case of an error.
 */
public class SurroundingTryCatchMethodAdapter extends AbstractMethodAdapter {

    private final Label[] surroundingTryCatch;
    private boolean addEndOfTryCatch = false;
    Config config = Config.instance();

    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     */
    public SurroundingTryCatchMethodAdapter(MethodVisitor mv, String name, String desc) {
        super(mv, name, desc);

        // Label used for try catch
        this.surroundingTryCatch = new Label[] {new Label(), new Label(), new Label()};
    }

    /**
     * Add a solve in case an exception happened inside the method
     *
     * @param maxStack
     * @param maxLocals
     */
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

        if (!addEndOfTryCatch) {
            super.visitMaxs(maxStack, maxLocals);
            return;
        }

        // Add end of try catch
        addEndOfSurrounding();

        visitTryCatchBlock(
                surroundingTryCatch[0],
                surroundingTryCatch[1],
                surroundingTryCatch[2],
                "java/lang/Throwable");
        super.visitMaxs(
                maxStack + 10, maxLocals + 10); // 10 might not be an adequate number but it works
    }

    @Override
    public void visitCode() {
        addBeginOfSurrounding();
    }

    private void addBeginOfSurrounding() {
        visitTryCatchBlock(
                surroundingTryCatch[0],
                surroundingTryCatch[1],
                surroundingTryCatch[2],
                "java/lang/Throwable");
        visitLabel(surroundingTryCatch[0]);
        addEndOfTryCatch = true;
    }

    private void addEndOfSurrounding() {

        visitLabel(surroundingTryCatch[1]);
        visitLabel(surroundingTryCatch[2]);

        visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
        visitVarInsn(Opcodes.ASTORE, 2);

        visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "solve", "()V", false);

        visitVarInsn(Opcodes.ALOAD, 2);
        visitInsn(Opcodes.ATHROW);
    }
}
