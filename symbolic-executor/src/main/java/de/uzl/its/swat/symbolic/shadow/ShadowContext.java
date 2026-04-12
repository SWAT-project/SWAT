package de.uzl.its.swat.symbolic.shadow;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.metadata.ClassDepot;
import de.uzl.its.swat.metadata.ClassDepotRuntime;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.symbolic.value.Value;
import java.util.*;

import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Type;
import org.slf4j.LoggerFactory;

public class ShadowContext {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ShadowContext.class);

    @Setter @Getter private Instruction nextInst;

    @Getter private final Stack<Frame> frameStack;

    private Frame activeFrame;

    private final ClassDepotRuntime classDepot = ClassDepot.getRuntimeInstance();

    // private Map<Integer, Frame> lambdaFrameStore;


    private final JVMHeap heap;

    @Getter private static final String INITIAL_FRAME_NAME = "Initial Stack Frame";

    public ShadowContext() {
        this.heap = new JVMHeap();
        this.frameStack = new Stack<>();
        activeFrame = new Frame(INITIAL_FRAME_NAME, "", 0);
        frameStack.add(activeFrame);
        // lambdaFrameStore = new HashMap<>();
    }

    public void putToHeap(int hashCode, Value<?, ?> value) {
        heap.put(hashCode, value);
    }

    public Value<?, ?> getFromHeap(int hashCode) {
        return heap.get(hashCode);
    }

    /**
     * Pushes a new frame onto the stack and makes it the active frame. A new frame on this stack
     * represents a new method invocation.
     *
     * @param frame The frame to be pushed onto the stack
     */
    public void pushFrame(Frame frame) {
        activeFrame = frame;
        frameStack.push(frame);
    }

    /**
     * Removes the top frame from the stack and makes the next frame the active frame.
     *
     * @return The frame that was removed from the stack
     */
    public Frame popFrame() {
        assert !frameStack.isEmpty() : "[SWAT] Unable to remove method frame, as no stack frames are left";
        Frame oldFrame = frameStack.pop();
        activeFrame = getActiveFrame();
        return oldFrame;
    }

    /**
     * Returns the frame that corresponds to the current method. This is the top frame on the stack.
     *
     * @return The active frame
     */
    public Frame getActiveFrame() {
        assert !frameStack.isEmpty()
                : "[SWAT] Unable to look at active method frame, as no stack frames are left";
        return frameStack.peek();
    }

    /**
     * Clears the operand stack. This is used when an exception is thrown and the operand stack
     * needs to be cleared.
     */
    public void clearOperandStack() {
        activeFrame.clearOperandStack();
    }

    /**
     * Pushes an operand onto the operand stack of the active frame.
     *
     * @param operand The operand to be pushed onto the stack
     */
    public void pushOperand(Value<?, ?> operand) throws NoThreadContextException {
        activeFrame.push(operand);
    }

    /**
     * Pushes a wide operand onto the operand stack of the active frame.
     *
     * @param operand The operand to be pushed onto the stack
     */
    public void pushWideOperand(Value<?, ?> operand) throws NoThreadContextException {
        activeFrame.push2(operand);
    }

    /**
     * Pops an operand from the operand stack of the active frame.
     *
     * @return The operand that was popped from the stack
     */
    public Value<?, ?> popOperand() throws NoThreadContextException {
        return activeFrame.pop();
    }

    /**
     * Pops a wide operand from the operand stack of the active frame.
     *
     * @return The operand that was popped from the stack
     */
    public Value<?, ?> popWideOperand() throws NoThreadContextException {
        return activeFrame.pop2();
    }

    /**
     * Peeks at the top operand on the operand stack of the active frame without popping it.
     *
     * @return The operand that is at the top of the stack
     */
    public Value<?, ?> peekOperand() {
        return activeFrame.peek();
    }

    /**
     * Peeks at the top operand (that is two wide) on the operand stack of the active frame without
     * popping it.
     *
     * @return The operand that is at the second top of the stack
     */
    public Value<?, ?> peekWideOperand() {
        return activeFrame.peek2();
    }

    /**
     * Sets a local variable in the active frame.
     *
     * @param index The index of the local variable
     * @param operand The value to be set as the local variable
     */
    public void setLocal(int index, Value<?, ?> operand) {
        activeFrame.setLocal(index, operand);
    }

    /**
     * Sets a wide local variable in the active frame.
     *
     * @param index The index of the local variable
     * @param operand The value to be set as the local variable
     */
    public void setWideLocal(int index, Value<?, ?> operand) {
        activeFrame.setLocal2(index, operand);
    }

    /**
     * Gets a local variable from the active frame.
     *
     * @param index The index of the local variable
     * @return The value of the local variable
     */
    public Value<?, ?> getLocal(int index) throws NoThreadContextException {
        return activeFrame.getLocal(index);
    }

    /**
     * Gets a wide local variable from the active frame.
     *
     * @param index The index of the local variable
     * @return The value of the local variable
     */
    public Value<?, ?> getWideLocal(int index) {
        return activeFrame.getLocal2(index);
    }

    /**
     * Gets all local variables from the active frame.
     *
     * @return The list of local variables
     */
    public ArrayList<Value<?, ?>> getLocals() {
        return activeFrame.getLocals();
    }

    /**
     * Sets the return value of the active frame.
     *
     * @param operand The value to be set as the return value
     */
    public void setReturnValue(Value<?, ?> operand) throws NoThreadContextException, NotImplementedException {
        activeFrame.setRet(operand);
    }

    /**
     * Gets the return value of the active frame.
     *
     * @return The return value of the active frame
     */
    public Value<?, ?> getReturnValue() {
        return activeFrame.getRet();
    }

    /*
    Clears the operand stack of the active frame and pushes the exception value as the only value on the operand stack.
     */
    public void handleException() throws NoThreadContextException {
        Value<?, ?> exception = peekOperand();
        clearOperandStack();
        pushOperand(exception);
    }

    /**
     * Initializes a new frame for the execution of a method.
     *
     * @param owner The class that owns the method
     * @param name The name of the method
     * @param desc The datatype description of the method arguments
     * @return The initialized frame
     */
    private Frame initializeFrame(String owner, String name, String desc) {
        Type retType = Type.getReturnType(desc);

        int nReturnWords;
        if (retType == Type.DOUBLE_TYPE || retType == Type.LONG_TYPE) {
            nReturnWords = 2;
        } else if (retType == Type.VOID_TYPE) {
            nReturnWords = 0;
        } else {
            nReturnWords = 1;
        }
        return new Frame(owner, name, nReturnWords);
    }

    /**
     * Fetches the arguments of a method call from the operand stack.
     *
     * @param types The datatype description of the method arguments
     * @return The arguments of the method call
     */
    private Value<?, ?>[] fetchArguments(Type[] types) throws NoThreadContextException {

        int len = types.length;
        Value<?, ?>[] arguments = new Value[len];
        for (int i = len - 1; i >= 0; i--) {
            if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                arguments[i] = popWideOperand();
            } else {
                Value<?, ?> arg = popOperand();
                arguments[i] = arg;
            }
            assert Util.isMatchingType(types[i], arguments[i])
                    : "[SWAT] Method parameter description and value do not match: "
                            + types[i]
                            + " | "
                            + arguments[i].getClass();
        }

        return arguments;
    }

    /**
     * Sets the arguments of a method call in the local variables of the active frame. In the actual
     * execution the parameters are passed to the stack frame as locals.
     *
     * @param types The datatype description of the method arguments
     * @param arguments The arguments of the method call
     */
    private void setArguments(Type[] types, Value<?, ?>[] arguments) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                activeFrame.addLocal2(arguments[i]);
            } else {
                activeFrame.addLocal(arguments[i]);
            }
        }
    }

    public Value<?, ?>[] fetchArgumentsFromLocals(Type[] types, boolean isInstance) throws NoThreadContextException {
        Value<?, ?>[] arguments = new Value[types.length];
        int offset = isInstance ? 1 : 0; // Initial offset for instance methods
        for (int i = 0; i < types.length; i++) {
            if (types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE) {
                arguments[i] = activeFrame.getLocal2( i + offset);
                offset++; // Increment offset for double and long types
            } else {
                arguments[i] = activeFrame.getLocal(i + offset);
            }
        }
        return arguments;
    }

    public ObjectValue<?, ?> getInstance() throws NoThreadContextException, NotImplementedException, ValueConversionException {
        return activeFrame.getLocal(0).asObjectValue();
    }

    /**
     * This method is called by INVOKE* It Creates a new frame for the execution of the method and
     * sets all locals (arguments)
     *
     * @param desc The Datatype description of the method arguments
     * @param owner The parent object of the invoked method
     * @param name The name of the method
     * @param isInstance If the method is a static method or an instance of an object
     */
    public void newStackFrame(String desc, String owner, String name, boolean isInstance) throws NoThreadContextException, NotImplementedException, ValueConversionException {
        Frame newFrame = initializeFrame(owner, name, desc);

        Type[] types = Type.getArgumentTypes(desc);
        Value<?, ?>[] arguments = fetchArguments(types);
        Value<?, ?> instance = isInstance ? popOperand().asObjectValue() : null;

        pushFrame(newFrame);
        activeFrame = newFrame;

        if (isInstance) {
            activeFrame.addLocal(instance); // adds the instance reference
        }

        setArguments(types, arguments);
    }
    // Todo where to live?


}
// CURRENTLY UNUSED LAMBDA IMPLEMENTATION
    /*
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
                throw t;
            }
        }
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
                     DynamicInvocation.invokeStaticMethod(
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
     }
     */
