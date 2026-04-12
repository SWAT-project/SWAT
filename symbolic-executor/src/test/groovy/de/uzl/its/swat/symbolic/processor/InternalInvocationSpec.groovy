package de.uzl.its.swat.symbolic.processor

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.instruction.GETVALUE_Object
import de.uzl.its.swat.symbolic.instruction.GETVALUE_int
import de.uzl.its.swat.symbolic.instruction.GETVALUE_short
import de.uzl.its.swat.symbolic.instruction.GETVALUE_boolean
import de.uzl.its.swat.symbolic.instruction.GETVALUE_long
import de.uzl.its.swat.symbolic.instruction.GETVALUE_char
import de.uzl.its.swat.symbolic.instruction.GETVALUE_byte
import de.uzl.its.swat.symbolic.instruction.GETVALUE_float
import de.uzl.its.swat.symbolic.instruction.GETVALUE_double
import de.uzl.its.swat.symbolic.value.Value

// Value types for primitives and String
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.BooleanObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue
import de.uzl.its.swat.symbolic.value.reference.lang.ByteObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.DoubleObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.FloatObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.LongObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.ShortObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import org.sosy_lab.java_smt.api.Formula

class InternalInvocationSpec extends BaseSymbolicInstructionProcessorSpec {

    def "Lift primitive int to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        int concrete = 10
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_int(iid, concrete, 0) },
                IntValue,
                "(IJ)I"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert IntValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == IntValue.symbolicPrefix + "_" + uid
    }

    def "Lift primitive boolean to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        boolean concrete = true
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_boolean(iid, concrete, 0) },
                BooleanValue,
                "(ZJ)Z"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert BooleanValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == BooleanValue.symbolicPrefix + "_" + uid
    }

    def "Lift primitive short to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        short concrete = 20
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_short(iid, concrete, 0) },
                ShortValue,
                "(SJ)S"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert ShortValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == ShortValue.symbolicPrefix + "_" + uid
    }

    def "Lift primitive long to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        long concrete = 100L
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_long(iid, concrete, 0) },
                LongValue,
                "(JJ)J"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert LongValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == LongValue.symbolicPrefix + "_" + uid
    }

    def "Lift primitive char to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        char concrete = 'A'
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_char(iid, concrete, 0) },
                CharValue,
                "(CJ)C"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert CharValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == CharValue.symbolicPrefix + "_" + uid
    }

    def "Lift primitive byte to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        byte concrete = 5
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_byte(iid, concrete, 0) },
                ByteValue,
                "(BJ)B"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert ByteValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == ByteValue.symbolicPrefix + "_" + uid
    }

    def "Lift primitive float to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        float concrete = 3.14f
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_float(iid, concrete, 0) },
                FloatValue,
                "(FJ)F"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert FloatValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == FloatValue.symbolicPrefix + "_" + uid
    }

    def "Lift primitive double to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        double concrete = 2.718
        Value operand = pushPrimitiveOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_double(iid, concrete, 0) },
                DoubleValue,
                "(DJ)D"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert DoubleValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == DoubleValue.symbolicPrefix + "_" + uid
    }

    def "Lift String to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        String concrete = "Hello"
        // Assuming you have a helper to push String operands.
        Value operand = pushStringOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                StringValue,
                "(Ljava/lang/String;J)Ljava/lang/String;"
        )
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert StringValue.isInstance(result)

        assert result.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(result.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == StringValue.symbolicPrefix + "_" + uid
    }

    def "Lift Boolean to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        boolean concrete = 10
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                BooleanObjectValue,
                "(Ljava/lang/Boolean;J)Ljava/lang/Boolean;",
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert BooleanObjectValue.isInstance(result)
        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == BooleanValue.symbolicPrefix + "_" + uid
    }

    def "Lift Byte to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        byte concrete = 10
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                ByteObjectValue,
                "(Ljava/lang/Byte;J)Ljava/lang/Byte;"
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert ByteObjectValue.isInstance(result)

        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == ByteValue.symbolicPrefix + "_" + uid
    }

    def "Lift Short to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        short concrete = 10
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                ShortObjectValue,
                "(Ljava/lang/Short;J)Ljava/lang/Short;"
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert ShortObjectValue.isInstance(result)

        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == ShortValue.symbolicPrefix + "_" + uid
    }

    def "Lift Character to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        char concrete = 10
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                CharacterObjectValue,
                "(Ljava/lang/Character;J)Ljava/lang/Character;"
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert CharacterObjectValue.isInstance(result)

        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == CharValue.symbolicPrefix + "_" + uid
    }

    def "Lift Integer to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        int concrete = 10
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                IntegerObjectValue,
                "(Ljava/lang/Integer;J)Ljava/lang/Integer;"
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert IntegerObjectValue.isInstance(result)

        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == IntValue.symbolicPrefix + "_" + uid
    }

    def "Lift Long to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        long concrete = 10
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                LongObjectValue,
                "(Ljava/lang/Long;J)Ljava/lang/Long;"
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert LongObjectValue.isInstance(result)

        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == LongValue.symbolicPrefix + "_" + uid
    }
    def "Lift Float to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        float concrete = 10.5f
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                FloatObjectValue,
                "(Ljava/lang/Float;J)Ljava/lang/Float;"
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert FloatObjectValue.isInstance(result)

        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == FloatValue.symbolicPrefix + "_" + uid
    }

    def "Lift Double to symbolic value"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        double concrete = 10.5d
        // Assuming you have a helper to push String operands.
        Value operand = pushBoxedOperand(concrete)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_Object(iid, ObjectValue.ADDRESS_UNKNOWN, concrete, 0) },
                DoubleObjectValue,
                "(Ljava/lang/Double;J)Ljava/lang/Double;"
        )
        def val = (result as BoxedValue).getVal()
        then:
        // Verify the result.
        result != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial value: " + operand)
        logger.info("=" * 50)
        logger.info("Final value: " + result)
        logger.info("=" * 50)

        assert DoubleObjectValue.isInstance(result)

        assert val.concrete == concrete
        def vars = solverContext.getFormulaManager().extractVariables(val.formula as Formula).keySet()
        assert vars.size() == 1
        assert vars.iterator().next() == DoubleValue.symbolicPrefix + "_" + uid
    }


    def "Lift two primitive ints to symbolic value from the same call site (uid)"() {
        given:
        String testClassName = Util.formatClassName("de.uzl.its.swat.test.TestClass")
        String testMethodName = "main"
        setupTestContext(testClassName, testMethodName)
        int concrete1 = 10
        int concrete2 = 20
        Value operand1 = pushPrimitiveOperand(concrete1)
        long uid = 42L
        pushPrimitiveOperand(uid)
        when:
        def result1 = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_int(iid, concrete1, 0) },
                IntValue,
                "(IJ)I"
        )

        Value operand2 = pushPrimitiveOperand(concrete2)
        pushPrimitiveOperand(uid)
        def result2 = executeLiftInsnSeq(
                uid,
                { long iid -> new GETVALUE_int(iid, concrete2, 0) },
                IntValue,
                "(IJ)I"
        )
        then:
        // Verify the result.
        result1 != null && result2 != null

        logger.info("=" * 21 + " Report " + "=" * 21)
        logger.info("Initial values: " + operand1 + " and " + operand2)
        logger.info("=" * 50)
        logger.info("Final value: " + result1 + " and " + result2)
        logger.info("=" * 50)

        assert IntValue.isInstance(result1) && IntValue.isInstance(result2)

        assert result1.concrete == concrete1 && result2.concrete == concrete2
        def vars1 = solverContext.getFormulaManager().extractVariables(result1.formula as Formula).keySet()
        assert vars1.size() == 1
        assert vars1.iterator().next() == IntValue.symbolicPrefix + "_" + uid
        def vars2 = solverContext.getFormulaManager().extractVariables(result2.formula as Formula).keySet()
        assert vars2.size() == 1
        assert vars2.iterator().next() == IntValue.symbolicPrefix + "_" + uid + "01"
    }
}
