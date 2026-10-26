package de.uzl.its.swat.instrument.instruction;

import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.metadata.ClassDepot;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.tree.MethodNode;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.symbolic.loopstate.LoopHeads;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * visit [ visitSource ] [ visitModule ][ visitNestHost ][ visitOuterClass ] ( visitAnnotation |
 * visitTypeAnnotation | visitAttribute )* ( visitNestMember | [ * visitPermittedSubclass ] |
 * visitInnerClass | visitRecordComponent | visitField | visitMethod )* visitEnd.
 */
public class InstructionClassAdapter extends ClassVisitor {
    private final String cname;
    /** Whether the class has a static initializer (see visitEnd). */
    private boolean hasClinit = false;
    /**
     * Constructor that calls the super from the default ClassVisitor
     *
     * @param cv Parent ClassVisitor
     */
    public InstructionClassAdapter(ClassVisitor cv, String cname) {
        super(Opcodes.ASM9, cv);
        this.cname = cname;
    }

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {
        long cIdx = ClassDepot.getInstrumentationInstance().getClassIndex(name);
        GlobalStateForInstrumentation.instance.setCid(cIdx);

        super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * Visits a method of the class. This method must return a new MethodVisitor instance (or null)
     * each time it is called, i.e., it should not return a previously returned visitor. Addition:
     * Add a custom method visitor that instruments each instruction
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
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("<clinit>")) {
            hasClinit = true;
        }
        if (cname.equals("de/uzl/its/swat/instrument/svcomp/Verifier") && !name.equals("assume")) {
            return mv;
        }

        if(Util.ignoreMethod(name)) {
            // Avoid Jacoco
            return mv;
        }

        if (mv != null) {
            InstructionMethodAdapter ima =
                    new InstructionMethodAdapter(
                            new TryCatchBlockSorter(
                                    new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions)
                                    , access, name, desc, signature, exceptions),
                            name.equals("<init>"),
                            (access & Opcodes.ACC_STATIC) != 0,
                            GlobalStateForInstrumentation.instance,
                            name,
                            desc,
                            cname);
            boolean loopHeads = Config.instance().isLoopStateMatching();
            boolean summaries = Config.instance().isLoopSummaries();
            if ((loopHeads || summaries) && !cname.startsWith("de/uzl/its/swat/")) {
                // Collect the method to find its loop heads and loop summaries, then hand it on
                return new MethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions) {
                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        if (loopHeads) {
                            ima.setLoopHeads(LoopHeads.analyze(cname, this));
                        }
                        if (summaries) {
                            ima.setLoopSummaries(de.uzl.its.swat.symbolic.summary.LoopSummaries.analyze(this));
                        }
                        accept(ima);
                    }
                };
            }
            return ima;
        }

        return null;
    }

    /**
     * With loop-state matching, gives a class without a static initializer one that records its
     * initialization (see ClassInits), as the existing ones do.
     */
    @Override
    public void visitEnd() {
        if (Config.instance().isLoopStateMatching() && !hasClinit && !cname.startsWith("de/uzl/its/swat/")) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            if (mv != null) {
                mv.visitCode();
                LoopHeads.emitClassInit(mv, cname);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(1, 0);
                mv.visitEnd();
            }
        }
        super.visitEnd();
    }
}
