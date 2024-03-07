package de.uzl.its.swat.symbolic;

import de.uzl.its.swat.Main;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Template for a Class, that contains the classes fields and static fields Template provided for
 * the ClassDepot where the Template is matched with its respective class
 */
class ClassTemplate {
    /** List of the classes fields */
    private final ArrayList<String> fields;
    /** List of the classes static fields */
    private final ArrayList<String> staticFields;

    /**
     * Constructor for the ClassTemplate Automatically populates the fields and static fields
     *
     * @param c The class for which the template is build
     */
    public ClassTemplate(Class<?> c) {
        fields = new ArrayList<>();
        staticFields = new ArrayList<>();
        populateAllFields(c);
    }

    /**
     * Called when the Template is created Populates fields and staticFields with the respective
     * Fields from the Class
     *
     * @param c The class for which the fields are populated
     */
    private void populateAllFields(Class<?> c) {

        // Get fields, this might cause a class load, which might trigger an instrumented class
        // loader. Hence we have to
        // disable the tracking during the load.
        Main.disableSymbolicTracking();
        Field[] fields = c.getDeclaredFields();
        Main.enableSymbolicTracking();

        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                addField(field.getName());
            } else {
                addStaticField(field.getName());
            }
        }
    }

    /**
     * Adds all (static) fields from another template (the parent class) to the current template
     *
     * @param pt the paren Template who's fields should bwe added to the child class
     */
    public void addFields(ClassTemplate pt) {
        fields.addAll(0, pt.fields);
        staticFields.addAll(0, pt.staticFields);
    }

    /**
     * Called when the Template is created Adds a field to the field array
     *
     * @param name The name of the field, derived from the Class object
     */
    private void addField(String name) {
        fields.add(name);
    }

    /**
     * Called when the Template is created Adds a static field to the field array
     *
     * @param name The name of the static field, derived from the Class object
     */
    private void addStaticField(String name) {
        staticFields.add(name);
    }

    /**
     * Getter for the index of a field from the template
     *
     * @param name The name of the field to get
     * @return The index of the field with the name
     */
    public int getFieldIndex(String name) {
        return fields.indexOf(name);
    }

    /**
     * Getter for the index of a static field from the template
     *
     * @param name The name of the static field to get
     * @return The index of thes tatic field with the name
     */
    public int getStaticFieldIndex(String name) {
        return staticFields.indexOf(name);
    }

    /**
     * Gets the total number of fields
     *
     * @return The total number of fields
     */
    public int nFields() {
        return fields.size();
    }

    /**
     * Gets the total number of static fields
     *
     * @return The total number of static fields
     */
    public int nStaticFields() {
        return staticFields.size();
    }

    /**
     * Override of the default toString method for printing the fields of a Class template
     *
     * @return The sstring representation of both the fieldas and static fields
     */
    @Override
    public String toString() {
        return "Fields: " + fields.toString() + "/nStatic fields: " + staticFields.toString();
    }
}
