package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.Intrinsics;
import de.uzl.its.swat.instrument.Transformer;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

/** A visitor to visit a Java method */
public class SVCompMethodAdapter extends LocalVariablesSorter {

    private static final String SYSTEM_EXIT = "de/uzl/its/swat/instrument/svcomp/SystemExit";

    private final String identifier;
    private final String cname;
    private final String name;
    private final String desc;
    private final String sourceFile;
    private final Map<Integer, Integer> nondetCallsPerLine;

    /** Return type descriptors of the nondet methods whose values are recorded for the witness. */
    private static final Set<String> WITNESS_VALUE_TYPES =
            Set.of("Z", "B", "S", "C", "I", "J", "F", "D", "Ljava/lang/String;");

    int lineNumber = 0;
    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     * @param desc A string description of the parameters of the method
     * @param sourceFile The source file name of the class (empty if unknown)
     * @param nondetCallsPerLine Per-class counter of nondet calls per source line (shared between
     *     the methods of the class)
     */
    public SVCompMethodAdapter(
            int access,
            MethodVisitor mv,
            String cname,
            String name,
            String desc,
            String sourceFile,
            Map<Integer, Integer> nondetCallsPerLine) {
        super(Opcodes.ASM9, access, desc, mv);
        identifier = cname + "/" + name;
        this.desc = desc;
        this.cname = cname;
        this.name = name;
        this.sourceFile = sourceFile;
        this.nondetCallsPerLine = nondetCallsPerLine;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        lineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
        // The weird concatenation is needed to avoid the relocation from the ShadowJar plugin to move this owner
        String targetOwner = "org/".concat("sosy_lab/sv_benchmarks/Verifier");
        String objectFactory = "org/".concat("sosy_lab/sv_benchmarks/ObjectFactory");
        if (name.equals("nondetObject")
                && owner.equals(targetOwner)
                && descriptor.equals("(Ljava/lang/Class;L" + objectFactory + ";)Ljava/lang/Object;")) {
            // nondetObject(type, factory) is defined as factory.createObject(); the factory makes its
            // nondeterministic choices with the primitive nondet methods, which are redirected when
            // the factory class is instrumented. Calling the factory from here (instead of from the
            // uninstrumented benchmark Verifier) keeps the call and the returned object tracked, and
            // every object the factory can return is covered by exploring its nondet values.
            // The object itself is not recorded for the witness (the factory's nondet values are).
            Transformer.addInstrumentedClass(cname, InternalTransformerType.SV_COMP);
            SVCompTransformer.getPrintBox().setContentPresent(true);
            SVCompTransformer.getPrintBox()
                    .addMsg("    => Found " + name + " in line " + lineNumber);
            SVCompTransformer.getPrintBox().addMsg("      => Calling the factory directly");
            // Stack: type, factory -> factory
            mv.visitInsn(Opcodes.SWAP);
            mv.visitInsn(Opcodes.POP);
            mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE, objectFactory, "createObject", "()Ljava/lang/Object;", true);
        } else if (name.startsWith("nondet") && owner.equals(targetOwner)) {
            Transformer.addInstrumentedClass(cname, InternalTransformerType.SV_COMP);
            String newOwner = "de/uzl/its/swat/instrument/svcomp/Verifier";
            // Add one long argument into the desc (will be the only argument) old : ()X where x is
            // some type (after the ) bracket in the original desc), new: (J)X
            String newDescriptor = "(J)" + descriptor.substring(2);

            String retType = descriptor.split("\\)")[1];
            long nextId = Verifier.getNextId();
            // Index of this call among the nondet calls on the same source line (bytecode order
            // follows the left-to-right evaluation order); lets the witness locate the call.
            int callIndex = nondetCallsPerLine.merge(lineNumber, 1, Integer::sum) - 1;
            SVCompTransformer.getPrintBox().setContentPresent(true);
            SVCompTransformer.getPrintBox()
                    .addMsg("    => Found " + name + " in line " + lineNumber);
            SVCompTransformer.getPrintBox().addMsg("      => Assigning ID: " + nextId);
            mv.visitLdcInsn(nextId);
            mv.visitMethodInsn(opcode, newOwner, name, newDescriptor, isInterface);
            if (WITNESS_VALUE_TYPES.contains(retType)) {
                // Record the returned value with the call location for the witness (trace data)
                if (retType.equals("D") || retType.equals("J")) {
                    visitInsn(Opcodes.DUP2);
                } else {
                    visitInsn(Opcodes.DUP);
                }
                mv.visitLdcInsn(name);
                mv.visitLdcInsn(cname);
                mv.visitLdcInsn(this.name + this.desc);
                mv.visitLdcInsn(sourceFile);
                mv.visitLdcInsn(lineNumber);
                mv.visitLdcInsn(callIndex);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "de/uzl/its/swat/witness/Witness",
                        "recordValue",
                        "("
                                + retType
                                + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V",
                        false);
                SVCompTransformer.getPrintBox().addMsg("      => Adding Witness recording");
            }
            mv.visitLdcInsn(nextId);
            /*
            if (descriptor.equals("()D") || descriptor.equals("()J")) {
                visitInsn(Opcodes.DUP2);
            } else {
                visitInsn(Opcodes.DUP);
            }

             */
            visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(Intrinsics.class),
                    "liftValue",
                    "(" + retType + "J" + ")" + retType,
                    false);
            SVCompTransformer.getPrintBox().addMsg("      => Adding symbolic tracking");
        } else if (name.equals("assume")) {
            String newOwner = "de/uzl/its/swat/instrument/svcomp/Verifier";
            mv.visitMethodInsn(opcode, newOwner, name, descriptor, isInterface);
        } else if (owner.equals("java/lang/System") && name.equals("exit") && descriptor.equals("(I)V")) {
            // Send the trace before the program ends (see SystemExit)
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SYSTEM_EXIT, "exit", descriptor, false);
        } else if (owner.equals("java/lang/Runtime") && (name.equals("exit") || name.equals("halt"))
                && descriptor.equals("(I)V")) {
            String newName = name.equals("exit") ? "runtimeExit" : "runtimeHalt";
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SYSTEM_EXIT, newName, "(Ljava/lang/Runtime;I)V", false);
        } else if (owner.equals("java/net/Socket")) {
            String newOwner = "de/uzl/its/swat/instrument/svcomp/Socket";
            mv.visitMethodInsn(opcode, newOwner, name, descriptor, isInterface);
        } else {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.ATHROW) {
            // An assertion fails where its AssertionError is thrown (see Witness.recordThrow)
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "de/uzl/its/swat/witness/Witness",
                    "recordThrow",
                    "(Ljava/lang/Throwable;)V",
                    false);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        // Replace the type for `new java.net.Socket` with the mock type
        if (opcode == Opcodes.NEW && type.equals("java/net/Socket")) {
            String newType = "de/uzl/its/swat/instrument/svcomp/Socket";
            super.visitTypeInsn(opcode, newType);
        } else {
            super.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        // Redirect static field references or any field accesses from `java.net.Socket` to the mock
        if (owner.equals("java/net/Socket")) {
            String newOwner = "de/uzl/its/swat/instrument/svcomp/Socket";
            super.visitFieldInsn(opcode, newOwner, name, descriptor);
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }
}
