package de.uzl.its.swat.metadata;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A class template that holds the field and method information for a class.
 * It maintains separate mappings for instance and static fields.
 * When building a template for a class, inherited fields are merged in so that
 * the parent's fields retain their indices.
 *
 * Additionally, we track short field names -> the "winning" fully qualified name.
 * This lets us unify references like "ChildClass#field" to the parent's actual
 * "ParentClass#field" if the child does not truly hide that field.
 */
public class ClassTemplate {

    // Indicates whether the template has been fully populated.
    @Getter @Setter
    private boolean complete = false;

    // The normalized/unique name of the class (e.g., "java/lang/String").
    @Getter
    private final String className;

    /**
     * Method information.
     */
    private final Map<String, Integer> methodToIndex = new HashMap<>();
    private final List<String> methodNames = new ArrayList<>();

    /**
     * Instance field mappings:
     *   key = fully qualified field identifier (e.g. "package/Class#field")
     *   value = index (slot) within the instance layout.
     */
    private final Map<String, Integer> instanceFieldToIndex = new HashMap<>();
    private final List<String> instanceFields = new ArrayList<>();

    /**
     * Static field mappings:
     *   key = fully qualified field identifier
     *   value = index in the static field list.
     */
    private final Map<String, Integer> staticFieldToIndex = new HashMap<>();
    private final List<String> staticFields = new ArrayList<>();

    /**
     * Short-name resolution for instance fields:
     *   key = short field name (e.g. "foo"),
     *   value = the fully qualified field identifier that "wins" in this class
     *           (e.g. "parentPkg/ParentClass#foo" or "childPkg/ChildClass#foo").
     */
    private final Map<String, String> shortNameToFqInstance = new HashMap<>();

    /**
     * Short-name resolution for static fields.
     */
    private final Map<String, String> shortNameToFqStatic   = new HashMap<>();

    public ClassTemplate(String className) {
        this.className = className;
    }

    /* ------------------------------------------------
     *   Field lookup / indexing
     * ------------------------------------------------ */

    /**
     * Primary lookup method. The fieldIdentifier can be either:
     *   1) Fully qualified, e.g. "pkg/ChildClass#foo"
     *   2) Already partially "resolved" but still referencing the child's name for an inherited field
     *
     * If we don't find a direct match in fieldToIndex, we parse the short name
     * and see if there's an inherited or parent-defined field that wins.
     */
    public int getFieldIndex(String fieldIdentifier, boolean isStatic) {
        Map<String, Integer> fieldToIndex = isStatic ? staticFieldToIndex : instanceFieldToIndex;

        // 1) If we have a direct match (fully qualified exactly),
        //    return its index right away.
        Integer directIndex = fieldToIndex.get(fieldIdentifier);
        if (directIndex != null) {
            return directIndex;
        }

        // 2) Otherwise, parse out the short name from "className#fieldName".
        int fieldBegin = fieldIdentifier.indexOf(Util.FIELD_DIVIDER);
        SWATAssert.enforce(fieldBegin > 0, "Invalid field identifier: {}",  fieldIdentifier);
        String shortName = fieldIdentifier.substring(fieldBegin + 1);

        // 3) Check the shortName -> FQ map. If we have an entry, use that.
        Map<String, String> shortMap = isStatic ? shortNameToFqStatic : shortNameToFqInstance;
        String winningFqName = shortMap.get(shortName);
        if (winningFqName != null) {
            // If we have a "winning" FQ name, that's the real field.
            Integer idx = fieldToIndex.get(winningFqName);
            if (idx != null) {
                return idx;
            }
        }

        // 4) If not found, that means we have a truly new field that wasn't discovered
        //    by merging. We'll add it. (Rare if we enumerated all fields, but possible.)
        return addField(fieldIdentifier, isStatic);
    }

    /**
     * Returns the field identifier at the specified index.
     */
    public String getFieldIdentifier(int fieldIndex, boolean isStatic) {
        List<String> fields = isStatic ? staticFields : instanceFields;
        return fields.get(fieldIndex);
    }

    /**
     * Adds a new field (if not already present) and returns its assigned index.
     * This also updates the shortName -> FQ map, overriding a parent's entry if needed.
     */
    public int addField(String fieldIdentifier, boolean isStatic) {
        Map<String, Integer> fieldToIndex = isStatic ? staticFieldToIndex : instanceFieldToIndex;
        List<String> fields = isStatic ? staticFields : instanceFields;
        Map<String, String> shortMap = isStatic ? shortNameToFqStatic : shortNameToFqInstance;

        if (fieldToIndex.containsKey(fieldIdentifier)) {
            return fieldToIndex.get(fieldIdentifier);
        }

        // Extract short name: "pkg/Class#field" -> "field"
        int hashPos = fieldIdentifier.indexOf('#');
        String shortName = fieldIdentifier.substring(hashPos + 1);

        // Overwrite the shortName -> FQ entry. (Hiding or brand new field.)
        shortMap.put(shortName, fieldIdentifier);

        int newIndex = fields.size();
        fieldToIndex.put(fieldIdentifier, newIndex);
        fields.add(fieldIdentifier);
        return newIndex;
    }

    /**
     * Merges fields from a parent or interface template into this template.
     * Inherited fields are added only if they do not already exist or are not hidden.
     */
    public void mergeFields(ClassTemplate otherTemplate) {
        // Merge instance fields.
        for (String parentFq : otherTemplate.instanceFields) {
            // If we already have that exact FQ name, skip.
            if (!instanceFieldToIndex.containsKey(parentFq)) {
                addField(parentFq, false);
            }
        }
        // Merge static fields.
        for (String parentFq : otherTemplate.staticFields) {
            if (!staticFieldToIndex.containsKey(parentFq)) {
                addField(parentFq, true);
            }
        }
    }

    /* ------------------------------------------------
     *   Method indexing
     * ------------------------------------------------ */

    int getMethodIndex(String methodIdentifier) {
        Integer existing = methodToIndex.get(methodIdentifier);
        if (existing != null) {
            return existing;
        }
        return addMethod(methodIdentifier);
    }

    String getMethodIdentifier(int mIdx) {
        return methodNames.get(mIdx);
    }

    private int addMethod(String methodIdentifier) {
        if (methodToIndex.containsKey(methodIdentifier)) {
            return methodToIndex.get(methodIdentifier);
        }
        int newIndex = methodNames.size();
        methodToIndex.put(methodIdentifier, newIndex);
        methodNames.add(methodIdentifier);
        return newIndex;
    }

    /* ------------------------------------------------
     *   Reflection-based addition
     * ------------------------------------------------ */

    /**
     * Adds fields from an array of reflected Field objects.
     * (Assuming we gather private/protected as well via getDeclaredFields.)
     */
    public void addFields(Field[] fields) {
        for (Field field : fields) {
            // "declaringClass#fieldName"
            String declClass = Util.formatClassName(field.getDeclaringClass().getName());
            String fieldIdentifier = Util.formatFieldName(declClass, field.getName());
            addField(fieldIdentifier, Modifier.isStatic(field.getModifiers()));
        }
    }

    /* ------------------------------------------------
     *   Utilities
     * ------------------------------------------------ */

    /**
     * Returns the count of fields in this template for the specified static-ness.
     */
    int getFieldCount(boolean isStatic) {
        return isStatic ? staticFields.size() : instanceFields.size();
    }

    /**
     * Returns the total number of fields (static + instance) in this template.
     */
    int getTotalFieldCount() {
        return staticFields.size() + instanceFields.size();
    }

    public boolean hasField(String fieldIdentifier, boolean isStatic) {
        Map<String, Integer> fieldToIndex = isStatic ? staticFieldToIndex : instanceFieldToIndex;
        return fieldToIndex.containsKey(fieldIdentifier);
    }
}
