package de.uzl.its.swat.instrument.springexception;

import java.util.HashMap;
import java.util.HashSet;

import de.uzl.its.swat.common.Util;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * visit [ visitSource ] [ visitModule ][ visitNestHost ][ visitOuterClass ] ( visitAnnotation |
 * visitTypeAnnotation | visitAttribute )* ( visitNestMember | [ * visitPermittedSubclass ] |
 * visitInnerClass | visitRecordComponent | visitField | visitMethod )* visitEnd.
 */
public class SpringExceptionClassAdapter extends ClassVisitor {

    private String cname;
    // HashMap<String, HashSet<String>> interfaceMap;

    /**
     * Constructor that calls the super from the default ClassVisitor
     *
     * @param cv Parent ClassVisitor
     * @param interfaceMap
     */
    public SpringExceptionClassAdapter(
            ClassVisitor cv, String cname, HashMap<String, HashSet<String>> interfaceMap) {
        super(Opcodes.ASM9, cv);
        this.cname = cname;
        // this.interfaceMap = interfaceMap;
    }

    /* @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

        if (!interfaceMap.containsKey(this.cname)){
            interfaceMap.put(this.cname, new HashSet<>(List.of(interfaces)));
        } else {
            interfaceMap.get(this.cname).addAll(List.of(interfaces));
        }

        if (interfaceMap.containsKey(superName)) {
            interfaceMap.get(this.cname).addAll(interfaceMap.get(superName));
        }

        if (cname.equals("org/springframework/boot/autoconfigure/web/servlet/error/BasicErrorController")
            || cname.equals("org/springframework/boot/autoconfigure/web/servlet/error/AbstractErrorController")) {
            System.out.println("!!!!!!!! " + name);
            System.out.println("!!!!!!!! " + superName);
            for (String s : interfaceMap.get(this.cname)) {
                System.out.println("!!!!!!!! " + s);
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    } */

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

        if(Util.ignoreMethod(name)) {
            // Avoid Jacoco
            return mv;
        }

        return new SpringExceptionMethodAdapter(mv, access, name, desc, this.cname);
    }
}
