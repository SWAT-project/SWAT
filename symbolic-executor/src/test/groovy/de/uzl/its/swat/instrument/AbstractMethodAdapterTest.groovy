package de.uzl.its.swat.instrument

import org.objectweb.asm.MethodVisitor
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Mockito.mock

import org.objectweb.asm.Opcodes

class AbstractMethodAdapterTest extends Specification {


    MethodVisitor mv = Mock(MethodVisitor)

    def "splitParameters should return correct parameter types"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")

        when:
        def result = adapter.splitParameters("(IZLjava/lang/String;)[I")

        then:
        result == ["I", "Z", "Ljava/lang/String;"]
    }

    def "splitParameters should return empty list when no parameters are provided"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")
        when:
        def result = adapter.splitParameters("()V")

        then:
        result.isEmpty()
    }

    def "splitParameters should correctly identify single primitive type"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")
        when:
        def result = adapter.splitParameters("(I)V")

        then:
        result == ['I']
    }

    def "splitParameters should correctly identify single object type"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")
        when:
        def result = adapter.splitParameters("(Ljava/lang/String;)V")

        then:
        result == ['Ljava/lang/String;']
    }

    def "splitParameters should correctly identify array types"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")
        when:
        def result = adapter.splitParameters("(I[Ljava/lang/String;[[Z)V")

        then:
        result == ['I', '[Ljava/lang/String;', '[[Z']
    }

    def "splitParameters should correctly identify mixed types"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")
        when:
        def result = adapter.splitParameters("(IZLjava/lang/Object;[I)V")

        then:
        result == ['I', 'Z', 'Ljava/lang/Object;', '[I']
    }

    def "splitParameters should correctly identify parameters regardless of return value"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")
        when:
        def result = adapter.splitParameters("(IZLjava/lang/Object;[I)[[Ljava/lang/Object;")

        then:
        result == ['I', 'Z', 'Ljava/lang/Object;', '[I']
    }

    def "splitParameters should return empty list when descriptor does not match pattern"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(IZLjava/lang/String;)[I")
        when:
        def result = adapter.splitParameters("IV")

        then:
        result.isEmpty()
    }

    def "symbolicInt should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicInt(1)

        then:
        1 * mv.visitVarInsn(Opcodes.ILOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(I)I", false)
        1 * mv.visitVarInsn(Opcodes.ISTORE, 1)
    }
    def "symbolicByte should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicByte(1)

        then:
        1 * mv.visitVarInsn(Opcodes.ILOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(B)B", false)
        1 * mv.visitVarInsn(Opcodes.ISTORE, 1)
    }
    def "symbolicShort should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicShort(1)

        then:
        1 * mv.visitVarInsn(Opcodes.ILOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(S)S", false)
        1 * mv.visitVarInsn(Opcodes.ISTORE, 1)
    }
    def "symbolicLong should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicLong(1)

        then:
        1 * mv.visitVarInsn(Opcodes.LLOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(J)J", false)
        1 * mv.visitVarInsn(Opcodes.LSTORE, 1)
    }

    def "symbolicFloat should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicFloat(1)

        then:
        1 * mv.visitVarInsn(Opcodes.FLOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(F)F", false)
        1 * mv.visitVarInsn(Opcodes.FSTORE, 1)
    }
    def "symbolicDouble should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicDouble(1)

        then:
        1 * mv.visitVarInsn(Opcodes.DLOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(D)D", false)
        1 * mv.visitVarInsn(Opcodes.DSTORE, 1)
    }
    def "symbolicBoolean should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicBoolean(1)

        then:
        1 * mv.visitVarInsn(Opcodes.ILOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(Z)Z", false)
        1 * mv.visitVarInsn(Opcodes.ISTORE, 1)
    }

    def "symbolicChar should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicChar(1)

        then:
        1 * mv.visitVarInsn(Opcodes.ILOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(C)C", false)
        1 * mv.visitVarInsn(Opcodes.ISTORE, 1)
    }

    def "symbolicString should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicString(1)

        then:
        1 * mv.visitVarInsn(Opcodes.ALOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(Ljava/lang/String;)Ljava/lang/String;", false)
        1 * mv.visitVarInsn(Opcodes.ASTORE, 1)
    }
    def "symbolicLongObject should invoke the correct sequence of visit methods"() {
        given:
        AbstractMethodAdapter adapter = new ConcreteMethodAdapter(mv, "someMethod", "(I)V")

        when:
        adapter.symbolicLongObject(1)

        then:
        1 * mv.visitVarInsn(Opcodes.ALOAD, 1)
        1 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "MakeSymbolic", "(Ljava/lang/Long;)Ljava/lang/Long;", false)
        1 * mv.visitVarInsn(Opcodes.ASTORE, 1)
    }

}

class ConcreteMethodAdapter extends AbstractMethodAdapter {
    ConcreteMethodAdapter(MethodVisitor mv, String name, String desc) {
        super(mv, name, desc)
    }
}
