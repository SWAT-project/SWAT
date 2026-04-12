package de.uzl.its.swat.symbolic.value.reference;

import org.sosy_lab.java_smt.api.SolverContext;

public class LambdaObjectValue extends ObjectValue {
    public long getKey() {
        return key;
    }

    public int getParentAddress() {
        return parentAddress;
    }

    private final int parentAddress;
    private final long key;

    public LambdaObjectValue(SolverContext context, int address, int parentAddress, long key) {
        super(context, address);
        this.parentAddress = parentAddress;
        this.key = key;
    }

    @Override
    public String toString() {
        return "LambdaObjectValue @"
                + Integer.toHexString(address)
                + " (key: "
                + key
                + ", parentAddress: "
                + Integer.toHexString(parentAddress)
                + ")";
    }
}
