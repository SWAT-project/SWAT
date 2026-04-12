package de.uzl.its.swat.instrument.annotation;

import de.uzl.its.swat.annotations.Symbolic;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.instrument.Intrinsics;
import de.uzl.its.swat.instrument.Utils;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@code MethodNode} implementation that instruments both annotated local variables
 * and annotated method parameters. For parameters annotated with {@code @Symbolic}, it inserts
 * instrumentation at method entry (in {@code visitCode}). For local variables annotated with
 * {@code @Symbolic}, it scans the instruction list at {@code visitEnd} and inserts instrumentation
 * after each corresponding store instruction.
 * <p>
 * </p>
 */
public class AnnotationMethodNode extends MethodNode {
    private final String methodName;
    private final String methodDesc;
    // Maps local variable indices to their type descriptors (e.g., "I", "Ljava/lang/String;", etc.)
    private final Map<Integer, String> localVarTypes = new HashMap<>();
    // Holds indices of local variables annotated with @Symbolic.
    private final Set<Integer> symbolicLocalIndices = new HashSet<>();
    // Holds indices of method parameters annotated with @Symbolic.
    private final Set<Integer> symbolicParamIndices = new HashSet<>();
    // The delegate MethodVisitor.
    private final MethodVisitor mv;

    /**
     * Constructs a new {@code AnnotationMethodNode}.
     *
     * @param api        the ASM API version
     * @param access     the method's access flags
     * @param name       the method name
     * @param desc       the method descriptor
     * @param signature  the method signature (may be null)
     * @param exceptions the method exceptions (may be null)
     * @param mv         the delegate {@code MethodVisitor}
     */
    public AnnotationMethodNode(int api, int access, String name, String desc,
                                String signature, String[] exceptions, MethodVisitor mv) {
        super(api, access, name, desc, signature, exceptions);
        this.methodName = name;
        this.methodDesc = desc;
        this.mv = mv;
    }

    /**
     * Records method parameter annotations. If a parameter is annotated with {@code @Symbolic},
     * its index is added to the symbolicParamIndices set.
     *
     * @param parameter  the parameter index
     * @param descriptor the annotation descriptor
     * @param visible    whether the annotation is visible at runtime
     * @return an AnnotationVisitor for further annotation processing
     */
    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        if (Type.getDescriptor(Symbolic.class).equals(descriptor)) {
            symbolicParamIndices.add(parameter);
        }
        return super.visitParameterAnnotation(parameter, descriptor, visible);
    }

    /**
     * Records local variable type information.
     *
     * @param name      the local variable name
     * @param desc      the type descriptor
     * @param signature the variable signature (may be null)
     * @param start     the start label of the variable's scope
     * @param end       the end label of the variable's scope
     * @param index     the variable index
     */
    @Override
    public void visitLocalVariable(String name, String desc, String signature,
                                   Label start, Label end, int index) {
        localVarTypes.put(index, desc);
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    /**
     * Records annotations on local variables. If the annotation descriptor matches {@code @Symbolic},
     * all affected variable indices are recorded.
     *
     * @param typeRef    a reference to the annotated type
     * @param typePath   the path to the annotated type argument
     * @param start      the start labels for the variable's scope
     * @param end        the end labels for the variable's scope
     * @param indices    the variable indices
     * @param descriptor the annotation descriptor
     * @param visible    whether the annotation is visible at runtime
     * @return an AnnotationVisitor for further annotation processing
     */
    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
                                                          Label[] start, Label[] end,
                                                          int[] indices, String descriptor,
                                                          boolean visible) {
        if (Type.getDescriptor(Symbolic.class).equals(descriptor)) {
            for (int index : indices) {
                symbolicLocalIndices.add(index);
            }
        }
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, indices, descriptor, visible);
    }

    /**
     * Inserts instrumentation code at the beginning of the method for each parameter annotated
     * with {@code @Symbolic}. For non-static methods, the 'this' parameter is skipped.
     */
    @Override
    public void visitCode() {
        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(methodDesc);
        InsnList paramInstrumentation = new InsnList();
        for (int i = 0; i < argTypes.length; i++) {
            if (symbolicParamIndices.contains(i)) {
                int localIndex = isStatic ? i : i + 1;
                Type t = argTypes[i];
                switch (t.getSort()) {
                    case Type.INT:
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                        paramInstrumentation.add(Utils.liftLocalVariable(localIndex, Opcodes.ILOAD, Opcodes.ISTORE, Type.INT_TYPE, true));
                        break;
                    case Type.LONG:
                        paramInstrumentation.add(Utils.liftLocalVariable(localIndex, Opcodes.LLOAD, Opcodes.LSTORE, Type.LONG_TYPE, true));
                        break;
                    case Type.FLOAT:
                        paramInstrumentation.add(Utils.liftLocalVariable(localIndex, Opcodes.FLOAD, Opcodes.FSTORE, Type.FLOAT_TYPE, true));
                        break;
                    case Type.DOUBLE:
                        paramInstrumentation.add(Utils.liftLocalVariable(localIndex, Opcodes.DLOAD, Opcodes.DSTORE, Type.DOUBLE_TYPE, true));
                        break;
                    case Type.ARRAY:
                        // Handle array types (int[], boolean[], etc.)
                        paramInstrumentation.add(Utils.liftLocalVariable(localIndex, Opcodes.ALOAD, Opcodes.ASTORE, t, true));
                        break;
                    case Type.OBJECT:
                        Type type = Type.getType(Object.class);
                        switch (t.getInternalName()) {
                            case "java/lang/String" -> type = Type.getType(String.class);
                            case "java/lang/Integer" -> type = Type.getType(Integer.class);
                            case "java/lang/Boolean" -> type = Type.getType(Boolean.class);
                            case "java/lang/Byte" -> type = Type.getType(Byte.class);
                            case "java/lang/Character" -> type = Type.getType(Character.class);
                            case "java/lang/Short" -> type = Type.getType(Short.class);
                            case "java/lang/Long" -> type = Type.getType(Long.class);
                            case "java/lang/Float" -> type = Type.getType(Float.class);
                            case "java/lang/Double" -> type = Type.getType(Double.class);
                            case "java/util/List" -> type = Type.getType(java.util.List.class);
                            case "java/util/ArrayList" -> type = Type.getType(java.util.ArrayList.class);
                            case "java/util/LinkedList" -> type = Type.getType(java.util.LinkedList.class);
                        }
                        if(type.equals(Type.getType(java.util.List.class)) ||
                                type.equals(Type.getType(java.util.ArrayList.class)) ||
                                type.equals(Type.getType(java.util.LinkedList.class))) {
                            paramInstrumentation.add(Utils.liftListVariable(localIndex, true, false));
                            break;
                        }
                        paramInstrumentation.add(Utils.liftLocalVariable(localIndex, Opcodes.ALOAD, Opcodes.ASTORE, type, true));
                        break;
                    default:
                        // Ignore unsupported types.
                        break;
                }
            }
        }
        if (paramInstrumentation.size() > 0) {
            instructions.insert(paramInstrumentation);
        }
        super.visitCode();
    }

    /**
     * At method end, iterates through the instruction list and inserts instrumentation code immediately
     * after any local variable store instruction where the variable was annotated with {@code @Symbolic}.
     */
    @Override
    public void visitEnd() {
        for (AbstractInsnNode insn : instructions.toArray()) {
            if (insn instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) insn;
                int opcode = varInsn.getOpcode();
                int var = varInsn.var;
                if (symbolicLocalIndices.contains(var)) {
                    InsnList instrumentation = new InsnList();
                    switch (opcode) {
                        case Opcodes.ISTORE:
                            instrumentation.add(Utils.liftLocalVariable(var, Opcodes.ILOAD, Opcodes.ISTORE, Type.INT_TYPE, true));
                            break;
                        case Opcodes.LSTORE:
                            instrumentation.add(Utils.liftLocalVariable(var, Opcodes.LLOAD, Opcodes.LSTORE, Type.LONG_TYPE, true));
                            break;
                        case Opcodes.FSTORE:
                            instrumentation.add(Utils.liftLocalVariable(var, Opcodes.FLOAD, Opcodes.FSTORE, Type.FLOAT_TYPE, true));
                            break;
                        case Opcodes.DSTORE:
                            instrumentation.add(Utils.liftLocalVariable(var, Opcodes.DLOAD, Opcodes.DSTORE, Type.DOUBLE_TYPE, true));
                            break;
                        case Opcodes.ASTORE:
                            String descriptor = localVarTypes.get(var);
                            Type t = Type.getType(Object.class);

                            // Check if it's an array type
                            if (descriptor.startsWith("[")) {
                                t = Type.getType(descriptor);
                            } else {
                                switch(descriptor) {
                                    case "Ljava/lang/String;" -> t = Type.getType(String.class);
                                    case "Ljava/lang/Integer;" -> t = Type.getType(Integer.class);
                                    case "Ljava/lang/Boolean;" -> t = Type.getType(Boolean.class);
                                    case "Ljava/lang/Byte;" -> t = Type.getType(Byte.class);
                                    case "Ljava/lang/Character;" -> t = Type.getType(Character.class);
                                    case "Ljava/lang/Short;" -> t = Type.getType(Short.class);
                                    case "Ljava/lang/Long;" -> t = Type.getType(Long.class);
                                    case "Ljava/lang/Float;" -> t = Type.getType(Float.class);
                                    case "Ljava/lang/Double;" -> t = Type.getType(Double.class);
                                    case "Ljava/util/List;" -> t = Type.getType(java.util.List.class);
                                    case "Ljava/util/ArrayList;" -> t = Type.getType(java.util.ArrayList.class);
                                    case "Ljava/util/LinkedList;" -> t = Type.getType(java.util.LinkedList.class);
                                    default -> SWATAssert.enforce(false, "Unsupported type: {}", descriptor);
                                }
                            }

                            if(t.equals(Type.getType(java.util.List.class)) ||
                                    t.equals(Type.getType(java.util.ArrayList.class)) ||
                                    t.equals(Type.getType(java.util.LinkedList.class))) {
                                instrumentation.add(Utils.liftListVariable(var, true, false));
                                break;
                            }
                            instrumentation.add(Utils.liftLocalVariable(var, Opcodes.ALOAD, Opcodes.ASTORE, t, true));
                            break;
                        default:
                            continue;
                    }
                    instructions.insert(insn, instrumentation);
                    symbolicLocalIndices.remove(var);
                }
            }
        }
        accept(mv);
    }


}
