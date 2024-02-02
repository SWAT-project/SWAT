package de.uzl.its.value.primitive.numeric.integral

import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue
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

class IntValueTest extends Specification {

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
				SolverContextFactory.Solvers.Z3);
		prover = context.newProverEnvironment(
				SolverContext.ProverOptions.GENERATE_MODELS)
		fmgr = context.getFormulaManager()
		bmgr = fmgr.getBooleanFormulaManager()
		imgr = fmgr.getIntegerFormulaManager()
		fpmgr = fmgr.getFloatingPointFormulaManager()
	}
	def cleanup(){
		prover.close()
		context.close()
	}

	@Shared
	ArrayList<Integer> testValues = [
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
	ArrayList<Integer> leftAssignment = testValues * testValues.size()
	@Shared
	ArrayList<Integer> rightAssignment = []

	def setupSpec() {
		testValues.each {
			rightAssignment = rightAssignment + [it] * testValues.size()
		}
	}

	def "IADD"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.IADD(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		noExceptionThrown()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] + it[1] }
	}

	def "ISUB"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.ISUB(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		noExceptionThrown()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] - it[1] }
	}

	def "IMUL"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.IMUL(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		noExceptionThrown()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] * it[1] }
	}

	def "IDIV"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)
		def i3 = null;
		when:

		AssertionError e = null;
		try {
			i3 = i1.IDIV(i2)
			prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))
		} catch (AssertionError a) {
			e = a
		}

		then:
		if (c2 == 0) {
			assert e instanceof AssertionError && e.message == "Division by zero!"
		} else {
			assert !prover.isUnsat()
			assert i3.concrete == res
		}




		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { if (it[1] == 0) 0 else it[0].intdiv(it[1]) }
	}


	def "IREM"(int c1, int c2, int res) {


		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)
		def i3 = null;

		when:


		Exception e = null;
		try {
			i3 = i1.IREM(i2)
			prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))
		} catch (ArithmeticException a) {
			e = a
		}

		then:
		if (c2 == 0) {
			assert e instanceof ArithmeticException
		} else {
			assert !prover.isUnsat()
			assert i3.concrete == res
		}




		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { if (it[1] == 0) 0 else it[0] % it[1] }
	}


	def "INEG"(int c1, int res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def i3 = i1.INEG()
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << testValues
		res << testValues.collect { -1 * it }
	}

	def "IINC"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def i3 = i1.IINC(c2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] + it[1] }
	}

	def "IAND"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.IAND(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] & it[1] }
	}

	def "ISHL"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.ISHL(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] << it[1] }
	}


	def "ISHR"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.ISHR(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] >> it[1] }
	}

	@Unroll
	def "IUSHR"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.IUSHR(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] >>> it[1] }
	}


	def "IOR"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.IOR(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] | it[1] }
	}

	@Unroll
	def "IXOR"(int c1, int c2, int res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def i3 = i1.IXOR(i2)
		prover.addConstraint(imgr.equal(i3.formula, imgr.makeNumber(res)))

		then:
		!prover.isUnsat()
		i3.concrete == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] ^ it[1] }
	}

	@Unroll
	def "IFEQ"(int c1, boolean res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def formula = i1.IFEQ()
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete == 0) == res



		where:
		c1 << testValues
		res << testValues.collect { it == 0 }
	}

	@Unroll
	def "IFGE"(int c1, boolean res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def formula = i1.IFGE()
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete >= 0) == res



		where:
		c1 << testValues
		res << testValues.collect { it >= 0 }
	}

	@Unroll
	def "IFGT"(int c1, boolean res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def formula = i1.IFGT()
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete > 0) == res



		where:
		c1 << testValues
		res << testValues.collect { it > 0 }
	}

	@Unroll
	def "IFLE"(int c1, boolean res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def formula = i1.IFLE()
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete <= 0) == res



		where:
		c1 << testValues
		res << testValues.collect { it <= 0 }
	}

	@Unroll
	def "IFLT"(int c1, boolean res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def formula = i1.IFLT()
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete < 0) == res



		where:
		c1 << testValues
		res << testValues.collect { it < 0 }
	}


	@Unroll
	def "IFNE"(int c1, boolean res) {



		given:
		def i1 = new IntValue(context, c1)

		when:

		def formula = i1.IFNE()
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete != 0) == res



		where:
		c1 << testValues
		res << testValues.collect { it != 0 }
	}
	@Unroll
	def "IF_ICMPEQ"(int c1, int c2, boolean res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def formula = i1.IF_ICMPEQ(i2)
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete == i2.concrete) == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] == it[1] }
	}

	@Unroll
	def "IF_ICMPGE"(int c1, int c2, boolean res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def formula = i1.IF_ICMPGE(i2)
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete >= i2.concrete) == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] >= it[1] }
	}
	@Unroll
	def "IF_ICMPGT"(int c1, int c2, boolean res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def formula = i1.IF_ICMPGT(i2)
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete > i2.concrete) == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] > it[1] }
	}
	@Unroll
	def "IF_ICMPLE"(int c1, int c2, boolean res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def formula = i1.IF_ICMPLE(i2)
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete <= i2.concrete) == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] <= it[1] }
	}
	@Unroll
	def "IF_ICMPLT"(int c1, int c2, boolean res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def formula = i1.IF_ICMPLT(i2)
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete < i2.concrete) == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] < it[1] }
	}
	@Unroll
	def "IF_ICMPNE"(int c1, int c2, boolean res) {



		given:
		def i1 = new IntValue(context, c1)
		def i2 = new IntValue(context, c2)

		when:

		def formula = i1.IF_ICMPNE(i2)
		prover.addConstraint(bmgr.equivalence(formula, bmgr.makeBoolean(res)))

		then:
		!prover.isUnsat()
		(i1.concrete != i2.concrete) == res



		where:
		c1 << leftAssignment
		c2 << rightAssignment
		res << [
			leftAssignment,
			rightAssignment
		].transpose().collect { it[0] != it[1] }
	}

	@Unroll
	def "I2D"(int c1, double res) {



		given:
		def i1 = new IntValue(context, c1)

		when:
		def d1 = i1.I2D()
		prover.addConstraint(fpmgr.equalWithFPSemantics(d1.formula,fpmgr.makeNumber(res, DoubleValue.precision)))
		then:
		!prover.isUnsat()
		(d1.concrete == res)



		where:
		c1 << testValues
		res << testValues.collect { (double) it }
	}

	@Unroll
	def "I2F"(int c1, float res) {



		given:
		def i1 = new IntValue(context, c1)

		when:
		def f1 = i1.I2F()
		prover.addConstraint(fpmgr.equalWithFPSemantics(f1.formula,fpmgr.makeNumber(res, FloatValue.precision)))
		then:
		!prover.isUnsat()
		(f1.concrete == res)



		where:
		c1 << testValues
		res << testValues.collect { (float) it }
	}
	@Unroll
	def "I2L"(int c1, long res) {



		given:
		def i1 = new IntValue(context, c1)

		when:
		def l1 = i1.I2L()
		prover.addConstraint(imgr.equal(l1.formula,imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		(l1.concrete == res)



		where:
		c1 << testValues
		res << testValues.collect { (long) it }
	}

	@Unroll
	def "I2B"(int c1, byte res) {



		given:
		def i1 = new IntValue(context, c1)

		when:
		def b1 = i1.I2B()
		prover.addConstraint(imgr.equal(b1.formula,imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		(b1.concrete == res)



		where:
		c1 << testValues
		res << testValues.collect { (byte) it }
	}
	@Unroll
	def "I2C"(int c1, char res) {



		given:
		def i1 = new IntValue(context, c1)

		when:
		def ch1 = i1.I2C()
		prover.addConstraint(imgr.equal(ch1.formula,imgr.makeNumber((int) res)))
		then:
		!prover.isUnsat()
		(ch1.concrete == res)



		where:
		c1 << testValues
		res << testValues.collect { (char) it.intValue() }
	}
	@Unroll
	def "I2S"(int c1, short res) {



		given:
		def i1 = new IntValue(context, c1)

		when:
		def s1 = i1.I2S()
		prover.addConstraint(imgr.equal(s1.formula,imgr.makeNumber(res)))
		then:
		!prover.isUnsat()
		(s1.concrete == res)



		where:
		c1 << testValues
		res << testValues.collect { (short) it }
	}
}