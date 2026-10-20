package de.uzl.its.swat.instrument.symbolicwrapper;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.springendpoint.SpringEndpointTransformer;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * visit [ visitSource ] [ visitModule ][ visitNestHost ][ visitOuterClass ] ( visitAnnotation |
 * visitTypeAnnotation | visitAttribute )* ( visitNestMember | [ * visitPermittedSubclass ] |
 * visitInnerClass | visitRecordComponent | visitField | visitMethod )* visitEnd.
 */
public class SymbolicWrapperClassAdapter extends ClassVisitor {

    static final String ENTRY_METHOD_NAME = "main";
    static final String ENTRY_METHOD_DESC = "([Ljava/lang/String;)V";

    private String cname;
    private final Config config = Config.instance();
    private final boolean declaresEntryMethod;

    /**
     * Constructor that calls the super from the default ClassVisitor
     *
     * @param cv Parent ClassVisitor
     * @param declaresEntryMethod whether the class declares a static main(String[]) method
     */
    public SymbolicWrapperClassAdapter(ClassVisitor cv, String cname, boolean declaresEntryMethod) {
        super(Opcodes.ASM9, cv);
        this.cname = cname;
        this.declaresEntryMethod = declaresEntryMethod;
    }

    /**
     * Checks whether a class declares a static main(String[]) method.
     *
     * @param cr The reader of the class
     * @return true if the class declares the method
     */
    public static boolean declaresEntryMethod(ClassReader cr) {
        boolean[] found = {false};
        cr.accept(
                new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access, String name, String desc, String signature, String[] exceptions) {
                        if ((access & Opcodes.ACC_STATIC) != 0
                                && name.equals(ENTRY_METHOD_NAME)
                                && desc.equals(ENTRY_METHOD_DESC)) {
                            found[0] = true;
                        }
                        return null;
                    }
                },
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found[0];
    }

    /**
     * Visits a method of the class. This method must return a new MethodVisitor instance (or null)
     * each time it is called, i.e., it should not return a previously returned visitor. Addition:
     * Add a custom method visitor that adds the beginning and end for the concolic engine.
     *
     * @param access the method's access flags (see Opcodes). This parameter also indicates if the
     *     method is synthetic and/or deprecated.
     * @param name the method's name.
     * @param desc the method's descriptor (see Type).
     * @param signature the method's signature. May be null if the method parameters, return type
     *     and exceptions do not use generic types.
     * @param exceptions the internal names of the method's exception classes (see
     *     Type.getInternalName()). May be null.
     * @return an object to visit the byte code of the method, or null if this class visitor is not
     *     interested in visiting the code of this method.
     */
    @Override
    public MethodVisitor visitMethod(
            int access, String name, String desc, String signature, String[] exceptions) {

        // Generate default MethodVisitor
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);


        if(Util.ignoreMethod(name)) {
            // Avoid Jacoco
            return mv;
        }

        if (mv != null) {
            switch (config.getInstrumentationTransformer()) {
                case SV_COMP -> {
                    if (name.equals("<clinit>") && declaresEntryMethod) {
                        SymbolicWrapperTransformer.getPrintBox().addMsg("Method: " + name);
                        return new StaticInitializerEntryMethodAdapter(
                                mv, cname + "/" + ENTRY_METHOD_NAME + ENTRY_METHOD_DESC);
                    }
                    if (!name.equals("main")) {
                        return mv;
                    }
                }
                case SPRING_ENDPOINT -> {
                    if (!SpringEndpointTransformer.getInstrumentedEndpoints()
                            .contains(cname + ":" + name)) {
                        return mv;
                    }
                }

                case WEB_SERVLET -> {
                    /*
                    if (!name.equals("doPost")) {
                        return mv;
                    }
                     */
                    new ErrorHandler()
                            .handleException(
                                    new RuntimeException(
                                            "Servlet Endpoints are not supported currently."));
                }

                case ANNOTATION -> {
                    String pattern =
                            config.getInstrumentationAnnotationSymbolicMethodName() == null ?
                                    "main" :
                                    config.getInstrumentationAnnotationSymbolicMethodName();
                    if (!Pattern.matches(pattern, name)) {
                        return mv;
                    }
                }
                case PARAMETER -> {
                    if (!Util.isSymbolicMethod(cname, name)) {
                        return mv;
                    }
                }
                case NONE -> {
                    return mv;
                }
            }

            SymbolicWrapperTransformer.getPrintBox().addMsg("Method: " + name);

            mv =
                    new SymbolicWrapperMethodAdapter(
                            new SurroundingTryCatchMethodAdapter(
                                    new TryCatchBlockSorter(
                                            mv, access, name, desc, signature, exceptions),
                                    cname,
                                    name,
                                    desc),
                            cname,
                            name,
                            desc);
            return mv;
        }
        return null;
    }
}
