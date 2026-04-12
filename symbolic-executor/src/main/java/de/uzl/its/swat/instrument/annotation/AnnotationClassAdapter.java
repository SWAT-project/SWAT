package de.uzl.its.swat.instrument.annotation;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.instrument.common.FieldInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code ClassVisitor} that instruments classes to support symbolic execution by handling
 * the {@code @Symbolic} annotation. This adapter instruments:
 * <ul>
 *   <li>Fields annotated with {@code @Symbolic} via {@link AnnotationFieldAdapter}.</li>
 *   <li>Constructors to process symbolic fields using {@link ConstructorAdapter}.</li>
 *   <li>Non-constructor methods to handle local variable annotations via {@link AnnotationMethodNode}.</li>
 * </ul>
 */
public class AnnotationClassAdapter extends ClassVisitor {
    private String className;
    private final List<FieldInfo> symbolicFields = new ArrayList<>();

    /**
     * Constructs a new {@code AnnotationClassAdapter} that delegates to the given {@code ClassVisitor}.
     *
     * @param cv the next {@code ClassVisitor} in the chain
     */
    public AnnotationClassAdapter(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    /**
     * Visits the class header and records the internal class name.
     *
     * @param version    the class version
     * @param access     the class's access flags
     * @param name       the internal name of the class
     * @param signature  the signature of the class, or {@code null} if not generic
     * @param superName  the internal name of the super class
     * @param interfaces the internal names of the class's interfaces
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * Visits a field of the class and wraps the returned {@code FieldVisitor}
     * with an {@link AnnotationFieldAdapter} to handle {@code @Symbolic} annotations.
     *
     * @param access    the field's access flags
     * @param name      the field's name
     * @param desc      the field's descriptor
     * @param signature the field's signature, or {@code null} if not generic
     * @param value     the field's initial value, or {@code null} if none
     * @return a visitor to visit field annotations and attributes
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        return new AnnotationFieldAdapter(fv, access, this.className, name, desc, symbolicFields);
    }

    /**
     * Visits a method of the class. For constructors, wraps the method visitor with a
     * {@link ConstructorAdapter} to instrument symbolic fields. For other methods, wraps with
     * an {@link AnnotationMethodNode} to handle local variable and parameter annotations.
     *
     * @param access     the method's access flags
     * @param name       the method's name
     * @param desc       the method's descriptor
     * @param signature  the method's signature, or {@code null} if not generic
     * @param exceptions the internal names of the method's exception classes, or {@code null}
     * @return a visitor to visit the method's code
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if(Util.ignoreMethod(name)) {
            // Avoid Jacoco
            return mv;
        }

        if (name.equals("<init>")) {
            // Instrument constructors to handle symbolic fields.
            return new ConstructorAdapter(new AnnotationMethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions, mv),
                    className, symbolicFields);
        } else {
            // Instrument non-constructors for local variable and parameter annotations.
            return new AnnotationMethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions, mv);
        }
    }
}
