package de.uzl.its.targets.finance.util;

import de.uzl.its.targets.finance.model.Transaction;

import java.util.List;

public final class InsertionSort {
    private InsertionSort() {}

    public static void sortTransactionsByDate(List<Transaction> txs) {
        int n = txs.size();
        for (int i = 1; i < n; i++) {
            Transaction key = txs.get(i);
            int j = i - 1;
            while (j >= 0 && txs.get(j).getDateMs() > key.getDateMs()) {
                txs.set(j + 1, txs.get(j));
                j = j - 1;
            }
            txs.set(j + 1, key);
        }
    }
}
