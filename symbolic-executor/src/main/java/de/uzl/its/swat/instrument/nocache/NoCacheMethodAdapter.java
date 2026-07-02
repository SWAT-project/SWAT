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
            // Record provenance (de-interned copy -> the interned literal canonical). The interned
            // literal is a compile-time constant, so re-LDC'ing it pushes the SAME canonical object
            // that every other occurrence of this literal shares; never null (no guard needed).
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(value);
            emitProvenanceRecord();
        } else {
            mv.visitLdcInsn(value);
        }
    }

    /**
     * Emit {@code Provenance.record(copy, canonical)} consuming the top two stack entries
     * {@code [..., copy, canonical]} and leaving {@code [...]}. Callers DUP the copy first so the copy
     * survives. Emitted via the raw delegate so it is not re-processed by this adapter.
     */
    private void emitProvenanceRecord() {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/common/Provenance", "record",
                "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
    }

    /**
     * After a de-interned boxed copy is on top of the stack and its primitive is in {@code primLocal},
     * record provenance to the box's REAL cached canonical. The valueOf rewrite replaced the original
     * call before it ran, so the canonical is not on the stack - we materialize it by calling the
     * genuine {@code valueOf} via the raw delegate (so it is NOT itself re-rewritten). Without this,
     * {@code root} of two cached boxes (e.g. valueOf(100)) would be self -> distinct -> {@code ==}
     * false, regressing real Java (cache hit -> true). Stack: {@code [copy] -> [copy]}.
     */
    private void recordBoxedProvenance(String owner, String valueOfDescriptor, int primLoadOpcode,
            int primLocal) {
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(primLoadOpcode, primLocal);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "valueOf", valueOfDescriptor, false);
        emitProvenanceRecord();
    }

    /**
     * Consume the primitive on top of the stack and leave a fresh, distinctly-identified boxed instance
     * in its place ({@code [prim] -> [new <Boxed>(prim)]}). The primitive is parked in a fresh local
     * (returned) because it is needed again after the {@code NEW} - both to feed the constructor and,
     * for {@link #rewriteValueOf}, to materialize the cached canonical. The local is required for the
     * category-2 {@code long}. Shared by the {@code valueOf} rewrite and {@link #deInternReturn}.
     */
    private int reboxFreshFromPrimitive(Boxed boxed) {
        int primLocal = newLocal(boxed.primType);
        mv.visitVarInsn(boxed.primType.getOpcode(Opcodes.ISTORE), primLocal);
        mv.visitTypeInsn(Opcodes.NEW, boxed.owner);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(boxed.primType.getOpcode(Opcodes.ILOAD), primLocal);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, boxed.owner, "<init>", boxed.ctorDescriptor(), false);
        return primLocal;
    }

    /**
     * Replace {@code <Boxed>.valueOf(prim)} (the primitive is on top of the stack) with
     * {@code new <Boxed>(prim)}, giving the produced box a distinct identity, and record provenance to
     * the real cached canonical so reference {@code ==} on cache hits still models real Java.
     */
    private void rewriteValueOf(Boxed boxed) {
        int primLocal = reboxFreshFromPrimitive(boxed);
        recordBoxedProvenance(boxed.owner, boxed.valueOfDescriptor(),
                boxed.primType.getOpcode(Opcodes.ILOAD), primLocal);
        NoCacheTransformer.getPrintBox()
                .addMsg("Replacing " + boxed.simpleName() + ".valueOf with new " + boxed.simpleName());
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
        // Replace <Boxed>.valueOf(prim) with new <Boxed>(prim) to defeat the wrapper cache (see Boxed).
        if (opcode == Opcodes.INVOKESTATIC && name.equals("valueOf")) {
            Boxed boxed = Boxed.forValueOf(owner, descriptor);
            if (boxed != null) {
                rewriteValueOf(boxed);
                return;
            }
        }
        // For all other method calls, proceed normally.
        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        // Output-boundary de-interning: de-intern a value-typed return from an un-instrumented
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
            // String has a copy constructor. Keep the original in a local so we can both build
            // new String(original) AND record provenance (copy -> original) afterward.
            Label done = new Label();
            mv.visitInsn(Opcodes.DUP);
            mv.visitJumpInsn(Opcodes.IFNULL, done); // null result: leave it, skip the wrap
            int origLocal = newLocal(Type.getObjectType("java/lang/String"));
            mv.visitVarInsn(Opcodes.ASTORE, origLocal);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, origLocal);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>",
                    "(Ljava/lang/String;)V", false); // [copy]
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, origLocal);
            emitProvenanceRecord(); // record(copy, original) -> [copy]
            mv.visitLabel(done);
            return;
        }
        Boxed boxed = Boxed.forReturnDescriptor(returnType.getDescriptor());
        if (boxed != null) {
            // Boxed wrappers have no copy constructor: keep the original boxed in a local, unbox it to
            // the primitive, rebox into a fresh instance, then record provenance (copy -> original).
            Label done = new Label();
            mv.visitInsn(Opcodes.DUP);
            mv.visitJumpInsn(Opcodes.IFNULL, done); // null result: leave it, skip the wrap
            int origLocal = newLocal(Type.getObjectType(boxed.owner));
            mv.visitVarInsn(Opcodes.ASTORE, origLocal);
            mv.visitVarInsn(Opcodes.ALOAD, origLocal);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxed.owner, boxed.unboxMethod,
                    boxed.unboxDescriptor(), false); // [prim]
            reboxFreshFromPrimitive(boxed); // [copy]
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, origLocal);
            emitProvenanceRecord(); // record(copy, original) -> [copy]
            mv.visitLabel(done);
        }
    }

    /**
     * The six cached boxed wrappers that are de-interned. Each carries the JVM primitive type descriptor
     * ({@code primDescriptor}, e.g. {@code "I"}, {@code "J"}, {@code "S"}) from which all method
     * descriptors are derived, so this enum is the single source of truth for both the {@code valueOf}
     * rewrite and the return de-intern. {@code primType} is the stack type used for the load/store
     * opcodes and {@code newLocal}: {@code short}/{@code byte}/{@code char} live on the operand stack
     * as {@code int}, hence {@link Type#INT_TYPE} for all of them and {@link Type#LONG_TYPE} only for
     * {@code long}.
     */
    private enum Boxed {
        INTEGER("java/lang/Integer", "intValue", "I", Type.INT_TYPE),
        LONG("java/lang/Long", "longValue", "J", Type.LONG_TYPE),
        SHORT("java/lang/Short", "shortValue", "S", Type.INT_TYPE),
        BYTE("java/lang/Byte", "byteValue", "B", Type.INT_TYPE),
        CHARACTER("java/lang/Character", "charValue", "C", Type.INT_TYPE),
        BOOLEAN("java/lang/Boolean", "booleanValue", "Z", Type.INT_TYPE);

        final String owner;
        final String unboxMethod;
        final String primDescriptor;
        final Type primType;

        Boxed(String owner, String unboxMethod, String primDescriptor, Type primType) {
            this.owner = owner;
            this.unboxMethod = unboxMethod;
            this.primDescriptor = primDescriptor;
            this.primType = primType;
        }

        /** The wrapper's own type descriptor, e.g. {@code Ljava/lang/Integer;}. */
        String boxedDescriptor() {
            return "L" + owner + ";";
        }

        /** {@code valueOf} factory descriptor, e.g. {@code (I)Ljava/lang/Integer;}. */
        String valueOfDescriptor() {
            return "(" + primDescriptor + ")" + boxedDescriptor();
        }

        /** Primitive constructor descriptor, e.g. {@code (I)V}. */
        String ctorDescriptor() {
            return "(" + primDescriptor + ")V";
        }

        /** Unbox accessor descriptor, e.g. {@code ()I} for {@code intValue}. */
        String unboxDescriptor() {
            return "()" + primDescriptor;
        }

        /** Simple class name for log messages, e.g. {@code Integer}. */
        String simpleName() {
            return owner.substring(owner.lastIndexOf('/') + 1);
        }

        /** The wrapper whose {@code valueOf(prim)} this call site invokes, or {@code null}. */
        static Boxed forValueOf(String owner, String descriptor) {
            for (Boxed b : values()) {
                if (b.owner.equals(owner) && b.valueOfDescriptor().equals(descriptor)) {
                    return b;
                }
            }
            return null;
        }

        /** The wrapper matching a value-typed return descriptor, or {@code null}. */
        static Boxed forReturnDescriptor(String descriptor) {
            for (Boxed b : values()) {
                if (b.boxedDescriptor().equals(descriptor)) {
                    return b;
                }
            }
            return null;
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
