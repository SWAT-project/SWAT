package de.uzl.its.swat.symbolic;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.ValueFactory;
import java.util.ArrayList;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A symbolic Stack Frame that stores Values in the symbolic Stack and symbolic locals */
public class Frame {

    // Special logger used to visualize the shadow state. Useful for debugging and learning
    // purposes.
    private static final Logger stateLogger = LoggerFactory.getLogger("ShadowStateLogger");

    /** Number of words that are returned on invoke method end */
    public final int nReturnWords;
    /**
     * The symbolic version of Javas locals -- GETTER -- Gets all locals of the Frame (used for
     * lambdas)
     *
     * @return All locals
     */
    @Getter private final ArrayList<Value<?, ?>> locals = new ArrayList<>(8);

    /** The symbolic version of Javas stack */
    @Getter private final ArrayList<Value<?, ?>> stack = new ArrayList<>(8);

    @Getter private Value<?, ?> ret;

    /**
     * Constructor for Frame
     *
     * @param nReturnWords Number of words that are returned on invoke method end
     */
    public Frame(int nReturnWords) {
        this.nReturnWords = nReturnWords;
        ret = PlaceHolder.instance;
    }

    /**
     * Setter for the return value of the symbolic stack frame
     *
     * @param v The return value for the invoked method
     */
    public void setRet(Value<?, ?> v) {
        if (v == null) {
            ret = ValueFactory.createNULLValue();
        } else {
            if (ret != null
                    && ret.getClass().equals(PlaceHolder.class)
                    && ((PlaceHolder) ret).isSymbolic) {
                if (((PlaceHolder) ret).origin.equals(PlaceHolder.ValueOrigin.UNSPECIFIED)) {
                    v.MAKE_SYMBOLIC();
                } else {
                    v.MAKE_SYMBOLIC(
                            PlaceHolder.valueOriginPrefixMap.get(((PlaceHolder) ret).origin));
                }
            }
            ret = v;
        }
    }

    /**
     * Sets a Value into the symbolic locals at a specific index If the index is larger than the
     * current max, a Placeholder Values are added until the index is reached
     *
     * @param o The Value that should be placed into the symbolic locals
     */
    public void addLocal(Value<?, ?> o) {
        locals.add(o);
    }

    /**
     * Adds a two byte Value into the symbolic locals, at the next available index (not Placeholder)
     * The two bytes are reflected by first adding a placeholder and then the actual value
     *
     * @param o The Value that should be placed into the symbolic locals
     */
    public void addLocal2(Value<?, ?> o) {
        locals.add(o);
        locals.add(PlaceHolder.instance);
    }

    /**
     * Sets a Value into the symbolic locals at a specific index If the index is larger than the
     * current max, a Placeholder Values are added until the index is reached
     *
     * @param index The index of the value
     * @param o The Value that should be placed into the symbolic locals
     */
    public void setLocal(int index, Value<?, ?> o) {
        int diff = index - locals.size();
        while (diff >= 0) {
            locals.add(PlaceHolder.instance);
            diff--;
        }
        locals.set(index, o);
    }

    /**
     * Gets a Value from the symbolic locals
     *
     * @param index The index to get from the symbolic locals, if the index does not exist, returns
     *     a Placeholder
     * @return The Value at position index in the symbolic locals
     */
    public Value<?, ?> getLocal(int index) {
        if (index < locals.size()) {
            return locals.get(index);
        }
        return PlaceHolder.instance;
    }

    /**
     * Sets a two byte Value into the symbolic locals at a specific index If the index is larger
     * than the current max, Placeholder Values are added until the index is reached The two bytes
     * are reflected by first adding a placeholder and then the actual value
     *
     * @param index The index of the value
     * @param o The Value that should be placed into the symbolic locals
     */
    public void setLocal2(int index, Value<?, ?> o) {
        int diff = index - locals.size();
        while (diff >= -1) {
            locals.add(PlaceHolder.instance);
            diff--;
        }
        locals.set(index, o);
    }

    /**
     * Gets a two byte Value from the symbolic locals
     *
     * @param index The index of the Value
     * @return The Value if the index exists, else a Placeholder
     */
    public Value<?, ?> getLocal2(int index) {
        if (index < locals.size()) {
            return locals.get(index);
        }
        return PlaceHolder.instance;
    }

    /**
     * Pushes one element onto the symbolic stack
     *
     * @param o Value to be pushed onto the symbolic stack
     */
    public void push(Value<?, ?> o) {
        if (o == null) {
            throw new RuntimeException("Value is null");
        }
        stack.add(o);
        printEntry(false, o);
    }

    /**
     * Pushes 2 elements onto the symbolic stack (used for e.g. for two byte datatype. THe second
     * pushed value is a placeholder)
     *
     * @param o Value to be pushed onto the symbolic stack
     */
    public void push2(Value<?, ?> o) {
        stack.add(o);
        stack.add(PlaceHolder.instance);
        printEntry(false, o);
        printEntry(false, PlaceHolder.instance);
    }

    /**
     * Pops the top element from the symbolic stack
     *
     * @return the top element from the symbolic stack
     */
    public Value<?, ?> pop() {
        if (stack.isEmpty()) {
            new ErrorHandler()
                    .handleException(new RuntimeException("Trying to pop from an empty stack!"));
        }
        Value<?, ?> v = stack.remove(stack.size() - 1);
        printEntry(true, v);
        return v;
    }

    /**
     * Pops the top 2 elements from the symbolic stack
     *
     * @return the top two elements from the symbolic stack
     */
    public Value<?, ?> pop2() {
        if (stack.size() < 2)
            new ErrorHandler()
                    .handleException(new RuntimeException("Trying to pop from an empty stack!"));
        Value<?, ?> unused = stack.remove(stack.size() - 1);
        printEntry(true, unused);
        assert unused instanceof PlaceHolder;
        Value<?, ?> v = stack.remove(stack.size() - 1);
        printEntry(true, v);
        return v;
    }

    /**
     * Peeks at the top element on the symbolic stack without popping it
     *
     * @return the top element from the symbolic stack
     */
    public Value<?, ?> peek() {
        if (!stack.isEmpty()) {
            return stack.get(stack.size() - 1);
        } else {
            throw new RuntimeException("Trying to peek on an empty stack!");
        }
    }

    /**
     * Peeks at the top 2 elements on the symbolic stack without popping them
     *
     * @return the top two elements from the symbolic stack
     */
    public Value<?, ?> peek2() {
        return stack.get(stack.size() - 2);
    }

    /** Clears the symbolic stack */
    public void clear() {
        stack.clear();
    }

    private void printEntry(boolean isRemoved, Value<?, ?> val) {
        String prefix = isRemoved ? "[<-- Popped]" : "[--> Pushed]";
        stateLogger.info(prefix + " " + val);
    }

    public void printStack() {
        int i;
        for (i = 0; i < stack.size(); i++) {
            stateLogger.info("[" + (stack.size() - i) + "]: " + stack.get(i));
            if (i == 4) break;
        }
        if (i < stack.size()) stateLogger.info("... (" + (stack.size() - i) + " more)");
    }
    /**
     * Override of the default toString method for printing the Current symbolic stack frame and
     * locals
     *
     * @return A string representation of the current symbolic stack frame and locals
     */
    @Override
    public String toString() {
        return "Stack: " + stack + "/nLocals: " + locals;
    }
}
