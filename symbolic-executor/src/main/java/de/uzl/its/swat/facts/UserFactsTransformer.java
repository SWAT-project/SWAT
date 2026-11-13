package de.uzl.its.swat.facts;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.instrument.SafeClassWriter;
import java.lang.instrument.ClassFileTransformer;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The rewrite for hand-written facts (see {@link UserFacts}), one of SWAT's rewrites
 * (instrument/rewrite/Rewrites, only with {@code userFacts} on), so that the static analysis sees
 * its result in the prepared classes:
 *
 * <ul>
 *   <li>a method annotated {@code @Target} starts with {@code UserFacts.target("@Target reached:
 *       ...")}, which throws an AssertionError: a violation for the executor. The static analysis
 *       reads the annotation itself (cfg-extraction's UserFacts), so the method keeps its code
 *       for it, and a graph of unprepared classes has the target too;
 *   <li>{@code Swat.summary()} becomes {@code UserFacts.summary("<id>")}, which returns true.
 * </ul>
 *
 * Classes that do not mention the annotations module are left alone.
 */
public class UserFactsTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> classBeingRedefined, ProtectionDomain d,
                            byte[] cbuf) {
        if (classBeingRedefined != null || cname == null || !Util.shouldInstrument(cname)
                || !new String(cbuf, StandardCharsets.ISO_8859_1).contains(UserFacts.PACKAGE)) {
            return null;
        }
        try {
            ClassReader cr = new ClassReader(cbuf);
            ClassWriter cw = new SafeClassWriter(cr, loader, ClassWriter.COMPUTE_FRAMES);
            cr.accept(new FactsClassAdapter(cw, cname), ClassReader.SKIP_FRAMES);
            return cw.toByteArray();
        } catch (RuntimeException e) {
            new ErrorHandler().handleException("[USER FACTS] Error while rewriting class: " + cname, e);
            return null;
        }
    }

    private static final class FactsClassAdapter extends ClassVisitor {
        private final String cname;

        FactsClassAdapter(ClassVisitor cv, String cname) {
            super(Opcodes.ASM9, cv);
            this.cname = cname;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
            MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);
            String method = cname + "." + name + desc;
            boolean canThrowFirst = !name.equals("<init>") && !name.equals("<clinit>");
            return new MethodVisitor(Opcodes.ASM9, mv) {
                private String target; // the @Target's message, null if none
                private int line = -1;
                private int summaries = 0;

                @Override
                public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                    AnnotationVisitor av = super.visitAnnotation(adesc, visible);
                    if (!UserFacts.TARGET.equals(adesc) || !canThrowFirst) {
                        return av;
                    }
                    target = "@Target reached: " + method;
                    return new AnnotationVisitor(Opcodes.ASM9, av) {
                        @Override
                        public void visit(String n, Object value) {
                            if ("value".equals(n) && value instanceof String s && !s.isEmpty()) {
                                target = "@Target reached: " + s + " (" + method + ")";
                            }
                            super.visit(n, value);
                        }
                    };
                }

                @Override
                public void visitCode() {
                    super.visitCode();
                    if (target != null) {
                        super.visitLdcInsn(target);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/".concat("swat/facts/UserFacts"),
                                "target", "(Ljava/lang/String;)V", false);
                    }
                }

                @Override
                public void visitLineNumber(int l, Label start) {
                    line = l;
                    super.visitLineNumber(l, start);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String mname, String mdesc, boolean itf) {
                    if (opcode == Opcodes.INVOKESTATIC && owner.equals(UserFacts.SWAT) && mname.equals("summary")
                            && mdesc.equals("()Z")) {
                        super.visitLdcInsn("Swat.summary " + method + " line " + line + " #" + summaries++);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/".concat("swat/facts/UserFacts"),
                                "summary", "(Ljava/lang/String;)Z", false);
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, mname, mdesc, itf);
                }
            };
        }
    }
}
