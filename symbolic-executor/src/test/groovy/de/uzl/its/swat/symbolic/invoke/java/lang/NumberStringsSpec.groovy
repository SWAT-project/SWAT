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
 * Compares the number-to-string models with the JDK on edge values: every radix from 2 to 36 and
 * radixes outside that range (which the JDK treats as 10). The number is a fresh variable fixed to
 * the input, the radix a constant (a symbolic radix is pinned to a constant by choice branches,
 * which need a running executor). The model must agree with the JDK in its concrete string and in
 * every solution of its formula.
 */
class NumberStringsSpec extends Specification {

	@Shared SolverContext ctx
	@Shared BitvectorFormulaManager bv
	@Shared BooleanFormulaManager bm
	@Shared FloatingPointFormulaManager fp
	@Shared StringFormulaManager sm
	@Shared int varCount = 0

	static final List<Integer> INTS = [0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE + 1,
									   9, 10, 35, 36, -36, 255, 256, 1295, 1296, 0x12345678, 0x87654321 as int, -1440]
	static final List<Long> LONGS = [0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE + 1, 35L, 36L,
									 0xffffffffL, 0x100000000L, Integer.MIN_VALUE as long, 0x123456789abcdef0L,
									 0xfedcba9876543210L, 47009447931686273L]
	static final List<Integer> RADIXES = (2..36) + [-1, 0, 1, 37, Integer.MIN_VALUE, Integer.MAX_VALUE]
	static final List<Short> SHORTS = [0, -1, 1, Short.MIN_VALUE, Short.MAX_VALUE, 10, -10, 0x1234, -0x1234].collect { (short) it }
	static final List<Byte> BYTES = [0, -1, 1, Byte.MIN_VALUE, Byte.MAX_VALUE, 10, -10].collect { (byte) it }

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
		String name = "n" + (varCount++)
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
		}
		throw new IllegalArgumentException(v.getClass().toString())
	}

	IntValue constant(int radix) {
		new IntValue(ctx, radix, bv.makeBitvector(32, radix))
	}

	/** The mismatch of {@code res} with {@code expected}, or null: the concrete string and every solution. */
	String mismatch(Value res, String expected, List<BooleanFormula> fixing) {
		if (res == PlaceHolder.instance || !(res instanceof StringValue)) {
			return "no model"
		}
		if (res.concrete != expected) {
			return "concrete ${res.concrete} != ${expected}"
		}
		def prover = ctx.newProverEnvironment()
		try {
			fixing.each { prover.addConstraint(it) }
			prover.addConstraint(bm.not(sm.equal(res.formula, sm.makeString(expected))))
			return prover.isUnsat() ? null : "formula admits a string other than ${expected}"
		} finally {
			prover.close()
		}
	}

	/** Runs {@code model(number, radix)} on all numbers and radixes and returns the mismatches. */
	List<String> radixMismatches(List numbers, Closure<Value> model, Closure<String> jdk) {
		def errors = []
		for (def n : numbers) {
			for (int r : RADIXES) {
				def sym = symbolic(n)
				def err = mismatch(model(sym[0], constant(r)), jdk(n, r), [sym[1]])
				if (err != null) errors << "${n}, radix ${r}: ${err}"
			}
		}
		errors
	}

	def "Integer.toString(int, int) agrees with the JDK"() {
		expect:
		radixMismatches(INTS, { x, r -> IntegerInvocation.invokeStaticMethod("toString", [x, r] as Value[], null) },
				{ int x, int r -> Integer.toString(x, r) }) == []
	}

	def "Integer.toUnsignedString(int, int) agrees with the JDK"() {
		expect:
		radixMismatches(INTS, { x, r -> IntegerInvocation.invokeStaticMethod("toUnsignedString", [x, r] as Value[], null) },
				{ int x, int r -> Integer.toUnsignedString(x, r) }) == []
	}

	def "Long.toString(long, int) agrees with the JDK"() {
		expect:
		radixMismatches(LONGS, { x, r -> LongInvocation.invokeStaticMethod("toString", [x, r] as Value[], null, null) },
				{ long x, int r -> Long.toString(x, r) }) == []
	}

	def "Long.toUnsignedString(long, int) agrees with the JDK"() {
		expect:
		radixMismatches(LONGS, { x, r -> LongInvocation.invokeStaticMethod("toUnsignedString", [x, r] as Value[], null, null) },
				{ long x, int r -> Long.toUnsignedString(x, r) }) == []
	}

	def "#type.#name agrees with the JDK"() {
		expect:
		values.findAll { v ->
			def sym = symbolic(v)
			mismatch(model(sym[0]), jdk(v), [sym[1]]) != null
		} == []

		where:
		type      | name               | values | model                                                                                     | jdk
		"Integer" | "toString"         | INTS   | { x -> IntegerInvocation.invokeStaticMethod("toString", [x] as Value[], null) }           | { int x -> Integer.toString(x) }
		"Integer" | "toUnsignedString" | INTS   | { x -> IntegerInvocation.invokeStaticMethod("toUnsignedString", [x] as Value[], null) }   | { int x -> Integer.toUnsignedString(x) }
		"Integer" | "toBinaryString"   | INTS   | { x -> IntegerInvocation.invokeStaticMethod("toBinaryString", [x] as Value[], null) }     | { int x -> Integer.toBinaryString(x) }
		"Integer" | "toOctalString"    | INTS   | { x -> IntegerInvocation.invokeStaticMethod("toOctalString", [x] as Value[], null) }      | { int x -> Integer.toOctalString(x) }
		"Long"    | "toString"         | LONGS  | { x -> LongInvocation.invokeStaticMethod("toString", [x] as Value[], null, null) }        | { long x -> Long.toString(x) }
		"Long"    | "toUnsignedString" | LONGS  | { x -> LongInvocation.invokeStaticMethod("toUnsignedString", [x] as Value[], null, null) }| { long x -> Long.toUnsignedString(x) }
		"Long"    | "toBinaryString"   | LONGS  | { x -> LongInvocation.invokeStaticMethod("toBinaryString", [x] as Value[], null, null) }  | { long x -> Long.toBinaryString(x) }
		"Long"    | "toOctalString"    | LONGS  | { x -> LongInvocation.invokeStaticMethod("toOctalString", [x] as Value[], null, null) }   | { long x -> Long.toOctalString(x) }
		"Short"   | "toString"         | SHORTS | { x -> ShortInvocation.invokeStaticMethod("toString", [x] as Value[], null) }             | { short x -> Short.toString(x) }
		"Byte"    | "toString"         | BYTES  | { x -> ByteInvocation.invokeStaticMethod("toString", [x] as Value[], null, null) }        | { byte x -> Byte.toString(x) }
	}

	def "the string of a #type in concatenation agrees with String.valueOf"() {
		expect:
		values.findAll { v ->
			def sym = symbolic(v)
			mismatch(sym[0].asStringValue(), String.valueOf(v), [sym[1]]) != null
		} == []

		where:
		type    | values
		"int"   | INTS
		"long"  | LONGS
		"short" | SHORTS
	}

	def "a concrete #type converts to the string of String.valueOf"() {
		expect:
		values.findAll { v ->
			mismatch(value(v).asStringValue(), String.valueOf(v), []) != null
		} == []

		where:
		type     | values                                                                                    | value
		"double" | [0.0d, -0.0d, 1.0d, 33.333d, 1e-109d, Double.MAX_VALUE, Double.NaN, Double.NEGATIVE_INFINITY] | { double d -> new DoubleValue(ctx, d, fp.makeNumber(d, FormulaType.getDoublePrecisionFloatingPointType())) }
		"float"  | [0.0f, -0.0f, 1.0f, 2.5f, 2.4012912E30f, Float.MIN_VALUE, Float.NaN, Float.POSITIVE_INFINITY] | { float f -> new FloatValue(ctx, f, fp.makeNumber(f, FormulaType.getSinglePrecisionFloatingPointType())) }
	}

	def "a radix outside 2 to 36 means 10"() {
		expect:
		NumberStrings.normaliseRadix(radix) == expected

		where:
		radix             | expected
		Integer.MIN_VALUE | 10
		-1                | 10
		0                 | 10
		1                 | 10
		2                 | 2
		10                | 10
		36                | 36
		37                | 10
		Integer.MAX_VALUE | 10
	}
}
