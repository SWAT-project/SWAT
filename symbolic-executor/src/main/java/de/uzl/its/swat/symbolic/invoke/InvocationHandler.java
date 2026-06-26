package de.uzl.its.swat.symbolic.invoke;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
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

public class InvocationHandler {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private static final ArrayList<String> IGNORED_INVOCATIONS =
            new ArrayList<>(
                    Arrays.asList(
                            "java/io/PrintStream/println",
                            "de/uzl/its/swat/instrument/Intrinsics",
                            "de/uzl/its/swat/common/UtilInstrumented",
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

        // G4: generic UF for a whitelisted pure return; stays null -> recovery concretizes (G2).
        Formula pureUF = null;
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

            // G4: model a whitelisted pure, value-returning call as a generic UF over its inputs
            // (instead of concretizing at recovery). Sound by construction: an axiom-free UF
            // over-approximates any deterministic function. Only when an input is symbolic;
            // `arguments` here already includes the receiver (prepended above).
            if (containsSymbolicArgument && PureMethods.isWhitelisted(owner, name, desc)) {
                pureUF = buildPureUF(owner, name, desc, arguments);
            }

            if(
                    // G4: a successfully UF-modeled pure call loses no context - the whitelist
                    // guarantees no side effects and the return is captured by the UF - so it must NOT
                    // downgrade SAFE; only flag context loss when we did not model the call.
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

        // G2/G4: tag an unmodeled placeholder return so visitGETVALUE_Object recovers a value-typed
        // result instead of identity-recovering it (which would re-bind the receiver's symbolic value,
        // e.g. String.toLowerCase() returning `this`). If a generic UF was built (G4), it rides along
        // and the result is modeled as that UF; otherwise recovery concretizes (G2). This MUST stay
        // after the context-loss check above, which compares retValue against PlaceHolder.instance by
        // identity.
        if (retValue == PlaceHolder.instance) {
            retValue = new PlaceHolder(PlaceHolder.ValueOrigin.UNMODELED_RETURN, pureUF);
        }
        return retValue;
    }

    /**
     * Build the generic UF {@code pure_<sig>(inputs)} for a whitelisted pure call, or null to fall
     * back to G2 concretization. v1 handles String returns only; the inputs (receiver + args) must
     * all be value-typed so their formula fully captures the input (sound; no stateful receivers).
     */
    private static Formula buildPureUF(
            String owner, String name, String desc, List<Value<?, ?>> inputs)
            throws NoThreadContextException {
        // v1 scope: only String-returning methods are materialized as UFs.
        if (!"java.lang.String".equals(Type.getReturnType(desc).getClassName())) {
            return null;
        }
        List<Formula> argFormulas = new ArrayList<>();
        for (Value<?, ?> v : inputs) {
            if (v.formula == null || !Util.isValueType(v.concrete)) {
                return null; // non-value-typed or formula-less input: defer to G2 concretize.
            }
            argFormulas.add((Formula) v.formula);
        }
        return ThreadHandler.getUFHandler(Thread.currentThread().getId())
                .getPureFunctionUF()
                .apply(PureMethods.ufName(owner, name, desc), FormulaType.StringType, argFormulas);
    }

}
