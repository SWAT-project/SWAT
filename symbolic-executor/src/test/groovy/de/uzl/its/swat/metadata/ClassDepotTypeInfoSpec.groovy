package de.uzl.its.swat.metadata

import spock.lang.Specification

/**
 * L0: the registered type metadata of a class is shared, so the lists ClassDepot hands out must
 * not grow when callers merge them (as ValueFactory.createObjectValue does on every NEW).
 */
class ClassDepotTypeInfoSpec extends Specification {

    def "the parent and interface lists of a class cannot be modified through the getters"() {
        given:
        def depot = ClassDepot.getInstrumentationInstance()
        depot.registerTypeInfoForClass("spec/TypeInfoA", ["java/lang/Object"], ["java/lang/Runnable"])

        when:
        depot.getInterfacesForClass("spec/TypeInfoA").addAll(depot.getParentsForClass("spec/TypeInfoA"))

        then:
        thrown(UnsupportedOperationException)
        depot.getInterfacesForClass("spec/TypeInfoA") == ["java/lang/Runnable"]
    }

    def "an unregistered class has empty lists"() {
        expect:
        ClassDepot.getRuntimeInstance().getParentsForClass("spec/TypeInfoUnknown").isEmpty()
        ClassDepot.getRuntimeInstance().getInterfacesForClass("spec/TypeInfoUnknown").isEmpty()
    }
}
