package de.uzl.its.swat.symbolic.invoke;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.symbolic.UFs.PureFunctionUF;
import de.uzl.its.swat.symbolic.UFs.PureMethods;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.StringFormulaManager;

public class InvocationHandler {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private static final ArrayList<String> IGNORED_INVOCATIONS =
            new ArrayList<>(
                    Arrays.asList(
                            "java/io/PrintStream/println",
                            "de/uzl/its/swat/instrument/Intrinsics",
                            "de/uzl/its/swat/common/UtilInstrumented",
                            // refEquals's body (stepped through, since UtilInstrumented is
                            // instrumented) calls these with a possibly-symbolic operand; ignore them
                            // so a reference comparison does not record spurious context loss. Both are
                            // pure/identity and their concretized results are all refEquals needs.
                            "de/uzl/its/swat/common/Util/shouldUseValueEquality",
                            "de/uzl/its/swat/common/Provenance",
                            "de/uzl/its/swat/witness/Witness",
                            "de/uzl/its/swat/instrument/svcomp/Verifier",
                            "java/io/PrintStream",
                            "java/lang/Class",
                            "java/io/BufferedReader",
                            "java/io/InputStream",
                            "java/util/Scanner"));

    public static Value<?, ?> invoke(
            SymbolicTraceHandler symbolicTraceHandler,
            String desc,
            String owner,
            String name,
            long invokeId,
            ArrayList<Value<?, ?>> arguments,
            boolean isInstance,
            ObjectValue<?, ?> instance) throws NoThreadContextException, ValueConversionException, NotImplementedException {

        Value<?, ?> retValue;
        Type[] types = Type.getArgumentTypes(desc);
        logger.trace("Invoking method {} in class {} with arguments {}", name, owner, arguments);

        // We do that first to include the pointer in instance invocation cases
        boolean containsSymbolicArgument = arguments.stream().anyMatch(v -> {
            try {
                return v.isSymbolic();
            } catch (NotImplementedException e) {
                // either handle, or wrap in an unchecked exception:
                throw new RuntimeException(e);
            }
        });

        if (isInstance) {
            containsSymbolicArgument = containsSymbolicArgument || instance.isSymbolic();
            retValue = instance.invokeMethod(name, types, arguments.toArray(new Value[0]));
        } else {
            retValue =
                    StaticInvocation.invokeMethod(
                            owner,
                            name,
                            types,
                            arguments.toArray(new Value[0]),
                            symbolicTraceHandler);
        }

        // Model of a whitelisted pure return (result UF over symbolic inputs + the same UF over
        // constant observed inputs, for the observed pair); stays null -> recovery concretizes.
        PureUFModel pureUF = null;
        // When the method is not implemented and its not on the ignore list, we record it
        if (retValue instanceof PlaceHolder &&
                !(IGNORED_INVOCATIONS.contains(owner + "/" + name)
                || IGNORED_INVOCATIONS.contains(owner))) {

            if(isInstance) arguments.add(0, instance);

            if(containsSymbolicArgument) {
                logger.warn(
                        "Missing invocation of symbolic method {} in class {} with arguments {}",
                        name,
                        owner,
                        desc
                );
            } else {
                logger.trace(
                        "Missing invocation of method {} in class {} with arguments {}",
                        name,
                        owner,
                        desc
                );
            }


            ThreadHandler.recordMissingInvocation(Thread.currentThread().getId(),  new InvocationEntry(
                    owner,
                    name,
                    desc,
                    isInstance,
                    invokeId,
                    containsSymbolicArgument));

            // Model a whitelisted pure, value-returning call as a generic UF over its inputs
            // (instead of concretizing at recovery). Sound by construction: an axiom-free UF
            // over-approximates any deterministic function. Only when an input is symbolic;
            // `arguments` here already includes the receiver (prepended above).
            if (containsSymbolicArgument && PureMethods.isWhitelisted(owner, name, desc)) {
                pureUF = buildPureUF(owner, name, desc, arguments);
            }

            if(
                    // A successfully UF-modeled pure call loses no context - the whitelist
                    // guarantees no side effects and the return is captured by the UF - so it must NOT
                    // downgrade SAFE; only flag context loss when the call was not modeled.
                    pureUF == null
                    && (retValue.equals(PlaceHolder.instance) // To detect a missing implementation
                    || retValue instanceof VoidValue vv && !vv.isSymbolic())  // To detect a missing implementation that returns nothing
                            && containsSymbolicArgument) {
                // Too strict? What about void methods that always have return value PlaceHolder.instance?
                logger.warn("Invocation of method {} in class {} with arguments {} cases context loss",
                        name,
                        owner,
                        desc);
                symbolicTraceHandler.recordSymbolicContextLoss();
            }
        }

        // Tag an unmodeled placeholder return so visitGETVALUE_Object recovers a value-typed
        // result instead of identity-recovering it (which would re-bind the receiver's symbolic value,
        // e.g. String.toLowerCase() returning `this`). If a generic UF was built, it rides along
        // and the result is modeled as that UF; otherwise recovery concretizes. This MUST stay
        // after the context-loss check above, which compares retValue against PlaceHolder.instance by
        // identity.
        if (retValue == PlaceHolder.instance) {
            retValue = new PlaceHolder(
                    PlaceHolder.ValueOrigin.UNMODELED_RETURN,
                    pureUF == null ? null : pureUF.result(),
                    pureUF == null ? null : pureUF.observedApplication());
        }
        return retValue;
    }

    /** A whitelisted pure call modeled as a generic UF: the result over symbolic inputs, and the
     * same UF over the constant observed inputs for the observed (input -> output) pair. */
    private record PureUFModel(Formula result, Formula observedApplication) {}

    /**
     * Build the generic UF {@code pure_<sig>(inputs)} for a whitelisted pure call, or null to fall
     * back to concretization. Handles String and all primitive returns (the return sort is
     * {@link #pureUFReturnType}); the inputs (receiver + args) must all be value-typed so their
     * formula fully captures the input (sound; no stateful receivers). Also builds the same UF applied
     * to the CONSTANT inputs (the observed-pair application) using the SAME cached declaration;
     * that is null unless the return is a String and every input is a String (the only case whose
     * recovery side asserts the observed pair, and where String concretes become constant formulas).
     */
    private static PureUFModel buildPureUF(
            String owner, String name, String desc, List<Value<?, ?>> inputs)
            throws NoThreadContextException {
        // The UF's return sort is the method's return type - String or any primitive. Unsupported
        // returns (void, arrays, non-String objects) yield null and fall back to concretization.
        FormulaType<?> returnType = pureUFReturnType(desc);
        if (returnType == null) {
            return null;
        }
        StringFormulaManager smgr =
                ThreadHandler.getSolverContext(Thread.currentThread().getId())
                        .getFormulaManager()
                        .getStringFormulaManager();
        List<Formula> symbolicArgs = new ArrayList<>();
        List<Formula> constArgs = new ArrayList<>();
        boolean observable = true; // an observed pair needs constant-buildable (String) inputs
        for (Value<?, ?> v : inputs) {
            if (v.formula == null || !Util.isValueType(v.concrete)) {
                return null; // non-value-typed or formula-less input: defer to concretization.
            }
            symbolicArgs.add((Formula) v.formula);
            if (v.concrete instanceof String s) {
                constArgs.add(smgr.makeString(s));
            } else {
                observable = false; // only String inputs become constant formulas here.
            }
        }
        PureFunctionUF uf = ThreadHandler.getUFHandler(Thread.currentThread().getId()).getPureFunctionUF();
        String ufName = PureMethods.ufName(owner, name, desc);
        Formula result = uf.apply(ufName, returnType, symbolicArgs);
        // Observed pair: built only for String returns, the sole case whose recovery side
        // (visitGETVALUE_Object) asserts the (constant inputs -> observed output) equality. The same
        // cached declaration is applied to the constant inputs so the pair constrains the very symbol
        // used in `result`. Primitive observed pairs are future work (with the explorer side).
        Formula observed = (observable && FormulaType.StringType.equals(returnType))
                ? uf.apply(ufName, returnType, constArgs) : null;
        return new PureUFModel(result, observed);
    }

    /**
     * The SMT return sort for a whitelisted pure method, matching the shadow value sorts exactly:
     * String; boolean; bitvectors of width 8/16/16/32/64 for byte/short/char/int/long; and
     * floating-point (single for float, double for double). Returns null for unsupported returns
     * (void, arrays, non-String objects), which fall back to concretization.
     */
    private static FormulaType<?> pureUFReturnType(String desc) {
        Type ret = Type.getReturnType(desc);
        switch (ret.getSort()) {
            case Type.BOOLEAN:
                return FormulaType.BooleanType;
            case Type.BYTE:
                return FormulaType.getBitvectorTypeWithSize(8);
            case Type.SHORT:
            case Type.CHAR:
                return FormulaType.getBitvectorTypeWithSize(16);
            case Type.INT:
                return FormulaType.getBitvectorTypeWithSize(32);
            case Type.LONG:
                return FormulaType.getBitvectorTypeWithSize(64);
            case Type.FLOAT:
                return FormulaType.getSinglePrecisionFloatingPointType();
            case Type.DOUBLE:
                return FormulaType.getDoublePrecisionFloatingPointType();
            case Type.OBJECT:
                return "java.lang.String".equals(ret.getClassName()) ? FormulaType.StringType : null;
            default:
                return null; // void, array, other reference types
        }
    }

}
