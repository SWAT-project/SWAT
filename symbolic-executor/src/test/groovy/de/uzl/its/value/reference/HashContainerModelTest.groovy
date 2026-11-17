package de.uzl.its.value.reference

import de.uzl.its.swat.symbolic.invoke.java.util.CollectionInvocation
import de.uzl.its.swat.symbolic.value.ContainerPlaceHolder
import de.uzl.its.swat.symbolic.value.PlaceHolder
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import de.uzl.its.swat.symbolic.value.reference.ContainerContents
import de.uzl.its.swat.symbolic.value.reference.HashEntries
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.DoubleObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.LongObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Specification

import static java.lang.Thread.currentThread

/** The exact model of hash containers with symbolic keys (CollectionInvocation.hashCall, HashEntries). */
class HashContainerModelTest extends Specification {

    static final String GET = "(Ljava/lang/Object;)Ljava/lang/Object;"
    static final String PUT = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    static final String HAS = "(Ljava/lang/Object;)Z"

    SolverContext context
    int nextAddress = 100

    def setup() {
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
        ThreadHandler.addThreadContext(currentThread().id, "Test-Thread", -2)
        context = ThreadHandler.getSolverContext(currentThread().id)
    }

    private boolean contextLoss() {
        return ThreadHandler.getSymbolicTraceHandler(currentThread().id).symbolicTrace.symbolicContextLoss
    }

    private StringValue string(String concrete, boolean symbolic = false) {
        def s = new StringValue(context, concrete, nextAddress++)
        if (symbolic) s.MAKE_SYMBOLIC()
        return s
    }

    private IntegerObjectValue integer(int concrete, boolean symbolic = false) {
        def v = new IntValue(context, concrete)
        if (symbolic) v.MAKE_SYMBOLIC()
        return new IntegerObjectValue(context, v, nextAddress++)
    }

    private ObjectValue nullValue() {
        return new ObjectValue(context, ObjectValue.ADDRESS_NULL)
    }

    /** A new container of the class, after its (modelled) constructor. */
    private ObjectValue container(String className) {
        def c = new ObjectValue(context, className, new IntValue(context, 0), nextAddress++)
        assert call(c, "<init>", "()V") != null
        return c
    }

    private Value call(ObjectValue instance, String name, String desc, Value... args) {
        return CollectionInvocation.invoke(instance.className, name, desc, args, true, instance, 0L)
    }

    def "only null, Strings and boxed integral values with a known shadow value are keys"() {
        given:
        def unknown = string("x")
        unknown.valueUnknown = true

        expect:
        HashEntries.supported(string("a", true))
        HashEntries.supported(integer(1, true))
        HashEntries.supported(nullValue())
        !HashEntries.supported(unknown)
        !HashEntries.supported(new DoubleObjectValue(context, new DoubleValue(context, 0.0d), 5))
        !HashEntries.supported(new ObjectValue(context, "Foo", new IntValue(context, 0), 6))
    }

    def "a boxed value built from an int-typed constant has the constant's value"() {
        given:
        def character = de.uzl.its.swat.symbolic.value.ValueFactory.createObjectValue(0, "java/lang/Character")
        def bool = de.uzl.its.swat.symbolic.value.ValueFactory.createObjectValue(0, "java/lang/Boolean")
        def small = de.uzl.its.swat.symbolic.value.ValueFactory.createObjectValue(0, "java/lang/Short")
        def tiny = de.uzl.its.swat.symbolic.value.ValueFactory.createObjectValue(0, "java/lang/Byte")

        expect: "the shadow NEW makes has a default value, which is no key"
        character.valueUnknown
        !HashEntries.supported(character)

        when:
        character.invokeMethod("<init>", [org.objectweb.asm.Type.CHAR_TYPE] as org.objectweb.asm.Type[], [new IntValue(context, 97)] as Value[])
        bool.invokeMethod("<init>", [org.objectweb.asm.Type.BOOLEAN_TYPE] as org.objectweb.asm.Type[], [new IntValue(context, 1)] as Value[])
        small.invokeMethod("<init>", [org.objectweb.asm.Type.SHORT_TYPE] as org.objectweb.asm.Type[], [new IntValue(context, -7)] as Value[])
        tiny.invokeMethod("<init>", [org.objectweb.asm.Type.BYTE_TYPE] as org.objectweb.asm.Type[], [new IntValue(context, -3)] as Value[])

        then:
        character.val.concrete == ('a' as char)
        bool.val.concrete == true
        small.val.concrete == (short) -7
        tiny.val.concrete == (byte) -3
    }

    def "keys of different classes are never equal, and null equals only null"() {
        given:
        def five = integer(5, true)
        def longFive = new LongObjectValue(context, new LongValue(context, 5L), nextAddress++)
        def fmgr = context.formulaManager

        expect:
        !HashEntries.equal(five, longFive).holds()
        fmgr.extractVariables(HashEntries.equal(five, longFive).condition()).isEmpty()
        !HashEntries.equal(string("5", true), five).holds()
        !HashEntries.equal(nullValue(), string("null", true)).holds()
        HashEntries.equal(nullValue(), nullValue()).holds()
        HashEntries.equal(five, integer(5)).holds()
        !fmgr.extractVariables(HashEntries.equal(five, integer(5)).condition()).isEmpty()
    }

    def "a lookup with a symbolic key records the chain up to the entry it equals"() {
        given:
        def map = container("java/util/HashMap")
        def v1 = string("one")
        def v2 = string("two")
        call(map, "put", PUT, string("a"), v1)
        call(map, "put", PUT, string("b"), v2)
        call(map, "put", PUT, string("c"), string("three"))

        when:
        def result = call(map, "get", GET, string("b", true))
        def branches = CollectionInvocation.takeBranches()

        then: "it differs from the first key and equals the second; the third is not compared"
        branches*.holds() == [false, true]
        result instanceof ContainerPlaceHolder
        result.exact
        result.expected.is(v2)
        !contextLoss()

        when: "no key is equal"
        result = call(map, "containsKey", HAS, string("z", true))
        branches = CollectionInvocation.takeBranches()

        then:
        branches*.holds() == [false, false, false]
        result.expected == false
        map.contents.hash().size() == 3
    }

    def "a put with an equal symbolic key replaces the value, another one adds an entry"() {
        given:
        def map = container("java/util/HashMap")
        def old = string("old")
        call(map, "put", PUT, string("k"), old)

        when:
        def result = call(map, "put", PUT, string("k", true), string("new"))

        then:
        CollectionInvocation.takeBranches()*.holds() == [true]
        result.expected.is(old)
        map.contents.hash().size() == 1
        !map.contents.hasSymbolicKeys() // the stored key is still the concrete one

        when:
        result = call(map, "put", PUT, string("j", true), string("other"))

        then:
        CollectionInvocation.takeBranches()*.holds() == [false]
        result.expected == null
        map.contents.hash().size() == 2
        map.contents.hasSymbolicKeys()
        call(map, "size", "()I").expected == 2
        !contextLoss()
    }

    def "a key of another class gives the record up: a context loss once a symbolic key is stored"() {
        given:
        def concrete = container("java/util/HashMap")
        def symbolic = container("java/util/HashMap")
        call(concrete, "put", PUT, string("k"), string("v"))
        call(symbolic, "put", PUT, string("k", true), string("v"))
        def other = new ObjectValue(context, "Foo", new IntValue(context, 0), nextAddress++)

        when:
        call(concrete, "put", PUT, other, string("v"))

        then:
        concrete.contents.hash() == null
        !contextLoss()

        when:
        def result = call(symbolic, "get", GET, other)

        then:
        result == null // not modelled
        symbolic.contents.hash() == null
        contextLoss()
        symbolic.contents.keysSymbolic(new HashSet<>()) // and no call by key is modelled any more
    }

    def "a call outside the tables gives the record up"() {
        given:
        def map = container("java/util/HashMap")
        call(map, "put", PUT, string("k", true), string("v"))

        expect:
        call(map, "containsValue", HAS, string("v")) == null
        map.contents.hash() == null
        contextLoss()
    }

    def "a result that differs from the record gives it up"() {
        given:
        def map = container("java/util/HashMap")
        call(map, "put", PUT, string("k", true), string("v"))
        def size = call(map, "size", "()I")

        when:
        CollectionInvocation.checkPrimitive(size, 1)

        then:
        map.contents.hash() != null
        !contextLoss()

        when:
        CollectionInvocation.checkPrimitive(size, 2)

        then:
        map.contents.hash() == null
        contextLoss()
    }

    def "the iteration order of a plain hash container with a symbolic key is unknown, a linked one's is not"() {
        given:
        def plain = container("java/util/HashMap")
        def linked = container("java/util/LinkedHashMap")
        call(plain, "put", PUT, string("b"), string("v"))
        call(linked, "put", PUT, string("b"), string("v"))

        expect:
        !plain.contents.hashOrderUnknown()

        when:
        call(plain, "put", PUT, string("a", true), string("w"))
        call(linked, "put", PUT, string("a", true), string("w"))

        then:
        plain.contents.hashOrderUnknown()
        !linked.contents.hashOrderUnknown()
        !contextLoss() // only iterating is one

        when: "a linked map copied from the plain one gets the entries in hash order"
        def copy = new ObjectValue(context, "java/util/LinkedHashMap", new IntValue(context, 0), nextAddress++)
        call(copy, "<init>", "(Ljava/util/Map;)V", plain)

        then:
        copy.contents.hash().size() == 2
        copy.contents.hashOrderUnknown()
    }

    def "a set built from a list compares every element with the ones before"() {
        given:
        def list = container("java/util/ArrayList")
        def a = string("z", true)
        call(list, "add", HAS, a)
        call(list, "add", HAS, string("z"))
        def set = new ObjectValue(context, "java/util/HashSet", new IntValue(context, 0), nextAddress++)

        when:
        def result = call(set, "<init>", "(Ljava/util/Collection;)V", list)

        then:
        result == PlaceHolder.instance
        CollectionInvocation.takeBranches()*.holds() == [true]
        set.contents.hash().size() == 1
        call(set, "contains", HAS, string("y", true)).expected == false

        when: "a call that is not modelled got the list: its elements are not known exactly"
        call(list, "remove", HAS, string("z"))
        def second = new ObjectValue(context, "java/util/HashSet", new IntValue(context, 0), nextAddress++)

        then:
        call(second, "<init>", "(Ljava/util/Collection;)V", list) == null
        second.contents == null
    }

    def "equals of two lists with exact elements compares them pair by pair"() {
        given:
        def x = container("java/util/ArrayList")
        def y = container("java/util/ArrayList")
        call(x, "add", HAS, string("k"))
        call(x, "add", HAS, string("m", true))
        call(x, "add", HAS, string("n"))
        call(y, "add", HAS, string("k"))
        call(y, "add", HAS, string("z"))

        expect: "different sizes: no comparison"
        call(x, "equals", HAS, y).expected == false
        CollectionInvocation.takeBranches().isEmpty()
        call(x, "equals", HAS, x).expected == true
        call(x, "equals", HAS, nullValue()).expected == false

        when:
        call(y, "add", HAS, string("n"))
        def result = call(x, "equals", HAS, y)

        then: "up to the first pair that differs"
        result.expected == false
        CollectionInvocation.takeBranches()*.holds() == [true, false]
        CollectionInvocation.invoke("java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                [x, y] as Value[], false, null, 0L).expected == false

        when: "a call that is not modelled got the list"
        CollectionInvocation.escaped(x, "java/util/Collections", "reverse", "(Ljava/util/List;)V", 0L)

        then:
        call(x, "equals", HAS, y) == null
    }

    def "addAll of a map's key set compares its keys with the set's elements"() {
        given:
        def map = container("java/util/HashMap")
        call(map, "put", PUT, string("j", true), string("v"))
        call(map, "put", PUT, string("k"), string("w"))
        def keys = new ObjectValue(context, 'java/util/HashMap$KeySet', new IntValue(context, 0), nextAddress++)
        def view = ContainerContents.viewOf(map.contents, false)
        view.setKeySetOf(map.contents)
        keys.setContents(view)
        def set = new ObjectValue(context, "java/util/HashSet", new IntValue(context, 0), nextAddress++)
        call(set, "<init>", "()V")
        call(set, "add", HAS, string("j"))

        when:
        def result = call(set, "addAll", "(Ljava/util/Collection;)Z", keys)

        then: "the symbolic key equals the element, the other key is new (a comparison of constants)"
        result.expected == true
        CollectionInvocation.takeBranches()*.holds() == [true, false]
        set.contents.hash().size() == 2

        when: "a view that is not marked as the key set (values()) is no source"
        def values = new ObjectValue(context, 'java/util/HashMap$Values', new IntValue(context, 0), nextAddress++)
        values.setContents(ContainerContents.viewOf(map.contents, false))

        then:
        call(set, "addAll", "(Ljava/util/Collection;)Z", values) == null
        set.contents.hash() == null
    }

    def "a ConcurrentHashMap and the lookups of an empty immutable map are modelled, a Hashtable only by reference"() {
        given:
        def concurrent = container("java/util/concurrent/ConcurrentHashMap")
        def table = container("java/util/Hashtable")
        def empty = new ObjectValue(context, 'java/util/Collections$EmptyMap', new IntValue(context, 0), nextAddress++)

        expect:
        concurrent.contents.hash() != null
        call(concurrent, "putIfAbsent", PUT, string("k", true), string("v")).exact
        table.contents.hash() == null
        call(table, "get", GET, string("k", true)) == null
        call(empty, "get", GET, string("k", true)) == PlaceHolder.instance
        call(empty, "put", PUT, string("k", true), string("v")) == null
    }
}
