package de.uzl.its.swat.instrument;

import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.config.Config;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.LoggerFactory;

@Setter
@Getter
public abstract class AbstractMethodAdapter extends MethodVisitor {

    private static final org.slf4j.Logger logger =
            LoggerFactory.getLogger(AbstractMethodAdapter.class);

    private static final String METHOD_DESC_REGEX = "\\((.*?)\\)";
    private static final char ARRAY = '[';
    private static final char OBJECT = 'L';
    private static final char METHOD = '(';
    private final String desc;
    private final String name;
    private final Config config = Config.instance();
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
     * Adds the invocation to the method responsible for making a primitive parameter symbolic
     *
     * @param paramIdx The index to the parameter
     * @param loadOpcode the integer representing the required Opcode for loading the parameter
     *     (from class Opcodes)
     * @param storeOpcode the integer representing the required Opcode for storing the parameter
     *     (from class Opcodes)
     * @param type The description od the datatype that is loaded
     */
    private void symbolicParameter(int paramIdx, int loadOpcode, int storeOpcode, String type) {
        String owner = "de/uzl/its/swat/Main";
        String method = "MakeSymbolic";
        String desc = "(" + type + ")" + type;
        visitVarInsn(loadOpcode, paramIdx);
        visitMethodInsn(Opcodes.INVOKESTATIC, owner, method, desc, false);
        visitVarInsn(storeOpcode, paramIdx);
    }

    public void symbolicInt(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, "I");
    }

    public void symbolicByte(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, "B");
    }

    public void symbolicShort(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, "S");
    }

    public void symbolicLong(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.LLOAD, Opcodes.LSTORE, "J");
    }

    public void symbolicFloat(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.FLOAD, Opcodes.FSTORE, "F");
    }

    public void symbolicDouble(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.DLOAD, Opcodes.DSTORE, "D");
    }

    public void symbolicBoolean(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, "Z");
    }

    public void symbolicChar(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.ILOAD, Opcodes.ISTORE, "C");
    }

    public void symbolicString(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.ALOAD, Opcodes.ASTORE, "Ljava/lang/String;");
    }

    public void symbolicLongObject(int paramIdx) {
        symbolicParameter(paramIdx, Opcodes.ALOAD, Opcodes.ASTORE, "Ljava/lang/Long;");
    }

    public void handleMethodParameters(int access, PrintBox printBox) {
        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        int paramIdx = isStatic ? 0 : 1;

        ArrayList<String> parameters = splitParameters(desc);
        for (String param : parameters) {
            printBox.addMsg("    => Parameter #" + paramIdx + ": " + param);
            printBox.setContentPresent(true);
            DataType type = DataType.getDataType(param);
            switch (type) {
                case INTEGER_TYPE:
                    symbolicInt(paramIdx);
                    paramIdx++;
                    break;
                case STRING_TYPE:
                    symbolicString(paramIdx);
                    paramIdx++;
                    break;
                case BOOLEAN_TYPE:
                    symbolicBoolean(paramIdx);
                    paramIdx++;
                    break;
                case CHAR_TYPE:
                    symbolicChar(paramIdx);
                    paramIdx++;
                    break;
                case BYTE_TYPE:
                    symbolicByte(paramIdx);
                    paramIdx++;
                    break;
                case SHORT_TYPE:
                    symbolicShort(paramIdx);
                    paramIdx++;
                    break;
                case FLOAT_TYPE:
                    symbolicFloat(paramIdx);
                    paramIdx++;
                    break;
                case LONG_TYPE:
                    symbolicLong(paramIdx);
                    paramIdx += 2;
                    break;
                case DOUBLE_TYPE:
                    symbolicDouble(paramIdx);
                    paramIdx++;
                    break;
                case OBJECT_TYPE:
                    handleObjectType(param, paramIdx);
                    paramIdx++;
                    break;
                default:
                    logger.warn(
                            String.format(
                                    "Unknown DataType in Parameter #%d -> %s", paramIdx, param));
                    paramIdx++;
            }
        }
    }

    private void handleObjectType(String param, int paramIdx) {
        if (Transformer.shouldInstrument(param.substring(1))) {
            Transformer.retransform(param);
            visitVarInsn(Opcodes.ALOAD, paramIdx);
            visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    param.substring(1),
                    config.getInstrumentationPrefix(),
                    "()V",
                    false);
        } else if (param.equals("Ljava/lang/Long")) {
            symbolicLongObject(paramIdx);
        } else {
            logger.warn(String.format("Unknown DataType in Parameter #%d -> %s", paramIdx, param));
        }
    }
}
