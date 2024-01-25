package de.uzl.its.swat.interpreters;

import de.uzl.its.swat.Main;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Serializable depot for classes represented using their name and a corresponding class template in
 * a tree map
 */
public class ClassDepot implements Serializable {
    /**
     * Map that contains Classnames and their corresponding templates Each Template contains the
     * classes fields and static fields (names)
     */
    private final Map<String, ClassTemplate> templates;
    /**
     * TODO understand this behaviour, validate it is wanted and document it here | Gitlab issue #17
     */
    private static final ClassDepot instance = new ClassDepot();

    /**
     * Getter for this classes instance.
     *
     * @return An instance of this ClassDepot
     */
    public static ClassDepot getInstance() {
        return instance;
    }

    /** Constructor that instantiates the template map */
    public ClassDepot() {
        templates = new TreeMap<>();
    }

    /**
     * Getter for the template of a class if the template is not yet present it is first created and
     * stored in the template tree If the class has a superclass (parent) its fields are also added
     * (recursively)
     *
     * @param cName The class name
     * @param clazz The class object
     * @return The class template containing the names of the fields and static fields
     */
    private ClassTemplate getOrCreateTemplate(String cName, Class<?> clazz) {
        ClassTemplate ct = templates.get(cName);
        if (ct != null) return ct;

        ct = new ClassTemplate(clazz);
        templates.put(cName, ct);
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            ClassTemplate pt = getOrCreateTemplate(parent.getName(), parent);
            ct.addFields(pt);
        }
        return ct;
    }

    /**
     * Loads a class for a given name and disables the logging during the load. Currently it exits
     * the program in case the class is not found.
     *
     * @param className classname to be loaded
     * @return the loaded class or null if class is not found
     */
    private Class<?> loadClassForName(String className) {
        try {
            Main.disableSymbolicTracking();
            Class<?> clazz =
                    Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            Main.enableSymbolicTracking();
            return clazz;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Runtime.getRuntime().halt(1);
        }

        // Default return
        return null;
    }

    /**
     * Gets the index of a field by name and class
     *
     * @param cName The class containing the field
     * @param field The field name
     * @return The index of the field, or -1 if the field Name is not found
     */
    public int getFieldIndex(String cName, String field) {
        Class<?> clazz = loadClassForName(cName);
        ClassTemplate ct = getOrCreateTemplate(cName, clazz);
        return ct.getFieldIndex(field);
    }

    /**
     * Gets the index of a static field by name and class
     *
     * @param cName The class containing the static field
     * @param field The static field name
     * @return The index of the static field, or -1 if the field Name is not found
     */
    public int getStaticFieldIndex(String cName, String field) {
        Class<?> clazz = loadClassForName(cName);
        ClassTemplate ct = getOrCreateTemplate(cName, clazz);
        return ct.getStaticFieldIndex(field);
    }

    /**
     * Gets the number of fields in the template
     *
     * @param cName The name of the class
     * @return The number of fields
     */
    public int nFields(String cName) {
        Class<?> clazz = loadClassForName(cName);
        ClassTemplate ct = getOrCreateTemplate(cName, clazz);
        return ct.nFields();
    }

    /**
     * Gets the number of static fields in the template
     *
     * @param cName The name of the class
     * @return The number of static fields
     */
    public int nStaticFields(String cName) {
        Class<?> clazz = loadClassForName(cName);
        ClassTemplate ct = getOrCreateTemplate(cName, clazz);
        return ct.nStaticFields();
    }
}
