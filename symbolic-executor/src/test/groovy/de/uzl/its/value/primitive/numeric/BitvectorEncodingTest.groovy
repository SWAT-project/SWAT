package de.uzl.its.value.primitive.numeric

import de.uzl.its.swat.symbolic.value.primitive.numeric.BitvectorEncoding
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.BitvectorFormula
import org.sosy_lab.java_smt.api.BooleanFormula
import org.sosy_lab.java_smt.api.FormulaType
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Specification

/**
 * BitvectorEncoding.toBitvector must equal int_to_bv in every model, and must not use int_to_bv
 * where the Integer formula is built from numerals, ite, ring operations and bv2int.
 */
class BitvectorEncodingTest extends Specification {

    SolverContext context
    def fmgr, bmgr, bvmgr, imgr, fpmgr, smgr

    def setup() {
        context = SolverContextFactory.createSolverContext(
                Configuration.defaultConfiguration(),
                BasicLogManager.create(Configuration.defaultConfiguration()),
                ShutdownManager.create().getNotifier(),
                SolverContextFactory.Solvers.Z3)
        fmgr = context.getFormulaManager()
        bmgr = fmgr.getBooleanFormulaManager()
        bvmgr = fmgr.getBitvectorFormulaManager()
        imgr = fmgr.getIntegerFormulaManager()
        fpmgr = fmgr.getFloatingPointFormulaManager()
        smgr = fmgr.getStringFormulaManager()
    }

    def cleanup() {
        context.close()
    }

    /**
     * Whether a and b are equal under many concrete assignments of their variables. (Proving it for
     * all models is the kind of int_to_bv query Z3 does not return on.)
     */
    boolean equivalent(BitvectorFormula a, BitvectorFormula b) {
        def vars = new LinkedHashMap(fmgr.extractVariables(a))
        vars.putAll(fmgr.extractVariables(b))
        def random = new Random(42)
        def doubles = [0.0d, -0.0d, 1.0d, 2.0d, -1.5d, 1e300d, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY]
        for (int round = 0; round < 40; round++) {
            def prover = context.newProverEnvironment()
            try {
                vars.each { String n, def v ->
                    def type = fmgr.getFormulaType(v)
                    if (type.isBitvectorType()) {
                        int w = type.getSize()
                        def value = round < 4 ? [0G, 1G, (1G << w) - 1G, 1G << (w - 1)][round] : new BigInteger(w, random)
                        prover.addConstraint(bvmgr.equal(v, bvmgr.makeBitvector(w, value)))
                    } else if (type.isFloatingPointType()) {
                        double d = round < doubles.size() ? doubles[round] : random.nextGaussian() * 4
                        prover.addConstraint(fpmgr.assignment(v, fpmgr.makeNumber(d, type)))
                    } else if (type.isStringType()) {
                        def str = (0..<random.nextInt(4)).collect { (char) ('a' as char) + random.nextInt(3) }.join()
                        prover.addConstraint(smgr.equal(v, smgr.makeString(str)))
                    } else if (type.isBooleanType()) {
                        prover.addConstraint(bmgr.equivalence(v, bmgr.makeBoolean(random.nextBoolean())))
                    } else {
                        throw new IllegalArgumentException("unexpected variable type " + type)
                    }
                }
                prover.addConstraint(bmgr.not(bvmgr.equal(a, b)))
                if (!prover.isUnsat()) {
                    return false
                }
            } finally {
                prover.close()
            }
        }
        return true
    }

    static boolean mixesSorts(def f) {
        def s = f.toString()
        return s.contains("int2bv") || s.contains("int_to_bv") || s.contains("bv2int") || s.contains("bv2nat")
    }

    IntegerFormula num(long v) { imgr.makeNumber(v) }

    BitvectorFormula bv(String name, int width) { bvmgr.makeVariable(width, name) }

    BooleanFormula fpLess(String name, double c) {
        def x = fpmgr.makeVariable(name, FormulaType.getDoublePrecisionFloatingPointType())
        return fpmgr.lessThan(x, fpmgr.makeNumber(c, FormulaType.getDoublePrecisionFloatingPointType()))
    }

    /** The Integer formulas the tests convert, by name. */
    IntegerFormula build(String name) {
        switch (name) {
            case "numerals": return num(-1)
            case "a large numeral": return imgr.makeNumber(new BigInteger("123456789012345678901"))
            case "ite of FP comparisons":
                return bmgr.ifThenElse(fpLess("d", 1.0d), num(-1), bmgr.ifThenElse(fpLess("d", 2.0d), num(0), num(1)))
            case "ring operations":
                return imgr.subtract(imgr.multiply(num(3), num(-7)), imgr.negate(num(1L << 40)))
            case "signed bv2int, same width": return bvmgr.toIntegerFormula(bv("x32", 32), true)
            case "unsigned bv2int, same width": return bvmgr.toIntegerFormula(bv("y32", 32), false)
            case "signed bv2int, narrower": return bvmgr.toIntegerFormula(bv("x8", 8), true)
            case "unsigned bv2int, narrower": return bvmgr.toIntegerFormula(bv("y16", 16), false)
            case "signed bv2int, wider": return bvmgr.toIntegerFormula(bv("x64", 64), true)
            case "bv2int arithmetic":
                return imgr.add(bvmgr.toIntegerFormula(bv("p", 32), true),
                        imgr.multiply(bvmgr.toIntegerFormula(bv("q", 32), false), num(-3)))
            case "a string length": return smgr.length(smgr.makeVariable("s"))
            case "a division": return imgr.divide(bvmgr.toIntegerFormula(bv("z", 32), true), num(3))
            case "a length minus one": return imgr.subtract(smgr.length(smgr.makeVariable("t")), num(1))
        }
        throw new IllegalArgumentException(name)
    }

    def "lowers #name at width #width without int_to_bv, equal to int_to_bv"() {
        given:
        IntegerFormula f = build(name)

        when:
        def lowered = BitvectorEncoding.toBitvector(fmgr, f, width)

        then:
        bvmgr.getLength(lowered) == width
        !mixesSorts(lowered)
        equivalent(lowered, bvmgr.makeBitvector(width, f))

        where:
        [name, width] << [
                ["numerals", [8, 16, 32, 64]],
                ["a large numeral", [8, 32, 64]],
                ["ite of FP comparisons", [32]],
                ["ring operations", [16, 32, 64]],
                ["signed bv2int, same width", [32]],
                ["unsigned bv2int, same width", [32]],
                ["signed bv2int, narrower", [16, 32, 64]],
                ["unsigned bv2int, narrower", [32, 64]],
                ["signed bv2int, wider", [8, 16, 32]],
                ["bv2int arithmetic", [32]],
        ].collectMany { n, ws -> ws.collect { w -> [n, w] } }
    }

    def "keeps int_to_bv for #name, still equal"() {
        when:
        def f = build(name)
        def lowered = BitvectorEncoding.toBitvector(fmgr, f, 32)

        then:
        equivalent(lowered, bvmgr.makeBitvector(32, f))

        where:
        name << ["a string length", "a division", "a length minus one"]
    }

    def "DCMP, FCMP and LCMP results are bit-vectors and match the Integer encoding (#kind)"() {
        given:
        def oldEncoding = { BooleanFormula isNaN, int nanResult, BooleanFormula gt, BooleanFormula eq ->
            def ite = bmgr.ifThenElse(eq, num(0), bmgr.ifThenElse(gt, num(1), num(-1)))
            bvmgr.makeBitvector(32, isNaN == null ? ite : bmgr.ifThenElse(isNaN, num(nanResult), ite))
        }

        when:
        IntValue result
        BitvectorFormula expected
        switch (kind) {
            case "DCMPL":
            case "DCMPG":
                def a = new DoubleValue(context, 1.0d); a.MAKE_SYMBOLIC("0")
                def b = new DoubleValue(context, 2.0d); b.MAKE_SYMBOLIC("1")
                result = kind == "DCMPL" ? a.DCMPL(b) : a.DCMPG(b)
                expected = oldEncoding(bmgr.or(fpmgr.isNaN(a.formula), fpmgr.isNaN(b.formula)),
                        kind == "DCMPL" ? -1 : 1,
                        fpmgr.greaterThan(a.formula, b.formula), fpmgr.equalWithFPSemantics(a.formula, b.formula))
                break
            case "FCMPL":
                def a = new FloatValue(context, 1.0f); a.MAKE_SYMBOLIC("0")
                def b = new FloatValue(context, 2.0f); b.MAKE_SYMBOLIC("1")
                result = a.FCMPL(b)
                expected = oldEncoding(null, 0,
                        fpmgr.greaterThan(a.formula, b.formula), fpmgr.equalWithFPSemantics(a.formula, b.formula))
                break
            default:
                def a = new LongValue(context, 1L); a.MAKE_SYMBOLIC("0")
                def b = new LongValue(context, 2L); b.MAKE_SYMBOLIC("1")
                result = a.LCMP(b)
                expected = oldEncoding(null, 0,
                        bvmgr.greaterThan(a.formula, b.formula, true), bvmgr.equal(a.formula, b.formula))
        }

        then:
        result.concrete == -1
        !mixesSorts(result.formula)
        equivalent(result.formula, expected)

        where:
        kind << ["DCMPL", "DCMPG", "FCMPL", "LCMP"]
    }

    def "boolean to int is a bit-vector ite"() {
        given:
        def b = new BooleanValue(context, true)
        b.MAKE_SYMBOLIC("0")

        when:
        def i = b.asIntValue()

        then:
        i.concrete == 1
        !mixesSorts(i.formula)
        equivalent(i.formula, bvmgr.makeBitvector(32, bmgr.ifThenElse(b.formula, num(1), num(0))))
    }

    def "javaCompare matches Double.compare and Float.compare"() {
        given:
        def values = [Double.NaN, Double.NEGATIVE_INFINITY, -1.5d, -0.0d, 0.0d, Double.MIN_VALUE, 2.0d, Double.POSITIVE_INFINITY]

        expect:
        [FormulaType.getDoublePrecisionFloatingPointType(), FormulaType.getSinglePrecisionFloatingPointType()].each { type ->
            def a = fpmgr.makeVariable("a", type)
            def b = fpmgr.makeVariable("b", type)
            def cmp = BitvectorEncoding.javaCompare(fmgr, a, b)
            for (double x : values) {
                for (double y : values) {
                    int expected = type.getMantissaSize() == 52 ? Double.compare(x, y) : Float.compare((float) x, (float) y)
                    def prover = context.newProverEnvironment()
                    prover.addConstraint(fpmgr.assignment(a, fpmgr.makeNumber(x, type)))
                    prover.addConstraint(fpmgr.assignment(b, fpmgr.makeNumber(y, type)))
                    prover.addConstraint(bmgr.not(bvmgr.equal(cmp, bvmgr.makeBitvector(32, expected))))
                    assert prover.isUnsat(): "compare($x, $y) should be $expected"
                    prover.close()
                }
            }
        }
    }
}
