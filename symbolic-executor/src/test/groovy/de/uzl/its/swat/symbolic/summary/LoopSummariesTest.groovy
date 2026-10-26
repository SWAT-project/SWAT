package de.uzl.its.swat.symbolic.summary

import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import spock.lang.Specification

class LoopSummariesTest extends Specification {

    /**
     * static void m(int n, int v) { int x = 0, s = 0; while (x < n) { <body> } }
     * x is slot 2, s is slot 3; the body ends with the back edge.
     */
    private static MethodNode loop(int exitOpcode, Closure body) {
        MethodNode mn = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "m", "(II)V", null, null)
        Label head = new Label()
        Label end = new Label()
        mn.visitCode()
        mn.visitInsn(Opcodes.ICONST_0)
        mn.visitVarInsn(Opcodes.ISTORE, 2)
        mn.visitInsn(Opcodes.ICONST_0)
        mn.visitVarInsn(Opcodes.ISTORE, 3)
        mn.visitLabel(head)
        mn.visitVarInsn(Opcodes.ILOAD, 2)
        mn.visitVarInsn(Opcodes.ILOAD, 0)
        mn.visitJumpInsn(exitOpcode, end)
        body(mn)
        mn.visitJumpInsn(Opcodes.GOTO, head)
        mn.visitLabel(end)
        mn.visitInsn(Opcodes.RETURN)
        mn.visitMaxs(3, 4)
        mn.visitEnd()
        return mn
    }

    def "a counting loop with a constant step gets closed forms"() {
        when:
        def plans = LoopSummaries.analyze(loop(Opcodes.IF_ICMPGE) { mn ->
            mn.visitIincInsn(2, 1)
            mn.visitIincInsn(3, 3)
        })

        then:
        plans.byJump().size() == 1
        def plan = plans.byJump().get(0) as LoopSummaries.CountingLoop
        plan.i() == 2
        plan.step() == 1
        plan.bound() == 0
        plan.steps() == [2: 1, 3: 3]
        plan.exitOnJump()
    }

    def "x = x + c and x = c in the body are understood"() {
        when:
        def plan = LoopSummaries.analyze(loop(Opcodes.IF_ICMPGE) { mn ->
            mn.visitVarInsn(Opcodes.ILOAD, 3)
            mn.visitIntInsn(Opcodes.BIPUSH, 7)
            mn.visitInsn(Opcodes.IADD)
            mn.visitVarInsn(Opcodes.ISTORE, 3)
            mn.visitInsn(Opcodes.ICONST_1)
            mn.visitVarInsn(Opcodes.ISTORE, 1)
            mn.visitIincInsn(2, 1)
        }).byJump().get(0) as LoopSummaries.CountingLoop

        then:
        plan.steps() == [2: 1, 3: 7]
        plan.constants() == [1: 1]
    }

    def "no closed form for #what"() {
        expect:
        LoopSummaries.analyze(loop(exit, body)).byJump().isEmpty()

        where:
        what                         | exit                 | body
        "a step of two"              | Opcodes.IF_ICMPGE    | { mn -> mn.visitIincInsn(2, 2) }
        "a non-strict bound"         | Opcodes.IF_ICMPGT    | { mn -> mn.visitIincInsn(2, 1) }
        "a body that changes bound"  | Opcodes.IF_ICMPGE    | { mn -> mn.visitIincInsn(2, 1); mn.visitIincInsn(0, 1) }
        "a body with a call"         | Opcodes.IF_ICMPGE    | { mn -> mn.visitIincInsn(2, 1); mn.visitMethodInsn(Opcodes.INVOKESTATIC, "X", "f", "()V", false) }
        "a body with a division"     | Opcodes.IF_ICMPGE    | { mn -> mn.visitIincInsn(2, 1); mn.visitVarInsn(Opcodes.ILOAD, 3); mn.visitInsn(Opcodes.ICONST_2); mn.visitInsn(Opcodes.IDIV); mn.visitVarInsn(Opcodes.ISTORE, 3) }
        "s = s + x (not a constant)" | Opcodes.IF_ICMPGE    | { mn -> mn.visitIincInsn(2, 1); mn.visitVarInsn(Opcodes.ILOAD, 3); mn.visitVarInsn(Opcodes.ILOAD, 2); mn.visitInsn(Opcodes.IADD); mn.visitVarInsn(Opcodes.ISTORE, 3) }
    }

    /** static int f(int i, int v) { int c = 0; <diamond on i < v>; return c; }, c in slot 2. */
    private static MethodNode diamond(Closure sides) {
        MethodNode mn = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "f", "(II)I", null, null)
        mn.visitCode()
        mn.visitInsn(Opcodes.ICONST_0)
        mn.visitVarInsn(Opcodes.ISTORE, 2)
        mn.visitVarInsn(Opcodes.ILOAD, 0)
        mn.visitVarInsn(Opcodes.ILOAD, 1)
        sides(mn)
        mn.visitVarInsn(Opcodes.ILOAD, 2)
        mn.visitInsn(Opcodes.IRETURN)
        mn.visitMaxs(3, 3)
        mn.visitEnd()
        return mn
    }

    def "if (i < v) c++ is a diamond"() {
        given:
        Label join = new Label()

        when:
        def plans = LoopSummaries.analyze(diamond { mn ->
            mn.visitJumpInsn(Opcodes.IF_ICMPGE, join)
            mn.visitIincInsn(2, 1)
            mn.visitLabel(join)
        })

        then:
        def d = plans.byJump().get(0) as LoopSummaries.Diamond
        d.fallThrough() == [new LoopSummaries.Op(Opcodes.IINC, 2, 1)]
        d.jump() == []
        d.stackDelta() == 0
        plans.joins().size() == 1
        plans.joins().values().first() == d.join()
    }

    def "a conditional expression is a diamond that leaves a value"() {
        given:
        Label otherwise = new Label()
        Label join = new Label()

        when:
        def plans = LoopSummaries.analyze(diamond { mn ->
            mn.visitJumpInsn(Opcodes.IF_ICMPGE, otherwise)
            mn.visitVarInsn(Opcodes.ILOAD, 2)
            mn.visitInsn(Opcodes.ICONST_2)
            mn.visitInsn(Opcodes.IADD)
            mn.visitJumpInsn(Opcodes.GOTO, join)
            mn.visitLabel(otherwise)
            mn.visitVarInsn(Opcodes.ILOAD, 2)
            mn.visitLabel(join)
            mn.visitVarInsn(Opcodes.ISTORE, 2)
        })

        then:
        def d = plans.byJump().get(0) as LoopSummaries.Diamond
        d.stackDelta() == 1
        d.jump() == [new LoopSummaries.Op(Opcodes.ILOAD, 2, 0)]
    }

    def "a side with a call is no diamond"() {
        given:
        Label join = new Label()

        expect:
        LoopSummaries.analyze(diamond { mn ->
            mn.visitJumpInsn(Opcodes.IF_ICMPGE, join)
            mn.visitMethodInsn(Opcodes.INVOKESTATIC, "X", "f", "()V", false)
            mn.visitIincInsn(2, 1)
            mn.visitLabel(join)
        }).byJump().isEmpty()
    }

    def "a side that something else jumps into is no diamond"() {
        given:
        Label join = new Label()
        Label middle = new Label()

        expect:
        LoopSummaries.analyze(diamond { mn ->
            mn.visitJumpInsn(Opcodes.IF_ICMPGE, join)
            mn.visitIincInsn(2, 1)
            mn.visitLabel(middle)
            mn.visitIincInsn(2, 1)
            mn.visitLabel(join)
            mn.visitVarInsn(Opcodes.ILOAD, 0)
            mn.visitJumpInsn(Opcodes.IFNE, middle)
        }).byJump().keySet() == [] as Set
    }
}
