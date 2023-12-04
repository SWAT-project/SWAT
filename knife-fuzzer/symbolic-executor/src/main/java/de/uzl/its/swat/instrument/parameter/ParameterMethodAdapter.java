package de.uzl.its.swat.instrument.parameter;

import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import org.objectweb.asm.MethodVisitor;

/**
 * A visitor to visit a Java method This class makes every parameter of the passed function
 * symbolic.
 */
public class ParameterMethodAdapter extends AbstractMethodAdapter {
    int access;
    String name;
    String desc;
    String signature;
    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public ParameterMethodAdapter(
            MethodVisitor mv, int access, String name, String desc, String signature) {
        super(mv, name, desc);
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
    }

    /**
     * Starts the visit of the method's code, if any (i.e. non-abstract method). Parses the methods
     * description and makes strings and ints symbolic
     */
    @Override
    public void visitCode() {
        handleMethodParameters(access);
    }
}
