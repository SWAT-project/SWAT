package de.uzl.its.swat.instrument.parameter;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.config.Config;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * visit [ visitSource ] [ visitModule ][ visitNestHost ][ visitOuterClass ] ( visitAnnotation |
 * visitTypeAnnotation | visitAttribute )* ( visitNestMember | [ * visitPermittedSubclass ] |
 * visitInnerClass | visitRecordComponent | visitField | visitMethod )* visitEnd.
 */
public class ParameterClassAdapter extends ClassVisitor {
    private final Config config = Config.instance();
    private final String cname;

    /**
     * Constructor that calls the super from the default ClassVisitor
     *
     * @param cv Parent ClassVisitor
     */
    public ParameterClassAdapter(ClassVisitor cv, String cname) {
        super(Opcodes.ASM9, cv);
        this.cname = cname;
    }

    /**
     * Visits a method of the class. This method must return a new MethodVisitor instance (or null)
     * each time it is called, i.e., it should not return a previously returned visitor. Addition:
     * Add a custom method visitor that instruments the parameters of the method to make them
     * symbolic
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


        if(Util.ignoreMethod(name)) {
            // Avoid Jacoco
            return mv;
        }

        // Check if the name matches the pattern
        if (!Util.isSymbolicMethod(cname, name))
            return mv;
        ParameterTransformer.getPrintBox().addMsg("Method: " + name);

        return new ParameterMethodAdapter(mv, access, name, desc, signature);
    }
}
