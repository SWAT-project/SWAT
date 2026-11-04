package de.uzl.its.swat.symbolic.invoke;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;

/**
 * Symbolic side of {@link de.uzl.its.swat.common.RefIdentity}. Its results are concrete facts about
 * where objects were created; only {@code valueDependentIdentity} affects the symbolic state.
 */
public final class RefIdentityInvocation {

    public static Value<?, ?> invokeStaticMethod(
            String name, Value<?, ?>[] args, SymbolicTraceHandler symbolicTraceHandler) throws NotImplementedException {
        if (name.equals("valueDependentIdentity")) {
            for (Value<?, ?> arg : args) {
                if (arg.isSymbolic()) {
                    // A reference comparison decided by symbolic values that SWAT does not encode.
                    symbolicTraceHandler.recordReferenceSemanticChange();
                    break;
                }
            }
        }
        return PlaceHolder.instance;
    }
}
