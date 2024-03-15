package de.uzl.its.swat.instrument.springendpoint;

import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A visitor to visit a Java method This class visits Spring RestEndpoints and makes all primitive
 * Datatype parameters symbolic and marks Class objects for later instrumenting.
 */
public class EndpointMappingMethodAdapter extends AbstractMethodAdapter {

    protected boolean isRequestMapping;
    int access;
    private final String cname;

    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param cname The class name
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public EndpointMappingMethodAdapter(MethodVisitor mv, int access, String cname, String name, String desc) {
        super(mv, name, desc);
        this.isRequestMapping = false;
        this.access = access;
        this.cname = cname;
    }

    /**
     * Starts the visit of the method's code, if any (i.e. non abstract method). Parses the methods
     * description and makes strings and ints symbolic if it is an Endpoint
     */
    @Override
    public void visitCode() {
        if (isRequestMapping) {
            handleMethodParameters(access, SpringEndpointTransformer.getPrintBox());
            SpringEndpointTransformer.addInstrumentedClass(cname);
            SpringEndpointTransformer.addInstrumentedEndpoint(cname + ":" + this.getName());
        }
    }
    /**
     * Visits annotation of this method. Checks if it is a REST endpoint
     *
     * @param descriptor the class descriptor of the annotation class.
     * @param visible true if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or null if this visitor is not interested
     *     in visiting this annotation.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {

        if (isRestEndpoint(descriptor)) {
            isRequestMapping = true;
        }

        return super.visitAnnotation(descriptor, visible);
    }

    public boolean isRestEndpoint(String annotation) {
        String[] endpointAnnotations = {
            "Lorg/springframework/web/bind/annotation/RequestMapping;",
            "Lorg/springframework/web/bind/annotation/PostMapping;",
            "Lorg/springframework/web/bind/annotation/DeleteMapping;",
            "Lorg/springframework/web/bind/annotation/GetMapping;",
            "Lorg/springframework/web/bind/annotation/PutMapping;"
        };
        for (String endpointAnnotation : endpointAnnotations) {
            if (annotation.equals(endpointAnnotation)) {
                return true;
            }
        }
        return false;
    }
}
