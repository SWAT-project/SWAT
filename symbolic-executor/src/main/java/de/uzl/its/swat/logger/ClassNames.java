package de.uzl.its.swat.logger;

import de.uzl.its.swat.interpreters.ClassDepot;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ClassNames implements Serializable {
    private Map<String, Integer> nameToIndex;
    private List<ObjectInfo> classList;

    private static final ClassNames instance = new ClassNames();

    public static ClassNames getInstance() {
        return instance;
    }

    private final ClassDepot classDepot;

    private ClassNames() {
        this.classDepot = ClassDepot.getInstance();
    }

    public int get(String className) {
        if (nameToIndex == null) {
            nameToIndex = new TreeMap<>();
        }
        if (classList == null) {
            classList = new ArrayList<>();
        }
        Integer i = nameToIndex.get(className);
        if (i == null) {
            i = classList.size();
            nameToIndex.put(className, i);
            classList.add(new ObjectInfo(className, classDepot));
        }
        return i;
    }

    public ObjectInfo get(int i) {
        return classList.get(i);
    }

    @Override
    public String toString() {
        return "ClassNames{\n"
                + "nameToIndex="
                + nameToIndex
                + "\n, classList="
                + classList
                + "\n}";
    }
}
