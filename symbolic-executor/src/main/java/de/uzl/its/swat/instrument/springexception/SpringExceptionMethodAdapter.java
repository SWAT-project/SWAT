package de.uzl.its.swat.instrument.springexception;

import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import de.uzl.its.swat.instrument.Intrinsics;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A visitor to visit a Java method This class visits Spring RestEndpoints and makes all primitive
 * Datatype parameters symbolic and marks Class objects for later instrumenting.
 */
public class SpringExceptionMethodAdapter extends AbstractMethodAdapter implements Opcodes {

    private boolean isExceptionHandler;
    private boolean isRequestMapping;
    private final String methodName;
    private final String cname;

    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public SpringExceptionMethodAdapter(
            MethodVisitor mv, int access, String name, String desc, String cname) {
        super(mv, name, desc);
        this.isExceptionHandler = false;
        this.cname = cname;
        this.methodName = name;
    }

    @Override
    public void visitInsn(int opcode) {
        // if (cname.equals("org/springframework/boot/web/servlet/support/ErrorPageFilter")) {
        //    System.out.println("@@@@@@@@@@@@@ " + cname + ":" + methodName);
        // }
        if (isExceptionHandler
                || isErrorPageFilterForwardToErrorPage()
                || isRequestMappingInErrorController()
                || isAuthenticationFailureHandlerOnAuthenticationFailure()) {
            if (opcode == IRETURN
                    || opcode == LRETURN
                    || opcode == FRETURN
                    || opcode == DRETURN
                    || opcode == ARETURN
                    || opcode == RETURN) {
                SpringExceptionTransformer.getPrintBox()
                        .addMsg(
                                "("
                                        + this.cname
                                        + "."
                                        + this.methodName
                                        + ")"
                                        + " => Adding termination: Intrinsics.terminate()");
                visitMethodInsn(
                        Opcodes.INVOKESTATIC, Type.getInternalName(Intrinsics.class), "terminate", "()V", false);
                mv.visitInsn(opcode);
            } else {
                mv.visitInsn(opcode);
            }
        } else {
            mv.visitInsn(opcode);
        }
    }

    // ToDo: This is an oversimplification. An application might create it's own
    // AuthenticationFailureHandler and implement
    //  error handling methods. This however requires tracing interface back up the inheritance
    // structure ...
    private boolean isAuthenticationFailureHandlerOnAuthenticationFailure() {
        return this.cname.startsWith("org/springframework/security/web/authentication")
                && this.methodName.equals("onAuthenticationFailure");
    }

    // ToDo: This is an oversimplification. An application might create it's own ErrorController and
    // implement
    //  error handling methods. This however requires tracing interface back up the inheritance
    // structure ...
    // ToDo: There seems to be a standard defining at least a bit about default error pages for
    // servlets
    //  look into it and see whether this can be handled framework agnositc
    private boolean isRequestMappingInErrorController() {
        return isRequestMapping
                && cname.equals(
                        "org/springframework/boot/autoconfigure/web/servlet/error/BasicErrorController");
    }

    private boolean isErrorPageFilterForwardToErrorPage() {
        return this.cname.equals("org/springframework/boot/web/servlet/support/ErrorPageFilter")
                && this.methodName.equals("forwardToErrorPage");
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
        // if (descriptor.contains("Exception")) {
        //     System.out.println("YYYYUUUU" + descriptor);
        // }
        if (isExceptionHandler(descriptor)) {
            isExceptionHandler = true;
        }
        if (isRequestMapping(descriptor)) {
            isRequestMapping = true;
        }

        return super.visitAnnotation(descriptor, visible);
    }

    private boolean isRequestMapping(String annotation) {
        String[] requestMappingAnnotations = {
            "Lorg/springframework/web/bind/annotation/RequestMapping;"
        };
        for (String requestMappingAnnotation : requestMappingAnnotations) {
            if (annotation.equals(requestMappingAnnotation)) {
                return true;
            }
        }
        return false;
    }

    public boolean isExceptionHandler(String annotation) {
        String[] exceptionAnnotations = {
            "Lorg/springframework/web/bind/annotation/ExceptionHandler;"
        };
        for (String endpointAnnotation : exceptionAnnotations) {
            if (annotation.equals(endpointAnnotation)) {
                return true;
            }
        }
        return false;
    }
}
