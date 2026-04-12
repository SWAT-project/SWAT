package de.uzl.its.swat.instrument.refequality;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

class RefEqualityMethodAdapter extends LocalVariablesSorter {

    public RefEqualityMethodAdapter(int api, int access, String descriptor, MethodVisitor mv) {
        super(api, access, descriptor, mv);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        // Intercept IF_ACMPEQ and IF_ACMPNE (reference equality comparisons)
        if (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE) {
            // Stack at this point: [..., ref1, ref2]
            // We can call Objects.equals directly without storing

            // Call Objects.equals(Object, Object) which consumes both refs from stack
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "de/uzl/its/swat/common/UtilInstrumented",
                    "refEquals",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                    false
            );

            // Now stack has: [..., boolean result (0 or 1)]
            // Convert the comparison based on original opcode
            if (opcode == Opcodes.IF_ACMPEQ) {
                // Original: if (ref1 == ref2) goto label
                // New: if (Objects.equals(ref1, ref2) == true) goto label
                mv.visitJumpInsn(Opcodes.IFNE, label);  // If not equal to 0 (i.e., true), jump
            } else { // IF_ACMPNE
                // Original: if (ref1 != ref2) goto label
                // New: if (Objects.equals(ref1, ref2) == false) goto label
                mv.visitJumpInsn(Opcodes.IFEQ, label);  // If equal to 0 (i.e., false), jump
            }

            RefEqualityTransformer.getPrintBox()
                    .addMsg("Replacing " + (opcode == Opcodes.IF_ACMPEQ ? "IF_ACMPEQ" : "IF_ACMPNE") +
                            " with UtilInstrumented.refEquals");
        } else {
            // For all other jump instructions, proceed normally
            mv.visitJumpInsn(opcode, label);
        }
    }
}