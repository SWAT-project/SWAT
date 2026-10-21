package de.uzl.its.swat.common

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.CheckClassAdapter
import spock.lang.Specification

/**
 * The transformers write instrumented classes through ASM's CheckClassAdapter only if
 * {@code instrumentation.checkClassAdapter} is set (for the class).
 */
class CheckClassAdapterSwitchSpec extends Specification {

    def "the writer is wrapped only when the check is on for the class"() {
        given:
        def cw = new ClassWriter(0)

        expect:
        (Util.withCheckClassAdapter(cw, "a/B", enabled, filter) instanceof CheckClassAdapter) == wrapped
        Util.withCheckClassAdapter(cw, "a/B", enabled, filter).is(cw) == !wrapped

        where:
        enabled | filter  || wrapped
        false   | "*"     || false
        false   | "a/B"   || false
        true    | "*"     || true
        true    | "a/B"   || true
        true    | "a/C"   || false
    }

    def "with the check on, an invalid class is rejected while it is written"() {
        given:
        ClassVisitor cv = Util.withCheckClassAdapter(new ClassWriter(0), "a/B", true, "*")
        cv.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "a/B", null, "java/lang/Object", null)
        def mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "m", "()V", null, null)
        mv.visitCode()

        when: "an instruction with an invalid opcode for visitInsn"
        mv.visitInsn(Opcodes.ILOAD)

        then:
        thrown(IllegalArgumentException)
    }

    def "with the check off, the same visit is passed on unchecked"() {
        given:
        ClassVisitor cv = Util.withCheckClassAdapter(new ClassWriter(0), "a/B", false, "*")
        cv.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "a/B", null, "java/lang/Object", null)
        def mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "m", "()V", null, null)
        mv.visitCode()

        when:
        mv.visitInsn(Opcodes.ILOAD)

        then:
        noExceptionThrown()
    }
}
