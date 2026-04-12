package de.uzl.its.swat.instrument.instruction;

import java.lang.reflect.Method;

public class MethodInvokeInformationHolder {
    public String methodName;
    public String methodDescriptor;
    public String owner;

    public Method methodObject;
    public Object ownerObject;
    public Object[] argObjects;

    public MethodInvokeInformationHolder(
            String methodName,
            String methodDescriptor,
            String owner,
            Method methodObject,
            Object ownerObject,
            Object[] argObjects) {
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.owner = owner;
        this.methodObject = methodObject;
        this.ownerObject = ownerObject;
        this.argObjects = argObjects;
    }
}
