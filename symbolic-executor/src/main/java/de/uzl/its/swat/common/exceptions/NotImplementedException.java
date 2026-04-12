package de.uzl.its.swat.common.exceptions;

public class NotImplementedException extends Exception{

    public NotImplementedException(Class<?> cls) {
        super("Not implemented: " + currentMethod() + " in " + cls.getName());
    }
    public NotImplementedException(String method, Class<?> cls) {
        super("Not implemented: " + method + " in " + cls.getName());
    }

    public NotImplementedException(String msg) {
        super(msg  + " not implemented in  " + currentMethod());
    }

    private static String currentMethod() {
        return StackWalker.getInstance()
                .walk(frames -> frames
                        .skip(1)          // skip the ctor
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("<unknown>"));
    }

}
