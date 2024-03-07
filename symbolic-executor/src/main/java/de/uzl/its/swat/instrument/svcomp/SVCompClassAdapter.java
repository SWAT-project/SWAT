package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.common.SystemLogger;
import org.slf4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * visit [ visitSource ] [ visitModule ][ visitNestHost ][ visitOuterClass ] ( visitAnnotation |
 * visitTypeAnnotation | visitAttribute )* ( visitNestMember | [ * visitPermittedSubclass ] |
 * visitInnerClass | visitRecordComponent | visitField | visitMethod )* visitEnd.
 */
public class SVCompClassAdapter extends ClassVisitor {

    private final String cname;

    private final Logger logger;
    private final SystemLogger systemLogger;

    /**
     * Constructor that calls the super from the default ClassVisitor
     *
     * @param cv Parent ClassVisitor
     */
    public SVCompClassAdapter(String cname, ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
        this.cname = cname;

        systemLogger = new SystemLogger();
        logger = systemLogger.getLogger();
    }

    /**
     * Visits a method of the class. This method must return a new MethodVisitor instance (or null)
     * each time it is called, i.e., it should not return a previously returned visitor. Addition:
     * Add a custom method visitor that instruments the parameters of all endpoint methods to make
     * them symbolic.
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
        if (!cname.equals("org/sosy_lab/sv_benchmarks/Verifier")) {
            systemLogger.addToBox("Method: " + name, false);
            return new SVCompMethodAdapter(access, mv, cname, name, desc);
        }
        return mv;
    }
}
