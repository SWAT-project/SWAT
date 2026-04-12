package de.uzl.its.swat.instrument.dataendpoint;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.DataType;
import java.util.*;

import de.uzl.its.swat.instrument.Transformer;
import de.uzl.its.swat.instrument.Utils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.InsnList;

import static org.objectweb.asm.Opcodes.ASM9;

/**
 * A visitor to visit a Java method This class visits Spring RestEndpoints and makes all primitive
 * Datatype parameters symbolic and marks Class objects for later instrumenting.
 */
public class SpringRepositoryMethodAdapter extends MethodVisitor {

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    private static final String OWNER_INTERFACE =
            "org.springframework.data.repository.CrudRepository";
    // private static final String OWNER_INTERFACE =
    //         "org.springframework.data.jpa.repository.JpaRepository";
    private static final Map<DataType, String> dataTypeString;
    static {
        dataTypeString =
                Map.ofEntries(
                        Map.entry(DataType.BOOLEAN_TYPE, "Z"),
                        Map.entry(DataType.BYTE_TYPE, "B"),
                        Map.entry(DataType.CHAR_TYPE, "C"),
                        Map.entry(DataType.DOUBLE_TYPE, "D"),
                        Map.entry(DataType.FLOAT_TYPE, "F"),
                        Map.entry(DataType.INTEGER_TYPE, "I"),
                        Map.entry(DataType.LONG_TYPE, "J"),
                        Map.entry(DataType.SHORT_TYPE, "S"),
                        Map.entry(DataType.VOID_TYPE, "V"),
                        Map.entry(DataType.STRING_TYPE, "Ljava/lang/String;"),
                        Map.entry(DataType.OBJECT_TYPE, "Ljava/lang/Object;"));
    }

    // protected int isACustomQuery;
    private LocalVariablesSorter lvs;
    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public SpringRepositoryMethodAdapter(MethodVisitor mv, String name, String desc) {
        super(ASM9, mv);
        lvs = (mv instanceof LocalVariablesSorter) ? (LocalVariablesSorter) mv : null;
    }

    /**
     * We add logic after every method call (except those filtered in the first if condition) to check whether the
     * call queries data from a database by checking the interface inheritance tree of the methods owner.
     * If the owner inherits from the interface specified as string in OWNER_INTERFACE, liftValue or
     * liftClass will be called on the return of the INVOKE... instruction.
     *
     * @param opcode
     * @param owner
     * @param name
     * @param descriptor
     * @param isInterface
     */
    @Override
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {

        Type returnType;
        returnType = Type.getReturnType(descriptor);

        // For certain owners or return type we do not want or we do not need to add the lift logic
        if (!owner.endsWith("SpringDataMethodAdapter")
                && !owner.startsWith("java/lang")
                && !owner.startsWith("de/uzl/its/swat")
                && !(returnType.equals(Type.VOID_TYPE)
                || (returnType.getSort() == Type.ARRAY)
                || (returnType.getSort() == Type.METHOD))) {

            String returnTypeString;

            try {
                returnTypeString = returnType.getDescriptor();
            } catch (IndexOutOfBoundsException e) {
                // TODO
                returnTypeString = "V";
            }

            int arrayLengthIdx = lvs.newLocal(Type.INT_TYPE);
            int loopCounterIdx = lvs.newLocal(Type.INT_TYPE);
            int interfacesIdx = lvs.newLocal(Type.getReturnType("()[Ljava/lang/Class;"));
            int classNameIdx = lvs.newLocal(Type.getType(String.class));
            int classNameIdx2 = lvs.newLocal(Type.getType(String.class));
            int retVarIdx = lvs.newLocal(returnType);

            DataType returnDataType = DataType.getDataType(returnTypeString);

            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

            visitVarInsn(returnType.getOpcode(Opcodes.ISTORE), retVarIdx);
            visitVarInsn(returnType.getOpcode(Opcodes.ILOAD), retVarIdx);

            String crudInterface = OWNER_INTERFACE;
            Label loop = new Label();
            Label callMakeSymbolic = new Label();
            Label done = new Label();

            visitLdcInsn(owner.replace("/", "."));

            // First we retrieve the java.lang.Class of the method's owner object
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Class",
                    "forName",
                    "(Ljava/lang/String;)Ljava/lang/Class;",
                    false);

            visitVarInsn(Opcodes.ASTORE, classNameIdx);
            visitVarInsn(Opcodes.ALOAD, classNameIdx);

            // With the class object, we can get the interface inheritance tree ...
            // ToDo (Issue 92): Performance optimization: Cache result of for loop
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "de/uzl/its/swat/common/Util",
                    "getAllInterfaces",
                    "(Ljava/lang/Class;)[Ljava/lang/Class;",
                    false);
            visitVarInsn(Opcodes.ASTORE, interfacesIdx);
            visitVarInsn(Opcodes.ALOAD, interfacesIdx);
            // ... and iterate over all interface to compare them against OWNER_INTERFACE
            visitInsn(Opcodes.ARRAYLENGTH);
            visitVarInsn(Opcodes.ISTORE, arrayLengthIdx);
            visitVarInsn(Opcodes.ILOAD, arrayLengthIdx);
            visitJumpInsn(Opcodes.IFEQ, done);
            visitLdcInsn(0);
            visitVarInsn(Opcodes.ISTORE, loopCounterIdx);
            visitLabel(loop);
            visitVarInsn(Opcodes.ALOAD, interfacesIdx);
            visitVarInsn(Opcodes.ILOAD, loopCounterIdx);
            visitInsn(Opcodes.AALOAD);
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getName",
                    "()Ljava/lang/String;",
                    false);
            visitVarInsn(Opcodes.ASTORE, classNameIdx2);
            visitVarInsn(Opcodes.ALOAD, classNameIdx2);
            visitLdcInsn(crudInterface);
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "equals",
                    "(Ljava/lang/Object;)Z",
                    false);
            // If one interface has the correct name (meaning equals does not return 0), we branch to the makeSymbolic
            // call.
            visitJumpInsn(Opcodes.IFNE, callMakeSymbolic);
            // Otherwise we continue with the loop
            visitIincInsn(loopCounterIdx, 1);
            visitVarInsn(Opcodes.ILOAD, loopCounterIdx);
            visitVarInsn(Opcodes.ILOAD, arrayLengthIdx);
            // if we iterated over all interfaces and none matched, we go to done and do not make anything symbolic
            visitJumpInsn(Opcodes.IF_ICMPGE, done);
            visitJumpInsn(Opcodes.GOTO, loop);
            visitLabel(callMakeSymbolic);
            makeSymbolic(returnDataType, Type.getReturnType(descriptor), returnTypeString);
            visitLabel(done);
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    private void makeSymbolic(DataType returnDataType, Type returnType, String returnTypeDescriptor) {
        // ToDo: Either invoke DJVM calls directly or separate handling of MakeSymbolic and
        //          the like in the
        //          backend to be added by another transformer
        if (dataTypeString.containsKey(returnDataType)) {
            handleReturnType(returnDataType, returnType, returnTypeDescriptor, "db");
        } else {
            SpringRepositoryTransformer.getPrintBox().addMsg("No string mapping for key (while making database return symbolic): "
                    + returnDataType);
        }
    }


    protected void handleReturnType(DataType returnParameterType, Type returnType, String returnTypeDescriptor, String symbolicVarPrefix) {

        if (returnParameterType == DataType.BYTE_TYPE
                || returnParameterType == DataType.CHAR_TYPE
                || returnParameterType == DataType.DOUBLE_TYPE
                || returnParameterType == DataType.FLOAT_TYPE
                || returnParameterType == DataType.INTEGER_TYPE
                || returnParameterType == DataType.LONG_TYPE
                || returnParameterType == DataType.SHORT_TYPE
                || returnParameterType == DataType.STRING_TYPE
                || returnParameterType == DataType.BOOLEAN_TYPE) {

            InsnList list = Utils.createLiftInstrumentation(returnType, false, false, symbolicVarPrefix);
            list.accept(mv);

        } else if (returnParameterType == DataType.OBJECT_TYPE) {
            String returnTypeDescriptorCleaned = returnTypeDescriptor.substring(1, returnTypeDescriptor.length() - 1);
            System.out.println("Return type: " + returnTypeDescriptorCleaned);
            if (Util.shouldInstrument(returnTypeDescriptorCleaned)) {

                Transformer.retransform(returnTypeDescriptorCleaned, true);
                mv.visitInsn(Opcodes.DUP);

                String returnTypeDescriptorDot = returnTypeDescriptor.substring(1, returnTypeDescriptor.length() - 1).replace("/", ".");
                visitLdcInsn(returnTypeDescriptorDot);
                visitLdcInsn(Config.instance().getInstrumentationPrefix());
                visitLdcInsn(symbolicVarPrefix);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/common/UtilInstrumented",
                        "liftClass", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                        false);
            } else {
                logger.warn("Unknown return Type {}", returnTypeDescriptor);
            }
        } else {
            logger.warn("Unknown DataType in return value with type {}", dataTypeString.get(returnParameterType));
        }
    }
}
