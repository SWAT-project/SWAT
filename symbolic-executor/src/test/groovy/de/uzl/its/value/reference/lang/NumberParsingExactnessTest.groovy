package de.uzl.its.value.reference.lang

import de.uzl.its.swat.symbolic.invoke.java.lang.NumberParsing
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.sosy_lab.java_smt.api.*
import spock.lang.Specification
import spock.lang.Unroll

import static java.lang.Thread.currentThread

/**
 * L0: the models of parsing numbers from Strings are exact. With the string fixed to a sample, the
 * branch of a call must hold iff Java does not throw a NumberFormatException, and the value must
 * be Java's only (for floats: where the model claims to be exact).
 */
class NumberParsingExactnessTest extends Specification {

    SolverContext context
    FormulaManager fmgr
    BooleanFormulaManager bmgr
    IntegerFormulaManager imgr
    StringFormulaManager smgr
    BitvectorFormulaManager bvmgr
    FloatingPointFormulaManager fpmgr

    static final List<String> EDGE = ["", "-", "+", "0", "-0", "+0", "+5", "--1", "+-1", "1-", "007",
            "2147483647", "2147483648", "-2147483648", "-2147483649", "00000000002147483647", "-000000000002147483648",
            " 1", "1 ", "0x1", "1_0", "١", "١٢٣", "-٠٠٧", "１２", "ａ", "१२",
            "32767", "32768", "-32768", "-32769", "127", "128", "-128", "-129",
            "9223372036854775807", "9223372036854775808", "-9223372036854775808", "-9223372036854775809",
            "ff", "FF", "-ff", "z", "Z", "zz", "1a", "7fffffff", "80000000", "-80000000", "zik0zj", "1y2p0ij32e8e7",
            "11111111111111111111111111111111", "-10000000000000000000000000000000",
            "1111111111111111111111111111111111111111111111111111111111111111"]

    static final List<String> FLOATS = ["0", "1", "-0", "+0", "1.5", ".5", "1.", "-.5F", "0.1", "0.3", "2.50", "33.3333",
            "3.3333333", "1.0000001", "123456.7", "16777215", "16777216", "16777217", "0.0000000001", "0.00000000001",
            "9007199254740991", "9007199254740992", "0.30000000000000004", "0.1234567890123456789012", "1.5f", "1.5d", "1.5D",
            "NaN", "+NaN", "-NaN", "Infinity", "-Infinity", "nan", "NaNf", "1e5", "1E-3", "1e", "e5", ".e1", "0x1p3",
            "0x1.8p1", "0X.8P-1f", "0x1", "0x", " 1.5 ", "\t2\n", "1 5", "", " ", ".", "+", "-", "1.5.5", "1_0", "١",
            "abc", "fixed", "1f5", "00000000000000000000001.5"]

    def setup() {
        ThreadHandler.init()
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
        ThreadHandler.addThreadContext(currentThread().id, "Test-Thread", -2)
        context = ThreadHandler.getSolverContext(currentThread().id)
        fmgr = context.formulaManager
        bmgr = fmgr.booleanFormulaManager
        imgr = fmgr.integerFormulaManager
        smgr = fmgr.stringFormulaManager
        bvmgr = fmgr.bitvectorFormulaManager
        fpmgr = fmgr.floatingPointFormulaManager
    }

    def cleanup() {
        context.close()
        ThreadHandler.removeThreadContext(currentThread().id)
    }

    /** A Java string as the solver sees it: one SMT char per UTF-16 char. */
    private StringFormula chars(String s) {
        StringFormula f = smgr.makeString("")
        for (char c : s.toCharArray()) {
            f = smgr.concat(f, smgr.fromCodePoint(imgr.makeNumber((int) c)))
        }
        return f
    }

    private boolean unsat(BooleanFormula... fs) {
        context.newProverEnvironment().withCloseable { p ->
            fs.each { p.addConstraint(it) }
            return p.isUnsat()
        }
    }

    /** Whether, with s fixed to the sample, f holds (and its negation is unsatisfiable). */
    private boolean holds(StringFormula s, String sample, BooleanFormula f) {
        return unsat(smgr.equal(s, chars(sample)), bmgr.not(f))
    }

    private boolean fails(StringFormula s, String sample, BooleanFormula f) {
        return unsat(smgr.equal(s, chars(sample)), f)
    }

    private static Long javaParse(String type, String s, int radix) {
        try {
            switch (type) {
                case "byte": return (long) Byte.parseByte(s, radix)
                case "short": return (long) Short.parseShort(s, radix)
                case "int": return (long) Integer.parseInt(s, radix)
                default: return Long.parseLong(s, radix)
            }
        } catch (NumberFormatException e) {
            return null
        }
    }

    /** The branch holds iff Java parses the sample, and then the value is Java's only. */
    private void checkIntegral(String type, String sample, int radix) {
        StringFormula s = smgr.makeVariable("s")
        Long java = javaParse(type, sample, radix)
        BooleanFormula valid = NumberParsing.intValid(context, s, radix, type)
        if (java == null) {
            assert fails(s, sample, valid): "$type '$sample' radix $radix: Java throws, the model parses"
        } else {
            assert holds(s, sample, valid): "$type '$sample' radix $radix: Java parses $java, the model throws"
            NumeralFormula.IntegerFormula v = NumberParsing.intValue(context, s, radix, type)
            assert holds(s, sample, imgr.equal(v, imgr.makeNumber(java))): "$type '$sample' radix $radix: not only $java"
        }
    }

    @Unroll
    def "parsing a #type in radix 10 is exact on the edge strings"() {
        expect:
        EDGE.each { checkIntegral(type, it, 10) }

        where:
        type << ["int", "long", "short", "byte"]
    }

    @Unroll
    def "parsing an int and a long is exact in radix #radix"() {
        expect:
        EDGE.each { checkIntegral("int", it, radix) }
        EDGE.each { checkIntegral("long", it, radix) }

        where:
        radix << (2..36)
    }

    @Unroll
    def "parsing a short and a byte is exact in radix #radix"() {
        expect:
        EDGE.each { checkIntegral("short", it, radix) }
        EDGE.each { checkIntegral("byte", it, radix) }

        where:
        radix << [2, 8, 16, 36]
    }

    def "non-ASCII digits parse, as Character.digit accepts them"() {
        expect:
        Integer.parseInt("١٢") == 12
        checkIntegral("int", "١٢", 10)
        checkIntegral("int", "１０", 10)
    }

    private BooleanFormula sameFloat(FloatingPointFormula f, double java, boolean dbl) {
        def type = dbl ? FormulaType.getDoublePrecisionFloatingPointType() : FormulaType.getSinglePrecisionFloatingPointType()
        if (Double.isNaN(java)) return fpmgr.isNaN(f)
        // Structural equality: tells -0.0 from 0.0.
        return fpmgr.assignment(f, dbl ? fpmgr.makeNumber(java, type) : fpmgr.makeNumber((double) (float) java, type))
    }

    @Unroll
    def "parsing a #name is exact where the model says so, and its branch holds iff Java parses"() {
        expect:
        FLOATS.each { sample ->
            StringFormula s = smgr.makeVariable("s")
            Double java
            try {
                java = dbl ? Double.parseDouble(sample) : (double) Float.parseFloat(sample)
            } catch (NumberFormatException e) {
                java = null
            }
            BooleanFormula valid = NumberParsing.floatValid(context, s)
            if (java == null) {
                assert fails(s, sample, valid): "'$sample': Java throws, the model parses"
                return
            }
            assert holds(s, sample, valid): "'$sample': Java parses, the model throws"
            BooleanFormula exact = NumberParsing.floatExact(context, s, dbl)
            boolean claimed = NumberParsing.floatExactConcrete(sample, dbl)
            assert claimed ? holds(s, sample, exact) : fails(s, sample, exact): "'$sample': exactness differs from its concrete check"
            if (claimed) {
                FloatingPointFormula v = NumberParsing.floatValue(context, s, dbl)
                assert holds(s, sample, sameFloat(v, java, dbl)): "'$sample': not only $java"
            }
        }

        where:
        name     | dbl
        "float"  | false
        "double" | true
    }

    def "the float grammar agrees with Java on random strings"() {
        given:
        def random = new Random(4711)
        def alphabet = "0123456789.+-eEpPxXfFdDaANnIity \t_"

        expect:
        (1..400).each {
            def sample = (1..random.nextInt(8)).collect { alphabet[random.nextInt(alphabet.length())] }.join()
            boolean java = true
            try {
                Double.parseDouble(sample)
            } catch (NumberFormatException e) {
                java = false
            }
            StringFormula s = smgr.makeVariable("s")
            BooleanFormula valid = NumberParsing.floatValid(context, s)
            assert java ? holds(s, sample, valid) : fails(s, sample, valid): "'$sample': Java ${java ? 'parses' : 'throws'}"
            if (java) {
                boolean claimed = NumberParsing.floatExactConcrete(sample, true)
                assert claimed ? holds(s, sample, NumberParsing.floatExact(context, s, true))
                        : fails(s, sample, NumberParsing.floatExact(context, s, true)): "'$sample': exactness differs"
            }
        }
    }

    def "the exact fragment covers the plain decimals, not exponents or many digits"() {
        expect:
        NumberParsing.floatExactConcrete("1.5", false)
        NumberParsing.floatExactConcrete("-0", false)
        NumberParsing.floatExactConcrete("16777215", false)
        !NumberParsing.floatExactConcrete("16777216", false)
        NumberParsing.floatExactConcrete("16777216", true)
        !NumberParsing.floatExactConcrete("1e5", true)
        !NumberParsing.floatExactConcrete(" 1", true)
        !NumberParsing.floatExactConcrete("0x1p3", true)
    }

    // ---- the branches of a call ----

    private StringValue symbolicString(String concrete) {
        return new StringValue(context, concrete, smgr.makeVariable("s"), -1)
    }

    @Unroll
    def "the last branch of Integer.parseInt('#sample') holds iff it does not throw"() {
        given:
        def s = symbolicString(sample)
        def branches = NumberParsing.branches("java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", [s] as Value[])

        expect:
        branches.size() == 1
        branches[0].holds() == (javaParse("int", sample, 10) != null)

        where:
        sample << ["", "-", "+", "2147483647", "2147483648", "-2147483648", " 1", "1 ", "0x1", "١"]
    }

    def "a symbolic radix is range checked and pinned; an invalid one throws for every string"() {
        given:
        def s = symbolicString("10")
        def r = new IntValue(context, 16, bvmgr.makeVariable(32, "r"))
        def bad = new IntValue(context, 37)

        when:
        def pinned = NumberParsing.branches("java/lang/Integer", "parseInt", "(Ljava/lang/String;I)I", [s, r] as Value[])
        def invalid = NumberParsing.branches("java/lang/Long", "parseLong", "(Ljava/lang/String;I)J", [s, bad] as Value[])

        then:
        pinned*.holds() == [true, true, true]
        invalid*.holds() == [false]
    }

    def "a concrete string has no branches"() {
        expect:
        NumberParsing.branches("java/lang/Integer", "parseInt", "(Ljava/lang/String;)I",
                [new StringValue(context, "12", -1)] as Value[]).isEmpty()
    }

    def "Float.parseFloat has the validity branch, then, on a return, whether its value is exact"() {
        given:
        def s = symbolicString(sample)

        expect:
        def args = [s] as Value[]
        (NumberParsing.branches("java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", args)
                + NumberParsing.returnBranches("java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", args))*.holds() == holds

        where:
        sample  | holds
        "fixed" | [false]
        "0.5"   | [true, true]
        "1e5"   | [true, false]
    }

    def "a value outside the exact fragment is concrete, with precision loss"() {
        given:
        def sth = ThreadHandler.getSymbolicTraceHandler(currentThread().id)

        when:
        FloatValue exact = NumberParsing.parseFloat(symbolicString("2.5"), sth)
        boolean lossAfterExact = sth.symbolicTrace.symbolicPrecisionLoss
        DoubleValue approx = NumberParsing.parseDouble(symbolicString("1e5"), sth)

        then:
        exact.concrete == 2.5f
        exact.isSymbolic()
        !lossAfterExact
        approx.concrete == 1e5d
        !approx.isSymbolic()
        sth.symbolicTrace.symbolicPrecisionLoss
    }
}
