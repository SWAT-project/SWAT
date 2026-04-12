package de.uzl.its.swat.instrument;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.springendpoint.SpringEndpointTransformer;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

@Setter
@Getter
public abstract class AbstractMethodAdapter extends MethodVisitor {

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private static final String METHOD_DESC_REGEX = "\\((.*?)\\)";
    private static final char ARRAY = '[';
    private static final char OBJECT = 'L';
    private static final char METHOD = '(';
    private final String desc;
    private final String name;
    private final Config config = Config.instance();

    protected static final Map<DataType, String> dataTypeString;
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

    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public AbstractMethodAdapter(MethodVisitor mv, String name, String desc) {
        super(Opcodes.ASM9, mv);
        this.desc = desc;
        this.name = name;
    }

    protected boolean shouldInstrument(String cname, String name) {
        switch (config.getInstrumentationTransformer()) {
            case NONE, WEB_SERVLET -> {
                return false;
            }
            case SV_COMP -> {
                return name.equals("main");
            }
            case ANNOTATION -> {
                String pattern =
                        config.getInstrumentationAnnotationSymbolicMethodName() == null ?
                                "main" :
                                config.getInstrumentationAnnotationSymbolicMethodName();
                return Pattern.matches(pattern, name);
            }
            case PARAMETER -> {
                return Util.isSymbolicMethod(cname, name);
            }
            case SPRING_ENDPOINT -> {
                return SpringEndpointTransformer.getInstrumentedEndpoints()
                        .contains(cname + ":" + name);
            }
        }
        return false;
    }

    /**
     * Given the string description of parameters of type (arg1arg2..argn)ret1 this method extracts
     * the parameters
     *
     * @param desc The method description in standard format
     * @return An ArrayList of parametes ([arg1, arg2, .., argn])
     */
    protected ArrayList<String> splitParameters(String desc) {
        ArrayList<String> params = new ArrayList<>();
        Matcher matcher = Pattern.compile(METHOD_DESC_REGEX).matcher(desc);
        if (matcher.find()) {
            desc = matcher.group(1);
            int i = 0;
            while (i < desc.length()) {
                StringBuilder param = new StringBuilder();
                i = parseParam(desc, i, param);
                params.add(param.toString());
            }
        }
        return params;
    }

    /**
     * Parses a single parameter from the description
     *
     * @param desc The description of the method in standard format
     * @param start The start index of the parameter that should be extracted
     * @param param A stringbuilder, to which the paramter will be written
     * @return The offset where the next parameter begins
     */
    private int parseParam(String desc, int start, StringBuilder param) {
        int i = start;
        while (desc.charAt(i) == ARRAY) {
            param.append(ARRAY);
            i++;
        }
        if (desc.charAt(i) == OBJECT) {
            int end = desc.indexOf(";", i);
            param.append(desc, i, end + 1);
            return end + 1;
        } else if (desc.charAt(i) == METHOD) {
            int end = desc.indexOf(")", i);
            param.append(desc, i, end + 1);
            return end + 1;
        } else {
            param.append(desc.charAt(i));
            return i + 1;
        }
    }

    /**
     * Handles the instrumentation of method parameters and prevents interning.
     * This method needs te be called first (at the beginning of visitCode) to prevent interned values
     *
     *
     * @param access The access flags of the method
     * @param printBox A PrintBox to log messages
     * @param addAssignment Whether to add an assignment to a fresh variable
     */
    public void handleMethodParameters(int access, PrintBox printBox, boolean addAssignment) {
        instrumentEntryPointParameters(access, desc);

        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        int paramIdx = isStatic ? 0 : 1;

        ArrayList<String> parameters = splitParameters(desc);
        for (String param : parameters) {
            DataType type = DataType.getDataType(param);
            InsnList paramInstrumentation = new InsnList();
            boolean isSymbolic = false;
            switch (type) {
                case INTEGER_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, Type.INT_TYPE, addAssignment));
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case BOOLEAN_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, Type.BOOLEAN_TYPE, addAssignment));
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case BYTE_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, Type.BYTE_TYPE, addAssignment));
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case CHAR_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, Type.CHAR_TYPE, addAssignment));
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case SHORT_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, Type.SHORT_TYPE, addAssignment));
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case LONG_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.LLOAD, Opcodes.LSTORE, Type.LONG_TYPE, addAssignment));
                    paramIdx += 2;
                    isSymbolic = true;
                    break;
                case FLOAT_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.FLOAD, Opcodes.FSTORE, Type.FLOAT_TYPE, addAssignment));
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case DOUBLE_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.DLOAD, Opcodes.DSTORE, Type.DOUBLE_TYPE, addAssignment));
                    paramIdx += 2;
                    isSymbolic = true;
                    break;
                case STRING_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ALOAD, Opcodes.ASTORE, Type.getType(DataType.STRING_TYPE.getIdentifier()), addAssignment)); //
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case OBJECT_TYPE:
                    Type t = switch (param) { // Todo: Use Type.getObjectType () also in AnnotationMethodNode -> Type.getObjectType(param.substring(1, param.length() - 1));
                        case "Ljava/lang/Integer;" -> Type.getType(Integer.class);
                        case "Ljava/lang/Boolean;" -> Type.getType(Boolean.class);
                        case "Ljava/lang/Byte;" -> Type.getType(Byte.class);
                        case "Ljava/lang/Character;" -> Type.getType(Character.class);
                        case "Ljava/lang/Short;" -> Type.getType(Short.class);
                        case "Ljava/lang/Long;" -> Type.getType(Long.class);
                        case "Ljava/lang/Float;" -> Type.getType(Float.class);
                        case "Ljava/lang/Double;" -> Type.getType(Double.class);
                        default -> null;
                    };
                    if (t != null) {
                        // Todo: How to handle assignment of objects?
                        paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ALOAD, Opcodes.ASTORE, t, false));
                        isSymbolic = true;
                    } else {
                        handleObjectType(param, paramIdx); //Todo also provide InsnList?
                    }
                    paramIdx++;
                    break;
                case LIST_TYPE:
                case ARRAYLIST_TYPE:
                case LINKEDLIST_TYPE:
                    handleListType(param, paramIdx, addAssignment);
                    paramIdx++;
                    isSymbolic = true;
                    break;
                case ARRAY_TYPE:
                    paramInstrumentation.add(Utils.liftLocalVariable(paramIdx, Opcodes.ALOAD, Opcodes.ASTORE, Type.getType(param), addAssignment)); //
                    paramIdx++;
                    isSymbolic = true;
                    break;

                default:
                    logger.warn("Unknown DataType in Parameter #{} -> {}", paramIdx, param);
                    paramIdx++;
            }
            if (isSymbolic){
                printBox.addMsg("    => Parameter #" + paramIdx + " -> symbolic uid " + (Intrinsics.peekCurrentUid() - 1) + ": " + type + " (" + param + ")");

            } else {
                printBox.addMsg("    => Parameter #" + paramIdx + ": " + type + " (" + param + ")");
            }
            printBox.setContentPresent(true);
            if (paramInstrumentation.size() > 0) {
                paramInstrumentation.accept(this);
            }
        }
    }

    private void handleObjectType(String param, int paramIdx) {
        String paramCname = param.substring(1, param.length() - 1);
        if (Util.shouldInstrument(paramCname)) {
            Transformer.retransform(paramCname, true);
            visitVarInsn(Opcodes.ALOAD, paramIdx);
            // visitMethodInsn(
            //         Opcodes.INVOKEVIRTUAL,
            //         paramCname,
            //         config.getInstrumentationPrefix(),
            //         "()V",
            //         false);
            String paramCnameDot = paramCname.replace("/", ".");
            visitLdcInsn(paramCnameDot);
            visitLdcInsn(config.getInstrumentationPrefix());
            visitInsn(Opcodes.ACONST_NULL);
            visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/common/UtilInstrumented",
                    "liftClass", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false);

        }  else {
            logger.warn("Unknown DataType in Parameter #{} -> {}", paramIdx, param);
        }
    }

    private void handleListType(String param, int paramIdx, boolean addAssignment) {
        logger.info("[DSE] Handling List parameter #{} -> {}", paramIdx, param);

        // Create instrumentation for the list itself
        InsnList listInstrumentation = Utils.liftListVariable(paramIdx, addAssignment, false);

        // Apply the instrumentation
        listInstrumentation.accept(this);
    }

    private void instrumentEntryPointParameters(int access, String descriptor) {
        // This needs to be called right at method entry to prevent interned values
        // from being used in symbolic execution
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        int localIndex = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1; // Skip 'this' if non-static

        for (Type argType : argTypes) {
            if (needsFreshCopy(argType)) {
                createFreshParameterCopy(argType, localIndex);
            }
            localIndex += argType.getSize();
        }
    }
    private boolean needsFreshCopy(Type type) {
        String typeName = type.getClassName();
        return typeName.equals("java.lang.String") ||
                typeName.equals("java.lang.Integer") ||
                typeName.equals("java.lang.Long") ||
                typeName.equals("java.lang.Short") ||
                typeName.equals("java.lang.Byte") ||
                typeName.equals("java.lang.Character") ||
                typeName.equals("java.lang.Boolean");
    }


    private void createFreshParameterCopy(Type type, int localIndex) {
        String typeName = type.getInternalName();

        if (type.getClassName().equals("java.lang.String")) {
            // Create fresh copy: new String(original)
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
            mv.visitInsn(Opcodes.DUP);
            // Load original parameter
            mv.visitVarInsn(Opcodes.ALOAD, localIndex);
            // Stack: new_string, new_string, original
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>",
                    "(Ljava/lang/String;)V", false);
            // Store back to parameter slot
            mv.visitVarInsn(Opcodes.ASTORE, localIndex);
        } else {
            // For wrapper types, create fresh instances
            createFreshWrapperCopy(type, localIndex);
        }
    }

    private void createFreshWrapperCopy(Type type, int localIndex) {
        String className = type.getInternalName();
        String primitiveType = getPrimitiveTypeDescriptor(type);

        // Create new wrapper instance first
        mv.visitTypeInsn(Opcodes.NEW, className);
        mv.visitInsn(Opcodes.DUP);
        // Stack: [new_wrapper, new_wrapper]

        // Load original wrapper and extract primitive value
        mv.visitVarInsn(Opcodes.ALOAD, localIndex);
        String valueMethod = getPrimitiveValueMethod(type);
        String valueDescriptor = "()" + primitiveType;
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, valueMethod, valueDescriptor, false);
        // Stack: [new_wrapper, new_wrapper, primitive_value]

        // Call constructor
        String constructorDesc = "(" + primitiveType + ")V";
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", constructorDesc, false);
        // Stack: [new_wrapper] (initialized)

        // Store back to parameter
        mv.visitVarInsn(Opcodes.ASTORE, localIndex);
    }

    private String getPrimitiveTypeDescriptor(Type type) {
        String className = type.getClassName();
        switch (className) {
            case "java.lang.Integer": return "I";
            case "java.lang.Long": return "J";
            case "java.lang.Short": return "S";
            case "java.lang.Byte": return "B";
            case "java.lang.Character": return "C";
            case "java.lang.Boolean": return "Z";
            case "java.lang.Float": return "F";
            case "java.lang.Double": return "D";
            default: throw new IllegalArgumentException("Unsupported type: " + className);
        }
    }

    private String getPrimitiveValueMethod(Type type) {
        String className = type.getClassName();
        switch (className) {
            case "java.lang.Integer": return "intValue";
            case "java.lang.Long": return "longValue";
            case "java.lang.Short": return "shortValue";
            case "java.lang.Byte": return "byteValue";
            case "java.lang.Character": return "charValue";
            case "java.lang.Boolean": return "booleanValue";
            case "java.lang.Float": return "floatValue";
            case "java.lang.Double": return "doubleValue";
            default: throw new IllegalArgumentException("Unsupported type: " + className);
        }
    }
}
