package de.uzl.its.value.primitive.numeric.integral

import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.BooleanFormulaManager
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager
import org.sosy_lab.java_smt.api.FormulaManager
import org.sosy_lab.java_smt.api.IntegerFormulaManager
import org.sosy_lab.java_smt.api.ProverEnvironment
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class LongValueTest extends Specification {

	SolverContext context
	ProverEnvironment prover
	FormulaManager fmgr
	BooleanFormulaManager bmgr
	IntegerFormulaManager imgr
	FloatingPointFormulaManager fpmgr

	def setup() {

		context = SolverContextFactory.createSolverContext(
				Configuration.defaultConfiguration(),
				BasicLogManager.create(Configuration.defaultConfiguration()),
				ShutdownManager.create().getNotifier(),
				SolverContextFactory.Solvers.Z3)
		prover = context.newProverEnvironment(
				SolverContext.ProverOptions.GENERATE_MODELS)
		fmgr = context.getFormulaManager()
		bmgr = fmgr.getBooleanFormulaManager()
		imgr = fmgr.getIntegerFormulaManager()
		fpmgr = fmgr.getFloatingPointFormulaManager()
	}

	def cleanup() {
		prover.close()
		context.close()
	}

	@Shared
	ArrayList<Long> testValues = [
		Long.MIN_VALUE,
		(Long.MIN_VALUE + 1L),
		-9679678L,
		-7L,
		-2L,
		-1L,
		0L,
		1L,
		2L,
		7L,
		9786967L,
		(Long.MAX_VALUE - 1L),
		Long.MAX_VALUE
	]
	@Shared
	def testValuesInt = [
		Integer.MIN_VALUE,
		Integer.MIN_VALUE + 1,
		-9679678,
		-7,
		-2,
		-1,
		0,
		1,
		2,
		7,
		9786967,
		Integer.MAX_VALUE - 1,
		Integer.MAX_VALUE
	]
	@Shared
	ArrayList<Long> leftAssignment = testValues * testValues.size()
	@Shared
	ArrayList<Long> rightAssignment = []
	@Shared
	ArrayList<Integer> rightAssignmentInt = []

	def setupSpec() {
		testValues.each {
			rightAssignment = rightAssignment + [it] * testValues.size()
			rightAssignmentInt = rightAssignmentInt + [it] * testValuesInt.size()
		}
	}

	def "LADD"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)

		when:

		def l3 = l1.LADD(l2)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] + it[1] }
	}

	def "LAND"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)

		when:

		def l3 = l1.LAND(l2)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] & it[1] }
	}

	def "LDIV"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)
		def l3 = null

		when:
		Exception e = null
		try {
			l3 = l1.LDIV(l2)
			prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
			println fmgr.dumpFormula(imgr.equal(l3.formula, imgr.makeNumber(res)))
		} catch (ArithmeticException a) {
			e = a
		}

		then:
		if (c2 == 0) {
			assert e instanceof ArithmeticException
		} else {
			assert !prover.isUnsat()
			assert l3.concrete == res
		}
		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { if (it[1] == 0) 0L else ((long) it[0]).intdiv((long) it[1]) }
	}

	def "LMUL"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)

		when:

		def l3 = l1.LMUL(l2)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] * it[1] }
	}

	def "LNEG"(long c1, long res) {


		given:
		def l1 = new LongValue(context, c1)

		when:

		def l2 = l1.LNEG()
		prover.addConstraint(imgr.equal(l2.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l2.concrete == res


		where:
		c1 << testValues
		res << testValues.collect { -1 * it }
	}

	def "LOR"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)

		when:

		def l3 = l1.LOR(l2)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] | it[1] }
	}


	def "LREM"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)
		def l3 = null

		when:
		Exception e = null
		try {
			l3 = l1.LREM(l2)
			prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
			println fmgr.dumpFormula(imgr.equal(l3.formula, imgr.makeNumber(res)))
		} catch (ArithmeticException a) {
			e = a
		}

		then:
		if (c2 == 0) {
			assert e instanceof ArithmeticException
		} else {
			assert !prover.isUnsat()
			assert l3.concrete == res
		}
		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { if (it[1] == 0) 0L else it[0] % it[1] }
	}

	def "LSHL"(long c1, int c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def i1 = new IntValue(context, c2)

		when:

		def l3 = l1.LSHL(i1)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignmentInt
		res << [
			leftAssignment,
			rightAssignmentInt
		].transpose().collect { it[0] << it[1] }
	}

	def "LSHR"(long c1, int c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def i1 = new IntValue(context, c2)

		when:

		def l3 = l1.LSHR(i1)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignmentInt
		res << [
			leftAssignment,
			rightAssignmentInt
		].transpose().collect { it[0] >> it[1] }
	}

	def "LSUB"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)

		when:

		def l3 = l1.LSUB(l2)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] - it[1] }
	}

	def "LUSHR"(long c1, int c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def i1 = new IntValue(context, c2)

		when:

		def l3 = l1.LUSHR(i1)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignmentInt
		res << [
			leftAssignment,
			rightAssignmentInt
		].transpose().collect { it[0] >>> it[1] }
	}

	def "LXOR"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)

		when:

		def l3 = l1.LXOR(l2)
		prover.addConstraint(imgr.equal(l3.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		noExceptionThrown()
		l3.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] ^ it[1] }
	}

	def "LCMP"(long c1, long c2, long res) {


		given:
		def l1 = new LongValue(context, c1)
		def l2 = new LongValue(context, c2)

		when:

		def i1 = l1.LCMP(l2)
		prover.addConstraint(imgr.equal(i1.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		noExceptionThrown()
		i1.concrete == res


		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] == it[1] ? 0 : (it[0] > it[1] ? 1 : -1) }
	}

	@Unroll
	def "L2D"(long c1, double res) {


		given:
		def l1 = new LongValue(context, c1)

		when:
		def d1 = l1.L2D()
		prover.addConstraint(fpmgr.equalWithFPSemantics(d1.formula, fpmgr.makeNumber(res, DoubleValue.precision)))
		then:
		!prover.isUnsat()
		(d1.concrete == res)


		where:
		c1 << testValues
		res << testValues.collect { (double) it }
	}

	@Unroll
	def "L2F"(long c1, float res) {


		given:
		def l1 = new LongValue(context, c1)

		when:
		def f1 = l1.L2F()
		prover.addConstraint(fpmgr.equalWithFPSemantics(f1.formula, fpmgr.makeNumber(res, FloatValue.precision)))
		then:
		!prover.isUnsat()
		(f1.concrete == res)


		where:
		c1 << testValues
		res << testValues.collect { (float) it }
	}

	@Unroll
	def "L2I"(long c1, int res) {


		given:
		def l1 = new LongValue(context, c1)

		when:
		def i1 = l1.L2I()
		prover.addConstraint(imgr.equal(i1.formula, imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		(i1.concrete == res)


		where:
		c1 << testValues
		res << testValues.collect { (int) it }
	}

	// ========================================================================
	// Long Narrowing Conversions Tests
	// ========================================================================

	def "Long->Byte: basic values"(long c1, byte res) {
		given:
		def l1 = new LongValue(context, c1)

		when:
		def b1 = l1.asByteValue()
		prover.addConstraint(imgr.equal(b1.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		b1.concrete == res

		where:
		c1      || res
		0L      || 0
		127L    || 127
		-128L   || -128
		255L    || -1      // Overflow wraps
		256L    || 0       // Overflow wraps
		1000L   || -24     // 1000 % 256 = 232, 232 - 256 = -24
		-1000L  || 24      // -1000 % 256 wraps
	}

	def "Long->Byte: symbolic UNSAT test"() {
		given:
		def l1 = new LongValue(context, 500L)
		l1.MAKE_SYMBOLIC("l2b_unsat")

		when:
		def b1 = l1.asByteValue()
		// Constrain long: 10 <= l <= 20
		prover.addConstraint(imgr.greaterOrEquals(l1.formula, imgr.makeNumber(10)))
		prover.addConstraint(imgr.lessOrEquals(l1.formula, imgr.makeNumber(20)))
		// But require byte > 100 (impossible!)
		prover.addConstraint(imgr.greaterThan(b1.formula, imgr.makeNumber(100)))

		then:
		prover.isUnsat()
	}

	def "Long->Byte: symbolic SAT test"() {
		given:
		def l1 = new LongValue(context, 50L)
		l1.MAKE_SYMBOLIC("l2b_sat")

		when:
		def b1 = l1.asByteValue()
		// Constrain long: 10 <= l <= 100
		prover.addConstraint(imgr.greaterOrEquals(l1.formula, imgr.makeNumber(10)))
		prover.addConstraint(imgr.lessOrEquals(l1.formula, imgr.makeNumber(100)))
		// Require byte in valid range (10-100, all fit in byte)
		prover.addConstraint(imgr.greaterOrEquals(b1.formula, imgr.makeNumber(10)))
		prover.addConstraint(imgr.lessOrEquals(b1.formula, imgr.makeNumber(100)))

		then:
		!prover.isUnsat()
	}

	def "Long->Short: basic values"(long c1, short res) {
		given:
		def l1 = new LongValue(context, c1)

		when:
		def s1 = l1.asShortValue()
		prover.addConstraint(imgr.equal(s1.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		s1.concrete == res

		where:
		c1          || res
		0L          || 0
		32767L      || 32767
		-32768L     || -32768
		65535L      || -1      // Overflow wraps
		65536L      || 0       // Overflow wraps
		100000L     || -31072  // Wraps around
		-100000L    || 31072   // Wraps around
	}

	def "Long->Short: symbolic UNSAT test"() {
		given:
		def l1 = new LongValue(context, 500L)
		l1.MAKE_SYMBOLIC("l2s_unsat")

		when:
		def s1 = l1.asShortValue()
		// Constrain long: 100 <= l <= 200
		prover.addConstraint(imgr.greaterOrEquals(l1.formula, imgr.makeNumber(100)))
		prover.addConstraint(imgr.lessOrEquals(l1.formula, imgr.makeNumber(200)))
		// But require short > 10000 (impossible!)
		prover.addConstraint(imgr.greaterThan(s1.formula, imgr.makeNumber(10000)))

		then:
		prover.isUnsat()
	}

	def "Long->Short: symbolic SAT test"() {
		given:
		def l1 = new LongValue(context, 1000L)
		l1.MAKE_SYMBOLIC("l2s_sat")

		when:
		def s1 = l1.asShortValue()
		// Constrain long: 500 <= l <= 2000
		prover.addConstraint(imgr.greaterOrEquals(l1.formula, imgr.makeNumber(500)))
		prover.addConstraint(imgr.lessOrEquals(l1.formula, imgr.makeNumber(2000)))
		// Require short in valid range (500-2000)
		prover.addConstraint(imgr.greaterOrEquals(s1.formula, imgr.makeNumber(500)))
		prover.addConstraint(imgr.lessOrEquals(s1.formula, imgr.makeNumber(2000)))

		then:
		!prover.isUnsat()
	}

	def "Long->Char: basic values"(long c1, char res) {
		given:
		def l1 = new LongValue(context, c1)

		when:
		def ch1 = l1.asCharValue()
		prover.addConstraint(imgr.equal(ch1.formula, imgr.makeNumber((int)res)))

		then:
		!prover.isUnsat()
		ch1.concrete == res

		where:
		c1          || res
		0L          || '\u0000'
		65L         || 'A'
		90L         || 'Z'
		1000L       || '\u03E8'
		65535L      || '\uFFFF'
		65536L      || '\u0000'  // Overflow wraps (unsigned)
		-1L         || '\uFFFF'  // Negative wraps to unsigned
		100000L     || '\u86A0'  // Wraps around unsigned
	}

	def "Long->Char: symbolic SAT test"() {
		given:
		def l1 = new LongValue(context, 65L)
		l1.MAKE_SYMBOLIC("l2c_sat")

		when:
		def ch1 = l1.asCharValue()
		// Constrain long: 65 <= l <= 90 (ASCII 'A' to 'Z')
		prover.addConstraint(imgr.greaterOrEquals(l1.formula, imgr.makeNumber(65)))
		prover.addConstraint(imgr.lessOrEquals(l1.formula, imgr.makeNumber(90)))
		// Require char in valid range (65-90, 'A'-'Z')
		prover.addConstraint(imgr.greaterOrEquals(ch1.formula, imgr.makeNumber(65)))
		prover.addConstraint(imgr.lessOrEquals(ch1.formula, imgr.makeNumber(90)))

		then:
		!prover.isUnsat()
	}

	def "Long->Char: symbolic UNSAT test"() {
		given:
		def l1 = new LongValue(context, 50L)
		l1.MAKE_SYMBOLIC("l2c_unsat")

		when:
		def ch1 = l1.asCharValue()
		// Constrain long: 0 <= l <= 100
		prover.addConstraint(imgr.greaterOrEquals(l1.formula, imgr.makeNumber(0)))
		prover.addConstraint(imgr.lessOrEquals(l1.formula, imgr.makeNumber(100)))
		// But require char > 10000 (impossible!)
		prover.addConstraint(imgr.greaterThan(ch1.formula, imgr.makeNumber(10000)))

		then:
		prover.isUnsat()
	}
}