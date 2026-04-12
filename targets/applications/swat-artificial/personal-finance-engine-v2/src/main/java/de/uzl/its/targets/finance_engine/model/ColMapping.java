package de.uzl.its.targets.finance_engine.model;

public class ColMapping {
    public int index;   // e.g. 0, 1, 2, ...
    public String field; // e.g. "date", "merchant", "amount", "currency"

    public ColMapping() {}

    public ColMapping(int index, String field) {
        this.index = index;
        this.field = field;
    }
}
