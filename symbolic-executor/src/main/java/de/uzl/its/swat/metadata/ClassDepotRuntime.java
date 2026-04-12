package de.uzl.its.swat.metadata;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.ThreadAlreadyDisabledException;
import de.uzl.its.swat.common.exceptions.ThreadAlreadyEnabledException;

import javax.annotation.Nullable;
import java.util.List;

public interface ClassDepotRuntime extends ClassDepotInstrumentation {
    int getFieldIndex(int cIdx, String fieldName, boolean isStatic) throws ThreadAlreadyDisabledException, NoThreadContextException, ThreadAlreadyEnabledException, ClassNotFoundException;
    int getTotalFieldCountWithReflection(String className, @Nullable Class<?> cls)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException;
    int getFieldCountWithReflection(String className, @Nullable Class<?> cls, boolean isStatic)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException;
    int getMethodIdxAtRuntime(String className, @Nullable Class<?> cls, String methodName, String desc)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException;
    List<String> getParentsForClass(String className);
    List<String> getInterfacesForClass(String className);
    int getClassCounter();
}
