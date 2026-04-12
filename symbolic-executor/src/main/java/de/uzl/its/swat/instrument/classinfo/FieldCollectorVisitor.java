package de.uzl.its.swat.instrument.classinfo;

import de.uzl.its.swat.instrument.common.FieldInfo;
import lombok.Getter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import java.util.ArrayList;
import java.util.List;

/**
 * A ClassVisitor that collects all declared fields.
 */
@Getter
public class FieldCollectorVisitor extends ClassVisitor {
    private String className;
    private final List<FieldInfo> fields = new ArrayList<>();

    public FieldCollectorVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        fields.add(new FieldInfo(this.className, name, descriptor, access));
        return super.visitField(access, name, descriptor, signature, value);
    }

}
