package de.uzl.its.swat.instrument.nocache;

import de.uzl.its.swat.common.Util;
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

        // G3: output-boundary de-interning. De-intern a value-typed return from an UN-instrumented
        // callee - the boundary where interned/shared values (literals, constants, this-returns,
        // cached boxes) enter shadow space. A fresh copy gives the produced value a distinct identity,
        // so the reference-keyed heap stays sound for value types. Skip the SWAT / sv-benchmarks
        // intrinsics (the symbolic-input designation / witness seam). Gated on the de-intern switch.
        if (Config.instance().isUseStringInterning()
                && !Util.shouldInstrument(owner)
                && !isDeInternSkippedOwner(owner)) {
            deInternReturn(Type.getReturnType(descriptor));
        }
    }

    /**
     * Emit a fresh, distinctly-identified copy of the value just returned (now on the stack), if its
     * type is an interning value type. Null-guarded - the wrap would NPE on a null return. String and
     * the six cached boxed wrappers are covered; Float/Double are intentionally excluded (uncached,
     * reference equality - see {@link Util#isDeInternedClass(Object)}).
     */
    private void deInternReturn(Type returnType) {
        if ("Ljava/lang/String;".equals(returnType.getDescriptor())) {
            // String has a copy constructor: reorder the on-stack reference into new String(ref).
            Label done = new Label();
            mv.visitInsn(Opcodes.DUP);
            mv.visitJumpInsn(Opcodes.IFNULL, done); // null result: leave it, skip the wrap
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
            mv.visitInsn(Opcodes.DUP_X1);
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>",
                    "(Ljava/lang/String;)V", false);
            mv.visitLabel(done);
            return;
        }
        Boxed boxed = Boxed.forDescriptor(returnType.getDescriptor());
        if (boxed != null) {
            // Boxed wrappers have no copy constructor: unbox to the primitive, then rebox into a fresh
            // instance (same shape as the valueOf de-interning above). A local holds the primitive
            // across the NEW (required for the category-2 long; harmless for the others).
            Label done = new Label();
            mv.visitInsn(Opcodes.DUP);
            mv.visitJumpInsn(Opcodes.IFNULL, done); // null result: leave it, skip the wrap
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxed.owner, boxed.unboxMethod,
                    boxed.unboxDescriptor, false);
            int local = newLocal(boxed.primType);
            mv.visitVarInsn(boxed.primType.getOpcode(Opcodes.ISTORE), local);
            mv.visitTypeInsn(Opcodes.NEW, boxed.owner);
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(boxed.primType.getOpcode(Opcodes.ILOAD), local);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, boxed.owner, "<init>",
                    boxed.ctorDescriptor, false);
            mv.visitLabel(done);
        }
    }

    /** The six cached boxed wrappers G3 de-interns, with their unbox method and primitive constructor. */
    private enum Boxed {
        INTEGER("java/lang/Integer", "intValue", "()I", "(I)V", Type.INT_TYPE),
        LONG("java/lang/Long", "longValue", "()J", "(J)V", Type.LONG_TYPE),
        SHORT("java/lang/Short", "shortValue", "()S", "(S)V", Type.INT_TYPE),
        BYTE("java/lang/Byte", "byteValue", "()B", "(B)V", Type.INT_TYPE),
        CHARACTER("java/lang/Character", "charValue", "()C", "(C)V", Type.INT_TYPE),
        BOOLEAN("java/lang/Boolean", "booleanValue", "()Z", "(Z)V", Type.INT_TYPE);

        final String owner;
        final String unboxMethod;
        final String unboxDescriptor;
        final String ctorDescriptor;
        final Type primType;

        Boxed(String owner, String unboxMethod, String unboxDescriptor, String ctorDescriptor,
                Type primType) {
            this.owner = owner;
            this.unboxMethod = unboxMethod;
            this.unboxDescriptor = unboxDescriptor;
            this.ctorDescriptor = ctorDescriptor;
            this.primType = primType;
        }

        static Boxed forDescriptor(String descriptor) {
            return switch (descriptor) {
                case "Ljava/lang/Integer;" -> INTEGER;
                case "Ljava/lang/Long;" -> LONG;
                case "Ljava/lang/Short;" -> SHORT;
                case "Ljava/lang/Byte;" -> BYTE;
                case "Ljava/lang/Character;" -> CHARACTER;
                case "Ljava/lang/Boolean;" -> BOOLEAN;
                default -> null;
            };
        }
    }

    /**
     * Owners whose value-typed returns must NOT be de-interned even though they are un-instrumented:
     * the symbolic-input designation / witness intrinsics. {@code shouldInstrument} already returns
     * false for these (they're excluded), so this explicit check is the load-bearing exclusion, not a
     * delegation to it.
     */
    private static boolean isDeInternSkippedOwner(String owner) {
        return owner.startsWith("de/uzl/its/swat/")
                || owner.equals("org/sosy_lab/sv_benchmarks/Verifier");
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
