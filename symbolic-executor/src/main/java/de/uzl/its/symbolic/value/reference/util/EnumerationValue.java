package de.uzl.its.symbolic.value.reference.util;

import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.SolverContext;

public class EnumerationValue extends ObjectValue<Object, Object> {

    public List<Value<?, ?>> getList() {
        return list;
    }

    private List<Value<?, ?>> list;

    public EnumerationValue(SolverContext context) {
        super(context, 42, -1);
        // list = new Enumeration<>();
    }

    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) {

        if (name.equals("<init>")) {
            return invokeInit(args);
        } else if (name.equals("add")) {
            if (args.length == 1) {
                return invokeAdd(args[0]);
            } else if (args.length == 2 && args[0] instanceof IntValue i) {
                invokeAdd(i, args[1]);
                return null; // ToDo maybe needs to be PlaceHolder.instance
            }
        } else if (name.equals("clear")) {
            invokeClear();
            return null; // ToDo maybe needs to be PlaceHolder.instance
        } else if (name.equals("get") && args.length == 1 && args[0] instanceof IntValue idx) {
            return invokeGet(idx);
        }
        return PlaceHolder.instance;
    }

    private PlaceHolder invokeInit(Value<?, ?>[] args) {
        if (args.length == 0) {
            this.list = new ArrayList<>();
        } else if (args[0] instanceof IntValue intValue) {
            this.list = new ArrayList<>(intValue.concrete);
        } else {
        }
        return PlaceHolder.instance;
    }

    public BooleanValue invokeAdd(Value<?, ?> v) {
        return new BooleanValue(context, list.add(v));
    }

    public void invokeAdd(IntValue idx, Value<?, ?> v) {
        list.add(idx.concrete, v);
    }

    public void invokeClear() {
        list.clear();
    }

    public Value<?, ?> invokeGet(IntValue idx) {
        return list.get(idx.concrete);
    }

    @Override
    public String toString() {
        return "ListValue @"
                + Integer.toHexString(address)
                + " { size="
                + list.size()
                + ", concrete="
                + list
                + "}";
    }
}
