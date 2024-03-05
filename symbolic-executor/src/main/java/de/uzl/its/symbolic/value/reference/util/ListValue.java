package de.uzl.its.symbolic.value.reference.util;

import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import de.uzl.its.symbolic.value.reference.lang.*;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.SolverContext;

public class ListValue extends ObjectValue<Object, Object> {

    public List<Value<?, ?>> getList() {
        return list;
    }

    private List<Value<?, ?>> list;

    public ListValue(SolverContext context, String className) {
        super(context, 42, -1);
        this.className = className;
        list = new ArrayList<>();
    }

    public ListValue(SolverContext context, String className, List<?> l) {
        super(context, 42, -1);
        this.className = className;
        list = new ArrayList<>();
        for (Object element : l) {
            if (element instanceof String el) {
                list.add(new StringValue(context, el, ObjectValue.ADDRESS_UNKNOWN));
            } else if (element instanceof List el) {
                list.add(new ListValue(context, element.getClass().getName(), el));
            } else if (element instanceof Integer el) {
                list.add(
                        new IntegerObjectValue(
                                context, new IntValue(context, el), ObjectValue.ADDRESS_UNKNOWN));
            } else if (element instanceof Long el) {
                list.add(
                        new LongObjectValue(
                                context, new LongValue(context, el), ObjectValue.ADDRESS_UNKNOWN));
            } else if (element instanceof Float el) {
                list.add(
                        new FloatObjectValue(
                                context, new FloatValue(context, el), ObjectValue.ADDRESS_UNKNOWN));
            } else if (element instanceof Double el) {
                list.add(
                        new DoubleObjectValue(
                                context,
                                new DoubleValue(context, el),
                                ObjectValue.ADDRESS_UNKNOWN));
            } else if (element instanceof Character el) {
                list.add(
                        new CharacterObjectValue(
                                context, new CharValue(context, el), ObjectValue.ADDRESS_UNKNOWN));
            } else {
                list.add(new ObjectValue<>(context, 42, ObjectValue.ADDRESS_UNKNOWN));
            }
        }
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
        } else if (name.equals("size")) {
            return invokeSize();
        } else if (name.equals("get") && args.length == 1 && args[0] instanceof IntValue idx) {
            return invokeGet(idx);
        } else if (name.equals("remove") && args.length == 1 && args[0] instanceof IntValue idx) {
            return invokeRemove(idx);
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

    public IntValue invokeSize() {
        return new IntValue(context, list.size());
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

    public Value<?, ?> invokeRemove(IntValue idx) {
        return list.remove((int) idx.concrete);
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
