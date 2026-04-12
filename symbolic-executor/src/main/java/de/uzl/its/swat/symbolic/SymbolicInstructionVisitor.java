package de.uzl.its.swat.symbolic;

import static de.uzl.its.swat.symbolic.value.reference.ObjectValue.ADDRESS_NULL;
import static de.uzl.its.swat.symbolic.value.reference.ObjectValue.ADDRESS_UNKNOWN;
import static java.lang.Thread.currentThread;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.*;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.coverage.BranchCoverage;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.instrument.Intrinsics;
import de.uzl.its.swat.metadata.ClassDepot;
import de.uzl.its.swat.metadata.ClassDepotRuntime;
import de.uzl.its.swat.symbolic.instruction.*;
import de.uzl.its.swat.symbolic.invoke.InvocationHandler;
import de.uzl.its.swat.symbolic.processor.InstructionProcessor;
import de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor;
import de.uzl.its.swat.symbolic.shadow.Frame;
import de.uzl.its.swat.symbolic.shadow.ShadowContext;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.ValueFactory;
import de.uzl.its.swat.symbolic.value.ValueType;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.StringBuilderValue;
import de.uzl.its.swat.symbolic.value.reference.array.*;
import de.uzl.its.swat.symbolic.value.reference.lang.*;
import de.uzl.its.swat.thread.ThreadHandler;

import java.lang.reflect.Modifier;
import java.util.*;
import lombok.Getter;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public class SymbolicInstructionVisitor implements IVisitor {
    // The stack of stack frames (method stacks)
    @Getter private final ShadowContext stack; //Todo rename?
    // Counter for the number of times an instruction ID (IID) was seen, used for creating trackable
    // unique ids in loops etc.
    private final HashMap<Long, Long> iidCounter = new HashMap<>();
    // The handler for the symbolic trace that is sent to the symbolic explorer
    @Getter private final SymbolicTraceHandler symbolicTraceHandler;

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    ClassDepotRuntime classDepot = ClassDepot.getRuntimeInstance();

    public SymbolicInstructionVisitor() throws NoThreadContextException {
        stack = new ShadowContext();
        symbolicTraceHandler = ThreadHandler.getSymbolicTraceHandler(currentThread().getId());
    }

    /**
     * Sets the next instruction to be executed
     *
     * @param inst The next instruction to be executed
     */
    public void setNextInst(Instruction inst) {
        stack.setNextInst(inst);
    }

    /**
     * Gets the next instruction to be executed
     *
     * @return The next instruction to be executed
     */
    public Instruction getNextInst() {
        return stack.getNextInst();
    }

    /**
     * Determines whether an exception triggered by looking for specific markers placed during
     * instrumentation. If the next instruction is not such a marker or special case, an exception
     * is assumed to have been triggered and the symbolic state is modified accordingly. Method
     * invocation exceptions are handled differently.
     *
     * @param inst The (next) instruction/ special marker after the instruction that could have
     *     caused an exception
     */
    private void checkAndSetException(Instruction inst) throws NoThreadContextException {
        symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), inst.getClass().getName());
        if (!((getNextInst() instanceof SPECIAL) && ((SPECIAL) getNextInst()).i == 0)
                && !(getNextInst() instanceof CLINIT)) {
            logger.info("Registered exception: {}", inst.getClass().getName());
            stack.clearOperandStack();
            stack.pushOperand(PlaceHolder.instance); // ToDo: add more specific placeholder
        }
    }

    /** Throws an error or exception in the symbolic frame. */
    private void enforceException() throws NoThreadContextException {
        logger.info("Enforced exception triggered");
        stack.clearOperandStack();
        stack.pushOperand(PlaceHolder.instance); // ToDo: add more specific placeholder
    }

    /**
     * Is used to see if a branch was taken when a branching condition is encountered ToDo (Nils):
     * Is the check correct? Why would i return true if the next is not special?
     *
     * @return True if branch was taken, false otherwise
     */
    private boolean isBranchTaken() {
        // The false branch does not have the special instruction.
        if (getNextInst() instanceof SPECIAL special) {
            // See InstructionMethodAdapter,
            // 1 corresponds to the true branch.
            return special.i != 1;
            // return (special).i == 1;
        }
        return true; // false;
    }

    public boolean checkArrayBounds(Value<?, ?> ref, IntValue idx, long iid) throws NoThreadContextException {
        SWATAssert.check(ref instanceof AbstractArrayValue || ref instanceof ObjectArrayValue, "Unknown array type!");

        if (ref instanceof AbstractArrayValue<?, ?, ?, ?, ?> arr) {
            BooleanFormula constraint = arr.checkIndex(idx);
            boolean result = 0 <= idx.concrete && idx.concrete < arr.size.concrete;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(iid));
            return result;
        } else {
            ObjectArrayValue arr = (ObjectArrayValue) ref;
            BooleanFormula constraint = arr.checkIndex(idx);
            boolean result = 0 <= idx.concrete && idx.concrete < arr.size.concrete;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(iid));
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
    public void visitAALOAD(AALOAD inst) throws SymbolicInstructionException {
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof ObjectArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(arr.getElement(idx));
                } else {
                    enforceException();
                }
            } else if (ref instanceof ArrayArrayValue<?> arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(arr.getElement(idx));
                } else {
                    enforceException();
                }

            } else if (ref instanceof StringArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(arr.getElement(idx));
                } else {
                    enforceException();
                }
            } else {
                stack.pushOperand(
                        ref.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores a reference value into an array
     *
     * @param inst The ASTORE instruction
     */
    public void visitAASTORE(AASTORE inst) throws SymbolicInstructionException{
        try {
            ObjectValue<?, ?> val = stack.popOperand().asObjectValue();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof ObjectArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException();
                }
            } else if (ref instanceof ArrayArrayValue<?> arr
                    && val instanceof AbstractArrayValue<?, ?, ?, ?, ?> val2) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val2);
                } else {
                    enforceException();
                }
            } else if (ref instanceof StringArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, (StringValue) val);
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[AASTORE]: Unknown array type: {}", ref.getClass().getSimpleName());
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Pushes NULL onto the symbolic stack
     *
     * @param inst The ACONST_NULL instruction
     */
    public void visitACONST_NULL(ACONST_NULL inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNULLValue());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Fetches a reference from the symbolic locals and puts it onto the symbolic stack
     *
     * @param inst One on the ALOAD (ALOAD, ALOAD_0 - ALOAD_3) instructions
     */
    public void visitALOAD(ALOAD inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(stack.getLocal(inst.var));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Creates a new array of references and puts the array onto the symbolic stack.
     *
     * @param inst The ANEWARRAY instruction.
     */
    public void visitANEWARRAY(ANEWARRAY inst) throws SymbolicInstructionException{
        try {
            IntValue size = stack.popOperand().asIntValue();
            BooleanFormula constraint = size.checkPositive();
            boolean result = size.concrete >= 0;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            stack.pushOperand(ValueFactory.createObjectArrayValue(inst.type, size));
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Takes the top value (has to be a reference) from the symbolic stack and sets it as the
     * symbolic return value
     *
     * @param inst The ARETURN instruction
     */
    public void visitARETURN(ARETURN inst) throws SymbolicInstructionException{
        try{
            symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "ARETURN");
            stack.setReturnValue(stack.popOperand());
            // checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Determines the size of an array and puts it onto the symbolic stack INFO: While the length is
     * tracked symbolically it is currently unclear to what effect that is useful See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/55">Issue</a>
     *
     * @param inst The ARRAYLENGTH instruction
     */
    public void visitARRAYLENGTH(ARRAYLENGTH inst) throws SymbolicInstructionException {
        try{
            Value<?, ?> v = stack.popOperand();
            if (v instanceof AbstractArrayValue<?, ?, ?, ?, ?> arr) {
                stack.pushOperand(arr.size);
            } else if (v instanceof ObjectArrayValue arr) {
                stack.pushOperand(arr.getSize());
            } else if (v instanceof ObjectValue<?, ?> ref) {
                // if (ref.getFields() != null) {
                //     stack.pushOperand(ref.getNFields());     <---- This is currently removed to ensure no wrong value ends up on the stack see Issue #99
                // } else {
                    stack.pushOperand(PlaceHolder.instance);
                // }

            } else {
                throw new SymbolicInstructionException(inst,
                        "Cannot determine the array length of a non array object: " + v);
            }
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores the top reference from the symbolic stack into the symbolic locals. The index of the
     * locals position is indicated by the instruction and not taken from the stack
     *
     * @param inst One of the ASTORE (ASTORE, ASTORE_0 - ASTORE_3) instructions. inst.var is the
     *     locals index
     */
    public void visitASTORE(ASTORE inst) throws SymbolicInstructionException{
        try{
            stack.setLocal(inst.var, stack.popOperand());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Throws an error or exception in the symbolic frame. The symbolic stack is cleared and only a
     * reference to the exception remains on the symbolic stack. ToDo (Nils): Could some information
     * be saved here to record that an exception has occurred? See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/57">...</a>
     *
     * @param inst The ATHROW instruction
     */
    public void visitATHROW(ATHROW inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> top = stack.peekOperand();
            stack.clearOperandStack();
            stack.pushOperand(top);
            checkAndSetException(inst); // Todo: is that necessary here?
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Retrieves a boolean or byte from an array and puts it onto the symbolic stack
     *
     * @param inst The BALOAD instruction.
     */
    public void visitBALOAD(BALOAD inst) throws SymbolicInstructionException{
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) stack.popOperand();
            if (arr instanceof BooleanArrayValue barr) {
                boolean result = checkArrayBounds(barr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(barr.getElement(idx));
                } else {
                    enforceException();
                }
            } else if(arr instanceof ByteArrayValue barr) {
                boolean result = checkArrayBounds(barr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(barr.getElement(idx));
                } else {
                    enforceException();
                }
         } else {
                logger.warn("[BALOAD]: Unknown array type");
                stack.pushOperand(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores a boolean or byte into an array
     *
     * @param inst The BASTORE instruction.
     */
    public void visitBASTORE(BASTORE inst) throws SymbolicInstructionException{
        try {
            Value<?, ?> val = stack.popOperand();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof BooleanArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val.asBooleanValue());
                } else {
                    enforceException();
                }
            } else if (ref instanceof ByteArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val.asByteValue());
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[BASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Pushes a byte onto the symbolic stack as an integer
     *
     * @param inst The BIPUSH instruction
     */
    public void visitBIPUSH(BIPUSH inst) throws SymbolicInstructionException{
        // ToDo (Nils): Some unforseen concequences because the byte is handled as an int?
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, inst.value));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Retrieves a char from an array and puts it onto the symbolic stack
     *
     * @param inst The CALOAD instruction.
     */
    public void visitCALOAD(CALOAD inst) throws SymbolicInstructionException{
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) stack.popOperand();
            if (arr instanceof CharArrayValue carr) {
                boolean result = checkArrayBounds(carr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(carr.getElement(idx));
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[CALOAD]: Unknown array type");
                stack.pushOperand(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores a char into an array
     *
     * @param inst The CASTORE instruction.
     */
    public void visitCASTORE(CASTORE inst) throws SymbolicInstructionException{
        try {
            CharValue val = stack.popOperand().asCharValue();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof CharArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[CASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * No symbolic tracking needed here. Exceptions in case the object cannot be cast need to be
     * caught.
     *
     * @param inst The CHECKCAST instruction
     */
    public void visitCHECKCAST(CHECKCAST inst) throws SymbolicInstructionException{
        try{
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Casts a double value to a float and puts the result onto the symbolic stack
     *
     * @param inst The D2F instruction
     */
    public void visitD2F(D2F inst) throws SymbolicInstructionException{
        try{
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushOperand(d1.D2F());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Casts a double to an integer and puts the int onto the symbolic stack
     *
     * @param inst The D2I instruction
     */
    public void visitD2I(D2I inst) throws SymbolicInstructionException{
        try{
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushOperand(d1.D2I());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Casts a double to a long and puts the long onto the symbolic stack
     *
     * @param inst The D2L instruction
     */
    public void visitD2L(D2L inst) throws SymbolicInstructionException{
        try{
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushWideOperand(d1.D2L());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Adds two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DADD instruction
     */
    public void visitDADD(DADD inst) throws SymbolicInstructionException{
        try{
            DoubleValue d2 = (DoubleValue) stack.popWideOperand();
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushWideOperand(d1.DADD(d2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Retrieves a double from an array and puts it onto the symbolic stack
     *
     * @param inst The DALOAD instruction.
     */
    public void visitDALOAD(DALOAD inst) throws SymbolicInstructionException{
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) stack.popOperand();
            if (arr instanceof DoubleArrayValue darr) {
                boolean result = checkArrayBounds(darr, idx, inst.iid);
                if (result) {
                    stack.pushWideOperand(darr.getElement(idx));
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[DALOAD]: Unknown array type");
                stack.pushWideOperand(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores a double into an array
     *
     * @param inst The DASTORE instruction.
     */
    public void visitDASTORE(DASTORE inst) throws SymbolicInstructionException{
        try {
            DoubleValue val = stack.popWideOperand().asDoubleValue();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof DoubleArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[DASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Compares two doubles and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The DCMPG instruction
     */
    public void visitDCMPG(DCMPG inst) throws SymbolicInstructionException{
        try{
            DoubleValue d2 = stack.popWideOperand().asDoubleValue();
            DoubleValue d1 = stack.popWideOperand().asDoubleValue();
            stack.pushOperand(d1.DCMPG(d2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Compares two doubles and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The DCMPL instruction
     */
    public void visitDCMPL(DCMPL inst) throws SymbolicInstructionException{
        try{
            DoubleValue d2 = stack.popWideOperand().asDoubleValue();
            DoubleValue d1 = stack.popWideOperand().asDoubleValue();
            stack.pushOperand(d1.DCMPL(d2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads the constant 0.0 onto the symbolic stack
     *
     * @param inst The DCONST_0 instruction
     */
    public void visitDCONST_0(DCONST_0 inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(ValueFactory.createNumericalValue(ValueType.doubleValue, 0.0));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads the constant 1.0 onto the symbolic stack
     *
     * @param inst The DCONST_1 instruction
     */
    public void visitDCONST_1(DCONST_1 inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(ValueFactory.createNumericalValue(ValueType.doubleValue, 1.0));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Divides two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DDIV instruction
     */
    public void visitDDIV(DDIV inst) throws SymbolicInstructionException{
        // ToDo (Nils):  What if one of the values is zero?
        try{
            DoubleValue d2 = (DoubleValue) stack.popWideOperand();
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushWideOperand(d1.DDIV(d2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads a double from the symbolic locals onto the symbolic stack.
     *
     * @param inst One of the DLOAD instructions (DLOAD, DLOAD_0 - DLOAD_3)
     */
    public void visitDLOAD(DLOAD inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(stack.getWideLocal(inst.var));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Multiplies two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DMUL instruction
     */
    public void visitDMUL(DMUL inst) throws SymbolicInstructionException{
        try{
            DoubleValue d2 = (DoubleValue) stack.popWideOperand();
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushWideOperand(d1.DMUL(d2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Negates a double and pushes the result onto the symbolic stack
     *
     * @param inst The DNEG instruction
     */
    public void visitDNEG(DNEG inst) throws SymbolicInstructionException{
        try{
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushWideOperand(d1.DNEG());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitDREM(DREM inst) throws SymbolicInstructionException{
        try{
            DoubleValue d2 = (DoubleValue) stack.popWideOperand();
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushWideOperand(d1.DREM(d2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Sets the return value of the current symbolic frame to the double value on top of the
     * symbolic stack
     *
     * @param inst The DRETURN instruction
     */
    public void visitDRETURN(DRETURN inst) throws SymbolicInstructionException{
        try{
            symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "DRETURN");
            stack.setReturnValue(stack.popWideOperand());
            // checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores the top value from the symbolic stack (double) into a slot in the symbolic locals
     *
     * @param inst One of the DSTORE instructions (DSTORE, DSTORE_0 - DSTORE_3)
     */
    public void visitDSTORE(DSTORE inst) throws SymbolicInstructionException{
        try{
            stack.setWideLocal(inst.var, stack.popWideOperand());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Subtracts two doubles and pushes the result onto the symbolic stack
     *
     * @param inst The DSUB instruction
     */
    public void visitDSUB(DSUB inst) throws SymbolicInstructionException{
        try{
            DoubleValue d2 = (DoubleValue) stack.popWideOperand();
            DoubleValue d1 = (DoubleValue) stack.popWideOperand();
            stack.pushWideOperand(d1.DSUB(d2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Duplicates the top value on the symbolic stack (only one word -> 8 bytes) | 1 | --> | 1 | | 1
     * |
     *
     * @param inst The DUP instruction
     */
    public void visitDUP(DUP inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(stack.peekOperand());} catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Duplicates the top value on the symbolic stack (two words -> 16 bytes) | 1 | | 1 | | 2 | -->
     * | 2 | | 1 | | 2 |
     *
     * @param inst The DUP2 instruction
     */
    public void visitDUP2(DUP2 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(stack.peekWideOperand());
            stack.pushOperand(stack.peekWideOperand());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Duplicate two words and insert them beneath a third word | 1 | | 1 | | 2 | --> | 2 | | 3 | |
     * 3 | | 1 | | 2 |
     *
     * @param inst The DUP2_X1 instruction
     */
    public void visitDUP2_X1(DUP2_X1 inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> word1 = stack.popOperand();
            Value<?, ?> word2 = stack.popOperand();
            Value<?, ?> word3 = stack.popOperand();
            stack.pushOperand(word2);
            stack.pushOperand(word1);
            stack.pushOperand(word3);
            stack.pushOperand(word2);
            stack.pushOperand(word1);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Duplicate two words and insert them beneath a fourth word | 1 | | 1 | | 2 | --> | 2 | | 3 | |
     * 3 | | 4 | | 4 | | 1 | | 2 |
     *
     * @param inst The DUP2_X2 instruction
     */
    public void visitDUP2_X2(DUP2_X2 inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> word1 = stack.popOperand();
            Value<?, ?> word2 = stack.popOperand();
            Value<?, ?> word3 = stack.popOperand();
            Value<?, ?> word4 = stack.popOperand();
            stack.pushOperand(word2);
            stack.pushOperand(word1);
            stack.pushOperand(word4);
            stack.pushOperand(word3);
            stack.pushOperand(word2);
            stack.pushOperand(word1);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Duplicate the top word and insert it beneath a second word | 1 | | 1 | | 2 | --> | 2 | | 1 |
     *
     * @param inst The DUP2_X1 instruction
     */
    public void visitDUP_X1(DUP_X1 inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> top = stack.popOperand();
            Value<?, ?> top2 = stack.popOperand();
            stack.pushOperand(top);
            stack.pushOperand(top2);
            stack.pushOperand(top);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }

    }

    /**
     * Duplicate the top word and insert it beneath the third word | 1 | | 1 | | 2 | --> | 2 | | 3 |
     * | 3 | | 1 |
     *
     * @param inst The DUP2_X1 instruction
     */
    public void visitDUP_X2(DUP_X2 inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> word1 = stack.popOperand();
            Value<?, ?> word2 = stack.popOperand();
            Value<?, ?> word3 = stack.popOperand();
            stack.pushOperand(word1);
            stack.pushOperand(word3);
            stack.pushOperand(word2);
            stack.pushOperand(word1);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Casts a float to a double and puts the result onto the symbolic stack
     *
     * @param inst The F2D instruction
     */
    public void visitF2D(F2D inst) throws SymbolicInstructionException{
        try{
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushWideOperand(f1.F2D());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Casts a float to an integer and puts the result onto the symbolic stack
     *
     * @param inst The F2I instruction
     */
    public void visitF2I(F2I inst) throws SymbolicInstructionException{
        try{
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.F2I());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitF2L(F2L inst) throws SymbolicInstructionException{
        try{
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushWideOperand(f1.F2L());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Adds two floats and adds the result onto the symbolic stack
     *
     * @param inst The FADD instruction
     */
    public void visitFADD(FADD inst) throws SymbolicInstructionException{
        try{
            FloatValue f2 = stack.popOperand().asFloatValue();
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FADD(f2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads a float from an array and puts it onto the symbolic stack
     *
     * @param inst The FALOAD instruction
     */
    public void visitFALOAD(FALOAD inst) throws SymbolicInstructionException{
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) stack.popOperand();
            if (arr instanceof FloatArrayValue farr) {
                boolean result = checkArrayBounds(farr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(farr.getElement(idx));
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[FALOAD]: Unknown array type");
                stack.pushOperand(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores a float into an array
     *
     * @param inst The FASTORE instruction
     */
    public void visitFASTORE(FASTORE inst) throws SymbolicInstructionException{
        try {
            FloatValue val = stack.popOperand().asFloatValue();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof FloatArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[FASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Compares two floats and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The FCMPG instruction
     */
    public void visitFCMPG(FCMPG inst) throws SymbolicInstructionException{
        try{
            FloatValue f2 = stack.popOperand().asFloatValue();
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FCMPG(f2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Compares two floats and encodes the result as an integer that is put onto the symbolic stack
     *
     * @param inst The FCMPL instruction
     */
    public void visitFCMPL(FCMPL inst) throws SymbolicInstructionException{
        try{
            FloatValue f2 = stack.popOperand().asFloatValue();
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FCMPL(f2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads the constant 0.0f as a float onto the symbolic stack
     *
     * @param inst The FCONST_0 instruction
     */
    public void visitFCONST_0(FCONST_0 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.floatValue, 0.0f));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads the constant 1.0f as a float onto the symbolic stack
     *
     * @param inst The FCONST_1 instruction
     */
    public void visitFCONST_1(FCONST_1 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.floatValue, 1.0f));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads the constant 2.0f as a float onto the symbolic stack
     *
     * @param inst The FCONST_2 instruction
     */
    public void visitFCONST_2(FCONST_2 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.floatValue, 2.0f));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Divides two floats and puts the result onto the symbolic stack
     *
     * @param inst The FDIV instruction
     */
    public void visitFDIV(FDIV inst) throws SymbolicInstructionException{
        try{
            FloatValue f2 = stack.popOperand().asFloatValue();
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FDIV(f2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads a float from the symbolic locals onto the symbolic stack
     *
     * @param inst One of the FLOAD instructions (FLOAD, FLOAD_0 - FLOAD_3)
     */
    public void visitFLOAD(FLOAD inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(stack.getLocal(inst.var));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Multiplies two floats and puts the result onto the symbolic stack
     *
     * @param inst The FMUL instruction
     */
    public void visitFMUL(FMUL inst) throws SymbolicInstructionException{
        try{
            FloatValue f2 = stack.popOperand().asFloatValue();
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FMUL(f2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Negates a float and puts the result onto the symbolic stack
     *
     * @param inst The FNEG instruction
     */
    public void visitFNEG(FNEG inst) throws SymbolicInstructionException{
        try{
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FNEG());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the remaider of the divion of two flaots and puts the result onto the symbolic
     * stack WARNING: This is currently only the concrete value
     *
     * @param inst The FREM instruction
     */
    public void visitFREM(FREM inst) throws SymbolicInstructionException{
        try{
            FloatValue f2 = stack.popOperand().asFloatValue();
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FREM(f2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Sets the return value of the current symbolic frame to the float value on top of the symbolic
     * stack
     *
     * @param inst The FRETURN instruction
     */
    public void visitFRETURN(FRETURN inst) throws SymbolicInstructionException{
        try{
            symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "FRETURN");
            stack.setReturnValue(stack.popOperand());
            // checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts the float on top of the symbolic stack into one of the symbolic locals of the current
     * frame
     *
     * @param inst One of the FSTORE instructions (FSTORE, FSTORE_0 - FSTORE_3)
     */
    public void visitFSTORE(FSTORE inst) throws SymbolicInstructionException{
        try{
            stack.setLocal(inst.var, stack.popOperand());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Subtracts two floats and puts the resulting FloatValuer onto the symbolic stack.
     *
     * @param inst The FSUB instruction.
     */
    public void visitFSUB(FSUB inst) throws SymbolicInstructionException{
        try{
            FloatValue f2 = stack.popOperand().asFloatValue();
            FloatValue f1 = stack.popOperand().asFloatValue();
            stack.pushOperand(f1.FSUB(f2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Fetches a field from an object instance and puts it onto the symbolic stack
     *
     * @param inst The GETFIELD instruction
     */
    public void visitGETFIELD(GETFIELD inst) throws SymbolicInstructionException{
        try {
            ObjectValue<?, ?> ref = stack.popOperand().asObjectValue();

            int fieldIndex = classDepot.getFieldIndex(inst.cIdx, inst.name, false);

            Value<?, ?> val = ref.getField(fieldIndex);
            if (val == PlaceHolder.instance) {
                // Specific placeholder to attribute the concrete value in the following GETVALUE_
                val = new PlaceHolder(PlaceHolder.ValueOrigin.GETFIELD, inst, ref);
            }
            if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
                stack.pushWideOperand(val);
            } else {
                stack.pushOperand(val);
            }
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    private Value<?, ?> fetchStaticField (GETSTATIC inst)
            throws ThreadAlreadyDisabledException, NoThreadContextException, ThreadAlreadyEnabledException, ClassNotFoundException {
        int runtimeFieldIndex = classDepot.getFieldIndex(inst.cIdx, inst.name, true);
        return ThreadHandler.getStaticField(
                        currentThread().getId(), inst.cIdx, runtimeFieldIndex);
    }

    /**
     * Fetches a static field from an object and puts it onto the symbolic stack
     *
     * @param inst The GETSTATIC instruction
     */
    public void visitGETSTATIC(GETSTATIC inst) throws SymbolicInstructionException{
        try {
            Value<?, ?> val;
            val = fetchStaticField(inst);
            if (val == PlaceHolder.instance) {
                // Specific placeholder to attribute the concrete value in the following GETVALUE_
                val = new PlaceHolder(PlaceHolder.ValueOrigin.GETSTATIC, inst, null);
            }


            if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
                stack.pushWideOperand(val);
            } else {
                stack.pushOperand(val);
            }
            // For static initializer
            // symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "GETSTATIC");

            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitGETVALUE_Object(GETVALUE_Object<?> inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> peek = stack.peekOperand();
            Value<?, ?> tmp;
            boolean isSymbolic = false;

            if (peek == PlaceHolder.symbolicInstance) {
                isSymbolic = true;
                peek = PlaceHolder.instance;
            }
            if (inst.i == 1) {
                isSymbolic = true;
            }

            /* if (peek instanceof LambdaPlaceHolder) {
                // remove the placeholder lambda value
                LambdaPlaceHolder lambdaPlaceHolder = (LambdaPlaceHolder) stack.popOperand();
                long key = lambdaPlaceHolder.getKey();
                int parentAddress = lambdaPlaceHolder.getParentAddress();
                // create a new object value (the return value from invoke dynamic)
                stack.pushOperand(ValueFactory.getLambdaObjectValue(inst.v, parentAddress, key));
            } else */

            if (peek instanceof PlaceHolder placeHolder) {

                // remove the placeholder value
                stack.popOperand();
                // try to get object
                tmp = stack.getFromHeap(inst.address);
                // check if the object was created earlier and then reuse it
                if (tmp != null) {
                    if (isSymbolic) {
                        tmp.MAKE_SYMBOLIC();
                    }
                    Logger shadowStateLogger = ThreadHandler.getShadowStateLogger(currentThread().getId());
                    shadowStateLogger.info("Recovered object from heap: {}", tmp);
                    if(tmp instanceof StringBuilderValue sbv){
                        StringBuilder sb = (StringBuilder) inst.val;
                        SWATAssert.check(sbv.getStringValue().concrete.equals(inst.val),
                                "Concrete value of the StringBuilder does not match the value in the stack: {} | {}",
                                sbv.getStringValue().concrete, sb.toString());
                    }
                    stack.pushOperand(tmp);
                } else if (inst.address == 0) {
                    SWATAssert.enforce(!isSymbolic, "Symbolic NULL object is not supported");
                    stack.pushOperand(ValueFactory.createNULLValue());
                } else {
                    tmp = ValueFactory.createObjectValue(inst.val, inst.address);
                    if (isSymbolic) {
                        tmp.MAKE_SYMBOLIC();
                    }
                    stack.pushOperand(tmp);
                    stack.putToHeap(inst.address, tmp); // save the object for future use
                    if (placeHolder.origin == PlaceHolder.ValueOrigin.GETFIELD) {
                        ObjectValue<?, ?> ref = placeHolder.referenceValue;
                        GETFIELD gfInst = (GETFIELD) placeHolder.inst;

                        int fieldIndex = classDepot.getFieldIndex(gfInst.cIdx, gfInst.name, false);
                        ref.setField(fieldIndex, tmp);
                        Logger shadowStateLogger = ThreadHandler.getShadowStateLogger(currentThread().getId());
                        shadowStateLogger.debug("Storing retrieved value in field {} of object {}",
                                gfInst.name, ref);
                    } else if (placeHolder.origin == PlaceHolder.ValueOrigin.GETSTATIC) {
                        GETSTATIC gsInst = (GETSTATIC) placeHolder.inst;
                        setStaticField(tmp, gsInst.cIdx, gsInst.name);
                        Logger shadowStateLogger = ThreadHandler.getShadowStateLogger(currentThread().getId());
                        shadowStateLogger.debug("Storing retrieved value in field {}",gsInst.name);
                    }
                }
            } else if ((peek.asObjectValue()).getAddress() == ADDRESS_UNKNOWN) {
                // set the address of the object
                if (inst.address == 0) {
                    stack.popOperand();
                    stack.pushOperand(ValueFactory.createNULLValue());
                } else if (inst.val != null) {
                    if (inst.val instanceof String s) {
                        SWATAssert.check(inst.val.equals(peek.concrete),
                                "Concrete value of the object does not match the value in the stack: {} | {}",
                                inst.val, peek.concrete);
                        if(peek.formula == null) {
                            // TODO This needs to be cleaned up!
                            stack.popOperand();
                            StringValue val = ValueFactory.createStringValue(s, inst.address);
                            stack.pushOperand(val);
                            stack.putToHeap(inst.address, val);
                        } else {
                            (peek.asObjectValue()).setAddress(inst.address);
                            stack.putToHeap(inst.address, peek);
                        }

                    } else {
                        (peek.asObjectValue()).setAddress(inst.address);
                        stack.putToHeap(inst.address, peek);
                    }
                } else {
                    // Need to obtain the Object address
                    (peek.asObjectValue()).setAddress(inst.address);
                    stack.putToHeap(inst.address, peek);
                }
            } else if ((peek.asObjectValue()).getAddress() == ADDRESS_NULL) {
                SWATAssert.check(inst.val == null,
                        "Object value is NULL, but the instruction has a non-null value: " + inst.val);
                //logger.warn("Object value is NULL, but the instruction has a non-null value: {}", inst.val);
                // Todo: This was tolerated for auto-generated values from databases but should be handled by catching autogenerated ids
            } else {
                if (inst.val == null) {
                    logger.warn("Tolerating this case for INVOKEDYNAMIC, but could be error case");
                } else {
                    // Check if we're in delegation context
                    boolean inDelegation = false;
                    long threadId = Thread.currentThread().getId();
                    InstructionProcessor processor = ThreadHandler.getProcessor(threadId);
                    if (processor instanceof SymbolicInstructionProcessor sip) {
                        inDelegation = sip.isInDelegation();
                    }

                    // During delegation, object addresses can differ (e.g., PushbackReader vs IntReader)
                    // We need to replace the stack object with the actual delegated object
                    if (!inDelegation) {
                        SWATAssert.check(peek.asObjectValue().getAddress() == inst.address,
                                "[{}] Addresses must match: {} | {} !", inst, peek, inst);
                    } else {
                        // In delegation context, fetch the actual delegated object and replace it on stack
                        if (peek.asObjectValue().getAddress() != inst.address) {
                            logger.debug("Delegation detected: stack has @{}, instruction expects @{}. Fetching delegated object.",
                                    String.format("%08x", peek.asObjectValue().getAddress()),
                                    String.format("%08x", inst.address));

                            // Pop the wrapper object (e.g., PushbackReader)
                            stack.popOperand();

                            // Fetch the delegated object from the heap (e.g., IntReader)
                            Object heapObj = stack.getFromHeap(inst.address);
                            if (heapObj instanceof Value<?, ?>) {
                                Value<?, ?> delegatedObject = (Value<?, ?>) heapObj;
                                // Push the delegated object onto the stack
                                stack.pushOperand(delegatedObject);
                                logger.debug("Replaced with delegated object: {}", delegatedObject);
                            } else if (inst.val != null) {
                                // If not in heap yet, create it from the instruction using ValueFactory
                                try {
                                    Value<?, ?> delegatedObject = de.uzl.its.swat.symbolic.value.ValueFactory.createObjectValue(inst.val, inst.address);
                                    stack.pushOperand(delegatedObject);
                                    stack.putToHeap(inst.address, delegatedObject);
                                    logger.debug("Created new delegated object from instruction: {}", delegatedObject);
                                } catch (Exception e) {
                                    logger.error("Failed to create delegated object from instruction value", e);
                                    // Fall back to pushing peek back
                                    logger.warn("Could not create delegated object @{}, keeping original",
                                            String.format("%08x", inst.address));
                                    stack.pushOperand(peek);
                                }
                            } else {
                                // Fall back to pushing peek back
                                logger.warn("Could not find delegated object @{}, keeping original",
                                        String.format("%08x", inst.address));
                                stack.pushOperand(peek);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Artificial instruction used to fetch concrete boolean values
     *
     * @param inst The (artificial) GETVALUE_boolean instruction
     */
    public void visitGETVALUE_boolean(GETVALUE_boolean inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.booleanValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_byte instruction
     */
    public void visitGETVALUE_byte(GETVALUE_byte inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.byteValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_char instruction
     */
    public void visitGETVALUE_char(GETVALUE_char inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.charValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_double instruction
     */
    public void visitGETVALUE_double(GETVALUE_double inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.doubleValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_float instruction
     */
    public void visitGETVALUE_float(GETVALUE_float inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.floatValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_int instruction
     */
    public void visitGETVALUE_int(GETVALUE_int inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.intValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_long instruction
     */
    public void visitGETVALUE_long(GETVALUE_long inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.longValue);
    }

    /**
     * Artificial instruction used to fetch concrete values
     *
     * @param inst The (artificial) GETVALUE_short instruction
     */
    public void visitGETVALUE_short(GETVALUE_short inst) throws SymbolicInstructionException{
        visitGETVALUE_primitive(inst, ValueType.shortValue);
    }

    public void visitGETVALUE_void(GETVALUE_void inst) throws SymbolicInstructionException{
        // TODO: Why does the case exist if it does nothing?
    }

    /**
     * No symbolic tracking needed for the uncoditional jump.
     *
     * @param inst Either the GOTO or GOTO_w instruction
     */
    public void visitGOTO(GOTO inst) throws SymbolicInstructionException{}

    /**
     * Converts an integer to a byte and puts the result onto the symbolic stack
     *
     * @param inst The I2B instruction
     */
    public void visitI2B(I2B inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.I2B());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Converts an integer to a char and puts the result onto the symbolic stack ToDo (Nils):
     * Symbolic information regarding the integer is currently lost here! See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/60">Issue
     * 60</a>
     *
     * @param inst The I2C instruction
     */
    public void visitI2C(I2C inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.I2C());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Converts an integer to a double and puts the result onto the symbolic stack
     *
     * @param inst The I2D instruction
     */
    public void visitI2D(I2D inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushWideOperand(i1.I2D());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Converts an integer to a float and puts the result onto the symbolic stack
     *
     * @param inst The I2F instruction
     */
    public void visitI2F(I2F inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.I2F());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Converts an integer to a long and puts the result onto the symbolic stack
     *
     * @param inst The I2L instruction
     */
    public void visitI2L(I2L inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushWideOperand(i1.I2L());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Converts an integer to a short and puts the result onto the symbolic stack
     *
     * @param inst The I2S instruction
     */
    public void visitI2S(I2S inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.I2S());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Adds two integers and pushes the result onto the symbolic stack
     *
     * @param inst The IADD instruction
     */
    public void visitIADD(IADD inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.IADD(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads an integer from an array and puts it onto the symbolic stack
     *
     * @param inst The IALOAD instruction
     */
    public void visitIALOAD(IALOAD inst) throws SymbolicInstructionException{
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof IntArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(arr.getElement(idx));
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[IALOAD]: Unknown array type");
                stack.pushOperand(
                        ref.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise and of two integers in binary representation and puts the result onto
     * the symbolic stack
     *
     * @param inst The IAND instruction
     */
    public void visitIAND(IAND inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.IAND(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores an integer into an array
     *
     * @param inst The IASTORE instruction
     */
    public void visitIASTORE(IASTORE inst) throws SymbolicInstructionException{
        try {
            IntValue val = stack.popOperand().asIntValue();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof IntArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[IASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Load the int value 0 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_0(ICONST_0 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, 0));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Load the int value 1 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_1(ICONST_1 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, 1));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Load the int value 2 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_2(ICONST_2 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, 2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Load the int value 3 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_3(ICONST_3 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, 3));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Load the int value 4 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_4(ICONST_4 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, 4));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Load the int value 5 onto the symbolic stack
     *
     * @param inst Current instruction
     */
    public void visitICONST_5(ICONST_5 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, 5));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Load the int value -1 onto the symbolic stack
     *
     * @param inst Current ICONST_M1 instruction
     */
    public void visitICONST_M1(ICONST_M1 inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, -1));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Divides two integers and puts the result onto the symbolic stack.
     *
     * @param inst Current IDIV instruction
     */
    public void visitIDIV(IDIV inst) throws SymbolicInstructionException{
        try {
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i2.checkZero();
            // Check for exception
            boolean result = i2.concrete != 0;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                stack.pushOperand(i1.IDIV(i2));
            } else {
                enforceException();
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Branches if the top value on the stack is equal to 0
     *
     * @param inst The IFEQ instruction
     */
    public void visitIFEQ(IFEQ inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            // IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IFEQ();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Branches if the top value on the stack is greater or equal to 0
     *
     * @param inst The IFGE instruction
     */
    public void visitIFGE(IFGE inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IFGE();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Branches if the top value on the stack is greater then 0
     *
     * @param inst The IFGT instruction
     */
    public void visitIFGT(IFGT inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IFGT();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Branches if the top value on the stack is less or equal to 0
     *
     * @param inst The IFLE instruction
     */
    public void visitIFLE(IFLE inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IFLE();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Branches if the top value on the stack is less then 0
     *
     * @param inst The IFLT instruction
     */
    public void visitIFLT(IFLT inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IFLT();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if the current Value is not equal to 0
     *
     * @param inst The IFNE instruction
     */
    public void visitIFNE(IFNE inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IFNE();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if an object is not null and saves the resulting symbolic formula as a path constraint
     *
     * @param inst The IFNONNULL instruction.
     */
    public void visitIFNONNULL(IFNONNULL inst) throws SymbolicInstructionException{
        try{
            ObjectValue<?, ?> o1 = stack.popOperand().asObjectValue();
            BooleanFormula constraint = o1.IFNONNULL();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if an object is null and saves the resulting symbolic formula as a path constraint
     *
     * @param inst The IFNULL instruction.
     */
    public void visitIFNULL(IFNULL inst) throws SymbolicInstructionException{
        try{
            ObjectValue<?, ?> o1 = stack.popOperand().asObjectValue();
            BooleanFormula constraint = o1.IFNULL();
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if two object references are equal and saves the resulting symbolic formula as a path
     * constraint
     *
     * @param inst The IF_ACMPEQ instruction.
     */
    public void visitIF_ACMPEQ(IF_ACMPEQ inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> v2 = stack.popOperand();
            Value<?, ?> v1 = stack.popOperand();
            BooleanFormula constraint;
            if (v1 instanceof StringValue s1 && v2 instanceof StringValue s2) {
                constraint = s1.IF_ACMPEQ(s2);
            } else {
                ObjectValue<?, ?> o1 = v1.asObjectValue();
                ObjectValue<?, ?> o2 = v2.asObjectValue();
                constraint = o1.IF_ACMPEQ(o2);
            }
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);
            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if two object references are not equal and saves the resulting symbolic formula as a
     * path constraint
     *
     * @param inst The IF_ACMPNE instruction.
     */
    public void visitIF_ACMPNE(IF_ACMPNE inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> v2 = stack.popOperand();
            Value<?, ?> v1 = stack.popOperand();
            BooleanFormula constraint;
            if (v1 instanceof StringValue s1 && v2 instanceof StringValue s2) {
                constraint = s1.IF_ACMPNE(s2);
            } else {
                ObjectValue<?, ?> o1 = v1.asObjectValue();
                ObjectValue<?, ?> o2 = v2.asObjectValue();
                constraint = o1.IF_ACMPNE(o2);
            }
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if the two integers are equal
     *
     * @param inst The IF_ICMPEQ instruction
     */
    public void visitIF_ICMPEQ(IF_ICMPEQ inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IF_ICMPEQ(i2);
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public long determineIid(long iid) {
        long currentCnt = iidCounter.getOrDefault(iid, 0L);
        iidCounter.put(iid, currentCnt + 1);

        return GlobalStateForInstrumentation.createLoopIid(iid, (int) currentCnt);
    }

    /**
     * Checks if the integer is greater or equal to the second integer
     *
     * @param inst The IF_ICMPGE instruction
     */
    public void visitIF_ICMPGE(IF_ICMPGE inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IF_ICMPGE(i2);
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if the integer is greater than the second integer
     *
     * @param inst The IF_ICMPGT instruction
     */
    public void visitIF_ICMPGT(IF_ICMPGT inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IF_ICMPGT(i2);
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if the integer is less than or equal to the second integer
     *
     * @param inst The IF_ICMPLE instruction
     */
    public void visitIF_ICMPLE(IF_ICMPLE inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IF_ICMPLE(i2);
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if the integer is greater than the second integer
     *
     * @param inst The IF_ICMPLT instruction
     */
    public void visitIF_ICMPLT(IF_ICMPLT inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IF_ICMPLT(i2);
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Checks if the wo integers are not equal
     *
     * @param inst The IF_ICMPNE instruction
     */
    public void visitIF_ICMPNE(IF_ICMPNE inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i1.IF_ICMPNE(i2);
            boolean isBranchTaken = isBranchTaken();
            long iid = determineIid(inst.iid);

            BranchCoverage.addVisitedBranch(inst.iid);

            symbolicTraceHandler.checkAndSetBranch(isBranchTaken, constraint, iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Increments an integer that is in the locals. No change to the symbolic stack
     *
     * @param inst The IINC instruction
     */
    public void visitIINC(IINC inst) throws SymbolicInstructionException{
        try{
            // ToDo (Nils): What happens if this is the first time the local is referenced? The next
            // line should return a Placeholder then
            IntValue i1 = stack.getLocal(inst.var).asIntValue();
            stack.setLocal(inst.var, i1.IINC(inst.increment));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads an integer from the locals onto the stack After this instruction a GETVALUE_int
     * instruction follows to fetch the concrete value if the sybolic locals did not contain the
     * value
     *
     * @param inst The ILOAD instruction (including ILOAD_0 - ILOAD_3)
     */
    public void visitILOAD(ILOAD inst) throws SymbolicInstructionException{
        try{
            // ToDO (Nils): Why are ILOAD_0 etc not present? Where did they catch them and used this
            // case?
            stack.pushOperand(stack.getLocal(inst.var));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Multiplies two integers and adds the result onto the symbolic stack
     *
     * @param inst The IMUL instruction
     */
    public void visitIMUL(IMUL inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.IMUL(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Negates an integer and puts the resulting value onto the symbolic stack
     *
     * @param inst The INEG instruction
     */
    public void visitINEG(INEG inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.INEG());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Removes the object reference from the symbolic stack and pushes true onto the symbolic stack.
     * ToDo (Nils): Find a way to push the correct value onto the stack here See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/61">Issue
     * 61</a>
     *
     * @param inst The INSTANCEOF instruction.
     */
    public void visitINSTANCEOF(INSTANCEOF inst) throws SymbolicInstructionException{
        try {
            stack.popOperand();
            stack.pushOperand(PlaceHolder.instance);
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Invokes an interface method on an object reference from the symbolic stack
     *
     * @param inst The INVOKEINTERFACE instruction
     */
    public void visitINVOKEINTERFACE(INVOKEINTERFACE inst) throws SymbolicInstructionException{
        try{
            stack.newStackFrame(inst.desc, inst.owner, inst.name, true);

            if (stack.getNextInst() instanceof INVOKEMETHOD_END) {

                ObjectValue<?, ?> instance = stack.getInstance();
                Value<?, ?>[] arguments = stack.fetchArgumentsFromLocals(Type.getArgumentTypes(inst.desc), true);
                Value<?, ?> retVal =
                        InvocationHandler.invoke(
                                symbolicTraceHandler,
                                inst.desc,
                                inst.owner,
                                inst.name,
                                inst.invokeId,
                                new ArrayList<>(Arrays.asList(arguments)),
                                true,
                                instance);
                stack.setReturnValue(retVal);

        }

        if (!inst.owner.equals("de/uzl/its/swat/Main")) {
            symbolicTraceHandler.recordInvocation(
                    determineIid(inst.iid), inst.getClass().getCanonicalName());
        }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Invokes an instance method on an object reference from the symbolic stack
     *
     * @param inst The INVOKESPECIAL instruction
     */
    public void visitINVOKESPECIAL(INVOKESPECIAL inst) throws SymbolicInstructionException{
        try{
            stack.newStackFrame(inst.desc, inst.owner, inst.name, true);

            if (stack.getNextInst() instanceof INVOKEMETHOD_END) {
                ObjectValue<?, ?> instance = stack.getInstance();
                Value<?, ?>[] arguments = stack.fetchArgumentsFromLocals(Type.getArgumentTypes(inst.desc), true);
                Value<?, ?> retVal =
                        InvocationHandler.invoke(
                                symbolicTraceHandler,
                                inst.desc,
                                inst.owner,
                                inst.name,
                                inst.invokeId,
                                new ArrayList<>(Arrays.asList(arguments)),
                                true,
                                instance);
                stack.setReturnValue(retVal);
            }

            if (!inst.owner.equals("de/uzl/its/swat/Main")) {
                symbolicTraceHandler.recordInvocation(
                        determineIid(inst.iid), inst.getClass().getCanonicalName());
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Invokes a static method
     *
     * @param inst The INVOKESTATIC instruction
     */
    public void visitINVOKESTATIC(INVOKESTATIC inst) throws SymbolicInstructionException{
        try{
            stack.newStackFrame(inst.desc, inst.owner, inst.name, false);

            if (stack.getNextInst() instanceof INVOKEMETHOD_END) {
               Value<?, ?>[] arguments = stack.fetchArgumentsFromLocals(Type.getArgumentTypes(inst.desc), false);
                Value<?, ?> retVal =
                        InvocationHandler.invoke(
                                symbolicTraceHandler,
                                inst.desc,
                                inst.owner,
                                inst.name,
                                inst.invokeId,
                                new ArrayList<>(Arrays.asList(arguments)),
                                false,
                                null);
                stack.setReturnValue(retVal);
            }

            if (!inst.owner.equals("de/uzl/its/swat/Main")) {
                symbolicTraceHandler.recordInvocation(
                        determineIid(inst.iid), inst.getClass().getCanonicalName());
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Invokes a virtual method on an object reference from the symbolic stack
     *
     * @param inst The INVOKEVIRTUAL instruction
     */
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL inst) throws SymbolicInstructionException{
        try{
            stack.newStackFrame(inst.desc, inst.owner, inst.name, true);

            if (stack.getNextInst() instanceof INVOKEMETHOD_END) {
                ObjectValue<?, ?> instance = stack.getInstance();
                Value<?, ?>[] arguments = stack.fetchArgumentsFromLocals(Type.getArgumentTypes(inst.desc), true);
                Value<?, ?> retVal =
                        InvocationHandler.invoke(
                                symbolicTraceHandler,
                                inst.desc,
                                inst.owner,
                                inst.name,
                                inst.invokeId,
                                new ArrayList<>(Arrays.asList(arguments)),
                                true,
                                instance);
                stack.setReturnValue(retVal);
            }

            if (!inst.owner.equals("de/uzl/its/swat/Main")) {
                symbolicTraceHandler.recordInvocation(
                        determineIid(inst.iid), inst.getClass().getCanonicalName());
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Invokes a dynamic method
     *
     * @param inst The INVOKEDYNAMIC instruction
     */
    public void visitINVOKEDYNAMIC(INVOKEDYNAMIC inst) throws SymbolicInstructionException{
        try{
            stack.newStackFrame(inst.desc, inst.owner, inst.name, false);

            if (stack.getNextInst() instanceof INVOKEMETHOD_END) {
                Value<?, ?>[] arguments = stack.fetchArgumentsFromLocals(Type.getArgumentTypes(inst.desc), false);
                Value<?, ?> retVal =
                        InvocationHandler.invoke(
                                symbolicTraceHandler,
                                inst.desc,
                                inst.owner,
                                inst.name,
                                inst.invokeId,
                                new ArrayList<>(Arrays.asList(arguments)),
                                false,
                                null);
                stack.setReturnValue(retVal);
            }

            if (!inst.owner.equals("de/uzl/its/swat/Main")) {
                symbolicTraceHandler.recordInvocation(
                        determineIid(inst.iid), inst.getClass().getCanonicalName());
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise OR of two integers and puts the result onto the symbolic stack
     *
     * @param inst The IOR instruction
     */
    public void visitIOR(IOR inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.IOR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the modulo of two integers and puts the result onto the symbolic stack
     *
     * @param inst The IREM instruction
     */
    public void visitIREM(IREM inst) throws SymbolicInstructionException{
        try {
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            BooleanFormula constraint = i2.checkZero();
            // Check for exception
            boolean result = i2.concrete != 0;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                stack.pushOperand(i1.IREM(i2));
            } else {
                enforceException();
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Sets the return value of the current symbolic frame to the int value on top of the symbolic
     * stack
     *
     * @param inst The IRETURN instruction
     */
    public void visitIRETURN(IRETURN inst) throws SymbolicInstructionException{
        try{
            symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "IRETURN");
            stack.setReturnValue(stack.popOperand());
            // checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Shifts an integer bitwise to the left and puts the result onto the symbolic stack
     *
     * @param inst The ISHL instruction
     */
    public void visitISHL(ISHL inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.ISHL(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Shifts an integer bitwise arithmetically to the right and puts the result onto the symbolic
     * stack
     *
     * @param inst The ISHR instruction
     */
    public void visitISHR(ISHR inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.ISHR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts an integer from the symbolic stack into the symbolic locals
     *
     * @param inst The ISTORE instruction
     */
    public void visitISTORE(ISTORE inst) throws SymbolicInstructionException{
        try{
            stack.setLocal(inst.var, stack.popOperand());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Subtracts two ints and puts the result onto the symbolic stack
     *
     * @param inst The ISUB instruction
     */
    public void visitISUB(ISUB inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.ISUB(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Shifts an integer bitwise logically to the right and puts the result onto the symbolic stack
     *
     * @param inst The IUSHR instruction
     */
    public void visitIUSHR(IUSHR inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.IUSHR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise exclusive OR of two integers and puts the result onto the symbolic
     * stack
     *
     * @param inst The IXOR instruction
     */
    public void visitIXOR(IXOR inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            IntValue i1 = stack.popOperand().asIntValue();
            stack.pushOperand(i1.IXOR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Jump subroutine. No symbolic handling needed. ToDo (Nils): The instruction pushes the return
     * address onto the symbolic stack Should a placeholder be put here? See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/62">Issue
     * 62</a>
     *
     * @param inst Either the JSR or JSR_W instruction
     */
    public void visitJSR(JSR inst) throws SymbolicInstructionException{}

    public void visitL2D(L2D inst) throws SymbolicInstructionException{
        try{
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.L2D());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitL2F(L2F inst) throws SymbolicInstructionException{
        try{
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushOperand(i1.L2F());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitL2I(L2I inst) throws SymbolicInstructionException{
        try{
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushOperand(i1.L2I());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Adds two longs and adds the result onto the symbolic stack
     *
     * @param inst The LADD instruction
     */
    public void visitLADD(LADD inst) throws SymbolicInstructionException{
        try{
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LADD(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads a long from an array and puts it onto the symbolic stack
     *
     * @param inst The LALOAD instruction
     */
    public void visitLALOAD(LALOAD inst) throws SymbolicInstructionException{
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) stack.popOperand();
            if (arr instanceof LongArrayValue larr) {
                boolean result = checkArrayBounds(larr, idx, inst.iid);
                if (result) {
                    stack.pushWideOperand(larr.getElement(idx));
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[LALOAD]: Unknown array type");
                stack.pushWideOperand(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise and of two longs in binary representation and puts the resulting long
     * onto the stack
     *
     * @param inst The LAND instruction
     */
    public void visitLAND(LAND inst) throws SymbolicInstructionException{
        try{
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LAND(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores a long into an array
     *
     * @param inst The LASTORE instruction
     */
    public void visitLASTORE(LASTORE inst) throws SymbolicInstructionException{
        try {
            LongValue val = stack.popWideOperand().asLongValue();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof LongArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[LASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Compares two longs and adds the result encoded as an integer onto the symbolic stack
     *
     * @param inst The LCMP instruction
     */
    public void visitLCMP(LCMP inst) throws SymbolicInstructionException{
        try{
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushOperand(i1.LCMP(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts the constant 0L as a long onto the symbolic stack
     *
     * @param inst The LCONST_0 instruction
     */
    public void visitLCONST_0(LCONST_0 inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(ValueFactory.createNumericalValue(ValueType.longValue, 0L));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts the constant 1L as a long onto the symbolic stack
     *
     * @param inst The LCONST_1 instruction
     */
    public void visitLCONST_1(LCONST_1 inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(ValueFactory.createNumericalValue(ValueType.longValue, 1L));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts a constant string onto the symbolic stack
     *
     * @param inst The (artificial) LDC_String instruction
     */
    public void visitLDC_String(LDC_String inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createStringValue(inst.c, inst.address));
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts an object onto the symbolic stack
     *
     * @param inst The (artificial) LDC_Object instruction
     */
    public void visitLDC_Object(LDC_Object inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> tmp = stack.getFromHeap(inst.c);
            if (tmp != null) {
                stack.pushOperand(tmp);
            } else if (inst.c == 0) {
                stack.pushOperand(ValueFactory.createNULLValue());
            } else {
                stack.pushOperand(tmp = ValueFactory.createObjectValue(null, inst.c));
                stack.putToHeap(inst.c, tmp);
            }
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts a constant double onto the symbolic stack
     *
     * @param inst The (artificial) LDC_double instruction
     */
    public void visitLDC_double(LDC_double inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(ValueFactory.createNumericalValue(ValueType.doubleValue, inst.c));
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Puts a constant float onto the symbolic stack
     *
     * @param inst The (artificial) LDC_float instruction
     */
    public void visitLDC_float(LDC_float inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.floatValue, inst.c));
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads a constant int onto the symbolic stack
     *
     * @param inst The (artificial) LDC_int instruction
     */
    public void visitLDC_int(LDC_int inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, inst.c));
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads a constant long onto the symbolic stack
     *
     * @param inst The (artificial) LDC_long instruction
     */
    public void visitLDC_long(LDC_long inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(ValueFactory.createNumericalValue(ValueType.longValue, inst.c));
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Divides two longs and puts the result onto the symbolic stack
     *
     * @param inst The LDIV instruction
     */
    public void visitLDIV(LDIV inst) throws SymbolicInstructionException{
        try {
            LongValue l2 = stack.popWideOperand().asLongValue();
            LongValue l1 = stack.popWideOperand().asLongValue();
            BooleanFormula constraint = l2.checkZero();
            // Check for exception
            boolean result = l2.concrete != 0;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                stack.pushWideOperand(l1.LDIV(l2));
            } else {
                enforceException();
            }

        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Loads a long from the symbolic locals and adds it to the symbolic stack
     *
     * @param inst One of the LLOAD isntructions (LLOAD, LLOAD_0 - LLOAD_3)
     */
    public void visitLLOAD(LLOAD inst) throws SymbolicInstructionException{
        try{
            stack.pushWideOperand(stack.getWideLocal(inst.var));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Multiplies two longs and puts the result onto the symbolic stack
     *
     * @param inst The LMUL instruction
     */
    public void visitLMUL(LMUL inst) throws SymbolicInstructionException{
        try{
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LMUL(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Negates a long and puts the result onto the symbolic stack
     *
     * @param inst The LNEG isntruction
     */
    public void visitLNEG(LNEG inst) throws SymbolicInstructionException{
        try{
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LNEG());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise or of two longs and puts the result onto the symbolic stack
     *
     * @param inst The LOR instruction
     */
    public void visitLOR(LOR inst) throws SymbolicInstructionException{
        try{
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LOR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the modulo of two longs and puts the result onto the symbolic stack
     *
     * @param inst The LREM instruction
     */
    public void visitLREM(LREM inst) throws SymbolicInstructionException{
        try {
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            BooleanFormula constraint = i2.checkZero();
            // Check for exception
            boolean result = i2.concrete != 0;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
            if (result) {
                stack.pushWideOperand(i1.LREM(i2));
            } else {
                enforceException();
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Sets the return value of the current symbolic frame to the long value on top of the symbolic
     * stack
     *
     * @param inst The LRETURN instruction
     */
    public void visitLRETURN(LRETURN inst) throws SymbolicInstructionException{
        try{
            symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "lRETURN");
            stack.setReturnValue(stack.popWideOperand());
            // checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise left shift of a long and puts the result onto the symbolic stack
     *
     * @param inst The LSHL instruction
     */
    public void visitLSHL(LSHL inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LSHL(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise right shift of a long and puts the result onto the symbolic stack
     *
     * @param inst The LSHR instruction
     */
    public void visitLSHR(LSHR inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LSHR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores the top value from the symbolic stack (long) into a slot in the symbolic locals
     *
     * @param inst One of the LSTORE instructions (LSTORE, LSTORE_0 - LSTORE_3)
     */
    public void visitLSTORE(LSTORE inst) throws SymbolicInstructionException{
        try{
            stack.setWideLocal(inst.var, stack.popWideOperand());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Subtracts two longs and puts the result onto the symbolic stack
     *
     * @param inst The LSUB instruction
     */
    public void visitLSUB(LSUB inst) throws SymbolicInstructionException{
        try{
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LSUB(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise logical right shift of a long and puts the result onto the symbolic
     * stack
     *
     * @param inst The LUSHR instruction
     */
    public void visitLUSHR(LUSHR inst) throws SymbolicInstructionException{
        try{
            IntValue i2 = stack.popOperand().asIntValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LUSHR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Calculates the bitwise exclusive or of two longs and puts the result onto the symbolic stack
     *
     * @param inst The LXOR instruction
     */
    public void visitLXOR(LXOR inst) throws SymbolicInstructionException{
        try{
            LongValue i2 = stack.popWideOperand().asLongValue();
            LongValue i1 = stack.popWideOperand().asLongValue();
            stack.pushWideOperand(i1.LXOR(i2));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * No symbolic behaviour defined.
     *
     * @param inst The MONITORENTER instruction.
     */
    public void visitMONITORENTER(MONITORENTER inst) throws SymbolicInstructionException{
        try{
            stack.popOperand();
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * No symbolic behaviour defined.
     *
     * @param inst The MONITOREXIT instruction.
     */
    public void visitMONITOREXIT(MONITOREXIT inst) throws SymbolicInstructionException{
        try{
            stack.popOperand();
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitCLINIT(CLINIT inst) throws SymbolicInstructionException{
        try {
            String owner = classDepot.getClassName(inst.cIdx);
            stack.newStackFrame(inst.desc, owner, inst.name, false);
            symbolicTraceHandler.recordInvocation(
                    determineIid(inst.iid), inst.getClass().getCanonicalName());
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
        // checkAndSetException(inst);
    }

    public void visitUNPACK_INVOKE_PARAMETER(UNPACK_INVOKE_PARAMETER inst)  throws SymbolicInstructionException{
        try{
            Value<?, ?> top = stack.popOperand();
            if (top instanceof ObjectArrayValue param) {
                SolverContext context = ThreadHandler.getSolverContext(currentThread().getId());
                for (int i = 0; i < param.getSize().concrete; i++) {
                    Value<?, ?> v = param.getElement(new IntValue(context, i));
                    if (v instanceof LongValue || v instanceof DoubleValue) {
                        stack.pushWideOperand(v);
                    } else {
                        stack.pushOperand(v);
                    }
                }
            } else {
                throw new SymbolicInstructionException(inst, "Unpacking of ObjectArray requires an Array of Objects.");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitSET_FIELD_REFLECTION(SET_FIELD_REFLECTION inst) throws SymbolicInstructionException{
        try{

            stack.newStackFrame(inst.desc, inst.owner, inst.name, true);

            if (stack.getNextInst() instanceof INVOKEMETHOD_END) {
                String reflectFieldName = inst.reflectFieldName;
                String reflectObjectOwner = inst.reflectObjectOwner;
                boolean isWideOperand = inst.isWideOperand;

                Value<?, ?> valueToSet;
                // Todo: Concerning the wideOperand, is the way in set or get field better? I guess both should work?
                if (isWideOperand) {
                    valueToSet = stack.getWideLocal(2);
                } else {
                    valueToSet = stack.getLocal(2);
                }


                // Value<?, ?> fieldObject = stack.getLocal(0);
                // ToDo: Any easier way? And does it always work?
                // ToDo: Is it possible, that the field is static?
                int cIdx = classDepot.getClassIndex(reflectObjectOwner);
                if (Modifier.isStatic(inst.modifiers)) {
                    setStaticField(valueToSet, cIdx, reflectFieldName);
                } else {
                    ObjectValue<?, ?> targetObject = (ObjectValue<?, ?>) stack.getLocal(1);
                    int fieldIndex = classDepot.getFieldIndex(cIdx, reflectFieldName, false);
                    targetObject.setField(fieldIndex, valueToSet);
                }


                stack.setReturnValue(PlaceHolder.instance);
            }

            // No INVOKEVIRTUAL after this instruction, thus no need to put everything back to the
            // shadow stack
            // ToDo: Is there a better solution with which we could preserver the general pattern with INVOKEVIRTUAL?
            // stack.pushOperand(fieldObject);
            // stack.pushOperand(targetObject);
            // if (isWideOperand) {
            //    stack.pushWideOperand(valueToSet);
            // } else {
            //     stack.pushOperand(valueToSet);
            // }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitGET_FIELD_REFLECTION(GET_FIELD_REFLECTION inst) throws SymbolicInstructionException{
        try{
            stack.newStackFrame(inst.desc, inst.owner, inst.name, true);

            if (stack.getNextInst() instanceof INVOKEMETHOD_END) {

                // ArrayList<Value<?, ?>> arguments = stack.getLocals();

                String reflectFieldName = inst.reflectFieldName;
                String reflectObjectOwner = inst.reflectObjectOwner;

                // Value<?, ?> fieldObject = stack.getLocal(0);

                int cIdx = classDepot.getClassIndex(reflectObjectOwner);

                Value<?, ?> getObject;
                if (Modifier.isStatic(inst.modifiers)) {
                    getObject = fetchStaticField(new GETSTATIC(inst.iid, cIdx, reflectFieldName, inst.desc));
                } else {
                    ObjectValue<?, ?> targetObject = (ObjectValue<?, ?>) stack.getLocal(1);

                    int fieldIndex = classDepot.getFieldIndex(cIdx, inst.name, false);
                    getObject = targetObject.getField(fieldIndex);
                }
                // if (getObject instanceof DoubleValue || getObject instanceof LongValue) {
                //     stack.pushWideOperand(getObject);
                // } else {
                //     stack.pushOperand(getObject);
                // }

                stack.setReturnValue(getObject);
            }

        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitNEW(NEW inst) throws SymbolicInstructionException{
        try {
            String className = classDepot.getClassName(inst.cIdx);
            int nFields = classDepot.getFieldCountWithReflection(className, null, false);
            stack.pushOperand(ValueFactory.createObjectValue(nFields, className));

            // For static initializer
            // setArgumentsAndNewFrame("()V", oi.getClassName().replace(".", "/"),
            // clinitFunctionName, false, inst);
            symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "NEW");
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitNEWARRAY(NEWARRAY inst) throws SymbolicInstructionException{
        try {
            IntValue size = stack.popOperand().asIntValue();
            BooleanFormula constraint = size.checkPositive();
            boolean result = size.concrete >= 0;
            symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));

            if(result) {
                switch (inst.atype) {
                    case 4 -> // T_BOOLEAN
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.booleanValue, size, -1));
                    case 5 -> // T_CHAR
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.charValue, size, -1));
                    case 6 -> // T_FLOAT
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.floatValue, size, -1));
                    case 7 -> // T_DOUBLE
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.doubleValue, size, -1));
                    case 8 -> // T_BYTE
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.byteValue, size, -1));
                    case 9 -> // T_SHORT
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.shortValue, size, -1));
                    case 10 -> // T_INT
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.intValue, size, -1));
                    case 11 -> // T_LONG
                            stack.pushOperand(ValueFactory.createArrayValue(ValueType.longValue, size, -1));
                    default -> throw new SymbolicInstructionException(inst, "Unknown primitive type: " + inst.atype + "!");
                }
            } else {
                enforceException();
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Nothing to do here ;D
     *
     * @param inst The NOP instruction.
     */
    public void visitNOP(NOP inst) throws SymbolicInstructionException{}

    /**
     * Pops the top value from the symbolic stack.
     *
     * @param inst The POP instruction
     */
    public void visitPOP(POP inst) throws SymbolicInstructionException{
        try{
            stack.popOperand();
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Pops the top two values from the symbolic stack.
     *
     * @param inst The POP2 instruction
     */
    public void visitPOP2(POP2 inst) throws SymbolicInstructionException{
        try{
            stack.popWideOperand();
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
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
    public void visitPUTFIELD(PUTFIELD inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> value;
            if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
                value = stack.popWideOperand();
            } else {
                value = stack.popOperand();
            }
            ObjectValue<?, ?> ref = stack.popOperand().asObjectValue();
            int fieldIndex = classDepot.getFieldIndex(inst.cIdx, inst.name, false);
            try {
                ref.setField(fieldIndex, value);
            } catch (Exception e) {
                throwError(inst, e);
            }
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    private void setStaticField(Value<?, ?> value, int cIdx, String name)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        int fieldIndex = classDepot.getFieldIndex(cIdx, name, true);
        ThreadHandler.setStaticField(
                Thread.currentThread().getId(), cIdx, fieldIndex, value);
    }

    /**
     * Puts a value into a static field of a class
     *
     * @param inst The PUTSTATIC instruction
     */
    public void visitPUTSTATIC(PUTSTATIC inst) throws SymbolicInstructionException{
        try {
            Value<?, ?> value;
            if (inst.desc.startsWith("D") || inst.desc.startsWith("J")) {
                value = stack.popWideOperand();
            } else {
                value = stack.popOperand();
            }

            setStaticField(value, inst.cIdx, inst.name);
            // For static initializer
            // symbolicTraceHandler.addSpecialElement(determineIid(inst.iid), "PUTSTATIC");

            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Unconditional jump no handling needed
     *
     * @param inst The RET instruction
     */
    public void visitRET(RET inst) throws SymbolicInstructionException{}

    /**
     * Returns void from method, no handling needed (here)
     *
     * @param inst The RETURN instruction
     */
    public void visitRETURN(RETURN inst) throws SymbolicInstructionException{
        // checkAndSetException(inst);
    }

    /**
     * Retrieves a short from an array and puts it onto the symbolic stack
     *
     * @param inst The SALOAD instruction.
     */
    public void visitSALOAD(SALOAD inst) throws SymbolicInstructionException{
        try {
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> arr = (ObjectValue<?, ?>) stack.popOperand();
            if (arr instanceof ShortArrayValue sarr) {
                boolean result = checkArrayBounds(sarr, idx, inst.iid);
                if (result) {
                    stack.pushOperand(sarr.getElement(idx));
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[SALOAD]: Unknown array type");
                stack.pushOperand(
                        arr.getName() != null
                                ? PlaceHolder.symbolicInstance
                                : PlaceHolder.instance);
                checkAndSetException(inst);
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Stores a short into an array
     *
     * @param inst The SASTORE instruction.
     */
    public void visitSASTORE(SASTORE inst) throws SymbolicInstructionException{
        try {
            ShortValue val = stack.popOperand().asShortValue();
            IntValue idx = stack.popOperand().asIntValue();
            ObjectValue<?, ?> ref = (ObjectValue<?, ?>) stack.popOperand();
            if (ref instanceof ShortArrayValue arr) {
                boolean result = checkArrayBounds(arr, idx, inst.iid);
                if (result) {
                    arr.storeElement(idx, val);
                } else {
                    enforceException();
                }
            } else {
                logger.warn("[SASTORE]: Unknown array type");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Pushes a short onto the symbolic stack as an integer
     *
     * @param inst The SIPUSH instruction
     */
    public void visitSIPUSH(SIPUSH inst) throws SymbolicInstructionException{
        try{
            stack.pushOperand(ValueFactory.createNumericalValue(ValueType.intValue, inst.value));
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Swaps the top two words on the symbolic stack
     *
     * @param inst The SWAP instruction
     */
    public void visitSWAP(SWAP inst) throws SymbolicInstructionException{
        try{
            Value<?, ?> v1 = stack.popOperand();
            Value<?, ?> v2 = stack.popOperand();
            stack.pushOperand(v1);
            stack.pushOperand(v2);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitINVOKEMETHOD_EXCEPTION(INVOKEMETHOD_EXCEPTION inst) throws SymbolicInstructionException{
        try{
            if (stack.getFrameStack().size() == 1) {
                logger.warn(
                        "[INVOKEMETHOD_EXCEPTION]: Terminating symbolic execution due to stack size"
                                + " (1)");
                Intrinsics.terminate();
                return;
            }
            stack.popFrame();
            stack.pushOperand(
                    PlaceHolder.instance); // placeholder for the exception object ToDo: More specific
            // placeholder
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Removes a frame from the stack frame and puts the removed frame's return value onto the old
     * (current) frame's stack
     *
     * @param inst INVOKEMETHOD_END
     */
    public void visitINVOKEMETHOD_END(INVOKEMETHOD_END inst) throws SymbolicInstructionException{
        try{
            Frame old = stack.popFrame();
            if (old.nReturnWords == 2) {
                stack.pushWideOperand(old.getRet());
            } else if (old.nReturnWords == 1) {
                stack.pushOperand(old.getRet());
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Removes a frame from the stack frame if the precedent new created a clinit frame, otherwise
     * does nothing.
     *
     * @param inst INVOKEMETHOD_END
     */
    public void visitINVOKECLINIT_END(INVOKECLINIT_END inst) throws SymbolicInstructionException{
        try{
            if (stack.getActiveFrame().getMethodName().equals(inst.name)) {
                stack.popFrame();
            } else {
                logger.warn("[INVOKECLINIT_END]: Unknown method");
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    public void visitMAKE_SYMBOLIC(MAKE_SYMBOLIC inst) throws SymbolicInstructionException{}

    public void visitLOOP_BEGIN(LOOP_BEGIN inst) throws SymbolicInstructionException{
        /*int loopCnt = loopCounter.getOrDefault(inst.iid, 0);
        loopCounter.put(inst.iid, loopCnt + 1);
        loops.push(inst.iid);
         */
    }

    public void visitLOOP_END(LOOP_END inst) throws SymbolicInstructionException{}

    public void visitSPECIAL(SPECIAL inst) throws SymbolicInstructionException{}

    /**
     * Creates a multidimensional array of references. Currently, no symbolic handling here because
     * arrays are not of primitive type? See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/63">Issue
     * 63</a>
     *
     * @param inst The MULTIANEWARRAY instruction
     */
    public void visitMULTIANEWARRAY(MULTIANEWARRAY inst) throws SymbolicInstructionException{

        try {
            IntValue[] dims = new IntValue[inst.dims];
            for (int i = 0; i < dims.length; i++) {
                IntValue i1 = stack.popOperand().asIntValue();
                BooleanFormula constraint = i1.checkPositive();
                boolean result = i1.concrete >= 0;
                symbolicTraceHandler.checkAndSetBranch(result, constraint, determineIid(inst.iid));
                dims[dims.length - i - 1] = i1;
            }

            SolverContext context = ThreadHandler.getSolverContext(currentThread().getId());
            ArrayArrayValue<?> arr = ArrayArrayValue.arrayMagic(context, inst.desc, dims);
            stack.pushOperand(arr);
            checkAndSetException(inst);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Builds a path-constraint based on where the execution continues. ToDo (Nils): Constraint
     * correctness not verified!
     *
     * @param inst The LOOKUPSWITCH instruction.
     */
    public void visitLOOKUPSWITCH(LOOKUPSWITCH inst) throws SymbolicInstructionException{
        try{
            int[] keys = inst.keys;

            IntValue i1 = stack.popOperand().asIntValue();
            for (int key : keys) {
                BooleanFormula result =
                        i1.IF_ICMPEQ(
                                ValueFactory.createNumericalValue(ValueType.intValue, key)
                                        .asIntValue());
                symbolicTraceHandler.checkAndSetBranch(
                        i1.concrete == key, result, determineIid(inst.iid));

                if (i1.concrete == key) return;
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    /**
     * Builds a path-constraint based on where the execution continues. ToDo (Nils): Constraint
     * correctness not verified!
     *
     * @param inst The TABLESWITCH instruction.
     */
    public void visitTABLESWITCH(TABLESWITCH inst) throws SymbolicInstructionException{
        try{
            IntValue i1 = stack.popOperand().asIntValue();
            for (int i = inst.min; i <= inst.max; i++) {
                BooleanFormula result =
                        i1.IF_ICMPEQ(
                                ValueFactory.createNumericalValue(ValueType.intValue, i).asIntValue());
                symbolicTraceHandler.checkAndSetBranch(
                        i1.concrete == i, result, 
                        GlobalStateForInstrumentation.createSwitchCaseIid(inst.iid, i, inst.min));

                if (i1.concrete == i) return;
            }

            BranchCoverage.addVisitedBranch(inst.iid);
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
        }
    }

    private boolean isIntegral(Object o) {
        return o instanceof Integer
                || o instanceof Short
                || o instanceof Character
                || o instanceof Boolean
                || o instanceof Byte;
    }

    private int toInteger(Object o) throws ValueConversionException{
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
        throw new ValueConversionException("Unable to convert " + o.getClass().getSimpleName() + " to int");
    }

    private boolean checkEquality(Object o1, Object o2) throws ValueConversionException {
        if (isIntegral(o1) && isIntegral(o2)) {
            return toInteger(o1) == toInteger(o2);
        } else {
            return o1.equals(o2);
        }
    }

    private void visitGETVALUE_primitive(GETVALUE_primitive inst, ValueType type) throws SymbolicInstructionException {
        try {
            boolean cat2 = type == ValueType.longValue || type == ValueType.doubleValue;
            Value<?, ?> peek = cat2 ? stack.peekWideOperand() : stack.peekOperand();
            boolean isSymbolic = false;

            if (peek == PlaceHolder.symbolicInstance) {
                peek = PlaceHolder.instance;
                isSymbolic = true;
            }
            if (inst.i == 1) {
                isSymbolic = true;
            }
            if (peek instanceof PlaceHolder placeHolder) {
                if (cat2) {
                    stack.popWideOperand();
                } else {
                    stack.popOperand();
                }
                Value<?, ?> v = ValueFactory.createNumericalValue(type, inst.v);
                if (isSymbolic) {
                    v.MAKE_SYMBOLIC();
                }

                if (cat2) {
                    stack.pushWideOperand(v);
                } else {
                    stack.pushOperand(v);
                }
                if (placeHolder.origin == PlaceHolder.ValueOrigin.GETFIELD) {
                    ObjectValue<?, ?> ref = placeHolder.referenceValue;
                    GETFIELD gfInst = (GETFIELD) placeHolder.inst;

                    int fieldIndex = classDepot.getFieldIndex(gfInst.cIdx, gfInst.name, false);
                    ref.setField(fieldIndex, v);
                    Logger shadowStateLogger = ThreadHandler.getShadowStateLogger(currentThread().getId());
                    shadowStateLogger.debug("Storing retrieved value in field {} of object {}",
                            gfInst.name, ref);
                } else if (placeHolder.origin == PlaceHolder.ValueOrigin.GETSTATIC) {
                    GETSTATIC gsInst = (GETSTATIC) placeHolder.inst;
                    setStaticField(v, gsInst.cIdx, gsInst.name);
                    Logger shadowStateLogger = ThreadHandler.getShadowStateLogger(currentThread().getId());
                    shadowStateLogger.debug("Storing retrieved value in field {}",gsInst.name);
                }
            } else if ((!(peek instanceof BoxedValue<?>) && peek.concrete == null)
                    || (peek instanceof BoxedValue<?> && ((BoxedValue<?>) peek).getVal().concrete == null)) {
                if (cat2) {
                    stack.popWideOperand();
                    stack.pushWideOperand(ValueFactory.createNumericalValue(type, inst.v));
                } else {
                    stack.popOperand();
                    stack.pushOperand(ValueFactory.createNumericalValue(type, inst.v));
                }
            } else if ((peek instanceof BoxedValue<?> && !checkEquality(((BoxedValue<?>)peek).getVal().concrete, inst.v))
                    || (!(peek instanceof BoxedValue<?>) && !checkEquality(peek.concrete, inst.v))) {
                SWATAssert.check(false, "[GETVALUE_primitive]: Value on stack does not match expected value! Expected: {}, Actual: {}",
                        inst.v, peek.concrete);
                if (cat2) {
                    stack.popWideOperand();
                    stack.pushWideOperand(ValueFactory.createNumericalValue(type, inst.v));
                } else {
                    stack.popOperand();
                    stack.pushOperand(ValueFactory.createNumericalValue(type, inst.v));
                }
            }
        } catch (Throwable t) {
            throw new SymbolicInstructionException(inst, t);
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
