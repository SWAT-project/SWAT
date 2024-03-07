package de.uzl.its.swat.symbolic.value.reference;

import de.uzl.its.swat.symbolic.value.Value;

public class LambdaPlaceHolder extends Value {
    public int getKey() {
        return key;
    }

    private final int key;

    public int getParentAddress() {
        return parentAddress;
    }

    // parentAddress is -1 if the lambda function is static
    private final int parentAddress;

    public LambdaPlaceHolder(int key, int parentAddress) {
        this.key = key;
        this.parentAddress = parentAddress;
    }

    @Override
    public String toString() {
        return "LambdaPlaceHolder (key: "
                + key
                + ", parentAddress: "
                + Integer.toHexString(parentAddress)
                + ")";
    }
}
