package de.uzl.its.swat.symbolic.invoke;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.Intrinsics;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadContext;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverContext;

import static java.lang.Thread.currentThread;

public class InternalInvocation {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    public static Value<?, ?> invokeStaticMethod(
            String name,
            Value<?, ?>[] args,
            Type[] types,
            SymbolicTraceHandler symbolicStateHandler) throws NoThreadContextException, NotImplementedException, ValueConversionException {
        logger.debug("Internal invocation of method {} with {} arguments: {}", name, args.length, args);
        if (name.equals("liftValue")
                && args.length == 2
                && args[1] instanceof LongValue uid) {
            return liftValue(uid, args[0], types[0], symbolicStateHandler);
        } else if (name.equals("liftValue")
                && args.length == 3
                && args[1] instanceof StringValue s
                && args[2] instanceof LongValue uid) {
            return liftValue(s, uid, args[0], types[0], symbolicStateHandler);
        } else if (name.equals("checkSink")) {
            return checkSink(args);
        } else if (name.equals("injectAssignment")) {
            return PlaceHolder.instance;
        }
        SWATAssert.check(false, "Unknown internal method {} with {} arguments: {}", name, args.length, Arrays.toString(args));
        return PlaceHolder.instance;
    }

    private static Value<?, ?> liftValue(
            LongValue uid, Value<?, ?> value, Type type, SymbolicTraceHandler symbolicStateHandler) throws NoThreadContextException, NotImplementedException, ValueConversionException {

        if ((value instanceof ObjectValue<?, ?> ov) && ov.isNullValue()) {
            logger.warn("Null value cannot be lifted.");
            return value;
        }

        String symbolicPrefix;
        if(value instanceof BoxedValue<?> boxedValue) {
            symbolicPrefix = boxedValue.getVal().getSymPrefix();
        } else {
            symbolicPrefix = value.getSymPrefix();
        }

        // Todo what about boxed primitives
        if(value instanceof NumericalValue<?,?>){
            value = Util.convertType(type, value);
        }
        String requestedName = symbolicPrefix + "_" +  uid.concrete;
        String newIdx = String.valueOf(uid.concrete);
        Map<String, Integer> symbolicIdxOccurrence = ThreadHandler.getSymbolicIdxOccurrence(currentThread().getId());
        int occurrence = symbolicIdxOccurrence.getOrDefault(requestedName, 0);
        symbolicIdxOccurrence.put(requestedName, occurrence + 1);
        if (occurrence > 0) {
            newIdx += String.format("%02d", occurrence);
            logger.warn("Multiple calls to liftValue with the same name: {}. Assigning new idx: {}", requestedName, newIdx);

        }
        String symbolicName = value.MAKE_SYMBOLIC(newIdx);
        symbolicStateHandler.addInput(symbolicName, value);

        // For arrays, also make the length symbolic
        if (value instanceof AbstractArrayValue<?, ?, ?, ?, ?> arrayValue) {
            IntValue size = arrayValue.size;
            if (size != null) {
                // Make the size symbolic with a specific name pattern
                String sizeName = size.MAKE_SYMBOLIC(symbolicName + "_length");
                symbolicStateHandler.addInput(sizeName, size);
                logger.info("Made array length symbolic: {}", sizeName);

                // Add upper bound constraint to prevent unbounded array lengths during exploration
                // Use unsigned comparison (ULE) since array lengths must be non-negative
                // This is added as an invariant (using addConstraint) not a branch, so it's always enforced
                int maxArrayLength = Config.instance().getMaxArrayLength();
                BitvectorFormulaManager bvmgr =
                    size.context.getFormulaManager().getBitvectorFormulaManager();
                BooleanFormula upperBound =
                    bvmgr.lessOrEquals(size.formula,
                                      bvmgr.makeBitvector(32, maxArrayLength),
                                      false); // false = unsigned comparison
                symbolicStateHandler.addConstraint(upperBound);
                logger.debug("Added array length upper bound constraint: {} <= {}", sizeName, maxArrayLength);
            }
        }



        return value;
    }

    private static Value<?, ?> liftValue(StringValue symbolicPrefix, LongValue uid,  Value<?, ?> value, Type type, SymbolicTraceHandler symbolicStateHandler) throws NoThreadContextException, NotImplementedException {

        if ((value instanceof ObjectValue<?, ?> ov) && ov.isNullValue()) {
            logger.warn("Null value cannot be lifted.");
            return value;
        }

        String symbolicVariablePrefix;
        if(value instanceof BoxedValue<?> boxedValue) {
            symbolicVariablePrefix = boxedValue.getVal().getSymPrefix();
        } else {
            symbolicVariablePrefix = value.getSymPrefix();
        }

        if (symbolicPrefix != null) {
            // Assemble prefix with its uid
            String prefix = symbolicPrefix.concrete + "_" + uid.concrete;
            // Assemble sub UID (for lists)
            prefix += "_" + symbolicVariablePrefix + "_" + ThreadHandler.getNextSubUid(currentThread().getId(),prefix);
            String symbolicName = value.MAKE_SYMBOLIC(prefix);
            symbolicStateHandler.addInput(symbolicName, value);
        } else {
            String symbolicName = value.MAKE_SYMBOLIC();
            symbolicStateHandler.addInput(symbolicName, value);
        }
        return value;
    }

    private static Value<?, ?> checkSink(Value<?, ?>[] args) throws NoThreadContextException {
        List<String> vars = new ArrayList<>();
        String identifier = (String) args[args.length - 2].concrete;
        String sink = (String) args[args.length - 1].concrete;
        for (int i = 0; i < args.length - 2; i++) {
            vars.addAll(unpackValue(args[i]));
        }
        long id = currentThread().getId();
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

    private static List<String> unpackValue(Value<?, ?> val) throws NoThreadContextException {
        List<String> vars = new ArrayList<>();
        SolverContext context = ThreadHandler.getSolverContext(currentThread().getId());
        if (val instanceof StringValue s) {
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
