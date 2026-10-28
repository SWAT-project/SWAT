package de.uzl.its.swat.instrument.symbolicwrapper;

import de.uzl.its.swat.instrument.Intrinsics;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Starts the symbolic execution at the beginning of the static initializer of a class that
 * declares the entry method. The JVM initialises that class before it calls the entry method, so
 * without this, inputs read by the initializer would run without a thread context.
 */
public class StaticInitializerEntryMethodAdapter extends MethodVisitor {

    private final String endpointID;

    /**
     * @param mv Parent MethodVisitor
     * @param endpointID The name of the endpoint whose class is initialised, as passed to
     *     Intrinsics.init by the endpoint itself
     */
    public StaticInitializerEntryMethodAdapter(MethodVisitor mv, String endpointID) {
        super(Opcodes.ASM9, mv);
        this.endpointID = endpointID;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        SymbolicWrapperTransformer.getPrintBox()
                .addMsg("    => Adding initializer: Intrinsics.initBeforeEntry(endpointID)");
        SymbolicWrapperTransformer.getPrintBox().setContentPresent(true);
        visitLdcInsn(endpointID);
        visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(Intrinsics.class),
                "initBeforeEntry",
                "(Ljava/lang/String;)V",
                false);
    }
}
