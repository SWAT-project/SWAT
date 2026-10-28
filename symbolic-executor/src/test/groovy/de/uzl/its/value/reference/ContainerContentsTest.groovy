package de.uzl.its.value.reference

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.ContainerContents
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Specification

import static java.lang.Thread.currentThread

class ContainerContentsTest extends Specification {

    SolverContext context

    def setup() {
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
        ThreadHandler.addThreadContext(currentThread().id, "Test-Thread", -2)
        context = ThreadHandler.getSolverContext(currentThread().id)
    }

    private StringValue symbolicString(int address) {
        def s = new StringValue(context, "abc", address)
        s.MAKE_SYMBOLIC()
        return s
    }

    private ObjectValue container(String className) {
        return new ObjectValue(context, className, new IntValue(context, 0), 42)
    }

    def "an object whose field holds a symbolic string is symbolic"() {
        given:
        def o = new ObjectValue(context, "Foo", new IntValue(context, 1), 7)
        o.setField(0, symbolicString(8))

        expect:
        o.isSymbolic()
    }

    def "a container holding a symbolic value is symbolic, also through a view of it"() {
        given:
        def list = container("java/util/ArrayList")
        list.setContents(new ContainerContents(ContainerContents.Kind.LIST))
        list.getContents().addValue(new StringValue(context, "c", 9))
        def view = container("java/util/ArrayList\$Itr")
        view.setContents(ContainerContents.viewOf(list.getContents(), false))

        expect:
        !list.isSymbolic()
        !view.isSymbolic()

        when:
        list.getContents().addValue(symbolicString(10))

        then:
        list.isSymbolic()
        view.isSymbolic()
    }

    def "a stored value is found by address, also through a view, unless addresses collide"() {
        given:
        def contents = new ContainerContents(ContainerContents.Kind.LIST)
        def s = symbolicString(11)
        contents.addValue(s)
        def view = ContainerContents.viewOf(contents, false)

        expect:
        contents.lookup(11).is(s)
        view.lookup(11).is(s)
        contents.lookup(12) == null

        when:
        contents.addValue(new StringValue(context, "abc", 11))

        then:
        contents.lookup(11) == null
    }

    def "a value stored before its address was known is found once it is"() {
        given:
        def contents = new ContainerContents(ContainerContents.Kind.LIST)
        def o = new ObjectValue(context, "Foo", new IntValue(context, 0))
        contents.addValue(o)

        when:
        o.setAddress(13)

        then:
        contents.lookup(13).is(o)
    }

    def "a map key that becomes symbolic is reported"() {
        given:
        def contents = new ContainerContents(ContainerContents.Kind.MAP)
        def key = new ObjectValue(context, "Foo", new IntValue(context, 1), 14)
        contents.addKey(key)

        expect:
        !contents.keysSymbolic(new HashSet<>())

        when:
        key.setField(0, symbolicString(15))

        then:
        contents.keysSymbolic(new HashSet<>())
    }
}
