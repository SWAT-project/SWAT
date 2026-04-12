package de.uzl.its.swat.instrument.annotation;

import de.uzl.its.swat.annotations.Symbolic;
import de.uzl.its.swat.instrument.common.FieldInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * A {@code FieldVisitor} that detects fields annotated with {@code @Symbolic} and records them
 * for later instrumentation.
 * <p>
 * When a field is visited, if an annotation matching {@code @Symbolic} is found, the field's
 * name, descriptor, and whether it is static are recorded into a shared list.
 * </p>
 */
public class AnnotationFieldAdapter extends FieldVisitor {
    private final String className;
    private final String fieldName;
    private final String fieldDesc;
    private final int access;
    private final List<FieldInfo> symbolicFields;

    /**
     * Constructs a new {@code AnnotationFieldAdapter}.
     *
     * @param fv              the delegate {@code FieldVisitor}
     * @param access          the field's access flags
     * @param fieldName       the name of the field
     * @param fieldDesc       the descriptor of the field
     * @param symbolicFields  the list to which detected symbolic field information will be added
     */
    public AnnotationFieldAdapter(FieldVisitor fv, int access, String className, String fieldName, String fieldDesc, List<FieldInfo> symbolicFields) {
        super(Opcodes.ASM9, fv);
        this.className = className;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
        this.access = access;
        this.symbolicFields = symbolicFields;
    }

    /**
     * Visits an annotation of the field. If the annotation descriptor matches {@code @Symbolic},
     * the field is recorded for later instrumentation.
     *
     * @param descriptor the descriptor of the annotation
     * @param visible    whether the annotation is visible at runtime
     * @return an {@code AnnotationVisitor} to visit the annotation values, or the result of the
     *         superclass method if not overridden
     */
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        if (Type.getDescriptor(Symbolic.class).equals(descriptor)) {
            AnnotationTransformer.getPrintBox().addMsg("Symbolic field identified: " + fieldName);
            symbolicFields.add(new FieldInfo(this.className, fieldName, fieldDesc, access));
        }
        return av;
    }
}
