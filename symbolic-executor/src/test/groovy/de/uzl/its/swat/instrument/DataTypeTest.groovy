package de.uzl.its.swat.instrument

import spock.lang.Specification

class DataTypeTest extends Specification {

    def "Should return the correct DataType for given identifier"() {
        expect:
        DataType.getDataType(identifier) == expectedType

        where:
        identifier                  || expectedType
        'I'                         || DataType.INTEGER_TYPE
        'Z'                         || DataType.BOOLEAN_TYPE
        'Ljava/lang/String;'        || DataType.STRING_TYPE
        'Lunknown/Type;'            || DataType.OBJECT_TYPE
        '['                         || DataType.ARRAY_TYPE
        '('                         || DataType.METHOD_TYPE
        'UNKNOWN'                   || DataType.UNKNOWN_TYPE
        '(ILjava/lang/String;)Z'    || DataType.METHOD_TYPE
        '[I'                        || DataType.ARRAY_TYPE
        '[Ljava/lang/String;'       || DataType.ARRAY_TYPE
        '[[I'                       || DataType.ARRAY_TYPE
    }

    def "Should return the identifier for a DataType"() {
        expect:
        dataType.getIdentifier() == identifier

        where:
        dataType                || identifier
        DataType.INTEGER_TYPE || 'I'
        DataType.BOOLEAN_TYPE || 'Z'
        DataType.CHAR_TYPE    || 'C'
        DataType.BYTE_TYPE    || 'B'
        DataType.SHORT_TYPE   || 'S'
        DataType.FLOAT_TYPE   || 'F'
        DataType.LONG_TYPE    || 'J'
        DataType.DOUBLE_TYPE  || 'D'
        DataType.VOID_TYPE    || 'V'
        DataType.OBJECT_TYPE  || 'L'
        DataType.ARRAY_TYPE   || '['
        DataType.METHOD_TYPE  || '('
        DataType.UNKNOWN_TYPE || 'UNKNOWN_TYPE'
        DataType.STRING_TYPE  || 'Ljava/lang/String'
    }
}
