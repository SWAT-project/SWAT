package de.uzl.its.swat.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ClassDepotInstrumentation {
    String getClassName(int cIdx);
    int getClassIndex(String className);
    int getMethodIdxForInstrumentation(String className, String methodName, String desc);
    String getMethodIdentifier(int cIdx, int mIdx);
    void registerTypeInfoForClass(String className, List<String> parents, List<String> interfaces);
    Map<String, Set<String>> getAncestorBaseCache();
}
