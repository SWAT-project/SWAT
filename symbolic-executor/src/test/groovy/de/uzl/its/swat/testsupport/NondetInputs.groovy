package de.uzl.its.swat.testsupport

import de.uzl.its.swat.instrument.svcomp.NondetIds
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.nio.file.Files
import java.nio.file.Path

/**
 * The input ids the SV-COMP rewrite gives the nondet calls of compiled test targets
 * ({@link NondetIds}), so that agent tests can pass {@code -Dswat.input.I_<id>=...}.
 */
class NondetInputs {

    /**
     * The ids of the nondet calls (except {@code nondetObject}) of the given classes: class by
     * class in the order given, methods in declaration order, calls in bytecode order.
     *
     * @param classes the directory holding the class files
     * @param classNames internal names, e.g. {@code "Main"} or {@code "objects/BoxFactory"}
     */
    static List<Long> ids(Path classes, String... classNames) {
        List<Long> ids = []
        for (String cname : classNames) {
            new ClassReader(Files.readAllBytes(classes.resolve(cname + ".class"))).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                    int ordinal = 0
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        void visitMethodInsn(int op, String owner, String mname, String mdesc, boolean itf) {
                            if (owner == "org/sosy_lab/sv_benchmarks/Verifier" && mname.startsWith("nondet")
                                    && mname != "nondetObject") {
                                ids << NondetIds.of(cname, name, desc, ordinal++)
                            }
                        }
                    }
                }
            }, ClassReader.SKIP_FRAMES)
        }
        return ids
    }
}
