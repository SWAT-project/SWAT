package de.uzl.its.swat.instrument.annotation;

import de.uzl.its.swat.instrument.Utils;
import de.uzl.its.swat.instrument.common.FieldInfo;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;

import java.util.List;

/**
 * A {@code MethodVisitor} adapter that instruments constructor methods to process
 * instance fields annotated with {@code @Symbolic}. Before any return instruction
 * in a constructor, this adapter injects code to replace the current field value with
 * its symbolic equivalent.
 * <p>
 * Static fields are not instrumented.
 * </p>
 */
public class ConstructorAdapter extends MethodVisitor {
    private final String className;
    private final List<FieldInfo> symbolicFields;
    private boolean instrumented = false;

    /**
     * Constructs a new {@code ConstructorAdapter}.
     *
     * @param mv              the delegate {@code MethodVisitor}
     * @param className       the internal name of the class being instrumented
     * @param symbolicFields  the list of symbolic field metadata to instrument
     */
    public ConstructorAdapter(MethodVisitor mv, String className, List<FieldInfo> symbolicFields) {
        super(Opcodes.ASM9, mv);
        this.className = className;
        this.symbolicFields = symbolicFields;
    }

    /**
     * Intercepts instructions in the constructor. Before a return instruction is encountered,
     * injects instrumentation code for each non-static field annotated with {@code @Symbolic}.
     *
     * @param opcode the opcode of the instruction to be visited
     */
    @Override
    public void visitInsn(int opcode) {
        // If a return instruction is reached and instrumentation has not yet been done, instrument fields.
        if (!instrumented && (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
            for (FieldInfo field : symbolicFields) {

                if (!Utils.isStatic(field.access())) {  // Skip static fields
                    instrumentField(field);
                }
            }
            instrumented = true;
        }
        super.visitInsn(opcode);
    }

    /**
     * Instruments an instance field to lift it to the symbolic domain and fetches an assignment if available.
     *
     * @param field the field metadata to instrument
     */
    private void instrumentField(FieldInfo field) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.GETFIELD, className, field.name(), field.desc());
        InsnList list = Utils.createLiftInstrumentation(field.getType(), true, false, null);
        list.accept(mv);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className, field.name(), field.desc());
    }
}
