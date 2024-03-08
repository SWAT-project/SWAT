package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.instrument.InternalTransformerType;
import de.uzl.its.swat.instrument.Transformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

/** A visitor to visit a Java method */
public class SVCompMethodAdapter extends LocalVariablesSorter {

    private final String identifier;
    private final String cname;
    private final String name;
    private final String desc;

    int lineNumber = 0;
    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public SVCompMethodAdapter(
            int access, MethodVisitor mv, String cname, String name, String desc) {
        super(Opcodes.ASM9, access, desc, mv);
        identifier = cname + "/" + name;
        this.desc = desc;
        this.cname = cname;
        this.name = name;
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
        if (name.startsWith("nondet") && owner.equals("org/sosy_lab/sv_benchmarks/Verifier")) {
            Transformer.addInstrumentedClass(cname, InternalTransformerType.SV_COMP);
            String newOwner = "de/uzl/its/swat/instrument/svcomp/Verifier";
            // Add one long argument into the desc (will be the only argument) old : ()X where x is
            // some type (after the ) bracket in the original desc), new: (J)X
            String newDescriptor = "(J)" + descriptor.substring(2);

            String retType = descriptor.split("\\)")[1];
            long nextId = Verifier.getNextId();
            SVCompTransformer.getPrintBox()
                    .addToBox("    => Found " + name + " in line " + lineNumber, true);
            SVCompTransformer.getPrintBox().addToBox("      => Assigning ID: " + nextId);
            mv.visitLdcInsn(nextId);
            mv.visitMethodInsn(opcode, newOwner, name, newDescriptor, isInterface);
            if (retType.equals("D") || retType.equals("J")) {
                visitInsn(Opcodes.DUP2);
            } else {
                visitInsn(Opcodes.DUP);
            }
            mv.visitLdcInsn(lineNumber);
            mv.visitLdcInsn(cname);
            mv.visitLdcInsn(this.name + this.desc);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "de/uzl/its/swat/witness/Witness",
                    "recordValue",
                    "(" + descriptor.substring(2) + "ILjava/lang/String;Ljava/lang/String;)V",
                    false);
            mv.visitLdcInsn(nextId);
            SVCompTransformer.getPrintBox().addToBox("      => Adding Witness recording");
            /*
            if (descriptor.equals("()D") || descriptor.equals("()J")) {
                visitInsn(Opcodes.DUP2);
            } else {
                visitInsn(Opcodes.DUP);
            }

             */
            visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "de/uzl/its/swat/Main",
                    "MakeSymbolic",
                    "(" + retType + "J" + ")" + retType,
                    false);
            SVCompTransformer.getPrintBox().addToBox("      => Adding symbolic tracking");
        } else if (name.equals("assume")) {
            String newOwner = "de/uzl/its/swat/instrument/svcomp/Verifier";
            mv.visitMethodInsn(opcode, newOwner, name, descriptor, isInterface);
        } else {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
