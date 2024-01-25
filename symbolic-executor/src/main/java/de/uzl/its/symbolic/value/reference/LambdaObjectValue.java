package de.uzl.its.symbolic.value.reference;

import org.sosy_lab.java_smt.api.SolverContext;

public class LambdaObjectValue extends ObjectValue {
    public int getKey() {
        return key;
    }

    public int getParentAddress() {
        return parentAddress;
    }

    private final int parentAddress;
    private final int key;

    public LambdaObjectValue(SolverContext context, int address, int parentAddress, int key) {
        super(context, -1, address);
        this.parentAddress = parentAddress;
        this.key = key;
    }

    @Override
    public String toString() {
        return "LambdaObjectValue @"
                + Integer.toHexString(address)
                + ", key: "
                + key
                + ", parentAddress: "
                + Integer.toHexString(parentAddress);
    }
}
