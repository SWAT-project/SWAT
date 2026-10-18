package de.uzl.its.swat.symbolic.invoke.java.lang

import de.uzl.its.swat.symbolic.value.PlaceHolder
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.BitvectorFormulaManager
import org.sosy_lab.java_smt.api.BooleanFormula
import org.sosy_lab.java_smt.api.BooleanFormulaManager
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager
import org.sosy_lab.java_smt.api.FormulaType
import org.sosy_lab.java_smt.api.SolverContext
import org.sosy_lab.java_smt.api.StringFormulaManager
import spock.lang.Shared
import spock.lang.Specification

/**
 * Compares the symbolic models of the Integer/Long/Short/Byte/Double/Float static methods with the
 * JDK on edge values. For every input the arguments are fresh variables fixed to the input, and the
 * model must agree with the JDK both in its concrete value and in every solution of its formula.
 */
class IntegralModelsSpec extends Specification {

	@Shared SolverContext ctx
	@Shared BitvectorFormulaManager bv
	@Shared BooleanFormulaManager bm
	@Shared FloatingPointFormulaManager fp
	@Shared StringFormulaManager sm
	@Shared int varCount = 0

	static final List<Integer> INTS = intEdges()
	static final List<Long> LONGS = longEdges()
	static final List<Short> SHORTS = [0, -1, 1, Short.MIN_VALUE, Short.MAX_VALUE, 0xff, 0x100, 0x1234, -0x1234, 0x8000 - 1].collect { (short) it }
	static final List<Byte> BYTES = [0, -1, 1, Byte.MIN_VALUE, Byte.MAX_VALUE, 0x40, -0x40].collect { (byte) it }
	static final List<Integer> DISTANCES = [0, 1, -1, 5, 31, 32, 33, 63, 64, 65, Integer.MIN_VALUE, Integer.MAX_VALUE]

	static List<Integer> intEdges() {
		def vs = [0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1,
				  0x12345678, 0x87654321 as int, 0x55555555, 0xaaaaaaaa as int, 0x0f0f0f0f, -7]
		(0..31).each { vs << (1 << it); vs << ((1 << it) - 1); vs << -(1 << it) }
		vs.unique()
	}

	static List<Long> longEdges() {
		def vs = [0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1,
				  0x123456789abcdef0L, 0xfedcba9876543210L, 0x5555555555555555L, 0xaaaaaaaaaaaaaaaaL, -7L,
				  0xffffffffL, 0x100000000L, Integer.MIN_VALUE as long, Integer.MAX_VALUE as long]
		(0..63).each { vs << (1L << it); vs << ((1L << it) - 1); vs << -(1L << it) }
		vs.unique()
	}

	def setupSpec() {
		ctx = SolverContextFactory.createSolverContext(
				Configuration.defaultConfiguration(),
				BasicLogManager.create(Configuration.defaultConfiguration()),
				ShutdownManager.create().getNotifier(),
				SolverContextFactory.Solvers.Z3)
		bv = ctx.getFormulaManager().getBitvectorFormulaManager()
		bm = ctx.getFormulaManager().getBooleanFormulaManager()
		fp = ctx.getFormulaManager().getFloatingPointFormulaManager()
		sm = ctx.getFormulaManager().getStringFormulaManager()
	}

	def cleanupSpec() {
		ctx.close()
	}

	// A symbolic argument with the given value: a fresh variable, and the constraint fixing it.
	List symbolic(Object v) {
		String name = "x" + (varCount++)
		switch (v) {
			case Integer:
				def x = bv.makeVariable(32, name)
				return [new IntValue(ctx, v as int, x), bv.equal(x, bv.makeBitvector(32, v as long))]
			case Long:
				def x = bv.makeVariable(64, name)
				return [new LongValue(ctx, v as long, x), bv.equal(x, bv.makeBitvector(64, v as long))]
			case Short:
				def x = bv.makeVariable(16, name)
				return [new ShortValue(ctx, v as short, x), bv.equal(x, bv.makeBitvector(16, v as long))]
			case Byte:
				def x = bv.makeVariable(8, name)
				return [new ByteValue(ctx, v as byte, x), bv.equal(x, bv.makeBitvector(8, v as long))]
			case Double:
				def x = fp.makeVariable(name, FormulaType.getDoublePrecisionFloatingPointType())
				return [new DoubleValue(ctx, v as double, x), fp.assignment(x, fp.makeNumber(v as double, FormulaType.getDoublePrecisionFloatingPointType()))]
			case Float:
				def x = fp.makeVariable(name, FormulaType.getSinglePrecisionFloatingPointType())
				return [new FloatValue(ctx, v as float, x), fp.assignment(x, fp.makeNumber(v as float, FormulaType.getSinglePrecisionFloatingPointType()))]
		}
		throw new IllegalArgumentException(v.getClass().toString())
	}

	BooleanFormula equalsExpected(Value res, Object expected) {
		switch (res) {
			case IntValue: return bv.equal(res.formula, bv.makeBitvector(32, expected as long))
			case LongValue: return bv.equal(res.formula, bv.makeBitvector(64, expected as long))
			case ShortValue: return bv.equal(res.formula, bv.makeBitvector(16, expected as long))
			case StringValue: return sm.equal(res.formula, sm.makeString(expected as String))
		}
		throw new IllegalArgumentException(res.getClass().toString())
	}

	/**
	 * Runs the model on every argument tuple and returns the mismatches: a wrong concrete value, or
	 * a solution of the formula (with the arguments fixed) that differs from the JDK result.
	 */
	List<String> mismatches(Closure<Value> model, Closure jdk, List<List> inputs) {
		def errors = []
		for (List argValues : inputs) {
			def expected
			try {
				expected = jdk(*argValues)
			} catch (ArithmeticException ignored) {
				continue // the JDK throws, so no value is pushed
			}
			def syms = argValues.collect { symbolic(it) }
			Value res = model(syms.collect { it[0] } as Value[])
			if (res == PlaceHolder.instance) {
				errors << "${argValues}: no model"
				continue
			}
			if (res.concrete != expected) {
				errors << "${argValues}: concrete ${res.concrete} != ${expected}"
			}
			def prover = ctx.newProverEnvironment()
			try {
				syms.each { prover.addConstraint(it[1]) }
				prover.addConstraint(bm.not(equalsExpected(res, expected)))
				if (!prover.isUnsat()) errors << "${argValues}: formula admits a value other than ${expected}"
			} finally {
				prover.close()
			}
		}
		errors
	}

	static List<List> unary(List vs) { vs.collect { [it] } }

	static List<List> pairs(List vs) { [vs, vs].combinations() }

	static Closure<Value> integer(String name) { { Value[] a -> IntegerInvocation.invokeStaticMethod(name, a, null) } }

	static Closure<Value> lng(String name) { { Value[] a -> LongInvocation.invokeStaticMethod(name, a, null, null) } }

	def "Integer.#name agrees with the JDK"() {
		expect:
		mismatches(integer(name), jdk, inputs) == []

		where:
		name                    | inputs                                     | jdk
		"bitCount"              | unary(INTS)                                | { int i -> Integer.bitCount(i) }
		"numberOfLeadingZeros"  | unary(INTS)                                | { int i -> Integer.numberOfLeadingZeros(i) }
		"numberOfTrailingZeros" | unary(INTS)                                | { int i -> Integer.numberOfTrailingZeros(i) }
		"reverse"               | unary(INTS)                                | { int i -> Integer.reverse(i) }
		"reverseBytes"          | unary(INTS)                                | { int i -> Integer.reverseBytes(i) }
		"highestOneBit"         | unary(INTS)                                | { int i -> Integer.highestOneBit(i) }
		"lowestOneBit"          | unary(INTS)                                | { int i -> Integer.lowestOneBit(i) }
		"signum"                | unary(INTS)                                | { int i -> Integer.signum(i) }
		"hashCode"              | unary(INTS)                                | { int i -> Integer.hashCode(i) }
		"toUnsignedLong"        | unary(INTS)                                | { int i -> Integer.toUnsignedLong(i) }
		"toBinaryString"        | unary(INTS)                                | { int i -> Integer.toBinaryString(i) }
		"toOctalString"         | unary(INTS)                                | { int i -> Integer.toOctalString(i) }
		"toHexString"           | unary(INTS)                                | { int i -> Integer.toHexString(i) }
		"toUnsignedString"      | unary(INTS)                                | { int i -> Integer.toUnsignedString(i) }
		"toString"              | unary(INTS)                                | { int i -> Integer.toString(i) }
		"compare"               | pairs(INTS.take(16))                       | { int x, int y -> Integer.compare(x, y) }
		"compareUnsigned"       | pairs(INTS.take(16))                       | { int x, int y -> Integer.compareUnsigned(x, y) }
		"divideUnsigned"        | pairs(INTS.take(16))                       | { int x, int y -> Integer.divideUnsigned(x, y) }
		"remainderUnsigned"     | pairs(INTS.take(16))                       | { int x, int y -> Integer.remainderUnsigned(x, y) }
		"min"                   | pairs(INTS.take(16))                       | { int x, int y -> Integer.min(x, y) }
		"max"                   | pairs(INTS.take(16))                       | { int x, int y -> Integer.max(x, y) }
		"sum"                   | pairs(INTS.take(16))                       | { int x, int y -> Integer.sum(x, y) }
		"rotateLeft"            | [INTS.take(16), DISTANCES].combinations()  | { int i, int d -> Integer.rotateLeft(i, d) }
		"rotateRight"           | [INTS.take(16), DISTANCES].combinations()  | { int i, int d -> Integer.rotateRight(i, d) }
	}

	def "Long.#name agrees with the JDK"() {
		expect:
		mismatches(lng(name), jdk, inputs) == []

		where:
		name                    | inputs                                      | jdk
		"bitCount"              | unary(LONGS)                                | { long i -> Long.bitCount(i) }
		"numberOfLeadingZeros"  | unary(LONGS)                                | { long i -> Long.numberOfLeadingZeros(i) }
		"numberOfTrailingZeros" | unary(LONGS)                                | { long i -> Long.numberOfTrailingZeros(i) }
		"reverse"               | unary(LONGS)                                | { long i -> Long.reverse(i) }
		"reverseBytes"          | unary(LONGS)                                | { long i -> Long.reverseBytes(i) }
		"highestOneBit"         | unary(LONGS)                                | { long i -> Long.highestOneBit(i) }
		"lowestOneBit"          | unary(LONGS)                                | { long i -> Long.lowestOneBit(i) }
		"signum"                | unary(LONGS)                                | { long i -> Long.signum(i) }
		"hashCode"              | unary(LONGS)                                | { long i -> Long.hashCode(i) }
		"toBinaryString"        | unary(LONGS)                                | { long i -> Long.toBinaryString(i) }
		"toOctalString"         | unary(LONGS)                                | { long i -> Long.toOctalString(i) }
		"toHexString"           | unary(LONGS)                                | { long i -> Long.toHexString(i) }
		"toUnsignedString"      | unary(LONGS)                                | { long i -> Long.toUnsignedString(i) }
		"toString"              | unary(LONGS)                                | { long i -> Long.toString(i) }
		"compare"               | pairs(LONGS.take(16))                       | { long x, long y -> Long.compare(x, y) }
		"compareUnsigned"       | pairs(LONGS.take(16))                       | { long x, long y -> Long.compareUnsigned(x, y) }
		"divideUnsigned"        | pairs(LONGS.take(16))                       | { long x, long y -> Long.divideUnsigned(x, y) }
		"remainderUnsigned"     | pairs(LONGS.take(16))                       | { long x, long y -> Long.remainderUnsigned(x, y) }
		"min"                   | pairs(LONGS.take(16))                       | { long x, long y -> Long.min(x, y) }
		"max"                   | pairs(LONGS.take(16))                       | { long x, long y -> Long.max(x, y) }
		"sum"                   | pairs(LONGS.take(16))                       | { long x, long y -> Long.sum(x, y) }
		"rotateLeft"            | [LONGS.take(16), DISTANCES].combinations()  | { long i, int d -> Long.rotateLeft(i, d) }
		"rotateRight"           | [LONGS.take(16), DISTANCES].combinations()  | { long i, int d -> Long.rotateRight(i, d) }
	}

	def "Short.#name agrees with the JDK"() {
		expect:
		mismatches({ Value[] a -> ShortInvocation.invokeStaticMethod(name, a, null) }, jdk, inputs) == []

		where:
		name              | inputs          | jdk
		"reverseBytes"    | unary(SHORTS)   | { short s -> Short.reverseBytes(s) }
		"hashCode"        | unary(SHORTS)   | { short s -> Short.hashCode(s) }
		"toUnsignedInt"   | unary(SHORTS)   | { short s -> Short.toUnsignedInt(s) }
		"toUnsignedLong"  | unary(SHORTS)   | { short s -> Short.toUnsignedLong(s) }
		"compare"         | pairs(SHORTS)   | { short x, short y -> Short.compare(x, y) }
		"compareUnsigned" | pairs(SHORTS)   | { short x, short y -> Short.compareUnsigned(x, y) }
	}

	def "Byte.#name agrees with the JDK"() {
		expect:
		mismatches({ Value[] a -> ByteInvocation.invokeStaticMethod(name, a, null, null) }, jdk, inputs) == []

		where:
		name             | inputs        | jdk
		"toUnsignedInt"  | unary(BYTES)  | { byte b -> Byte.toUnsignedInt(b) }
		"toUnsignedLong" | unary(BYTES)  | { byte b -> Byte.toUnsignedLong(b) }
		"compare"        | pairs(BYTES)  | { byte x, byte y -> Byte.compare(x, y) }
	}

	static final List<Double> DOUBLES = [0.0d, -0.0d, 1.0d, -1.0d, 2.0d, 0.5d, Double.MIN_VALUE, -Double.MIN_VALUE,
										 Double.MIN_NORMAL, Double.MAX_VALUE, -Double.MAX_VALUE,
										 Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, 3.141592653589793d]
	static final List<Float> FLOATS = [0.0f, -0.0f, 1.0f, -1.0f, 2.0f, 0.5f, Float.MIN_VALUE, -Float.MIN_VALUE,
									   Float.MIN_NORMAL, Float.MAX_VALUE, -Float.MAX_VALUE,
									   Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN, 3.1415927f]

	def "Double.#name agrees with the JDK"() {
		expect:
		mismatches({ Value[] a -> DoubleInvocation.invokeStaticMethod(name, a, null, null) }, jdk, unary(DOUBLES)) == []

		where:
		name                  | jdk
		"doubleToLongBits"    | { double d -> Double.doubleToLongBits(d) }
		// the canonical NaN is the only NaN a nondet double or the solver can produce
		"doubleToRawLongBits" | { double d -> Double.doubleToRawLongBits(d) }
	}

	def "Float.#name agrees with the JDK"() {
		expect:
		mismatches({ Value[] a -> FloatInvocation.invokeStaticMethod(name, a, null, null) }, jdk, unary(FLOATS)) == []

		where:
		name                | jdk
		"floatToIntBits"    | { float f -> Float.floatToIntBits(f) }
		"floatToRawIntBits" | { float f -> Float.floatToRawIntBits(f) }
	}

	def "a NaN has a single bit pattern under floatToIntBits and doubleToLongBits"() {
		given: "an unconstrained double and float"
		def d = fp.makeVariable("dNaN", FormulaType.getDoublePrecisionFloatingPointType())
		def f = fp.makeVariable("fNaN", FormulaType.getSinglePrecisionFloatingPointType())
		Value dBits = DoubleInvocation.invokeStaticMethod("doubleToLongBits", [new DoubleValue(ctx, Double.NaN, d)] as Value[], null, null)
		Value fBits = FloatInvocation.invokeStaticMethod("floatToIntBits", [new FloatValue(ctx, Float.NaN, f)] as Value[], null, null)
		def prover = ctx.newProverEnvironment()

		when: "either is NaN with a non-canonical pattern"
		prover.addConstraint(bm.or(
				bm.and(fp.isNaN(d), bm.not(bv.equal(dBits.formula, bv.makeBitvector(64, 0x7ff8000000000000L)))),
				bm.and(fp.isNaN(f), bm.not(bv.equal(fBits.formula, bv.makeBitvector(32, 0x7fc00000L))))))

		then:
		prover.isUnsat()

		cleanup:
		prover.close()
	}
}
