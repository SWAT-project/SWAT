package de.uzl.its.swat.symbolic.invoke;

import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.thread.ThreadHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.symbolic.value.reference.util.ListValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverContext;

public class InternalInvocation {

    public static Value<?, ?> invokeMethod(
            String name,
            Value<?, ?>[] args,
            Type[] types,
            SymbolicTraceHandler symbolicStateHandler) {
        if (name.equals("MakeSymbolic") && args.length == 1) {
            return makeSymbolic(args[0], types[0], symbolicStateHandler);
        } else if (name.equals("MakeSymbolic")
                && args.length == 2
                && args[1] instanceof LongValue l) {
            return makeSymbolic(args[0], l, types[0], symbolicStateHandler);
        } else if (name.equals("checkSink")) {
            return checkSink(args);
        }

        return PlaceHolder.instance;
    }

    private static Value<?, ?> makeSymbolic(
            Value<?, ?> arg, Type type, SymbolicTraceHandler symbolicStateHandler) {
        Value<?, ?> val =
                switch (type.getDescriptor()) {
                    case "I" -> arg.asIntValue();
                    case "Z" -> arg.asBooleanValue();
                    case "C" -> arg.asCharValue();
                    case "D" -> arg.asDoubleValue();
                    case "F" -> arg.asFloatValue();
                    case "J" -> arg.asLongValue();
                    case "S" -> arg.asShortValue();
                    case "B" -> arg.asByteValue();
                    case "Ljava/lang/String;" -> arg.asStringValue();
                    default -> arg;
                };
        String symbolicName = val.MAKE_SYMBOLIC();
        symbolicStateHandler.addInput(symbolicName, val);
        return val;
    }

    private static Value<?, ?> makeSymbolic(
            Value<?, ?> arg, LongValue idx, Type type, SymbolicTraceHandler symbolicStateHandler) {
        Value<?, ?> val =
                switch (type.getDescriptor()) {
                    case "I" -> arg.asIntValue();
                    case "Z" -> arg.asBooleanValue();
                    case "C" -> arg.asCharValue();
                    case "D" -> arg.asDoubleValue();
                    case "F" -> arg.asFloatValue();
                    case "J" -> arg.asLongValue();
                    case "S" -> arg.asShortValue();
                    case "B" -> arg.asByteValue();
                    case "Ljava/lang/String;" -> arg.asStringValue();
                    default -> arg;
                };
        String symbolicName = val.MAKE_SYMBOLIC(idx.concrete);
        symbolicStateHandler.addInput(symbolicName, val);
        return val;
    }

    private static Value<?, ?> checkSink(Value<?, ?>[] args) {
        List<String> vars = new ArrayList<>();
        String identifier = (String) args[args.length - 2].concrete;
        String sink = (String) args[args.length - 1].concrete;
        for (int i = 0; i < args.length - 2; i++) {
            vars.addAll(unpackValue(args[i]));
        }
        long id = Thread.currentThread().getId();
        StringBuilder result =
                new StringBuilder(
                        "{"
                                + "\"method\":\""
                                + identifier
                                + "\",\"Sink\":\""
                                + sink
                                + "\",\"Free Variables\":[");
        int i = 0;
        for (String freeVar : vars) {
            if (i != 0) result.append(",");
            i++;
            result.append("\"").append(freeVar).append("\"");
        }
        result.append("]}");
        return PlaceHolder.instance;
    }

    private static List<String> unpackValue(Value<?, ?> val) {
        List<String> vars = new ArrayList<>();
        SolverContext context = ThreadHandler.getSolverContext(Thread.currentThread().getId());
        if (val instanceof ListValue listVal) {
            for (Value<?, ?> v : listVal.getList()) {
                vars.addAll(unpackValue(v));
            }
        } else if (val instanceof StringValue s) {
            Map<String, Formula> freeVars =
                    context.getFormulaManager().extractVariablesAndUFs(s.formula);
            for (String key : freeVars.keySet()) {
                if (key.matches("[a-z]+[0-9]+")) vars.add(key);
            }
        } else if (val instanceof ObjectValue<?, ?> objVal && objVal.getFields() != null) {
            for (Value<?, ?> v : objVal.getFields()) {
                vars.addAll(unpackValue(v));
            }
        } else if (val instanceof AbstractArrayValue<?, ?, ?, ?, ?> arr) {
            int size = arr.size.concrete;
            for (int i = 0; i < size; i++) {

                Map<String, Formula> freeVars =
                        context.getFormulaManager()
                                .extractVariablesAndUFs(
                                        (Formula)
                                                arr.getElement(new IntValue(arr.getContext(), i)));
                for (String key : freeVars.keySet()) {
                    if (key.matches("[a-z]+[0-9]+")) vars.add(key);
                }
            }
        } else if (val != null && val.formula != null) {
            Map<String, Formula> freeVars =
                    context.getFormulaManager().extractVariablesAndUFs((Formula) val.formula);
            vars.addAll(freeVars.keySet());
        }
        return vars;
    }
}
