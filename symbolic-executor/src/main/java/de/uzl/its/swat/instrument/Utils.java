package de.uzl.its.swat.instrument;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class Utils implements Opcodes {
    private static final Config config = Config.instance();
    private static final GlobalStateForInstrumentation instrumentationState = GlobalStateForInstrumentation.instance;

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    public static void print(String specifier, String val) {
        System.out.println(specifier + ": " + val);
    }


    public static boolean isBoxedPrimitive(Type type) {
        if(type.equals(Type.getType(Integer.class))) return true;
        if(type.equals(Type.getType(Long.class))) return true;
        if(type.equals(Type.getType(Short.class))) return true;
        if(type.equals(Type.getType(Byte.class))) return true;
        if(type.equals(Type.getType(Float.class))) return true;
        if(type.equals(Type.getType(Double.class))) return true;
        if(type.equals(Type.getType(Character.class))) return true;
        if(type.equals(Type.getType(Boolean.class))) return true;
        return false;
    }

    public static boolean isBoxedPrimitive(String desc){
        SWATAssert.check(desc.startsWith("L") && desc.endsWith(";"), "Descriptor is not a valid object type: {}", desc);
        return switch (desc) {
            case "Ljava/lang/Integer;", "Ljava/lang/Boolean;", "Ljava/lang/Byte;", "Ljava/lang/Character;", "Ljava/lang/Short;", "Ljava/lang/Long;", "Ljava/lang/Float;", "Ljava/lang/Double;" -> true;
            default -> false;
        };
    }

    public static InsnList addUnsupportedCall(){
        InsnList instrumentation = new InsnList();

        instrumentation.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                Type.getInternalName(Intrinsics.class),
                "unsupported",
                "()V",
                false));

        return instrumentation;
    }

    /**
     * Injects code to lift a class parameter to symbolic, reintrumenting the class to turn its class variables to
     * symbolic fields. The class instance is assumed to be on the top of the stack.
     * @param cname the internal name of the class to lift
     */
    public static InsnList liftClass(String cname) {
        // The class instance is assumed to be on the top of the stack.
        // Stack: [ref]
        if(cname.startsWith("L")) cname = cname.substring(1, cname.length() - 1); // Remove leading L and trailing ;
        InsnList instrumentation = new InsnList();
        if (Util.shouldInstrument(cname)) {
            Transformer.retransform(cname, true);

            instrumentation.add(new InsnNode(Opcodes.DUP));
            // Stack: [ref, ref]

            // Call the instrumented lifting method that calls the helper method that was added to the (data) cls
            instrumentation.add(new LdcInsnNode(cname.replace("/", "."))); // This notation is needed here
            instrumentation.add(new LdcInsnNode(config.getInstrumentationPrefix())); // This notation is needed here
            instrumentation.add(new InsnNode(Opcodes.ACONST_NULL));
            instrumentation.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "de/uzl/its/swat/common/UtilInstrumented",
                    "liftClass", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false));
            // Stack: [ref]

        }  else {
            //instrumentation.add(new InsnNode(Opcodes.POP));
            logger.warn("Symbolic parameter with out of scope class: {}", cname);
        }
        // Stack: [ref]
        return instrumentation;
    }


    /**
     * Create a sequence of instructions to inject instrumentation code for lifting a concrete value to symbolic.
     * Note: The value to be lifted is assumed to be on the top of the stack!
     * The sequence of instructions is as follows:
     * <pre>
     *     LDC <i>uid</i>
     *     INVOKESTATIC Intrinsics.injectAssignment(<i>uid</i>, <i>value</i>)
     *     LDC <i>uid</i>
     *     INVOKESTATIC Intrinsics.liftValue(<i>uid</i>, <i>value</i>)
     *
     * @param type The type of the value to be lifted.
     * @return The sequence of instructions.
     */
    public static InsnList createLiftInstrumentation(Type type, boolean addAssignment, boolean prefixOnStack, String prefix) {
        // Note: The value to be lifted (and the prefix) is assumed to be on the top of the stack!
        // [val, (prefix)]
        InsnList list = new InsnList();
        long uid = Intrinsics.getNextUid();
        String desc = String.format("(%sJ)%s", type.getDescriptor(), type.getDescriptor());

        if(prefixOnStack) {
            SWATAssert.check(prefix == null, "Prefix must be null if prefixOnStack is true");
            SWATAssert.check(!addAssignment, "Cannot add assignment if prefix is on stack (not implemented)");
            desc = String.format("(%sLjava/lang/String;J)%s", type.getDescriptor(), type.getDescriptor());
            // [val, prefix]
        } else{
            if(addAssignment) {
                SWATAssert.check(prefix == null, "Prefix must be null if addAssignment is true (not implemented)");
                // [val]
                // Add the unique identifier for the value to be lifted.
                list.add(new LdcInsnNode(uid));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        Type.getInternalName(Intrinsics.class),
                        "injectAssignment",
                        desc,
                        false));
                // [val]
            }
            if(prefix != null) {
                // Add the prefix for the value to be lifted.
                list.add(new LdcInsnNode(prefix));
                desc = String.format("(%sLjava/lang/String;J)%s", type.getDescriptor(), type.getDescriptor());
                // [val, prefix]
            }
        }
        // Add the unique identifier for the value to be lifted.
        list.add(new LdcInsnNode(uid));
        // [val, (prefix), uid]

        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                Type.getInternalName(Intrinsics.class),
                "liftValue",
                desc,
                false));
        // [val]
        return list;
    }


    /**
     * Creates an instruction list that loads a local variable, and lifts it to the symbolic domain,
     * while also checking if an assignment is available for the variable.
     *
     * @param localIndex  the index of the local variable to instrument
     * @param loadOpcode  the opcode used to load the variable
     * @param storeOpcode the opcode used to store the result
     * @param type  the type descriptor of the variable
     * @return an {@code InsnList} containing the instrumentation instructions
     */
    public static InsnList liftLocalVariable(int localIndex, int loadOpcode, int storeOpcode, Type type, boolean addAssignment) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(loadOpcode, localIndex));
        list.add(Utils.createLiftInstrumentation(type, addAssignment, false, null));
        list.add(new VarInsnNode(storeOpcode, localIndex));
        return list;
    }

    /**
     * Creates an instruction list that iterates through a list
     * and lifts each element individually to the symbolic domain.
     * If localIndex is -1 the list is assumed to be on the stack.
     *
     * @param localIndex  the index of the local variable to instrument
     * @param addAssignment whether to add assignment tracking
     * @param leaveOnStack whether to leave the list reference on the stack after instrumentation (true for field lifting, false for parameter initialization)
     * @return an {@code InsnList} containing the instrumentation instructions
     */
    public static InsnList liftListVariable(int localIndex, boolean addAssignment, boolean leaveOnStack) {
        // If the list is on top of the stack, set localIndex to -1
        InsnList list = new InsnList();
        String symPrefix = "List";

        if(localIndex != -1) { // Load the list from local variable
            list.add(new VarInsnNode(Opcodes.ALOAD, localIndex));
        }
        // Stack: [list]

        // Check if list is null
        LabelNode nullLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new JumpInsnNode(Opcodes.IFNULL, nullLabel));
        // Stack: [list]

        list.add(new InsnNode(Opcodes.DUP));
        // Stack: [list, list]

        // Get ListIterator: list.listIterator()
        list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "listIterator", "()Ljava/util/ListIterator;", true));
        // Stack: [list, listIterator]

        // Loop through ListIterator
        LabelNode loopStart = new LabelNode();
        LabelNode loopEnd = new LabelNode();

        list.add(loopStart);

        // Check if iterator has next: listIterator.hasNext()
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/ListIterator", "hasNext", "()Z", true));
        list.add(new JumpInsnNode(Opcodes.IFEQ, loopEnd));
        // Stack: [list, listIterator]

        // Get next element: listIterator.next()
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/ListIterator", "next", "()Ljava/lang/Object;", true));
        // Stack: [list, listIterator, element]

        // Apply lift instrumentation to the element 
        Type elementType = Type.getType(Object.class);
        list.add(Utils.createLiftInstrumentation(elementType, addAssignment, false, symPrefix));
        // Stack: [list, listIterator, liftedElement]

        // Since we track by address, no need to put the element back - just pop the lifted element
        list.add(new InsnNode(Opcodes.POP));
        // Stack: [list, listIterator]

        // Jump back to loop start
        list.add(new JumpInsnNode(Opcodes.GOTO, loopStart));

        // Loop end - pop the listIterator and jump to end
        list.add(loopEnd);
        // Stack: [list, listIterator]
        list.add(new InsnNode(Opcodes.POP));
        // Stack: [list]
        list.add(new JumpInsnNode(Opcodes.GOTO, endLabel));

        list.add(nullLabel);
        // Stack: [list]

        // End label
        list.add(endLabel);
        // Stack: [list]

        if (!leaveOnStack) {
            // For parameter initialization: pop the list to leave empty stack
            list.add(new InsnNode(Opcodes.POP));
            // Stack: []
        }
        // Stack: [list] if leaveOnStack, [] otherwise

        return list;
    }

    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    public static void addBipushInsn(MethodVisitor mv, int val) {
        switch (val) {
            case 0 -> mv.visitInsn(ICONST_0);
            case 1 -> mv.visitInsn(ICONST_1);
            case 2 -> mv.visitInsn(ICONST_2);
            case 3 -> mv.visitInsn(ICONST_3);
            case 4 -> mv.visitInsn(ICONST_4);
            case 5 -> mv.visitInsn(ICONST_5);
            default -> mv.visitLdcInsn(val);
        }
    }

    public static void addJpushInsn(MethodVisitor mv, long val) {
        mv.visitLdcInsn(val);
    }

    public static void addSpecialInsn(MethodVisitor mv, int val) {
        addJpushInsn(mv, instrumentationState.incAndGetId());
        addBipushInsn(mv, val);

        mv.visitMethodInsn(
                INVOKESTATIC, config.getInstrumentationDispatcher(), "SPECIAL", "(JI)V", false);
    }

    /**
     * Add a set to code to read the given type from the top of the concrete stack and invoke
     * GETVALUE method of the analysis class.
     */
    public static void addValueReadInsn(MethodVisitor mv, String desc, String methodNamePrefix) {
        Type t;
        int identifier = 0;

        if (desc.contains(":")) {
            String[] s = desc.split(":");
            SWATAssert.check(s.length == 3, "Unexpected description (Expected \"owner:name:desc\"): {}", desc);
            desc = s[2];
        }

        if (desc.startsWith("(")) {
            t = Type.getReturnType(desc);
        } else {
            t = Type.getType(desc);
        }
        switch (t.getSort()) {
            case Type.DOUBLE -> {
                mv.visitInsn(DUP2);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "double",
                        "(DJI)V",
                        false);
            }
            case Type.LONG -> {
                mv.visitInsn(DUP2);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "long",
                        "(JJI)V",
                        false);
            }
            case Type.ARRAY, Type.OBJECT -> {
                mv.visitInsn(DUP);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "Object",
                        "(Ljava/lang/Object;JI)V",
                        false);
            }
            case Type.BOOLEAN -> {
                mv.visitInsn(DUP);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "boolean",
                        "(ZJI)V",
                        false);
            }
            case Type.BYTE -> {
                mv.visitInsn(DUP);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "byte",
                        "(BJI)V",
                        false);
            }
            case Type.CHAR -> {
                mv.visitInsn(DUP);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "char",
                        "(CJI)V",
                        false);
            }
            case Type.FLOAT -> {
                mv.visitInsn(DUP);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "float",
                        "(FJI)V",
                        false);
            }
            case Type.INT -> {
                mv.visitInsn(DUP);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "int",
                        "(IJI)V",
                        false);
            }
            case Type.SHORT -> {
                mv.visitInsn(DUP);
                addJpushInsn(mv, instrumentationState.incAndGetId());
                addBipushInsn(mv, identifier);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "short",
                        "(SJI)V",
                        false);
            }
            case Type.VOID -> {
                addJpushInsn(mv, instrumentationState.incAndGetId());
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getInstrumentationDispatcher(),
                        methodNamePrefix + "void",
                        "(J)V",
                        false);
            }
            default -> {
                System.err.println("Unknown field or method descriptor " + desc);
                System.exit(1);
            }
        }
    }

    /**
     * Adds either the beginning of the try block or the end of the try block including the catch block.
     * @param mv The method visitor
     * @param begin True if the beginning of the try block should be added, false if the end of the try block should be added
     */
    public static void addTryCatchBlock(MethodVisitor mv, boolean begin, TryCatchBlock tryCatchBlock){
        if (begin) {
            // Add the beginning of the try block
            tryCatchBlock.visit(mv);
            mv.visitLabel(tryCatchBlock.start());
        } else {
            // Add the end of the try block and the catch block
            mv.visitLabel(tryCatchBlock.end());
            mv.visitJumpInsn(Opcodes.GOTO, tryCatchBlock.jumpTarget());
            mv.visitLabel(tryCatchBlock.handler());

            mv.visitInsn(Opcodes.DUP); // The exception object is on the stack

            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
            // Print string unsing system.out
            mv.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "java/lang/System",
                    "out",
                    "Ljava/io/PrintStream;");
            mv.visitLdcInsn("[SWAT] Unrecoverable exception in symbolic execution");
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/io/PrintStream",
                    "println",
                    "(Ljava/lang/String;)V",
                    false);

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;", false);
            mv.visitLdcInsn(1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Runtime", "exit", "(I)V", false);
            mv.visitInsn(Opcodes.ATHROW); // Rethrow the exception. This will never happen due to the exit call above but the verifier does not know that.
            mv.visitLabel(tryCatchBlock.jumpTarget());


        }
    }
}
