package de.uzl.its.swat.instrument.nocache;

import de.uzl.its.swat.config.Config;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;


class NoCacheMethodAdapter extends LocalVariablesSorter {
    public NoCacheMethodAdapter(int api, int access, String descriptor, MethodVisitor mv) {
        super(api, access, descriptor, mv);
    }

    // Replace LDC instructions that load a String constant with code that creates a new String.
    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof String && Config.instance().isUseStringInterning()) {
            // Generate: new java/lang/String; DUP; LDC "literal"; INVOKESPECIAL <init>(Ljava/lang/String;)V
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>",
                    "(Ljava/lang/String;)V", false);
            //NoCacheTransformer.getPrintBox()
            //        .addMsg("Replacing LDC with new String");
        } else {
            mv.visitLdcInsn(value);
        }
    }

    // Intercept method calls to disable interning and caching.
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // Skip String.intern() calls.
        if (opcode == Opcodes.INVOKEVIRTUAL &&
                owner.equals("java/lang/String") &&
                name.equals("intern") &&
                descriptor.equals("()Ljava/lang/String;") &&
                Config.instance().isUseStringInterning()) {
            NoCacheTransformer.getPrintBox()
                    .addMsg("Removing String.intern() call");
            return;
        }
        // Replace Integer.valueOf(int) with new Integer(int)
        if (opcode == Opcodes.INVOKESTATIC &&
                owner.equals("java/lang/Integer") &&
                name.equals("valueOf") &&
                descriptor.equals("(I)Ljava/lang/Integer;")) {
            int localVarIndex = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(Opcodes.ISTORE, localVarIndex);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/Integer");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ILOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
            NoCacheTransformer.getPrintBox()
                    .addMsg("Replacing Integer.valueOf with new Integer");
            return;
        }
        // Replace Long.valueOf(long) with new Long(long)
        if (opcode == Opcodes.INVOKESTATIC &&
                owner.equals("java/lang/Long") &&
                name.equals("valueOf") &&
                descriptor.equals("(J)Ljava/lang/Long;")) {
            int localVarIndex = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(Opcodes.LSTORE, localVarIndex);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.LLOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V", false);
            NoCacheTransformer.getPrintBox()
                    .addMsg("Replacing Long.valueOf with new Long");
            return;
        }
        // Replace Short.valueOf(short) with new Short(short)
        if (opcode == Opcodes.INVOKESTATIC &&
                owner.equals("java/lang/Short") &&
                name.equals("valueOf") &&
                descriptor.equals("(S)Ljava/lang/Short;")) {
            int localVarIndex = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(Opcodes.ISTORE, localVarIndex);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/Short");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ILOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V", false);
            NoCacheTransformer.getPrintBox()
                    .addMsg("Replacing Short.valueOf with new Short");
            return;
        }
        // Replace Byte.valueOf(byte) with new Byte(byte)
        if (opcode == Opcodes.INVOKESTATIC &&
                owner.equals("java/lang/Byte") &&
                name.equals("valueOf") &&
                descriptor.equals("(B)Ljava/lang/Byte;")) {
            int localVarIndex = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(Opcodes.ISTORE, localVarIndex);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/Byte");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ILOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V", false);
            NoCacheTransformer.getPrintBox()
                    .addMsg("Replacing Byte.valueOf with new Byte");
            return;
        }
        // Replace Character.valueOf(char) with new Character(char)
        if (opcode == Opcodes.INVOKESTATIC &&
                owner.equals("java/lang/Character") &&
                name.equals("valueOf") &&
                descriptor.equals("(C)Ljava/lang/Character;")) {
            int localVarIndex = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(Opcodes.ISTORE, localVarIndex);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/Character");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ILOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V", false);
            NoCacheTransformer.getPrintBox()
                    .addMsg("Replacing Character.valueOf with new Character");
            return;
        }
        // Replace Boolean.valueOf(boolean) with new Boolean(boolean)
        if (opcode == Opcodes.INVOKESTATIC &&
                owner.equals("java/lang/Boolean") &&
                name.equals("valueOf") &&
                descriptor.equals("(Z)Ljava/lang/Boolean;")) {
            int localVarIndex = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(Opcodes.ISTORE, localVarIndex);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/Boolean");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ILOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V", false);
            NoCacheTransformer.getPrintBox()
                    .addMsg("Replacing Boolean.valueOf with new Boolean");
            return;
        }
        // For all other method calls, proceed normally.
        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    /*
    // Instrument object reference comparisons (IF_ACMPEQ and IF_ACMPNE) to print a warning.
    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE) {
            // Dynamically allocate two local variables for the two object references.
            int refLocal1 = newLocal(Type.getType("Ljava/lang/Object;"));
            int refLocal2 = newLocal(Type.getType("Ljava/lang/Object;"));
            // Store the two references.
            mv.visitVarInsn(Opcodes.ASTORE, refLocal2);
            mv.visitVarInsn(Opcodes.ASTORE, refLocal1);
            // Load them back to pass to our helper.
            mv.visitVarInsn(Opcodes.ALOAD, refLocal1);
            mv.visitVarInsn(Opcodes.ALOAD, refLocal2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/example/InstrumentationHelper", "checkEquality",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
            if (opcode == Opcodes.IF_ACMPEQ) {
                mv.visitJumpInsn(Opcodes.IFNE, label);
            } else {
                mv.visitJumpInsn(Opcodes.IFEQ, label);
            }
        } else {
            mv.visitJumpInsn(opcode, label);
        }
    }
    */
}
