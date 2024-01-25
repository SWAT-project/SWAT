package de.uzl.its.swat.interpreters;

import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import java.util.ArrayList;

/** A symbolic Stack Frame that stores Values in the symbolic Stack and symbolic locals */
public class Frame {
    /** Number of words that are returned on invoke method end */
    public final int nReturnWords;
    /** The symbolic version of Javas locals */
    private final ArrayList<Value<?, ?>> locals = new ArrayList<>(8);

    /** The symbolic version of Javas stack */
    private final ArrayList<Value<?, ?>> stack = new ArrayList<>(8);

    /** The Return Value of the symbolic stack frame, initially this is a Placeholder instance */
    private Value<?, ?> ret;

    /**
     * Constructor for Frame
     *
     * @param nReturnWords Number of words that are returned on invoke method end
     */
    public Frame(int nReturnWords) {
        this.nReturnWords = nReturnWords;
        ret = PlaceHolder.instance;
    }

    public ArrayList<Value<?, ?>> getStack() {
        return stack;
    }

    /**
     * Getter for the return value of the symbolic stack frame
     *
     * @return The return value of the invoked method
     */
    public Value<?, ?> getRet() {
        return ret;
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
     * Gets all locals of the Frame (used for lambdas)
     *
     * @return All locals
     */
    public ArrayList<Value<?, ?>> getLocals() {
        return locals;
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
    }

    /**
     * Pops the top element from the symbolic stack
     *
     * @return the top element from the symbolic stack
     */
    public Value<?, ?> pop() {
        if (stack.size() == 0) {
            throw new RuntimeException("Trying to pop from an empty stack!");
        }
        return stack.remove(stack.size() - 1);
    }

    /**
     * Pops the top 2 elements from the symbolic stack
     *
     * @return the top two elements from the symbolic stack
     */
    public Value<?, ?> pop2() {
        stack.remove(stack.size() - 1);
        return stack.remove(stack.size() - 1);
    }

    /**
     * Peeks at the top element on the symbolic stack without popping it
     *
     * @return the top element from the symbolic stack
     */
    public Value<?, ?> peek() {
        if (stack.size() > 0) {
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
