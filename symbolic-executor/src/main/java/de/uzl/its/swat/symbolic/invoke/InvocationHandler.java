package de.uzl.its.swat.symbolic.invoke;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;

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

            if(
                    (retValue.equals(PlaceHolder.instance) // To detect a missing implementation
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
        return retValue;
    }

}
