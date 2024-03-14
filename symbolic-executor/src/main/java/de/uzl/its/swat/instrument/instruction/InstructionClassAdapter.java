package de.uzl.its.swat.instrument.instruction;

import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.symbolic.ClassNames;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * visit [ visitSource ] [ visitModule ][ visitNestHost ][ visitOuterClass ] ( visitAnnotation |
 * visitTypeAnnotation | visitAttribute )* ( visitNestMember | [ * visitPermittedSubclass ] |
 * visitInnerClass | visitRecordComponent | visitField | visitMethod )* visitEnd.
 */
public class InstructionClassAdapter extends ClassVisitor {
    private final String cname;
    /**
     * Constructor that calls the super from the default ClassVisitor
     *
     * @param cv Parent ClassVisitor
     */
    public InstructionClassAdapter(ClassVisitor cv, String cname) {
        super(Opcodes.ASM9, cv);
        this.cname = cname;
    }

    /**
     * Visits a method of the class. This method must return a new MethodVisitor instance (or null)
     * each time it is called, i.e., it should not return a previously returned visitor. Addition:
     * Add a custom method visitor that instruments each instruction
     *
     * @param access the method's access flags (see Opcodes). This parameter also indicates if the
     *     method is synthetic and/or deprecated.
     * @param name the method's name.
     * @param desc the method's descriptor (see Type).
     * @param signature the method's signature. May be null if the method parameters, return type
     *     and exceptions do not use generic types.
     * @param exceptions the internal names of the method's exception classes (see
     *     Type.getInternalName()). May be null.
     * @return an object to visit the byte code of the method, or null if this class visitor is not
     *     interested in visiting the code of this method.
     */
    @Override
    public MethodVisitor visitMethod(
            int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (cname.equals("de/uzl/its/swat/instrument/svcomp/Verifier") && !name.equals("assume")) {
            return mv;
        }
        if (mv != null) {
            return new InstructionMethodAdapter(
                    mv,
                    name.equals("<init>"),
                    GlobalStateForInstrumentation.instance,
                    ClassNames.getInstance(),
                    name);
        }

        return null;
    }
}
