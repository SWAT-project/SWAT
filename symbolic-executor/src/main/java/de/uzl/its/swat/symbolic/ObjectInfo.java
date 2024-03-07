package de.uzl.its.swat.symbolic;

import de.uzl.its.swat.symbolic.value.PlaceHolder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/** Containing the static info of a class and dynamic values of the static fields. */
public class ObjectInfo implements Serializable {
    private final String className;
    private final ClassDepot classDepot;
    public Object[] statics;
    Map<String, Integer> fieldNameToIndex;
    ArrayList<FieldInfo> fieldList;
    Map<String, Integer> staticFieldNameToIndex;
    ArrayList<FieldInfo> staticFieldList;
    int nStaticFields;
    private int nFields;

    public ObjectInfo(String className, ClassDepot classDepot) {
        this.className = className.replace('/', '.');
        nFields = -1;
        this.classDepot = classDepot;
    }

    private static Map<String, Integer> createMap(Map<String, Integer> fieldNameToIndex) {
        if (fieldNameToIndex == null) {
            return new TreeMap<>();
        }
        return fieldNameToIndex;
    }

    private static ArrayList<FieldInfo> createList(ArrayList<FieldInfo> f) {
        if (f == null) {
            return new ArrayList<>();
        }
        return f;
    }

    public String getClassName() {
        return className;
    }

    private int get(
            String fieldName,
            boolean isStatic,
            Map<String, Integer> fieldNameToIndex,
            ArrayList<FieldInfo> fieldList) {
        Integer i = fieldNameToIndex.get(fieldName);
        if (i == null) {
            i = fieldList.size();
            fieldNameToIndex.put(fieldName, i);
            fieldList.add(new FieldInfo(className, fieldName, isStatic, classDepot));
        }
        return i;
    }

    public int getIdx(String fieldName, boolean isStatic) {
        if (isStatic) {
            staticFieldNameToIndex = createMap(staticFieldNameToIndex);
            staticFieldList = createList(staticFieldList);
            return get(fieldName, true, staticFieldNameToIndex, staticFieldList);
        }
        fieldNameToIndex = createMap(fieldNameToIndex);
        fieldList = createList(fieldList);
        return get(fieldName, false, fieldNameToIndex, fieldList);
    }

    public FieldInfo get(int i, boolean isStatic) {
        if (isStatic) {
            return staticFieldList.get(i);
        }
        return fieldList.get(i);
    }

    public Object getStaticField(int fieldId) {
        if (fieldId == -1) return PlaceHolder.instance;
        initialize();
        return statics[fieldId]; // null if no value known
    }

    public void setStaticField(int fieldId, Object value) {
        initialize();
        statics[fieldId] = value;
    }

    @Override
    public String toString() {
        return "ObjectInfo{"
                + "fieldNameToIndex="
                + fieldNameToIndex
                + ", fieldList="
                + fieldList
                + ", staticFieldNameToIndex="
                + staticFieldNameToIndex
                + ", staticFieldList="
                + staticFieldList
                + ", nStaticFields="
                + nStaticFields
                + ", nFields="
                + nFields
                + ", statics="
                + (statics == null ? null : Arrays.asList(statics))
                + ", className='"
                + className
                + '\''
                + '}';
    }

    private void initialize() {
        if (nFields == -1) {
            nFields = classDepot.nFields(className);
            nStaticFields = classDepot.nStaticFields(className);
            statics = new Object[nStaticFields];
        }
    }

    public int getNFields() {
        initialize();
        return nFields;
    }

    public int getNStaticFields() {
        initialize();
        return nStaticFields;
    }
}
