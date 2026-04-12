package de.uzl.its.targets.finance_engine.util;

import java.util.List;

public class InsertionSorter {
    public interface KeyExtractor<T> {
        long key(T item);
    }

    public static <T> void sortByKey(List<T> list, KeyExtractor<T> ke) {
        for (int i = 1; i < list.size(); ++i) {
            T cur = list.get(i);
            long keyCur = ke.key(cur);
            int j = i - 1;
            while (j >= 0 && ke.key(list.get(j)) > keyCur) {
                if (j + 1 < list.size()) list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, cur);
        }
    }
}
