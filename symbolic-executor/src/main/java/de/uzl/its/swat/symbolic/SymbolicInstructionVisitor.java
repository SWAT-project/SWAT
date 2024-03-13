package de.uzl.its.swat.symbolic;

import static de.uzl.its.swat.symbolic.value.reference.ObjectValue.ADDRESS_UNKNOWN;

import de.uzl.its.swat.Main;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.symbolic.instruction.*;
import de.uzl.its.swat.symbolic.invoke.DynamicInvocation;
import de.uzl.its.swat.symbolic.invoke.StaticInvocation;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.ValueFactory;
import de.uzl.its.swat.symbolic.value.ValueType;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.swat.symbolic.value.reference.LambdaObjectValue;
import de.uzl.its.swat.symbolic.value.reference.LambdaPlaceHolder;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.array.*;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import lombok.Getter;
import lombok.SneakyThrows;
import org.objectweb.asm.Type;
import org.slf4j.LoggerFactory;
import org.sosy_lab.java_smt.api.*;

public class SymbolicInstructionVisitor implements IVisitor {
    @Getter private final Stack<Frame> stack;
    private final HashMap<Integer, Integer> iidCounter = new HashMap<>();
    private final ClassNames classNames;
    private final Map<Integer, Value<?, ?>> objects;
    @Getter private final SymbolicTraceHandler symbolicStateHandler;
    Map<Integer, Frame> lambdaFrameStore = new HashMap<>();
    @Getter private Frame currentFrame;
    private Instruction next;

    private static final org.slf4j.Logger logger =
            LoggerFactory.getLogger(SymbolicInstructionVisitor.class);

    public SymbolicInstructionVisitor(ClassNames classNames) {
        stack = new Stack<>();

        stack.add(currentFrame = new Frame("Initial Frame", "",0));
        this.classNames = classNames;
        objects = new HashMap<>();
        symbolicStateHandler = new SymbolicTraceHandler();
    }

    private void checkAndSetException(Instruction inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), inst.getClass().getName());
        if ((!(next instanceof SPECIAL) || ((SPECIAL) next).i != 0)) {
            if (!(inst instanceof NEW && next instanceof SPECIAL && ((SPECIAL) next).i == 2)) {
                currentFrame.clear();
                currentFrame.push(PlaceHolder.instance);
                // TODO proper handling | issue #14
                // the problem is that if a ,cinit. so a static call is here its init
                // instrumentation is
                // becoming the next instruction and not the special(0)
            }
        }
    }

    private void enforceException(Instruction unused) {
        currentFrame.clear();
        currentFrame.push(PlaceHolder.instance);
    }

    /**
     * Is used to see if a branch was taken when a branching condition is encountered ToDo (Nils):
     * Is the check correct? Why would i return true if the next is not special?
     *
     * @return True if branch was taken, false otherwise
     */
    private boolean isBranchTaken() {
        // The false branch does not have the special instruction.
        if (next instanceof SPECIAL special) {
            // See InstructionMethodAdapter,
            // 1 corresponds to the true branch.
            return special.i != 1;
            // return (special).i == 1;
        }
        return true; // false;
    }

    public boolean checkArrayBounds(Value<?, ?> ref, IntValue idx, int iid) {
        assert ref instanceof AbstractArrayValue || ref instanceof ObjectArrayValue
                : "Unknown array type!";
        if (ref instanceof AbstractArrayValue<?, ?, ?, ?, ?> arr) {
            BooleanFormula constraint = arr.checkIndex(idx);
            boolean result = 0 <= idx.concrete && idx.concrete < arr.size.concrete;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(iid));
            return result;
        } else {
            ObjectArrayValue arr = (ObjectArrayValue) ref;
            BooleanFormula constraint = arr.checkIndex(idx);
            boolean result = 0 <= idx.concrete && idx.concrete < arr.size.concrete;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(iid));
            return result;
        }
    }
    /**
     * Loads a reference from an array and stores the reference on the symbolic stack ToDo (Nils):
     * Reference arrays are not symbolically tracked currently See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/58">...</a>
     *
     * @param inst The AALOAD instruction
     */
    public void visitAALOAD(AALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof ObjectArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    currentFrame.push(arr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else if (ref instanceof ArrayArrayValue<?> arr) {
                currentFrame.push(arr.getElement(idx));

            } else if (ref instanceof StringArrayValue arr) {
                currentFrame.push(arr.getElement(idx));

            } else {
                currentFrame.push(
                        ref.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Stores a reference value into an array
     *
     * @param inst The ASTORE instruction
     */
    public void visitAASTORE(AASTORE inst) {
        try {
            ObjectValue val = currentFrame.pop().asObjectValue();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof ObjectArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException(inst);
                }
            } else if (ref instanceof ArrayArrayValue arr
                    && val instanceof AbstractArrayValue val2) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val2);
                } else {
                    enforceException(inst);
                }
            } else if (ref instanceof StringArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, (StringValue) val);
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[AASTORE]: Unknown array type: " + ref.getClass().getSimpleName());
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Pushes NULL onto the symbolic stack
     *
     * @param inst The ACONST_NULL instruction
     */
    public void visitACONST_NULL(ACONST_NULL inst) {
        currentFrame.push(ValueFactory.createNULLValue());
    }

    /**
     * Fetches a reference from the symbolic locals and puts it onto the symbolic stack
     *
     * @param inst One on the ALOAD (ALOAD, ALOAD_0 - ALOAD_3) instructions
     */
    public void visitALOAD(ALOAD inst) {
        currentFrame.push(currentFrame.getLocal(inst.var));
    }

    /**
     * Creates a new array of references and puts the array onto the symbolic stack.
     *
     * @param inst The ANEWARRAY instruction.
     */
    public void visitANEWARRAY(ANEWARRAY inst) {
        try {
            IntValue size = currentFrame.pop().asIntValue();
            BooleanFormula constraint = size.checkPositive();
            boolean result = size.concrete >= 0;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            currentFrame.push(ValueFactory.createObjectArrayValue(inst.type, size));
        } catch (Exception e) {
            throwError(inst, e);
        }
        checkAndSetException(inst);
    }

    /**
     * Takes the top value (has to be a reference) from the symbolic stack and sets it as the
     * symbolic return value
     *
     * @param inst The ARETURN instruction
     */
    public void visitARETURN(ARETURN inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "ARETURN");
        currentFrame.setRet(currentFrame.pop());
        // checkAndSetException(inst);
    }

    /**
     * Determines the size of an array and puts it onto the symbolic stack INFO: While the length is
     * tracked symbolically it is currently unclear to what effect that is useful See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/55">Issue</a>
     *
     * @param inst The ARRAYLENGTH instruction
     */
    public void visitARRAYLENGTH(ARRAYLENGTH inst) {
        Value<?, ?> v = currentFrame.pop();
        if (v instanceof AbstractArrayValue<?, ?, ?, ?, ?> arr) {
            currentFrame.push(arr.size);
        } else if (v instanceof ObjectArrayValue arr) {
            currentFrame.push(arr.getSize());
        } else if (v instanceof ObjectValue<?, ?> ref) {
            if (ref.getFields() != null) {
                currentFrame.push(ref.getnFields());
            } else {
                currentFrame.push(PlaceHolder.instance);
            }

        } else {
            throw new RuntimeException(
                    "Cannot determine the array length of a non array object: " + v);
        }
        checkAndSetException(inst);
    }

    /**
     * Stores the top reference from the symbolic stack into the symbolic locals. The index of the
     * locals position is indicated by the instruction and not taken from the stack
     *
     * @param inst One of the ASTORE (ASTORE, ASTORE_0 - ASTORE_3) instructions. inst.var is the
     *     locals index
     */
    public void visitASTORE(ASTORE inst) {
        currentFrame.setLocal(inst.var, currentFrame.pop());
    }

    /**
     * Throws an error or exception in the symbolic frame. The symbolic stack is cleared and only a
     * reference to the exception remains on the symbolic stack. ToDo (Nils): Could some information
     * be saved here to record that an exception has occurred? See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/57">...</a>
     *
     * @param inst The ATHROW instruction
     */
    public void visitATHROW(ATHROW inst) {
        Value<?, ?> top = currentFrame.peek();
        currentFrame.clear();
        currentFrame.push(top);
        checkAndSetException(inst);
    }

    /**
     * Retrieves a boolean or byte from an array and puts it onto the symbolic stack
     *
     * @param inst The BALOAD instruction.
     */
    public void visitBALOAD(BALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) currentFrame.pop();
            if (arr instanceof BooleanArrayValue barr) {
                boolean result = checkArrayBounds(barr, idx, inst.iid);
                if (result) {
                    currentFrame.push(barr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[BALOAD]: Unknown array type");
                currentFrame.push(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Stores a boolean or byte into an array
     *
     * @param inst The BASTORE instruction.
     */
    public void visitBASTORE(BASTORE inst) {
        try {
            Value<?, ?> val = currentFrame.pop();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof BooleanArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val.asBooleanValue());
                } else {
                    enforceException(inst);
                }
            } else if (ref instanceof ByteArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val.asByteValue());
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[BASTORE]: Unknown array type");
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Pushes a byte onto the symbolic stack as an integer
     *
     * @param inst The BIPUSH instruction
     */
    public void visitBIPUSH(BIPUSH inst) {
        // ToDo (Nils): Some unforseen concequences because the byte is handled as an int?
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, inst.value));
    }

    /**
     * Retrieves a char from an array and puts it onto the symbolic stack
     *
     * @param inst The CALOAD instruction.
     */
    public void visitCALOAD(CALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) currentFrame.pop();
            if (arr instanceof CharArrayValue carr) {
                boolean result = checkArrayBounds(carr, idx, inst.iid);
                if (result) {
                    currentFrame.push(carr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[CALOAD]: Unknown array type");
                currentFrame.push(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Stores a char into an array
     *
     * @param inst The CASTORE instruction.
     */
    public void visitCASTORE(CASTORE inst) {
        try {
            CharValue val = currentFrame.pop().asCharValue();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof CharArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[CASTORE]: Unknown array type");
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * No symbolic tracking needed here. Exceptions in case the object cannot be cast need to be
     * caught.
     *
     * @param inst The CHECKCAST instruction
     */
    public void visitCHECKCAST(CHECKCAST inst) {
        checkAndSetException(inst);
    }

    /**
     * Casts a double value to a float and puts the result onto the symbolic stack
     *
     * @param inst The D2F instruction
     */
    public void visitD2F(D2F inst) {
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push(d1.D2F());
    }

    /**
     * Casts a double to an integer and puts the int onto the symbolic stack
     *
     * @param inst The D2I instruction
     */
    public void visitD2I(D2I inst) {
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push(d1.D2I());
    }

    /**
     * Casts a double to a long and puts the long onto the symbolic stack
     *
     * @param inst The D2L instruction
     */
    public void visitD2L(D2L inst) {
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push2(d1.D2L());
    }

    /**
     * Adds two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DADD instruction
     */
    public void visitDADD(DADD inst) {
        DoubleValue d2 = (DoubleValue) currentFrame.pop2();
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push2(d1.DADD(d2));
    }

    /**
     * Retrieves a double from an array and puts it onto the symbolic stack
     *
     * @param inst The DALOAD instruction.
     */
    public void visitDALOAD(DALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) currentFrame.pop();
            if (arr instanceof DoubleArrayValue darr) {
                boolean result = checkArrayBounds(darr, idx, inst.iid);
                if (result) {
                    currentFrame.push2(darr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[DALOAD]: Unknown array type");
                currentFrame.push(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Stores a double into an array
     *
     * @param inst The DASTORE instruction.
     */
    public void visitDASTORE(DASTORE inst) {
        try {
            DoubleValue val = currentFrame.pop2().asDoubleValue();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof DoubleArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[DASTORE]: Unknown array type");
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Compares two doubles and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The DCMPG instruction
     */
    public void visitDCMPG(DCMPG inst) {
        DoubleValue d2 = currentFrame.pop2().asDoubleValue();
        DoubleValue d1 = currentFrame.pop2().asDoubleValue();
        currentFrame.push(d1.DCMPG(d2));
    }

    /**
     * Compares two doubles and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The DCMPL instruction
     */
    public void visitDCMPL(DCMPL inst) {
        DoubleValue d2 = currentFrame.pop2().asDoubleValue();
        DoubleValue d1 = currentFrame.pop2().asDoubleValue();
        currentFrame.push(d1.DCMPL(d2));
    }

    /**
     * Loads the constant 0.0 onto the symbolic stack
     *
     * @param inst The DCONST_0 instruction
     */
    public void visitDCONST_0(DCONST_0 inst) {
        currentFrame.push2(ValueFactory.createNumericalValue(ValueType.doubleValue, 0.0));
    }

    /**
     * Loads the constant 1.0 onto the symbolic stack
     *
     * @param inst The DCONST_1 instruction
     */
    public void visitDCONST_1(DCONST_1 inst) {
        currentFrame.push2(ValueFactory.createNumericalValue(ValueType.doubleValue, 1.0));
    }

    /**
     * Divides two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DDIV instruction
     */
    public void visitDDIV(DDIV inst) {
        // ToDo (Nils):  What if one of the values is zero?
        DoubleValue d2 = (DoubleValue) currentFrame.pop2();
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push2(d1.DDIV(d2));
    }

    /**
     * Loads a double from the symbolic locals onto the symbolic stack.
     *
     * @param inst One of the DLOAD instructions (DLOAD, DLOAD_0 - DLOAD_3)
     */
    public void visitDLOAD(DLOAD inst) {
        currentFrame.push2(currentFrame.getLocal2(inst.var));
    }

    /**
     * Multiplies two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DMUL instruction
     */
    public void visitDMUL(DMUL inst) {
        DoubleValue d2 = (DoubleValue) currentFrame.pop2();
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push2(d1.DMUL(d2));
    }

    /**
     * Negates a double and pushes the result onto the symbolic stack
     *
     * @param inst The DNEG instruction
     */
    public void visitDNEG(DNEG inst) {
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push2(d1.DNEG());
    }

    public void visitDREM(DREM inst) {
        DoubleValue d2 = (DoubleValue) currentFrame.pop2();
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push2(d1.DREM(d2));
    }

    /**
     * Sets the return value of the current symbolic frame to the double value on top of the
     * symbolic stack
     *
     * @param inst The DRETURN instruction
     */
    public void visitDRETURN(DRETURN inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "DRETURN");
        currentFrame.setRet(currentFrame.pop2());
        // checkAndSetException(inst);
    }

    /**
     * Stores the top value from the symbolic stack (double) into a slot in the symbolic locals
     *
     * @param inst One of the DSTORE instructions (DSTORE, DSTORE_0 - DSTORE_3)
     */
    public void visitDSTORE(DSTORE inst) {
        currentFrame.setLocal2(inst.var, currentFrame.pop2());
    }

    /**
     * Subtracts two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DSUB instruction
     */
    public void visitDSUB(DSUB inst) {
        DoubleValue d2 = (DoubleValue) currentFrame.pop2();
        DoubleValue d1 = (DoubleValue) currentFrame.pop2();
        currentFrame.push2(d1.DSUB(d2));
    }

    /**
     * Duplicates the top value on the symbolic stack (only one word -> 8 bytes) | 1 | --> | 1 | | 1
     * |
     *
     * @param inst The DUP instruction
     */
    public void visitDUP(DUP inst) {
        currentFrame.push(currentFrame.peek());
    }

    /**
     * Duplicates the top value on the symbolic stack (two words -> 16 bytes) | 1 | | 1 | | 2 | -->
     * | 2 | | 1 | | 2 |
     *
     * @param inst The DUP2 instruction
     */
    public void visitDUP2(DUP2 inst) {
        currentFrame.push(currentFrame.peek2());
        currentFrame.push(currentFrame.peek2());
    }

    /**
     * Duplicate two words and insert them beneath a third word | 1 | | 1 | | 2 | --> | 2 | | 3 | |
     * 3 | | 1 | | 2 |
     *
     * @param inst The DUP2_X1 instruction
     */
    public void visitDUP2_X1(DUP2_X1 inst) {
        Value<?, ?> word1 = currentFrame.pop();
        Value<?, ?> word2 = currentFrame.pop();
        Value<?, ?> word3 = currentFrame.pop();
        currentFrame.push(word2);
        currentFrame.push(word1);
        currentFrame.push(word3);
        currentFrame.push(word2);
        currentFrame.push(word1);
    }

    /**
     * Duplicate two words and insert them beneath a fourth word | 1 | | 1 | | 2 | --> | 2 | | 3 | |
     * 3 | | 4 | | 4 | | 1 | | 2 |
     *
     * @param inst The DUP2_X2 instruction
     */
    public void visitDUP2_X2(DUP2_X2 inst) {
        Value<?, ?> word1 = currentFrame.pop();
        Value<?, ?> word2 = currentFrame.pop();
        Value<?, ?> word3 = currentFrame.pop();
        Value<?, ?> word4 = currentFrame.pop();
        currentFrame.push(word2);
        currentFrame.push(word1);
        currentFrame.push(word4);
        currentFrame.push(word3);
        currentFrame.push(word2);
        currentFrame.push(word1);
    }

    /**
     * Duplicate the top word and insert it beneath a second word | 1 | | 1 | | 2 | --> | 2 | | 1 |
     *
     * @param inst The DUP2_X1 instruction
     */
    public void visitDUP_X1(DUP_X1 inst) {
        Value<?, ?> top = currentFrame.pop();
        Value<?, ?> top2 = currentFrame.pop();
        currentFrame.push(top);
        currentFrame.push(top2);
        currentFrame.push(top);
    }

    /**
     * Duplicate the top word and insert it beneath the third word | 1 | | 1 | | 2 | --> | 2 | | 3 |
     * | 3 | | 1 |
     *
     * @param inst The DUP2_X1 instruction
     */
    public void visitDUP_X2(DUP_X2 inst) {
        Value<?, ?> word1 = currentFrame.pop();
        Value<?, ?> word2 = currentFrame.pop();
        Value<?, ?> word3 = currentFrame.pop();
        currentFrame.push(word1);
        currentFrame.push(word3);
        currentFrame.push(word2);
        currentFrame.push(word1);
    }

    /**
     * Casts a float to a double and puts the result onto the symbolic stack
     *
     * @param inst The F2D instruction
     */
    public void visitF2D(F2D inst) {
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push2(f1.F2D());
    }

    /**
     * Casts a float to an integer and puts the result onto the symbolic stack
     *
     * @param inst The F2I instruction
     */
    public void visitF2I(F2I inst) {
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.F2I());
    }

    public void visitF2L(F2L inst) {

        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push2(f1.F2L());
    }

    /**
     * Adds two floats and adds the result onto the symbolic stack
     *
     * @param inst The FADD instruction
     */
    public void visitFADD(FADD inst) {
        FloatValue f2 = currentFrame.pop().asFloatValue();
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FADD(f2));
    }

    /**
     * Loads a float from an array and puts it onto the symbolic stack
     *
     * @param inst The FALOAD instruction
     */
    public void visitFALOAD(FALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) currentFrame.pop();
            if (arr instanceof FloatArrayValue farr) {
                boolean result = checkArrayBounds(farr, idx, inst.iid);
                if (result) {
                    currentFrame.push(farr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[FALOAD]: Unknown array type");
                currentFrame.push(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Stores a float into an array
     *
     * @param inst The FASTORE instruction
     */
    public void visitFASTORE(FASTORE inst) {
        try {
            FloatValue val = currentFrame.pop().asFloatValue();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof FloatArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[FASTORE]: Unknown array type");
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Compares two floats and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The FCMPG instruction
     */
    public void visitFCMPG(FCMPG inst) {
        FloatValue f2 = currentFrame.pop().asFloatValue();
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FCMPG(f2));
    }

    /**
     * Compares two floats and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The FCMPL instruction
     */
    public void visitFCMPL(FCMPL inst) {
        FloatValue f2 = currentFrame.pop().asFloatValue();
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FCMPL(f2));
    }

    /**
     * Loads the constant 0.0f as a float onto the symbolic stack
     *
     * @param inst The FCONST_0 instruction
     */
    public void visitFCONST_0(FCONST_0 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.floatValue, 0.0f));
    }

    /**
     * Loads the constant 1.0f as a float onto the symbolic stack
     *
     * @param inst The FCONST_1 instruction
     */
    public void visitFCONST_1(FCONST_1 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.floatValue, 1.0f));
    }

    /**
     * Loads the constant 2.0f as a float onto the symbolic stack
     *
     * @param inst The FCONST_2 instruction
     */
    public void visitFCONST_2(FCONST_2 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.floatValue, 2.0f));
    }

    /**
     * Divides two floats and puts the result onto the symbolic stack
     *
     * @param inst The FDIV instruction
     */
    public void visitFDIV(FDIV inst) {
        FloatValue f2 = currentFrame.pop().asFloatValue();
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FDIV(f2));
    }

    /**
     * Loads a float from the symbolic locals onto the symbolic stack
     *
     * @param inst One of the FLOAD instructions (FLOAD, FLOAD_0 - FLOAD_3)
     */
    public void visitFLOAD(FLOAD inst) {
        currentFrame.push(currentFrame.getLocal(inst.var));
    }

    /**
     * Multiplies two floats and puts the result onto the symbolic stack
     *
     * @param inst The FMUL instruction
     */
    public void visitFMUL(FMUL inst) {
        FloatValue f2 = currentFrame.pop().asFloatValue();
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FMUL(f2));
    }

    /**
     * Negates a float and puts the result onto the symbolic stack
     *
     * @param inst The FNEG instruction
     */
    public void visitFNEG(FNEG inst) {
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FNEG());
    }

    /**
     * Calculates the remaider of the divion of two flaots and puts the result onto the symbolic
     * stack WARNING: This is currently only the concrete value
     *
     * @param inst The FREM instruction
     */
    public void visitFREM(FREM inst) {
        FloatValue f2 = currentFrame.pop().asFloatValue();
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FREM(f2));
    }

    /**
     * Sets the return value of the current symbolic frame to the float value on top of the symbolic
     * stack
     *
     * @param inst The FRETURN instruction
     */
    public void visitFRETURN(FRETURN inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "FRETURN");
        currentFrame.setRet(currentFrame.pop());
        // checkAndSetException(inst);
    }

    /**
     * Puts the float on top of the symbolic stack into one of the symbolic locals of the current
     * frame
     *
     * @param inst One of the FSTORE instructions (FSTORE, FSTORE_0 - FSTORE_3)
     */
    public void visitFSTORE(FSTORE inst) {
        currentFrame.setLocal(inst.var, currentFrame.pop());
    }

    /**
     * Subtracts two floats and puts the resulting FloatValuer onto the symbolic stack.
     *
     * @param inst The FSUB instruction.
     */
    public void visitFSUB(FSUB inst) {
        FloatValue f2 = currentFrame.pop().asFloatValue();
        FloatValue f1 = currentFrame.pop().asFloatValue();
        currentFrame.push(f1.FSUB(f2));
    }

    /**
     * Fetches a field from an object instance and puts it onto the symbolic stack
     *
     * @param inst The GETFIELD instruction
     */
    public void visitGETFIELD(GETFIELD inst) {
        try {
            ObjectInfo oi = classNames.get(inst.cIdx);
            FieldInfo fi = oi.get(inst.fIdx, false);
            ObjectValue<?, ?> ref = currentFrame.pop().asObjectValue();
            Value val;
            if (ref.getAddress() == 0) {
                logger.warn("GETFIELD on NULL object");
                val = PlaceHolder.instance;
            } else {
                val = ref.getField(fi.getFieldId());
            }
            if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
                currentFrame.push2(val);
            } else {
                currentFrame.push(val);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
        checkAndSetException(inst);
    }

    /**
     * Fetches a static field from an object and puts it onto the symbolic stack
     *
     * @param inst The GETSTATIC instruction
     */
    public void visitGETSTATIC(GETSTATIC inst) {
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        try {
            Value<?, ?> v;
            ObjectInfo oi = classNames.get(inst.cIdx);
            FieldInfo fi = oi.get(inst.fIdx, true);
            v =
                    ThreadHandler.getStaticField(
                            Thread.currentThread().getId(), oi, inst.cIdx, fi.getFieldId());
            if (v == null) {
                Object concrete = oi.getStaticField(fi.getFieldId());
                if (concrete == null) {
                    v = PlaceHolder.instance;
                } else {
                    switch (inst.desc) {
                        case "Z", "I", "C" -> v = new IntValue(context, (Integer) concrete);
                        case "D" -> v = new DoubleValue(context, (Double) concrete);
                        case "F" -> v = new FloatValue(context, (Float) concrete);
                        case "J" -> v = new LongValue(context, (Long) concrete);
                        case "[I" -> v = new IntArrayValue(context, (int[]) concrete, -1);
                        case "Ljava/lang/String;" -> v =
                                new StringValue(context, (String) concrete, -1);
                        default -> v = new ObjectValue(context, 100, -1);
                    }
                }
            }
            if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
                currentFrame.push2(v);
            } else {
                currentFrame.push(v);
            }
            // For static initializer
            symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "GETSTATIC");

        } catch (Exception e) {
            throwError(inst, e);
        }
        // checkAndSetException(inst);
    }

    public void visitGETVALUE_Object(GETVALUE_Object<?> inst) {
        Value<?, ?> peek = currentFrame.peek();
        Value<?, ?> tmp;
        boolean isSymbolic = false;
        if (peek == PlaceHolder.symbolicInstance) {
            isSymbolic = true;
            peek = PlaceHolder.instance;
        }
        if (inst.i == 1) {
            isSymbolic = true;
        }

        if (peek instanceof LambdaPlaceHolder) {
            // remove the placeholder lambda value
            LambdaPlaceHolder lambdaPlaceHolder = (LambdaPlaceHolder) currentFrame.pop();
            int key = lambdaPlaceHolder.getKey();
            int parentAddress = lambdaPlaceHolder.getParentAddress();
            // create a new object value (the return value from invoke dynamic)
            currentFrame.push(ValueFactory.getLambdaObjectValue(inst.v, parentAddress, key));
        } else if (peek == PlaceHolder.instance
                || (peek.asObjectValue().getAddress() != ADDRESS_UNKNOWN
                        && peek.asObjectValue().getAddress() != inst.v)) {
            // remove the placeholder value
            currentFrame.pop();
            // try to get object
            tmp = objects.get(inst.v);
            // check if the object was created earlier and then reuse it
            if (tmp != null) {
                if (isSymbolic) {
                    String name = tmp.MAKE_SYMBOLIC();
                }
                currentFrame.push(tmp);
            } else if (inst.v == 0) {
                if (isSymbolic) {
                    throw new RuntimeException("Cannot make NULL symbolic!");
                }
                currentFrame.push(ValueFactory.createNULLValue());
            } else {
                tmp = ValueFactory.createObjectValue(inst.val, inst.v);
                if (isSymbolic) {
                    String name = tmp.MAKE_SYMBOLIC();
                }
                currentFrame.push(tmp);
                objects.put(inst.v, tmp); // save the object for future use
            }
        } else if ((peek.asObjectValue()).getAddress() == ADDRESS_UNKNOWN) {
            // set the address of the object
            if (inst.v == 0) {
                currentFrame.pop();
                currentFrame.push(ValueFactory.createNULLValue());
            } else if (inst.val != null) {
                if (inst.val instanceof String s && peek.formula == null) {
                    // TODO This needs to be cleaned up!
                    currentFrame.pop();
                    currentFrame.push(ValueFactory.createStringValue(s, inst.v));
                } else {
                    (peek.asObjectValue()).setAddress(inst.v);
                    objects.put(inst.v, peek);
                }
            } else {
                // Need to obtain the Object address
                (peek.asObjectValue()).setAddress(inst.v);
                objects.put(inst.v, peek);
            }
        }
    }

    /**
     * Artificial instruction used to fetch concrete boolean values
     *
     * @param inst The (artificial) GETVALUE_boolean instruction
     */
    public void visitGETVALUE_boolean(GETVALUE_boolean inst) {
        visitGETVALUE_primitive(inst, ValueType.booleanValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_byte instruction
     */
    public void visitGETVALUE_byte(GETVALUE_byte inst) {
        visitGETVALUE_primitive(inst, ValueType.byteValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_char instruction
     */
    public void visitGETVALUE_char(GETVALUE_char inst) {
        visitGETVALUE_primitive(inst, ValueType.charValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_double instruction
     */
    public void visitGETVALUE_double(GETVALUE_double inst) {
        visitGETVALUE_primitive(inst, ValueType.doubleValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_float instruction
     */
    public void visitGETVALUE_float(GETVALUE_float inst) {
        visitGETVALUE_primitive(inst, ValueType.floatValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_int instruction
     */
    public void visitGETVALUE_int(GETVALUE_int inst) {
        visitGETVALUE_primitive(inst, ValueType.intValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_long instruction
     */
    public void visitGETVALUE_long(GETVALUE_long inst) {
        visitGETVALUE_primitive(inst, ValueType.longValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_short instruction
     */
    public void visitGETVALUE_short(GETVALUE_short inst) {
        visitGETVALUE_primitive(inst, ValueType.shortValue);
    }

    public void visitGETVALUE_void(GETVALUE_void inst) {
        // TODO: Why does the case exist if it does nothing?
    }

    /**
     * No symbolic tracking needed for the uncoditional jump.
     *
     * @param inst Either the GOTO or GOTO_w instruction
     */
    public void visitGOTO(GOTO inst) {}

    /**
     * Converts an integer to a byte and puts the result onto the symbolic stack
     *
     * @param inst The I2B instruction
     */
    public void visitI2B(I2B inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.I2B());
    }

    /**
     * Converts an integer to a char and puts the result onto the symbolic stack ToDo (Nils):
     * Symbolic information regarding the integer is currently lost here! See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/60">Issue
     * 60</a>
     *
     * @param inst The I2C instruction
     */
    public void visitI2C(I2C inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.I2C());
    }

    /**
     * Converts an integer to a double and puts the result onto the symbolic stack
     *
     * @param inst The I2D instruction
     */
    public void visitI2D(I2D inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push2(i1.I2D());
    }

    /**
     * Converts an integer to a float and puts the result onto the symbolic stack
     *
     * @param inst The I2F instruction
     */
    public void visitI2F(I2F inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.I2F());
    }

    /**
     * Converts an integer to a long and puts the result onto the symbolic stack
     *
     * @param inst The I2L instruction
     */
    public void visitI2L(I2L inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push2(i1.I2L());
    }

    /**
     * Converts an integer to a short and puts the result onto the symbolic stack
     *
     * @param inst The I2S instruction
     */
    public void visitI2S(I2S inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.I2S());
    }

    /**
     * Adds two integers and pushes the result onto the symbolic stack
     *
     * @param inst The IADD instruction
     */
    public void visitIADD(IADD inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.IADD(i2));
    }

    /**
     * Loads an integer from an array and puts it onto the symbolic stack
     *
     * @param inst The IALOAD instruction
     */
    public void visitIALOAD(IALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof IntArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    currentFrame.push(arr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[IALOAD]: Unknown array type");
                currentFrame.push(
                        ref.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Calculates the bitwise and of two integers in binary representation and puts the result onto
     * the symbolic stack
     *
     * @param inst The IAND instruction
     */
    public void visitIAND(IAND inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.IAND(i2));
    }

    /**
     * Stores an integer into an array
     *
     * @param inst The IASTORE instruction
     */
    public void visitIASTORE(IASTORE inst) {
        try {
            IntValue val = currentFrame.pop().asIntValue();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof IntArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[IASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throwError(inst, t);
        }
    }

    /**
     * Load the int value 0 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_0(ICONST_0 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, 0));
    }

    /**
     * Load the int value 1 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_1(ICONST_1 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, 1));
    }

    /**
     * Load the int value 2 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_2(ICONST_2 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, 2));
    }

    /**
     * Load the int value 3 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_3(ICONST_3 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, 3));
    }

    /**
     * Load the int value 4 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_4(ICONST_4 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, 4));
    }

    /**
     * Load the int value 5 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_5(ICONST_5 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, 5));
    }

    /**
     * Load the int value -1 onto the symbolic stack
     *
     * @param inst Current ICONST_M1 instruction
     */
    public void visitICONST_M1(ICONST_M1 inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, -1));
    }

    /**
     * Divides two integers and puts the result onto the symbolic stack.
     *
     * @param inst Current IDIV instruction
     */
    public void visitIDIV(IDIV inst) {
        try {
            IntValue i2 = currentFrame.pop().asIntValue();
            IntValue i1 = currentFrame.pop().asIntValue();
            BooleanFormula constraint = i2.checkZero();
            // Check for exception
            boolean result = i2.concrete != 0;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                currentFrame.push(i1.IDIV(i2));
            } else {
                enforceException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Branches if the top value on the stack is equal to 0
     *
     * @param inst The IFEQ instruction
     */
    public void visitIFEQ(IFEQ inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        // IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IFEQ();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Branches if the top value on the stack is greater or equal to 0
     *
     * @param inst The IFGE instruction
     */
    public void visitIFGE(IFGE inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IFGE();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Branches if the top value on the stack is greater then 0
     *
     * @param inst The IFGT instruction
     */
    public void visitIFGT(IFGT inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IFGT();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Branches if the top value on the stack is less or equal to 0
     *
     * @param inst The IFLE instruction
     */
    public void visitIFLE(IFLE inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IFLE();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Branches if the top value on the stack is less then 0
     *
     * @param inst The IFLT instruction
     */
    public void visitIFLT(IFLT inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IFLT();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if the current Value is not equal to 0
     *
     * @param inst The IFNE instruction
     */
    public void visitIFNE(IFNE inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IFNE();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if an object is not null and saves the resulting symbolic formula as a path constraint
     *
     * @param inst The IFNONNULL instruction.
     */
    public void visitIFNONNULL(IFNONNULL inst) {
        ObjectValue o1 = currentFrame.pop().asObjectValue();
        BooleanFormula constraint = o1.IFNONNULL();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if an object is null and saves the resulting symbolic formula as a path constraint
     *
     * @param inst The IFNULL instruction.
     */
    public void visitIFNULL(IFNULL inst) {
        ObjectValue o1 = currentFrame.pop().asObjectValue();
        BooleanFormula constraint = o1.IFNULL();
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if two object references are equal and saves the resulting symbolic formula as a path
     * constraint
     *
     * @param inst The IF_ACMPEQ instruction.
     */
    public void visitIF_ACMPEQ(IF_ACMPEQ inst) {
        Value v2 = currentFrame.pop();
        Value v1 = currentFrame.pop();
        BooleanFormula constraint;
        if (v1 instanceof StringValue s1 && v2 instanceof StringValue s2) {
            constraint = s1.IF_ACMPEQ(s2);
        } else {
            ObjectValue o1 = v1.asObjectValue();
            ObjectValue o2 = v2.asObjectValue();
            constraint = o1.IF_ACMPEQ(o2);
        }
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if two object references are not equal and saves the resulting symbolic formula as a
     * path constraint
     *
     * @param inst The IF_ACMPNE instruction.
     */
    public void visitIF_ACMPNE(IF_ACMPNE inst) {
        Value v2 = currentFrame.pop();
        Value v1 = currentFrame.pop();
        BooleanFormula constraint;
        if (v1 instanceof StringValue s1 && v2 instanceof StringValue s2) {
            constraint = s1.IF_ACMPNE(s2);
        } else {
            ObjectValue o1 = v1.asObjectValue();
            ObjectValue o2 = v2.asObjectValue();
            constraint = o1.IF_ACMPNE(o2);
        }
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);

        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

    }

    /**
     * Checks if the two integers are equal
     *
     * @param inst The IF_ICMPEQ instruction
     */
    public void visitIF_ICMPEQ(IF_ICMPEQ inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IF_ICMPEQ(i2);
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    public int determineIid(int iid) {
        int current_cnt = iidCounter.getOrDefault(iid, 0);
        iidCounter.put(iid, current_cnt + 1);
        return Integer.valueOf(String.valueOf(iid) + String.valueOf(current_cnt));
        // if (loops.empty()) return iid;
        // return Integer.valueOf(String.valueOf(iid) +
        // String.valueOf(loopCounter.get(loops.peek())));
    }
    /**
     * Checks if the integer is greater or equal to the second integer
     *
     * @param inst The IF_ICMPGE instruction
     */
    public void visitIF_ICMPGE(IF_ICMPGE inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IF_ICMPGE(i2);
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if the integer is greater than the second integer
     *
     * @param inst The IF_ICMPGT instruction
     */
    public void visitIF_ICMPGT(IF_ICMPGT inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IF_ICMPGT(i2);
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if the integer is less than or equal to the second integer
     *
     * @param inst The IF_ICMPLE instruction
     */
    public void visitIF_ICMPLE(IF_ICMPLE inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IF_ICMPLE(i2);
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if the integer is greater than the second integer
     *
     * @param inst The IF_ICMPLT instruction
     */
    public void visitIF_ICMPLT(IF_ICMPLT inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IF_ICMPLT(i2);
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Checks if the wo integers are not equal
     *
     * @param inst The IF_ICMPNE instruction
     */
    public void visitIF_ICMPNE(IF_ICMPNE inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        BooleanFormula constraint = i1.IF_ICMPNE(i2);
        boolean isBranchTaken = isBranchTaken();
        int iid = determineIid(inst.iid);
        /* if (!loops.isEmpty() && isBranchTaken) {
            logger.info("[LOOP EXIT] Branch instruction iid: " + inst.iid);
            assert loops.pop() == inst.iid : " Loop mismatch!";
        } */

        symbolicStateHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
    }

    /**
     * Increments an integer that is in the locals. No change to the symbolic stack
     *
     * @param inst The IINC instruction
     */
    public void visitIINC(IINC inst) {
        // ToDo (Nils): What happens if this is the first time the local is referenced? The next
        // line should return a Placeholder then
        IntValue i1 = currentFrame.getLocal(inst.var).asIntValue();
        currentFrame.setLocal(inst.var, i1.IINC(inst.increment));
    }

    /**
     * Loads an integer from the locals onto the stack After this instruction a GETVALUE_int
     * instruction follows to fetch the concrete value if the sybolic locals did not contain the
     * value
     *
     * @param inst The ILOAD instruction (including ILOAD_0 - ILOAD_3)
     */
    public void visitILOAD(ILOAD inst) {
        // ToDO (Nils): Why are ILOAD_0 etc not present? Where did they catch them and used this
        // case?
        currentFrame.push(currentFrame.getLocal(inst.var));
    }

    /**
     * Multiplies two integers and adds the result onto the symbolic stack
     *
     * @param inst The IMUL instruction
     */
    public void visitIMUL(IMUL inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.IMUL(i2));
    }

    /**
     * Negates an integer and puts the resulting value onto the symbolic stack
     *
     * @param inst The INEG instruction
     */
    public void visitINEG(INEG inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.INEG());
    }

    /**
     * Removes the object reference from the symbolic stack and pushes true onto the symbolic stack.
     * ToDo (Nils): Find a way to push the correct value onto the stack here See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/61">Issue
     * 61</a>
     *
     * @param inst The INSTANCEOF instruction.
     */
    public void visitINSTANCEOF(INSTANCEOF inst) {
        try {
            currentFrame.pop();
            currentFrame.push(
                    ValueFactory.createNumericalValue(
                            ValueType.booleanValue, true)); // could be wrong boolean value
        } catch (Exception e) {
            throwError(inst, e);
        }
        checkAndSetException(inst);
    }

    /**
     * Just a method for validating the engine!
     *
     * @param type Expected datatype of the Value
     * @param val Value to be validated
     * @return True if the Value is of the provided Type
     */
    private boolean isMatchingType(Type type, Value<?, ?> val) {
        if (val == ValueFactory.createNULLValue()) return true;
        String descriptor = type.getDescriptor();
        return switch (descriptor) {
            case "I" -> val instanceof IntValue;
            case "Z" -> val instanceof IntValue || val instanceof BooleanValue;
            case "C" -> val instanceof IntValue || val instanceof CharValue;
            case "D" -> val instanceof DoubleValue;
            case "F" -> val instanceof FloatValue;
            case "J" -> val instanceof LongValue;
            case "S" -> val instanceof ShortValue;
            case "B" -> val instanceof ByteValue;
                // case "Ljava/lang/String;" -> val instanceof StringValue;
            default -> val instanceof ObjectValue;
        };
        /*
        B	byte	signed byte
        C	char	Unicode character code point in the Basic Multilingual Plane, encoded with UTF-16
        D	double	double-precision floating-point value
        F	float	single-precision floating-point value
        I	int	integer
        J	long	long integer
        L ClassName ;	reference	an instance of class ClassName
        S	short	signed short
        Z	boolean	true or false
        */
    }

    private void storeLambdaFrame(
            Type[] types, Value<?, ?>[] arguments, int key, boolean isStatic) {

        int len = types.length;
        try {
            // This frame should not be used it is a placeholder for the parameters
            Frame lambdaFrame = new Frame("LambdaFrame", "", -1);

            for (int i = isStatic ? 0 : 1; i < len; i++) {
                if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                    lambdaFrame.addLocal2(arguments[i]);
                } else {
                    lambdaFrame.addLocal(arguments[i]);
                }
            }
            lambdaFrameStore.put(key, lambdaFrame);
        } catch (Throwable t) {
            throwError(null, t);
        }
    }

    /**
     * This method is called by INVOKE* It Creates a new frame for the execution of the method and
     * sets all locals (arguments)
     *
     * @param desc The Datatype description of the method arguments
     * @param owner The parent object of the invoked method
     * @param name The name of the method
     * @param isInstance If the method is a static method or an instance of an object
     * @param inst The instruction that invoked the method call
     */
    @SneakyThrows
    private void setArgumentsAndNewFrame(
            String desc, String owner, String name, boolean isInstance, Instruction inst) {

        if (owner.equals("java/net/Socket")) {
            logger.info("Socket mocking not implemented: " + owner + "/" + name);
            throw new Throwable("Socket mocking not implemented: " + owner + "/" + name);
        }
        Type[] types = Type.getArgumentTypes(desc);
        Type retType = Type.getReturnType(desc);
        int len = types.length;

        int nReturnWords;
        if (retType == Type.DOUBLE_TYPE || retType == Type.LONG_TYPE) {
            nReturnWords = 2;
        } else if (retType == Type.VOID_TYPE) {
            nReturnWords = 0;
        } else {
            nReturnWords = 1;
        }
        Frame newFrame = new Frame(owner, name, nReturnWords);
        // expressions?!
        stack.push(newFrame);
        Value<?, ?>[] arguments = new Value[len];
        for (int i = len - 1; i >= 0; i--) {
            if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                arguments[i] = currentFrame.pop2();
            } else {
                Value<?, ?> arg = currentFrame.pop();
                if (arg instanceof LambdaObjectValue) {
                    // We need to expand the frame and add all arguments from the lambda frame to
                    // the locals
                    arguments[i] = arg;
                    // TODO: implement
                    // This block : (java/util/stream/Stream) can potentially be removed and
                    // implemented here for more generic handling
                } else {
                    // Normal parameter
                    arguments[i] = arg;
                }
            }
            if (!isMatchingType(types[i], arguments[i])) {
                throw new RuntimeException(
                        "ERROR! The types dont match: "
                                + types[i]
                                + " | "
                                + arguments[i].getClass());
            }
        }
        ObjectValue instance = null;
        if (isInstance) {
            instance = currentFrame.pop().asObjectValue();
        }

        if (inst instanceof INVOKEDYNAMIC) {
            if (owner.equals("java/lang/invoke/LambdaMetafactory")) {
                // (6) is static (7) is dynamic
                boolean isStatic = ((INVOKEDYNAMIC) inst).lambda.contains("(6)");

                // TODO maybe unforeseen cases here? dont use contains but use a regular expression
                // to be more certain of the case
                if (!isStatic
                        && !(((INVOKEDYNAMIC) inst).lambda.contains("(7)")
                                || ((INVOKEDYNAMIC) inst).lambda.contains("(5)"))) {}

                storeLambdaFrame(types, arguments, inst.iid, isStatic);
                int parentAddress = -1;
                if (!isStatic) {
                    // The lambda needs the parent (this pointer)
                    // TODO: Maybe add checks? This will likely crash if stack is corrupt
                    if (arguments.length > 0) {
                        // The expected case, we need the pointer
                        parentAddress = ((ObjectValue) arguments[0]).getAddress();

                    } else {
                        parentAddress = 0;
                        // TODO: No idea if that works, there are cases when
                        // LambdaMetafactory.accepts without args is called (class::method)
                    }
                }
                newFrame.setRet(new LambdaPlaceHolder(inst.iid, parentAddress));
            } else {
                newFrame.setRet(
                        DynamicInvocation.invokeMethod(
                                owner, name, arguments, ((INVOKEDYNAMIC) inst).lambda));
            }

        } else if (instance instanceof LambdaObjectValue) {
            Frame lambdaFrame = lambdaFrameStore.get(((LambdaObjectValue) instance).getKey());
            if (lambdaFrame == null) {
                throw new RuntimeException("No lambda frame found!");
            }
            int parentAddress = ((LambdaObjectValue) instance).getParentAddress();
            if (parentAddress > 0) {
                newFrame.addLocal(objects.get(parentAddress));
            }
            for (Value<?, ?> local : lambdaFrame.getLocals()) {
                newFrame.addLocal(local);
            }

            for (int i = 0; i < len; i++) {
                if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                    newFrame.addLocal2(arguments[i]);
                } else {
                    newFrame.addLocal(arguments[i]);
                }
            }
        } else if (owner.equals("java/util/stream/Stream") && name.equals("forEach")) {
            for (int i = 0; i < len; i++) {
                if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                    newFrame.addLocal2(arguments[i]);
                } else {
                    Value<?, ?> argument = arguments[i];
                    if (argument instanceof LambdaObjectValue) {
                        Frame lambdaFrame =
                                lambdaFrameStore.get(((LambdaObjectValue) argument).getKey());
                        int parentAddress = ((LambdaObjectValue) argument).getParentAddress();
                        if (parentAddress != -1) {
                            newFrame.addLocal(objects.get(parentAddress));
                        }
                        for (Value<?, ?> v : lambdaFrame.getLocals()) {
                            newFrame.addLocal(v);
                        }
                    } else {
                        newFrame.addLocal(argument);
                    }
                }
            }
        } else {

            if (isInstance) {
                newFrame.addLocal(instance); // get the this-pointer
            }
            for (int i = 0; i < len; i++) {
                if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                    newFrame.addLocal2(arguments[i]);
                } else {
                    newFrame.addLocal(arguments[i]);
                }
            }
        }

        currentFrame = newFrame;

        if ((next instanceof INVOKEMETHOD_END
                        || next instanceof INVOKEMETHOD_EXCEPTION
                        || next == null)
                && !(inst instanceof INVOKEDYNAMIC)) {
            if (isInstance) {
                currentFrame.setRet(instance.invokeMethod(name, types, arguments));
            } else {
                currentFrame.setRet(
                        StaticInvocation.invokeMethod(
                                owner, name, types, arguments, symbolicStateHandler));
            }
            if (currentFrame.getRet().equals(PlaceHolder.instance)
                    && !(retType == Type.VOID_TYPE)) {

                ArrayList<String> blocklist =
                        new ArrayList<>(
                                Arrays.asList(
                                        "java/io/PrintStream/println",
                                        "de/uzl/its/swat/Main",
                                        "de/uzl/its/swat/instrument/svcomp/Verifier",
                                        "java/io/PrintStream",
                                        "java/lang/Class",
                                        "java/io/BufferedReader",
                                        "java/io/InputStream",
                                        "java/util/Scanner"));
                if (!(blocklist.contains(owner + "/" + name)
                        || blocklist.contains(owner)
                        || name.equals("<init>"))) {

                    ThreadHandler.logInvocation(
                            Thread.currentThread().getId(),
                            new LogRecord(
                                    Level.WARNING,
                                    "Method not implemented: " + owner + "/" + name));
                }
            }
        }
    }

    /**
     * Invokes an interface method on an object reference from the symbolic stack
     *
     * @param inst The INVOKEINTERFACE instruction
     */
    public void visitINVOKEINTERFACE(INVOKEINTERFACE inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "INVOKEINTERFACE");
        setArgumentsAndNewFrame(inst.desc, inst.owner, inst.name, true, inst);
    }

    /**
     * Invokes an instance method on an object reference from the symbolic stack
     *
     * @param inst The INVOKESPECIAL instruction
     */
    public void visitINVOKESPECIAL(INVOKESPECIAL inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "INVOKESPECIAL");
        setArgumentsAndNewFrame(inst.desc, inst.owner, inst.name, true, inst);
    }

    /**
     * Invokes a static method
     *
     * @param inst The INVOKESTATIC instruction
     */
    public void visitINVOKESTATIC(INVOKESTATIC inst) {
        setArgumentsAndNewFrame(inst.desc, inst.owner, inst.name, false, inst);
        if (!Objects.equals(inst.owner, "de/uzl/its/swat/Main")) {
            symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "INVOKESTATIC");
        }
    }

    /**
     * Invokes a virtual method on an object reference from the symbolic stack
     *
     * @param inst The INVOKEVIRTUAL instruction
     */
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "INVOKEVIRTUAL");
        setArgumentsAndNewFrame(inst.desc, inst.owner, inst.name, true, inst);
    }

    /**
     * Invokes a dynamic method
     *
     * @param inst The INVOKEDYNAMIC instruction
     */
    public void visitINVOKEDYNAMIC(INVOKEDYNAMIC inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "INVOKEDYNAMIC");
        setArgumentsAndNewFrame(inst.desc, inst.owner, inst.name, false, inst);
    }

    /**
     * Calculates the bitwise OR of two integers and puts the result onto the symbolic stack
     *
     * @param inst The IOR instruction
     */
    public void visitIOR(IOR inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.IOR(i2));
    }

    /**
     * Calculates the modulo of two integers and puts the result onto the symbolic stack
     *
     * @param inst The IREM instruction
     */
    public void visitIREM(IREM inst) {
        try {
            IntValue i2 = currentFrame.pop().asIntValue();
            IntValue i1 = currentFrame.pop().asIntValue();
            BooleanFormula constraint = i2.checkZero();
            // Check for exception
            boolean result = i2.concrete != 0;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                currentFrame.push(i1.IREM(i2));
            } else {
                enforceException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Sets the return value of the current symbolic frame to the int value on top of the symbolic
     * stack
     *
     * @param inst The IRETURN instruction
     */
    public void visitIRETURN(IRETURN inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "IRETURN");
        currentFrame.setRet(currentFrame.pop());
        // checkAndSetException(inst);
    }

    /**
     * Shifts an integer bitwise to the left and puts the result onto the symbolic stack
     *
     * @param inst The ISHL instruction
     */
    public void visitISHL(ISHL inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.ISHL(i2));
    }

    /**
     * Shifts an integer bitwise arithmetically to the right and puts the result onto the symbolic
     * stack
     *
     * @param inst The ISHR instruction
     */
    public void visitISHR(ISHR inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.ISHR(i2));
    }

    /**
     * Puts an integer from the symbolic stack into the symbolic locals
     *
     * @param inst The ISTORE instruction
     */
    public void visitISTORE(ISTORE inst) {
        currentFrame.setLocal(inst.var, currentFrame.pop());
    }

    /**
     * Subtracts two ints and puts the result onto the symbolic stack
     *
     * @param inst The ISUB instruction
     */
    public void visitISUB(ISUB inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.ISUB(i2));
    }

    /**
     * Shifts an integer bitwise logically to the right and puts the result onto the symbolic stack
     *
     * @param inst The IUSHR instruction
     */
    public void visitIUSHR(IUSHR inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.IUSHR(i2));
    }

    /**
     * Calculates the bitwise exclusive OR of two integers and puts the result onto the symbolic
     * stack
     *
     * @param inst The IXOR instruction
     */
    public void visitIXOR(IXOR inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        IntValue i1 = currentFrame.pop().asIntValue();
        currentFrame.push(i1.IXOR(i2));
    }

    /**
     * Jump subroutine. No symbolic handling needed. ToDo (Nils): The instruction pushes the return
     * address onto the symbolic stack Should a placeholder be put here? See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/62">Issue
     * 62</a>
     *
     * @param inst Either the JSR or JSR_W instruction
     */
    public void visitJSR(JSR inst) {}

    public void visitL2D(L2D inst) {
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.L2D());
    }

    public void visitL2F(L2F inst) {
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push(i1.L2F());
    }

    public void visitL2I(L2I inst) {
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push(i1.L2I());
    }

    /**
     * Adds two longs and adds the result onto the symbolic stack
     *
     * @param inst The LADD instruction
     */
    public void visitLADD(LADD inst) {
        LongValue i2 = currentFrame.pop2().asLongValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LADD(i2));
    }

    /**
     * Loads a long from an array and puts it onto the symbolic stack
     *
     * @param inst The LALOAD instruction
     */
    public void visitLALOAD(LALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) currentFrame.pop();
            if (arr instanceof LongArrayValue larr) {
                boolean result = checkArrayBounds(larr, idx, inst.iid);
                if (result) {
                    currentFrame.push2(larr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[LALOAD]: Unknown array type");
                currentFrame.push(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Calculates the bitwise and of two longs in binary representation and puts the resulting long
     * onto the stack
     *
     * @param inst The LAND instruction
     */
    public void visitLAND(LAND inst) {
        LongValue i2 = currentFrame.pop2().asLongValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LAND(i2));
    }

    /**
     * Stores a long into an array
     *
     * @param inst The LASTORE instruction
     */
    public void visitLASTORE(LASTORE inst) {
        try {
            LongValue val = currentFrame.pop2().asLongValue();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof LongArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[LASTORE]: Unknown array type");
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Compares two longs and adds the result encoded as an integer onto the symbolic stack
     *
     * @param inst The LCMP instruction
     */
    public void visitLCMP(LCMP inst) {
        LongValue i2 = currentFrame.pop2().asLongValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push(i1.LCMP(i2));
    }

    /**
     * Puts the constant 0L as a long onto the symbolic stack
     *
     * @param inst The LCONST_0 instruction
     */
    public void visitLCONST_0(LCONST_0 inst) {
        currentFrame.push2(ValueFactory.createNumericalValue(ValueType.longValue, 0L));
    }

    /**
     * Puts the constant 1L as a long onto the symbolic stack
     *
     * @param inst The LCONST_1 instruction
     */
    public void visitLCONST_1(LCONST_1 inst) {
        currentFrame.push2(ValueFactory.createNumericalValue(ValueType.longValue, 1L));
    }

    /**
     * Puts a constant string onto the symbolic stack
     *
     * @param inst The (artificial) LDC_String instruction
     */
    public void visitLDC_String(LDC_String inst) {
        currentFrame.push(ValueFactory.createStringValue(inst.c, inst.address));
        checkAndSetException(inst);
    }

    /**
     * Puts an object onto the symbolic stack
     *
     * @param inst The (artificial) LDC_Object instruction
     */
    public void visitLDC_Object(LDC_Object inst) {
        Value<?, ?> tmp = objects.get(inst.c);
        if (tmp != null) {
            currentFrame.push(tmp);
        } else if (inst.c == 0) {
            currentFrame.push(ValueFactory.createNULLValue());
        } else {
            currentFrame.push(tmp = ValueFactory.createObjectValue(null, inst.c));
            objects.put(inst.c, tmp);
        }
        checkAndSetException(inst);
    }

    /**
     * Puts a constant double onto the symbolic stack
     *
     * @param inst The (artificial) LDC_double instruction
     */
    public void visitLDC_double(LDC_double inst) {
        currentFrame.push2(ValueFactory.createNumericalValue(ValueType.doubleValue, inst.c));
        checkAndSetException(inst);
    }

    /**
     * Puts a constant float onto the symbolic stack
     *
     * @param inst The (artificial) LDC_float instruction
     */
    public void visitLDC_float(LDC_float inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.floatValue, inst.c));
        checkAndSetException(inst);
    }

    /**
     * Loads a constant int onto the symbolic stack
     *
     * @param inst The (artificial) LDC_int instruction
     */
    public void visitLDC_int(LDC_int inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, inst.c));
        checkAndSetException(inst);
    }

    /**
     * Loads a constant long onto the symbolic stack
     *
     * @param inst The (artificial) LDC_long instruction
     */
    public void visitLDC_long(LDC_long inst) {
        currentFrame.push2(ValueFactory.createNumericalValue(ValueType.longValue, inst.c));
        checkAndSetException(inst);
    }

    /**
     * Divides two longs and puts the result onto the symbolic stack
     *
     * @param inst The LDIV instruction
     */
    public void visitLDIV(LDIV inst) {
        try {
            LongValue l2 = currentFrame.pop2().asLongValue();
            LongValue l1 = currentFrame.pop2().asLongValue();
            BooleanFormula constraint = l2.checkZero();
            // Check for exception
            boolean result = l2.concrete != 0;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                currentFrame.push2(l1.LDIV(l2));
            } else {
                enforceException(inst);
            }

        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Loads a long from the symbolic locals and adds it to the symbolic stack
     *
     * @param inst One of the LLOAD isntructions (LLOAD, LLOAD_0 - LLOAD_3)
     */
    public void visitLLOAD(LLOAD inst) {
        currentFrame.push2(currentFrame.getLocal2(inst.var));
    }

    /**
     * Multiplies two longs and puts the result onto the symbolic stack
     *
     * @param inst The LMUL instruction
     */
    public void visitLMUL(LMUL inst) {
        LongValue i2 = currentFrame.pop2().asLongValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LMUL(i2));
    }

    /**
     * Negates a long and puts the result onto the symbolic stack
     *
     * @param inst The LNEG isntruction
     */
    public void visitLNEG(LNEG inst) {
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LNEG());
    }

    /**
     * Calculates the bitwise or of two longs and puts the result onto the symbolic stack
     *
     * @param inst The LOR instruction
     */
    public void visitLOR(LOR inst) {
        LongValue i2 = currentFrame.pop2().asLongValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LOR(i2));
    }

    /**
     * Calculates the modulo of two longs and puts the result onto the symbolic stack
     *
     * @param inst The LREM instruction
     */
    public void visitLREM(LREM inst) {
        try {
            LongValue i2 = currentFrame.pop2().asLongValue();
            LongValue i1 = currentFrame.pop2().asLongValue();
            BooleanFormula constraint = i2.checkZero();
            // Check for exception
            boolean result = i2.concrete != 0;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                currentFrame.push2(i1.LREM(i2));
            } else {
                enforceException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Sets the return value of the current symbolic frame to the long value on top of the symbolic
     * stack
     *
     * @param inst The LRETURN instruction
     */
    public void visitLRETURN(LRETURN inst) {
        symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "lRETURN");
        currentFrame.setRet(currentFrame.pop2());
        // checkAndSetException(inst);
    }

    /**
     * Calculates the bitwise left shift of a long and puts the result onto the symbolic stack
     *
     * @param inst The LSHL instruction
     */
    public void visitLSHL(LSHL inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LSHL(i2));
    }

    /**
     * Calculates the bitwise right shift of a long and puts the result onto the symbolic stack
     *
     * @param inst The LSHR instruction
     */
    public void visitLSHR(LSHR inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LSHR(i2));
    }

    /**
     * Stores the top value from the symbolic stack (long) into a slot in the symbolic locals
     *
     * @param inst One of the LSTORE instructions (LSTORE, LSTORE_0 - LSTORE_3)
     */
    public void visitLSTORE(LSTORE inst) {
        currentFrame.setLocal2(inst.var, currentFrame.pop2());
    }

    /**
     * Subtracts two longs and puts the result onto the symbolic stack
     *
     * @param inst The LSUB instruction
     */
    public void visitLSUB(LSUB inst) {
        LongValue i2 = currentFrame.pop2().asLongValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LSUB(i2));
    }

    /**
     * Calculates the bitwise logical right shift of a long and puts the result onto the symbolic
     * stack
     *
     * @param inst The LUSHR instruction
     */
    public void visitLUSHR(LUSHR inst) {
        IntValue i2 = currentFrame.pop().asIntValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LUSHR(i2));
    }

    /**
     * Calculates the bitwise exclusive or of two longs and puts the result onto the symbolic stack
     *
     * @param inst The LXOR instruction
     */
    public void visitLXOR(LXOR inst) {
        LongValue i2 = currentFrame.pop2().asLongValue();
        LongValue i1 = currentFrame.pop2().asLongValue();
        currentFrame.push2(i1.LXOR(i2));
    }

    /**
     * No symbolic behaviour defined.
     *
     * @param inst The MONITORENTER instruction.
     */
    public void visitMONITORENTER(MONITORENTER inst) {
        checkAndSetException(inst);
    }

    /**
     * No symbolic behaviour defined.
     *
     * @param inst The MONITOREXIT instruction.
     */
    public void visitMONITOREXIT(MONITOREXIT inst) {
        checkAndSetException(inst);
    }

    public void visitNEW(NEW inst) {
        try {
            ObjectInfo oi = classNames.get(inst.cIdx);
            currentFrame.push(ValueFactory.createObjectValue(oi.getNFields(), oi.getClassName()));

            // For static initializer
            symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "NEW");
        } catch (Exception e) {
            throwError(inst, e);
        }
        checkAndSetException(inst);
    }

    public void visitNEWARRAY(NEWARRAY inst) {
        // ToDo (Nils): Why is the try catch here?
        try {
            IntValue size = currentFrame.pop().asIntValue();
            BooleanFormula constraint = size.checkPositive();
            boolean result = size.concrete >= 0;
            symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));

            switch (inst.atype) {
                case 4 -> // T_BOOLEAN
                currentFrame.push(ValueFactory.createArrayValue(ValueType.booleanValue, size, -1));
                case 5 -> // T_CHAR
                currentFrame.push(ValueFactory.createArrayValue(ValueType.charValue, size, -1));
                case 6 -> // T_FLOAT
                currentFrame.push(ValueFactory.createArrayValue(ValueType.floatValue, size, -1));
                case 7 -> // T_DOUBLE
                currentFrame.push(ValueFactory.createArrayValue(ValueType.doubleValue, size, -1));
                case 8 -> // T_BYTE
                currentFrame.push(ValueFactory.createArrayValue(ValueType.byteValue, size, -1));
                case 9 -> // T_SHORT
                currentFrame.push(ValueFactory.createArrayValue(ValueType.shortValue, size, -1));
                case 10 -> // T_INT
                currentFrame.push(ValueFactory.createArrayValue(ValueType.intValue, size, -1));
                case 11 -> // T_LONG
                currentFrame.push(ValueFactory.createArrayValue(ValueType.longValue, size, -1));
                default -> throw new RuntimeException(
                        "Unknown primitive type: " + inst.atype + "!");
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
        checkAndSetException(inst);
    }

    /**
     * Nothing to do here ;D
     *
     * @param inst The NOP instruction.
     */
    public void visitNOP(NOP inst) {}

    /**
     * Pops the top value from the symbolic stack.
     *
     * @param inst The POP instruction
     */
    public void visitPOP(POP inst) {
        currentFrame.pop();
    }

    /**
     * Pops the top two values from the symbolic stack.
     *
     * @param inst The POP2 instruction
     */
    public void visitPOP2(POP2 inst) {
        currentFrame.pop2();
    }

    /**
     * Puts a value into the field of an object reference The handling has changed in the JVM: - <a
     * href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5.putfield">SE11
     * (new)</a> - <a
     * href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html#jvms-6.5.putfield">SE7
     * (Old)</a>
     *
     * @param inst The PUTFIELD instruction
     */
    public void visitPUTFIELD(PUTFIELD inst) {
        ObjectInfo oi = classNames.get(inst.cIdx);
        FieldInfo fi = oi.get(inst.fIdx, false);
        Value<?, ?> value;
        if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
            value = currentFrame.pop2();
        } else {
            value = currentFrame.pop();
        }
        ObjectValue ref = currentFrame.pop().asObjectValue();
        try {
            ref.setField(fi.getFieldId(), value);
        } catch (Exception e) {
            throwError(inst, e);
        }
        checkAndSetException(inst);
    }

    /**
     * Puts a value into a static field of a class
     *
     * @param inst The PUTSTATIC instruction
     */
    public void visitPUTSTATIC(PUTSTATIC inst) {
        try {
            ObjectInfo oi = classNames.get(inst.cIdx);
            FieldInfo fi = oi.get(inst.fIdx, true);
            Value<?, ?> value;
            if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
                value = currentFrame.pop2();
            } else {
                value = currentFrame.pop();
            }
            ThreadHandler.setStaticField(
                    Thread.currentThread().getId(), oi, inst.cIdx, fi.getFieldId(), value);
            oi.setStaticField(fi.getFieldId(), value.concrete);

            // For static initializer
            symbolicStateHandler.addSpecialElement(determineIid(inst.iid), "PUTSTATIC");

        } catch (Exception e) {
            throwError(inst, e);
        }
        // checkAndSetException(inst);
    }

    /**
     * Unconditional jump no handling needed
     *
     * @param inst The RET instruction
     */
    public void visitRET(RET inst) {}

    /**
     * Returns void from method, no handling needed (here)
     *
     * @param inst The RETURN instruction
     */
    public void visitRETURN(RETURN inst) {
        // checkAndSetException(inst);
    }

    /**
     * Retrieves a short from an array and puts it onto the symbolic stack
     *
     * @param inst The SALOAD instruction.
     */
    public void visitSALOAD(SALOAD inst) {
        try {
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) currentFrame.pop();
            if (arr instanceof ShortArrayValue sarr) {
                boolean result = checkArrayBounds(sarr, idx, inst.iid);
                if (result) {
                    currentFrame.push(sarr.getElement(idx));
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[SALOAD]: Unknown array type");
                currentFrame.push(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Stores a short into an array
     *
     * @param inst The SASTORE instruction.
     */
    public void visitSASTORE(SASTORE inst) {
        try {
            ShortValue val = currentFrame.pop().asShortValue();
            IntValue idx = currentFrame.pop().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) currentFrame.pop();
            if (ref instanceof ShortArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException(inst);
                }
            } else {
                logger.warn("[SASTORE]: Unknown array type");
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Pushes a short onto the symbolic stack as an integer
     *
     * @param inst The SIPUSH instruction
     */
    public void visitSIPUSH(SIPUSH inst) {
        currentFrame.push(ValueFactory.createNumericalValue(ValueType.intValue, inst.value));
    }

    /**
     * Swaps the top two words on the symbolic stack
     *
     * @param inst The SWAP instruction
     */
    public void visitSWAP(SWAP inst) {
        Value<?, ?> v1 = currentFrame.pop();
        Value<?, ?> v2 = currentFrame.pop();
        currentFrame.push(v1);
        currentFrame.push(v2);
    }

    public void visitINVOKEMETHOD_EXCEPTION(INVOKEMETHOD_EXCEPTION inst) {
        if (stack.size() == 1) {
            Main.terminate();
            return;
        }
        stack.pop();
        currentFrame = stack.peek();
        currentFrame.clear();
        currentFrame.push(PlaceHolder.instance); // placeholder for the exception object
    }

    /**
     * Removes a frame from the stack frame and puts the removed frame's return value onto the old
     * (current) frame's stack
     *
     * @param inst INVOKEMETHOD_END
     */
    public void visitINVOKEMETHOD_END(INVOKEMETHOD_END inst) {
        Frame old = stack.pop();
        currentFrame = stack.peek();
        if (old.nReturnWords == 2) {
            currentFrame.push2(old.getRet());
        } else if (old.nReturnWords == 1) {
            currentFrame.push(old.getRet());
        }
    }

    public void visitMAKE_SYMBOLIC(MAKE_SYMBOLIC inst) {}

    public void visitLOOP_BEGIN(LOOP_BEGIN inst) {
        /*int loopCnt = loopCounter.getOrDefault(inst.iid, 0);
        logger.info(
                "[LOOP BEGIN] Branch instruction iid: " + inst.iid + ", Loop count: " + loopCnt);
        loopCounter.put(inst.iid, loopCnt + 1);
        loops.push(inst.iid);

         */
    }

    public void visitLOOP_END(LOOP_END inst) {
        /*
        logger.info("[LOOP END] Branch instruction iid: " + inst.iid);
        int iid = loops.pop();
        assert iid == inst.iid;

         */
    }

    public void visitSPECIAL(SPECIAL inst) {}

    public void setNext(Instruction next) {
        this.next = next;
    }

    private ObjectValue initMultiArray(String desc, IntValue[] dims, int idx) {
        if (idx == dims.length - 1) {
            logger.info(
                    "[MULTIANEWARRAY]: Creating array for LAST dimension: "
                            + idx
                            + " of length"
                            + dims[idx]);
        } else {
            logger.info(
                    "[MULTIANEWARRAY]: Creating array for dimension: "
                            + idx
                            + " of length"
                            + dims[idx]);
        }
        // ToDo (Nils): This needs to be properly done
        ObjectArrayValue ref = ValueFactory.createObjectArrayValue(desc, dims[idx]);
        if (idx < dims.length - 1) {
            for (int i = 0; i < dims[idx].concrete; i++) {
                ref.getFields()[i] = initMultiArray(desc, dims, idx + 1);
            }
        }
        return ref;
    }

    /**
     * Creates a multidimensional array of references. Currently, no symbolic handling here because
     * arrays are not of primitive type? See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/63">Issue
     * 63</a>
     *
     * @param inst The MULTIANEWARRAY instruction
     */
    public void visitMULTIANEWARRAY(MULTIANEWARRAY inst) {
        IntValue[] dims = new IntValue[inst.dims];
        try {
            for (int i = 0; i < dims.length; i++) {
                IntValue i1 = currentFrame.pop().asIntValue();
                BooleanFormula constraint = i1.checkPositive();
                boolean result = i1.concrete >= 0;
                symbolicStateHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
                dims[dims.length - i - 1] = i1;
            }

            SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
            ArrayArrayValue arr = ArrayArrayValue.arrayMagic(context, inst.desc, dims);
            currentFrame.push(arr);
        } catch (Exception e) {
            throwError(inst, e);
        }
        checkAndSetException(inst);
    }

    /**
     * Builds a path-constraint based on where the execution continues. ToDo (Nils): Constraint
     * correctness not verified!
     *
     * @param inst The LOOKUPSWITCH instruction.
     */
    public void visitLOOKUPSWITCH(LOOKUPSWITCH inst) {
        int[] keys = inst.keys;

        IntValue i1 = currentFrame.pop().asIntValue();
        for (int key : keys) {
            BooleanFormula result =
                    i1.IF_ICMPEQ(
                            ValueFactory.createNumericalValue(ValueType.intValue, key)
                                    .asIntValue());
            symbolicStateHandler.checkAndSetBranch(
                    i1.concrete == key, result, determineIid(inst.iid));

            if (i1.concrete == key) return;
        }
    }

    /**
     * Builds a path-constraint based on where the execution continues. ToDo (Nils): Constraint
     * correctness not verified!
     *
     * @param inst The TABLESWITCH instruction.
     */
    public void visitTABLESWITCH(TABLESWITCH inst) {
        IntValue i1 = currentFrame.pop().asIntValue();
        for (int i = inst.min; i <= inst.max; i++) {
            BooleanFormula result =
                    i1.IF_ICMPEQ(
                            ValueFactory.createNumericalValue(ValueType.intValue, i).asIntValue());
            symbolicStateHandler.checkAndSetBranch(
                    i1.concrete == i,
                    result,
                    Integer.valueOf(Integer.toString(i) + Integer.toString(inst.iid)));

            if (i1.concrete == i) return;
        }
    }

    private boolean isIntegral(Object o) {
        return o instanceof Integer
                || o instanceof Short
                || o instanceof Character
                || o instanceof Boolean
                || o instanceof Byte;
    }

    private int toInteger(Object o) {
        if (o instanceof Integer i) {
            return i;
        } else if (o instanceof Short s) {
            return s;
        } else if (o instanceof Boolean b) {
            return b ? 1 : 0;
        } else if (o instanceof Byte b) {
            return b;
        } else if (o instanceof Character c) {
            return c;
        }
        throw new RuntimeException("This should not happen! SymbolicInterpreter::isIntegral(2627)");
    }

    private boolean checkEquality(Object o1, Object o2) {
        if (isIntegral(o1) && isIntegral(o2)) {
            return toInteger(o1) == toInteger(o2);
        } else {
            return o1.equals(o2);
        }
    }

    private void visitGETVALUE_primitive(GETVALUE_primitive inst, ValueType type) {
        try {
            boolean cat2 = type == ValueType.longValue || type == ValueType.doubleValue;
            Value<?, ?> peek = cat2 ? currentFrame.peek2() : currentFrame.peek();
            boolean isSymbolic = false;

            if (peek == PlaceHolder.symbolicInstance) {
                peek = PlaceHolder.instance;
                isSymbolic = true;
            }
            if (inst.i == 1) {
                isSymbolic = true;
            }
            if (peek == PlaceHolder.instance) {
                if (cat2) {
                    currentFrame.pop2();
                } else {
                    currentFrame.pop();
                }
                Value<?, ?> v = ValueFactory.createNumericalValue(type, inst.v);
                if (isSymbolic) {
                    v.MAKE_SYMBOLIC();
                }

                if (cat2) {
                    currentFrame.push2(v);
                } else {
                    currentFrame.push(v);
                }
            } else if (peek.concrete == null) {
                if (cat2) {
                    currentFrame.pop2();
                    currentFrame.push2(ValueFactory.createNumericalValue(type, inst.v));
                } else {
                    currentFrame.pop();
                    currentFrame.push(ValueFactory.createNumericalValue(type, inst.v));
                }
            } else if (!checkEquality(peek.concrete, inst.v)) {
                if (cat2) {
                    currentFrame.pop2();
                    currentFrame.push2(ValueFactory.createNumericalValue(type, inst.v));
                } else {
                    currentFrame.pop();
                    currentFrame.push(ValueFactory.createNumericalValue(type, inst.v));
                }
            }
        } catch (Exception e) {
            throwError(inst, e);
        }
    }

    /**
     * Error handler for all errors that occur during the shadow execution
     *
     * @param inst The instruction that caused the error
     * @param e The exception that was thrown
     */
    private void throwError(Instruction inst, Throwable e) {
        new ErrorHandler().handleException(inst.toString(), e);
    }
}
