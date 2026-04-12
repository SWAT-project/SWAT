package de.uzl.its.swat.instrument.springendpoint;

import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A visitor to visit a Java method This class visits Spring RestEndpoints and makes all primitive
 * Datatype parameters symbolic and marks Class objects for later instrumenting.
 */
public class EndpointMappingMethodAdapter extends AbstractMethodAdapter {

    private boolean isRequestMapping;
    private final boolean isInterfaceMapping;
    private int access;
    private final String cname;

    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param cname The class name
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public EndpointMappingMethodAdapter(
            MethodVisitor mv,
            int access,
            String cname,
            String name,
            String desc,
            boolean isInterfaceMapping) {
        super(mv, name, desc);
        this.isRequestMapping = false;
        this.isInterfaceMapping = isInterfaceMapping;
        this.access = access;
        this.cname = cname;
    }

    @Override
    public void visitCode() {
        // instrument if either the method itself had @GetMapping/etc. or the interface did
        if (isRequestMapping || isInterfaceMapping) {
            handleMethodParameters(access, SpringEndpointTransformer.getPrintBox(), false);
            SpringEndpointTransformer.addInstrumentedClass(cname);
            SpringEndpointTransformer.addInstrumentedEndpoint(cname + ":" + this.getName());
        }
        super.visitCode();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        // catch method-level annotations too
        if (SpringEndpointTransformer.isRestEndpointAnnotation(descriptor)) {
            isRequestMapping = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

}
