package de.uzl.its.swat.instrument.classvariables;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.DataType;
import de.uzl.its.swat.instrument.Utils;
import java.util.ArrayList;

import de.uzl.its.swat.instrument.common.FieldInfo;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * visit [ visitSource ] [ visitModule ][ visitNestHost ][ visitOuterClass ] ( visitAnnotation |
 * visitTypeAnnotation | visitAttribute )* ( visitNestMember | [ * visitPermittedSubclass ] |
 * visitInnerClass | visitRecordComponent | visitField | visitMethod )* visitEnd. This class makes
 * all Variables declared in the class scope symbolic
 */
public class ClassVariablesClassAdapter extends ClassVisitor {


    private final ArrayList<FieldInfo> fields = new ArrayList<>(0);
    private final String cname;
    private boolean isCreated = false;
    private final Config config = Config.instance();

    /**
     * Constructor that calls the super from the default ClassVisitor
     *
     * @param cv Parent ClassVisitor
     */
    public ClassVariablesClassAdapter(ClassVisitor cv, String cname) {
        super(Opcodes.ASM9, cv);
        this.cname = cname;
    }

    @Override
    public FieldVisitor visitField(
            int access, String name, String descriptor, String signature, Object value) {

        FieldVisitor fv = cv.visitField(access, name, descriptor, signature, value);
        fields.add(new FieldInfo(this.cname, name, descriptor, access));
        return fv;
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

        if (!isCreated) createMethod();

        return mv;
    }

    private void handleClassVariables(MethodVisitor mv) {
        for (FieldInfo field : fields) {
            boolean isStatic = Utils.isStatic(field.access());
            if (isStatic) continue; // Skip static fields

            ClassVariablesTransformer.getPrintBox().addMsg("Field: " + field.name() + " " + field.desc());
            ClassVariablesTransformer.getPrintBox().setContentPresent(true);
            DataType fieldType = DataType.getDataType(field.desc());

            // Declare jump targets
            Label liftWithoutPrefixString = new Label();
            Label liftingDone = new Label();

            // Load "this" for the PUTFIELD at the end
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            // [this]

            // Get the field value
            mv.visitVarInsn(Opcodes.ALOAD, 0); // Load "this" again for GETFIELD
            mv.visitFieldInsn(Opcodes.GETFIELD, cname, field.name(), field.desc());
            // [this, field]

            // Check if symbolic prefix is provided
            mv.visitVarInsn(Opcodes.ALOAD, 1); // Load prefix string
            mv.visitJumpInsn(Opcodes.IFNULL, liftWithoutPrefixString);

            // Add lifting with prefix
            mv.visitVarInsn(Opcodes.ALOAD, 1); // Load prefix
            // [this, field, prefix]

            switch (fieldType) {
                case INTEGER_TYPE, BYTE_TYPE, SHORT_TYPE, CHAR_TYPE, BOOLEAN_TYPE,
                     LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, STRING_TYPE -> {
                    // This method has to consume [val, prefix] and leave [val]
                    InsnList lift = Utils.createLiftInstrumentation(
                            field.getType(), false, true, null);
                    lift.accept(mv);
                    // [this, field]
                }
                default -> {
                    InsnList unsupported = Utils.addUnsupportedCall();
                    unsupported.accept(mv);
                    mv.visitInsn(Opcodes.POP);
                    // [this, field]
                }
            }

            mv.visitJumpInsn(Opcodes.GOTO, liftingDone);

            // Lift without prefix string
            mv.visitLabel(liftWithoutPrefixString);
            // [this, field]

            switch (fieldType) {
                case INTEGER_TYPE, BYTE_TYPE, SHORT_TYPE, CHAR_TYPE, BOOLEAN_TYPE,
                     LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, STRING_TYPE -> {
                    InsnList lift = Utils.createLiftInstrumentation(
                            field.getType(), false, false, null);
                    lift.accept(mv);
                    // [this, field]
                }
                case LINKEDLIST_TYPE, LIST_TYPE, ARRAYLIST_TYPE -> {
                    InsnList lift = Utils.liftListVariable(-1, false, true);
                    lift.accept(mv);
                    // [this, field]
                }
                case OBJECT_TYPE -> {
                    if (Utils.isBoxedPrimitive(field.getType())) {
                        InsnList lift = Utils.createLiftInstrumentation(
                                field.getType(), false, false, null);
                        lift.accept(mv);
                        // [this, field]
                        break;
                    }
                    InsnList lift = Utils.liftClass(field.desc());
                    lift.accept(mv);
                    // [this, field]

                }
                case ARRAY_TYPE -> {
                    SWATAssert.enforce(false, "Arrays not supported (yet)");

                }
                default -> SWATAssert.enforce(false, "Unknown data type: " + fieldType);
            }

            // Store the lifted value back to the field
            mv.visitLabel(liftingDone);
            // Stack: [this, field]
            mv.visitFieldInsn(Opcodes.PUTFIELD, cname, field.name(), field.desc());
            // Stack: []
        }
    }


    /** Adds a method to the current class that makes all class variables symbolic. */
    private void createMethod() {
        isCreated = true;

        MethodVisitor mv = cv.visitMethod(
                Opcodes.ACC_PUBLIC,
                config.getInstrumentationPrefix(),
                "(Ljava/lang/String;)V",
                null,
                null);

        mv.visitCode();

        handleClassVariables(mv);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0); // Let COMPUTE_FRAMES handle it
        mv.visitEnd();
    }
}
