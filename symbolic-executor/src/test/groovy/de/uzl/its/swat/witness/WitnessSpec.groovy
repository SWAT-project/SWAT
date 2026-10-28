package de.uzl.its.swat.witness

import de.uzl.its.swat.symbolic.trace.dto.NondetReturnDTO
import de.uzl.its.swat.symbolic.trace.dto.ViolationDTO
import spock.lang.Specification

/**
 * L0: the witness recorder renders nondet return values and the violation's stack frames in the
 * form the witness library expects (see the witnesscreator API contract).
 */
class WitnessSpec extends Specification {

    def setup() {
        Witness.reset()
    }

    def cleanup() {
        Witness.reset()
    }

    def "a nondet return value is recorded with its call location"() {
        when:
        Witness.recordValue(42, "nondetInt", "pkg/Main", "main([Ljava/lang/String;)V", "Main.java", 12, 1)
        List<NondetReturnDTO> recorded = Witness.getNondetReturns()

        then:
        recorded.size() == 1
        with(recorded[0]) {
            function == "nondetInt"
            type == "int"
            value == "42"
            bits == null
            className == "pkg.Main"
            method == "main([Ljava/lang/String;)V"
            sourceFile == "Main.java"
            line == 12
            callIndex == 1
        }
    }

    def "values are rendered in their witness string form (#type #description)"() {
        when:
        Witness.recordValue(*args)
        NondetReturnDTO recorded = Witness.getNondetReturns()[0]

        then:
        recorded.type == type
        recorded.value == expectedValue
        recorded.bits == expectedBits

        where:
        description     | args                                                                     | type      | expectedValue  | expectedBits
        "true"          | [true, "nondetBoolean", "Main", "main()V", "Main.java", 1, 0]                       | "boolean" | "true"         | null
        "negative"      | [(byte) -5, "nondetByte", "Main", "main()V", "Main.java", 1, 0]                     | "byte"    | "-5"           | null
        "min"           | [Short.MIN_VALUE, "nondetShort", "Main", "main()V", "Main.java", 1, 0]              | "short"   | "-32768"       | null
        "code point"    | [(char) 'A', "nondetChar", "Main", "main()V", "Main.java", 1, 0]                    | "char"    | "65"           | null
        "high code"     | [(char) 0xFFFF, "nondetChar", "Main", "main()V", "Main.java", 1, 0]                 | "char"    | "65535"        | null
        "min"           | [Integer.MIN_VALUE, "nondetInt", "Main", "main()V", "Main.java", 1, 0]              | "int"     | "-2147483648"  | null
        "max"           | [Long.MAX_VALUE, "nondetLong", "Main", "main()V", "Main.java", 1, 0]                | "long"    | "9223372036854775807" | null
        "plain"         | [0.1f, "nondetFloat", "Main", "main()V", "Main.java", 1, 0]                         | "float"   | "0.1"          | "0x3dcccccd"
        "small"         | [-2.022784E-6f, "nondetFloat", "Main", "main()V", "Main.java", 1, 0]                | "float"   | "-2.022784E-6" | "0x" + Integer.toHexString(Float.floatToRawIntBits(-2.022784E-6f))
        "negative zero" | [-0.0f, "nondetFloat", "Main", "main()V", "Main.java", 1, 0]                        | "float"   | "-0.0"         | "0x80000000"
        "NaN"           | [Float.NaN, "nondetFloat", "Main", "main()V", "Main.java", 1, 0]                    | "float"   | "NaN"          | "0x7fc00000"
        "infinity"      | [Float.NEGATIVE_INFINITY, "nondetFloat", "Main", "main()V", "Main.java", 1, 0]      | "float"   | "-Infinity"    | "0xff800000"
        "plain"         | [1.23E-10d, "nondetDouble", "Main", "main()V", "Main.java", 1, 0]                   | "double"  | "1.23E-10"     | "0x" + Long.toHexString(Double.doubleToRawLongBits(1.23E-10d))
        "negative zero" | [-0.0d, "nondetDouble", "Main", "main()V", "Main.java", 1, 0]                       | "double"  | "-0.0"         | "0x8000000000000000"
        "infinity"      | [Double.POSITIVE_INFINITY, "nondetDouble", "Main", "main()V", "Main.java", 1, 0]    | "double"  | "Infinity"     | "0x7ff0000000000000"
        "raw"           | ["a \"quoted\"\\n", "nondetString", "Main", "main()V", "Main.java", 1, 0]           | "String"  | "a \"quoted\"\\n" | null
        "null"          | [(String) null, "nondetString", "Main", "main()V", "Main.java", 1, 0]               | "String"  | ""             | null
    }

    def "float and double strings round-trip to the recorded value"() {
        when:
        Witness.recordValue(f, "nondetFloat", "Main", "main()V", "Main.java", 1, 0)
        Witness.recordValue(d, "nondetDouble", "Main", "main()V", "Main.java", 1, 0)
        def recorded = Witness.getNondetReturns()

        then:
        Float.floatToRawIntBits(Float.parseFloat(recorded[0].value)) == Float.floatToRawIntBits(f)
        Double.doubleToRawLongBits(Double.parseDouble(recorded[1].value)) == Double.doubleToRawLongBits(d)

        where:
        f << [0.33333334f, Float.MIN_VALUE, 16777217.0f, -3.4028235E38f]
        d << [0.3333333333333333d, Double.MIN_VALUE, 9007199254740993.0d, 1.7976931348623157E308d]
    }

    def "nondet returns keep their execution order"() {
        when:
        Witness.recordValue(1, "nondetInt", "Main", "main()V", "Main.java", 5, 0)
        Witness.recordValue(2, "nondetInt", "Main", "main()V", "Main.java", 5, 1)
        Witness.recordValue(3L, "nondetLong", "Main", "main()V", "Main.java", 6, 0)

        then:
        Witness.getNondetReturns()*.value == ["1", "2", "3"]
        Witness.getNondetReturns()*.callIndex == [0, 1, 0]
    }

    def "the violation records the throwable and its frames innermost first without JDK and SWAT frames"() {
        given:
        def throwable = new AssertionError("boom")
        throwable.setStackTrace([
                new StackTraceElement("java.lang.Integer", "parseInt", "Integer.java", 652),
                new StackTraceElement("de.uzl.its.swat.instrument.Intrinsics", "init", "Intrinsics.java", 1),
                new StackTraceElement("pkg.Helper", "check", "Helper.java", 20),
                new StackTraceElement("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0", null, -2),
                new StackTraceElement("Main", "main", "Main.java", 8),
        ] as StackTraceElement[])

        when:
        ViolationDTO violation = Witness.describe(throwable)

        then:
        violation.throwable == "java.lang.AssertionError"
        violation.frames*.className == ["pkg.Helper", "Main"]
        violation.frames*.sourceFile == ["Helper.java", "Main.java"]
        violation.frames*.line == [20, 8]
    }

    def "the violation frames are capped"() {
        given:
        def throwable = new RuntimeException()
        throwable.setStackTrace((1..(Witness.MAX_FRAMES + 10)).collect {
            new StackTraceElement("pkg.Rec", "rec", "Rec.java", it)
        } as StackTraceElement[])

        expect:
        Witness.describe(throwable).frames.size() == Witness.MAX_FRAMES
        Witness.describe(throwable).frames[0].line == 1
    }

    def "an unknown source file is recorded as empty"() {
        given:
        def throwable = new RuntimeException()
        throwable.setStackTrace([new StackTraceElement("Main", "main", null, -1)] as StackTraceElement[])

        expect:
        Witness.describe(throwable).frames[0].sourceFile == ""
        Witness.describe(throwable).frames[0].line == -1
    }

    def "no violation is recorded until a throwable escapes"() {
        expect:
        Witness.getViolation() == null
    }

    def "the first AssertionError thrown is recorded as the violation"() {
        given:
        def first = new AssertionError("first")
        first.setStackTrace([new StackTraceElement("Main", "main", "Main.java", 8)] as StackTraceElement[])
        def second = new AssertionError("second")
        second.setStackTrace([new StackTraceElement("Main", "main", "Main.java", 12)] as StackTraceElement[])

        when:
        Witness.recordThrow(first)
        Witness.recordThrow(second)

        then:
        Witness.getViolation().throwable == "java.lang.AssertionError"
        Witness.getViolation().frames*.line == [8]
    }

    def "other throwables are not recorded when thrown"() {
        when:
        Witness.recordThrow(new RuntimeException())
        Witness.recordThrow(null)

        then:
        Witness.getViolation() == null
    }

    def "the thrown AssertionError escaping later keeps its violation"() {
        given:
        def error = new AssertionError()
        error.setStackTrace([new StackTraceElement("Main", "main", "Main.java", 8)] as StackTraceElement[])
        def escaping = new RuntimeException()
        escaping.setStackTrace([new StackTraceElement("Main", "main", "Main.java", 20)] as StackTraceElement[])

        when:
        Witness.recordThrow(error)
        Witness.recordTarget(error)

        then:
        Witness.getViolation().frames*.line == [8]

        when: "another throwable escapes the entry method"
        Witness.recordTarget(escaping)

        then:
        Witness.getViolation().throwable == "java.lang.RuntimeException"
        Witness.getViolation().frames*.line == [20]
    }
}
