package de.uzl.its.swat.symbolic.loopstate

import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import spock.lang.Specification

class LoopHeadsTest extends Specification {

    /** static void m(int n, long l) { int x = 0; while (x < n) { x++; } } */
    private static MethodNode countingLoop(boolean valueOnStack) {
        MethodNode mn = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "m", "(IJ)V", null, null)
        Label head = new Label()
        Label end = new Label()
        mn.visitCode()
        mn.visitInsn(Opcodes.ICONST_0)
        mn.visitVarInsn(Opcodes.ISTORE, 3)
        if (valueOnStack) {
            mn.visitInsn(Opcodes.ICONST_0)
        }
        mn.visitLabel(head)
        mn.visitVarInsn(Opcodes.ILOAD, 3)
        mn.visitVarInsn(Opcodes.ILOAD, 0)
        mn.visitJumpInsn(Opcodes.IF_ICMPGE, end)
        mn.visitIincInsn(3, 1)
        mn.visitJumpInsn(Opcodes.GOTO, head)
        mn.visitLabel(end)
        if (valueOnStack) {
            mn.visitInsn(Opcodes.POP)
        }
        mn.visitInsn(Opcodes.RETURN)
        mn.visitMaxs(3, 4)
        mn.visitEnd()
        return mn
    }

    def "finds the target of a backward jump and the locals defined there"() {
        when:
        def heads = LoopHeads.analyze("Fixture", countingLoop(false))

        then:
        heads.size() == 1
        def head = heads.values().first()
        head.name() == "Fixture.m(IJ)V#0"
        head.slots() as List == [0, 1, 3]
        head.kinds() as List == ['I' as char, 'J' as char, 'I' as char]
        LoopHeads.get(head.name()) == head
    }

    def "skips a head with values on the operand stack"() {
        expect:
        LoopHeads.analyze("Fixture2", countingLoop(true)).isEmpty()
    }

    def "a method without backward jumps has no heads"() {
        given:
        MethodNode mn = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "f", "(I)I", null, null)
        Label skip = new Label()
        mn.visitCode()
        mn.visitVarInsn(Opcodes.ILOAD, 0)
        mn.visitJumpInsn(Opcodes.IFEQ, skip)
        mn.visitInsn(Opcodes.ICONST_1)
        mn.visitInsn(Opcodes.IRETURN)
        mn.visitLabel(skip)
        mn.visitInsn(Opcodes.ICONST_0)
        mn.visitInsn(Opcodes.IRETURN)
        mn.visitMaxs(1, 1)
        mn.visitEnd()

        expect:
        LoopHeads.analyze("Fixture3", mn).isEmpty()
    }
}
