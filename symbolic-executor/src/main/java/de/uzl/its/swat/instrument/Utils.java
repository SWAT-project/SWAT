package de.uzl.its.swat.instrument;

import de.uzl.its.swat.config.Config;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Utils implements Opcodes {
    private static final Config config = Config.instance();

    public static void addBipushInsn(MethodVisitor mv, int val) {
        switch (val) {
            case 0 -> mv.visitInsn(ICONST_0);
            case 1 -> mv.visitInsn(ICONST_1);
            case 2 -> mv.visitInsn(ICONST_2);
            case 3 -> mv.visitInsn(ICONST_3);
            case 4 -> mv.visitInsn(ICONST_4);
            case 5 -> mv.visitInsn(ICONST_5);
            default -> mv.visitLdcInsn(val);
        }
    }

    public static void addSpecialInsn(MethodVisitor mv, int val) {
        addBipushInsn(mv, val);
        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "SPECIAL", "(I)V", false);
    }

    /**
     * Add a set to code to read the given type from the top of the concrete stack and invoke
     * GETVALUE method of the analysis class.
     */
    public static void addValueReadInsn(MethodVisitor mv, String desc, String methodNamePrefix) {
        Type t;
        int identifier = 0;

        if (desc.contains(":")) {
            String[] s = desc.split(":");
            if (s.length != 3)
                throw new RuntimeException(
                        "Unexpected description (Expected \"owner:name:desc\"): " + desc);
            desc = s[2];
        }

        if (desc.startsWith("(")) {
            t = Type.getReturnType(desc);
        } else {
            t = Type.getType(desc);
        }
        switch (t.getSort()) {
            case Type.DOUBLE -> {
                mv.visitInsn(DUP2);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "double",
                        "(DI)V",
                        false);
            }
            case Type.LONG -> {
                mv.visitInsn(DUP2);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "long",
                        "(JI)V",
                        false);
            }
            case Type.ARRAY, Type.OBJECT -> {
                mv.visitInsn(DUP);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "Object",
                        "(Ljava/lang/Object;I)V",
                        false);
            }
            case Type.BOOLEAN -> {
                mv.visitInsn(DUP);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "boolean",
                        "(ZI)V",
                        false);
            }
            case Type.BYTE -> {
                mv.visitInsn(DUP);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "byte",
                        "(BI)V",
                        false);
            }
            case Type.CHAR -> {
                mv.visitInsn(DUP);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "char",
                        "(CI)V",
                        false);
            }
            case Type.FLOAT -> {
                mv.visitInsn(DUP);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "float",
                        "(FI)V",
                        false);
            }
            case Type.INT -> {
                mv.visitInsn(DUP);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "int",
                        "(II)V",
                        false);
            }
            case Type.SHORT -> {
                mv.visitInsn(DUP);
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        methodNamePrefix + "short",
                        "(SI)V",
                        false);
            }
            case Type.VOID -> mv.visitMethodInsn(
                    INVOKESTATIC,
                    config.getAnalysisClass(),
                    methodNamePrefix + "void",
                    "()V",
                    false);
            default -> {
                System.err.println("Unknown field or method descriptor " + desc);
                System.exit(1);
            }
        }
    }
}
