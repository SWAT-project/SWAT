package de.uzl.its.swat.instrument.refequality;

import de.uzl.its.swat.common.Util;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class RefEqualityClassAdapter extends ClassVisitor {
    private final String cname;

    public RefEqualityClassAdapter(ClassVisitor classVisitor, String cname) {
        super(Opcodes.ASM9, classVisitor);
        this.cname = cname;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if(Util.ignoreMethod(name)) {
            // Avoid Jacoco
            return mv;
        }

        // Use our custom method visitor that replaces reference equality checks.
        return new RefEqualityMethodAdapter(api, access, descriptor, mv);
    }
}