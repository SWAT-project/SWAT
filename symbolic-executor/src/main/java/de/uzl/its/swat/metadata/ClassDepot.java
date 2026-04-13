package de.uzl.its.swat.metadata;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.ThreadAlreadyDisabledException;
import de.uzl.its.swat.common.exceptions.ThreadAlreadyEnabledException;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

/**
 * A singleton repository that tracks classes and their corresponding fields.
 * Each unique (formatted) class name is assigned a stable index.
 * Field indices within a class are assigned based on a per–class-hierarchy layout.
 *
 * Inherited instance fields are merged into a subclass’s layout (preserving their original index).
 * For static fields, the template of the referenced class (typically the one from the constant pool) is used.
 */
public class ClassDepot implements Serializable, ClassDepotInstrumentation, ClassDepotRuntime {

    // Global counter to assign a unique index to each class.
    @Getter
    private int classCounter = 0;

    // Maps a formatted class name to its unique integer index.
    private final Map<String, Integer> classToIndex = new HashMap<>();

    // Maps the class index back to its formatted name.
    private final Map<Integer, String> indexToClass = new HashMap<>();

    /**
     * Holds ClassTemplate instances keyed by class index.
     * Each template contains the field layout (instance and static) for that class,
     * including fields inherited from its superclasses and interfaces.
     */
    private final Map<Integer, ClassTemplate> classTemplates = new HashMap<>();

    private final Map<String, List<String>> classToParents = new HashMap<>();
    private final Map<String, List<String>> classToInterfaces = new HashMap<>();

    private final Map<String, Set<String>> ancestorBaseCache = new HashMap<>();


    // Singleton instance.
    private static final ClassDepot instance = new ClassDepot();

    private ClassDepot() {
        // Prevent outside instantiation.
    }

    public static ClassDepotRuntime getRuntimeInstance() {
        return instance;
    }

    public synchronized Map<String, Set<String>> getAncestorBaseCache() {
        return ancestorBaseCache;
    }

    public static ClassDepotInstrumentation getInstrumentationInstance() {
        return instance;
    }

    /**
     * Registers type metadata (parent and interface names) for a class.
     * Idempotent: silently returns if the class is already registered with identical metadata.
     * Asserts if the class is already registered with different metadata.
     */
    public synchronized void registerTypeInfoForClass(String className, List<String> parents, List<String> interfaces) {
        String normalized = Util.formatClassName(className);
        if (ancestorBaseCache.containsKey(normalized)) {
            SWATAssert.enforce(
                    parents.equals(classToParents.get(normalized))
                            && interfaces.equals(classToInterfaces.get(normalized)),
                    "Class " + normalized + " already registered in ancestorBaseCache with different metadata");
            return;
        }
        classToParents.put(normalized, parents);
        classToInterfaces.put(normalized, interfaces);
    }

    public synchronized List<String> getParentsForClass(String className) {
        return classToParents.getOrDefault(Util.formatClassName(className), Collections.emptyList());
    }

    public synchronized List<String> getInterfacesForClass(String className) {
        return classToInterfaces.getOrDefault(Util.formatClassName(className), Collections.emptyList());
    }

    /**
     * Retrieves (or creates) a ClassTemplate for the given class using reflection.
     * This method loads the class if necessary, merges in parent and interface fields,
     * and then caches the complete template.
     */
    private ClassTemplate getFillOrCreateTemplateWithReflection(String className, @Nullable Class<?> clazz)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        className = Util.formatClassName(className);
        int cIdx = getClassIndex(className);
        ClassTemplate template = classTemplates.get(cIdx);
        if (template != null && template.isComplete()) {
            return template;
        }

        // Load class via reflection if not provided.
        if (clazz == null) {
            clazz = Util.loadClassForName(className);
        }
        if(template == null) {
            template = new ClassTemplate(className);
        }

        // Process parent class: merge its fields so inherited fields retain their indices.
        Class<?> parentClazz = Util.getSuperclass(clazz);
        if (parentClazz != null && parentClazz != Object.class) {
            String parentName = Util.formatClassName(parentClazz.getName());
            ClassTemplate parentTemplate = getFillOrCreateTemplateWithReflection(parentName, parentClazz);
            template.mergeFields(parentTemplate);
        }

        // Process implemented interfaces.
        Class<?>[] interfaces = Util.getInterfaces(clazz);
        for (Class<?> interfaceClazz : interfaces) {
            String interfaceName = Util.formatClassName(interfaceClazz.getName());
            ClassTemplate interfaceTemplate = getFillOrCreateTemplateWithReflection(interfaceName, interfaceClazz);
            template.mergeFields(interfaceTemplate);
        }

        // Add fields declared in the current class using reflection.
        template.addFields(Util.getDeclaredFields(clazz));

        // Mark the template as complete and cache it.
        template.setComplete(true);
        classTemplates.put(cIdx, template);

        return template;
    }

    @Override
    public String getMethodIdentifier(int cIdx, int mIdx) {
        ClassTemplate ct = classTemplates.get(cIdx);
        if (ct != null) {
            return ct.getMethodIdentifier(mIdx);
        } else {
           new ErrorHandler().handleException(new RuntimeException("Class " + cIdx + " not found. Method " + mIdx));
           return null;
        }
    }

    @Override
    public synchronized int getMethodIdxForInstrumentation(String className, String methodName, String desc) {
        int cIdx = getClassIndex(className);
        String methodNameFormatted = Util.formatMethodName(className, methodName, desc);
        ClassTemplate template = classTemplates.get(cIdx);

        if (template == null) {
            template = new ClassTemplate(Util.formatClassName(className));
            classTemplates.put(cIdx, template);
        }

        return template.getMethodIndex(methodNameFormatted);
    }

    @Override
    public int getMethodIdxAtRuntime(String className, @Nullable Class<?> cls, String methodName, String desc)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        String methodNameFormatted = Util.formatMethodName(className, methodName, desc);
        ClassTemplate template = getFillOrCreateTemplateWithReflection(className, cls);

        return template.getMethodIndex(methodNameFormatted);
    }

    /**
     * For instrumented field access, this helper returns the field index.
     * If the field name is not fully qualified (does not contain the divider),
     * it is formatted using the concrete class name.
     *
     * In the static case (or when no instance is available), the concreteClassName
     * is taken from the instruction’s reference.
     *
     * @param cIdx             the class index (as stored in the depot)
     * @param fieldName        the field name from the instruction
     * @param isStatic         whether the field is static
     * @return the field index from the corresponding ClassTemplate
     */
    public int getFieldIndex(int cIdx, String fieldName, boolean isStatic) throws ThreadAlreadyDisabledException, NoThreadContextException, ThreadAlreadyEnabledException, ClassNotFoundException {
        String className = getClassName(cIdx);
        ClassTemplate template = classTemplates.get(cIdx);
        if (template == null || !template.isComplete()) {
            template = getFillOrCreateTemplateWithReflection(className, null);
        }

        String fieldIdentifier = fieldName.contains(Util.FIELD_DIVIDER)
                ? fieldName
                : Util.formatFieldName(className, fieldName);
        return template.getFieldIndex(fieldIdentifier, isStatic);
    }

    @Override
    public int getTotalFieldCountWithReflection(String className, @Nullable Class<?> cls)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        ClassTemplate ct = getFillOrCreateTemplateWithReflection(className, cls);
        int totalFieldCount = ct.getTotalFieldCount();
        SWATAssert.check(totalFieldCount ==
                Util.getFieldCount(className, cls, false) +
                Util.getFieldCount(className, cls, true), "Total field count mismatch");
        return totalFieldCount;
    }

    @Override
    public int getFieldCountWithReflection(String className, @Nullable Class<?> cls, boolean isStatic)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException, ClassNotFoundException {
        ClassTemplate ct = getFillOrCreateTemplateWithReflection(className, cls);
        int fieldCount = ct.getFieldCount(isStatic);
        SWATAssert.check(fieldCount == Util.getFieldCount(className, cls, isStatic), "Field count mismatch");
        return fieldCount;
    }

    /**
     * Returns a stable integer "class index" for the specified class name.
     * The class name is normalized before being stored.
     */
    @Override
    public synchronized int getClassIndex(String className) {
        String normalized = Util.formatClassName(className);

        Integer classIndex = classToIndex.get(normalized);
        if (classIndex == null) {
            classIndex = classCounter++;
            classToIndex.put(normalized, classIndex);
            indexToClass.put(classIndex, normalized);
        }
        return classIndex;
    }

    /**
     * <p>
     * Retrieves the formatted class name (e.g., "java/lang/String") corresponding to a given integer
     * index. This is the inverse operation of {@link #getClassIndex(String)}.
     * </p>
     *
     * @param cIdx The class index.
     * @return The formatted class name associated with the provided index, or {@code null} if none exists.
     */
    @Override
    public String getClassName(int cIdx) {
        if (cIdx >= classCounter) {
            System.out.println("Class index " + cIdx + " is out of bounds. Maximum index is " + (classCounter - 1));
        }
        return indexToClass.get(cIdx);
    }
}
