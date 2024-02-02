package de.uzl.its.value.reference


import de.uzl.its.symbolic.value.Value
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.symbolic.value.primitive.numeric.integral.CharValue
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.symbolic.value.reference.StringBuilderValue
import de.uzl.its.symbolic.value.reference.lang.StringValue
import org.objectweb.asm.Type
import org.sosy_lab.common.ShutdownManager
import org.sosy_lab.common.configuration.Configuration
import org.sosy_lab.common.log.BasicLogManager
import org.sosy_lab.java_smt.SolverContextFactory
import org.sosy_lab.java_smt.api.SolverContext
import org.sosy_lab.java_smt.api.StringFormula
import spock.lang.Ignore
import spock.lang.Specification

class StringBuilderValueTest extends Specification {

    def context =
            SolverContextFactory.createSolverContext(
                    Configuration.defaultConfiguration(),
                    BasicLogManager.create(Configuration.defaultConfiguration()),
                    ShutdownManager.create().getNotifier(),
                    SolverContextFactory.Solvers.Z3)
    def prover = context.newProverEnvironment(
            SolverContext.ProverOptions.GENERATE_MODELS)
    def bmgr = context.getFormulaManager().getBooleanFormulaManager()
    def imgr = context.getFormulaManager().getIntegerFormulaManager()
    def smgr = context.getFormulaManager().getStringFormulaManager()

    def "substring(int start)" () {
        setup:
        prover.push()
        int start = 2
        def exampleString = "abcdefghijk"
        String exampleSubstring = exampleString.substring(start)
        def startIndex = new IntValue(context, start)
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE] as Type[]
        def args = [startIndex] as Value<?, ?>[]

        when:
        def stringValue = (StringValue) stringBuilderValue.invokeMethod("substring", desc, args)
        prover.addConstraint(smgr.equal(stringValue.formula,
                smgr.makeString(exampleSubstring)))

        then:
        stringValue.concrete == exampleSubstring
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "append(CharSequence s)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        def appendString = "appending..."
        def appendStringValue = new StringValue(context, appendString, -1)
        def exampleAppendedString = new StringBuilder(exampleString).append(appendString).toString()
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.getType(String.class)] as Type[]
        def args = [appendStringValue] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("append", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(exampleAppendedString)))

        then:
        stringBuilderValue.stringValue.concrete == exampleAppendedString
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "append(char c)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        char appendChar = 'a'
        def appendCharValue = new CharValue(context, appendChar)
        def exampleAppendedString = new StringBuilder(exampleString).append(appendChar).toString()
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc =[Type.CHAR_TYPE] as Type[]
        def args = [appendCharValue] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("append", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeVariable("str")))//smgr.makeString(exampleAppendedString)))


        then:
        println Type.CHAR_TYPE.getDescriptor()
        println stringBuilderValue.stringValue.formula
        println exampleAppendedString
        stringBuilderValue.stringValue.concrete == exampleAppendedString
        !prover.isUnsat()
        println prover.getModel()

        cleanup:
        prover.pop()
    }

    // ToDo: asStringValue Not implemented yet
    @Ignore
    def "append(double d)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        Double appendDouble = 7.34
        def appendDoubleValue = new DoubleValue(context, appendDouble)
        def exampleAppendedString = new StringBuilder(exampleString).append(appendDouble).toString()
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.DOUBLE_TYPE] as Type[]
        def args = [appendDoubleValue] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("append", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(exampleAppendedString)))

        then:
        stringBuilderValue.stringValue.concrete == exampleAppendedString
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    // ToDo: How about negativ integer values? We could only filter for the concrete value
    def "append(int i)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        Integer appendInteger = 7
        def appendIntegerValue = new IntValue(context, appendInteger)
        def exampleAppendedString = new StringBuilder(exampleString).append(appendInteger).toString()
        println exampleAppendedString
        println Type.INT_TYPE.getDescriptor()
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE] as Type[]
        def args = [appendIntegerValue] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("append",desc , args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(exampleAppendedString)))

        then:
        stringBuilderValue.stringValue.concrete == exampleAppendedString
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "append(CharSequence s, int start, int end)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        int startIndex = 2, endindex = 4
        def appendString = "appending..."
        def appendStringValue = new StringValue(context, appendString, -1)
        def exampleAppendedString = new StringBuilder(exampleString).append(appendString, startIndex, endindex).toString()
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.getType(CharSequence.class),
                    Type.INT_TYPE,
                    Type.INT_TYPE] as Type[]
        def args = [appendStringValue,
                    new IntValue(context, startIndex),
                    new IntValue(context, endindex)] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("append", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(exampleAppendedString)))

        then:
        stringBuilderValue.stringValue.concrete == exampleAppendedString
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "charAt(int index)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        int index = 2
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE] as Type[]
        def args = [new IntValue(context, index)] as Value<?, ?>[]

        when:
        CharValue charValue = (CharValue) stringBuilderValue.invokeMethod("charAt", desc, args)
        prover.addConstraint(imgr.equal(charValue.formula,
                imgr.makeNumber(new StringBuilder(exampleString).charAt(index) as Integer)))

        then:
        charValue.concrete == new StringBuilder(exampleString).charAt(index)
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "indexOf(String str)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        def searchString = "fgh"
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.getType(String.class)] as Type[]
        def args = [new StringValue(context, searchString, -100)]  as Value<?, ?>[]

        when:
        IntValue index = (IntValue) stringBuilderValue.invokeMethod("indexOf", desc, args)
        prover.addConstraint(imgr.equal(index.formula,
                imgr.makeNumber(new StringBuilder(exampleString).indexOf(searchString))))

        then:
        index.getConcrete() == new StringBuilder(exampleString).indexOf(searchString)
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "insert(int offset, String str)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        def insertString = "42"
        def insertOffset = 5
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE, Type.getType(String.class)] as Type[]
        def args =  [new IntValue(context, insertOffset),
                     new StringValue(context, insertString, -1)] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("insert", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(new StringBuilder(exampleString).insert(insertOffset, insertString).toString())))

        then:
        stringBuilderValue.stringValue.concrete == new StringBuilder(exampleString).insert(insertOffset, insertString).toString()
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    // ToDo: asStringValue Not implemented yet
    @Ignore
    def "insert(int offset, double d)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        def insertDouble = 42.42
        def insertOffset = 5
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE, Type.DOUBLE_TYPE] as Type[]
        def args = [new IntValue(context, insertOffset),
                    new DoubleValue(context, insertDouble)] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("insert", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(new StringBuilder(exampleString).insert(insertOffset, insertDouble).toString())))

        then:
        stringBuilderValue.stringValue.concrete == new StringBuilder(exampleString).insert(insertOffset, insertDouble).toString()
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "insert(int offset, char c)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        char insertChar = 'z'
        def insertOffset = 5
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE, Type.CHAR_TYPE] as Type[]
        def args = [new IntValue(context, insertOffset),
                    new CharValue(context, insertChar)] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("insert", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(new StringBuilder(exampleString).insert(insertOffset, insertChar).toString())))

        then:
        stringBuilderValue.stringValue.concrete == new StringBuilder(exampleString).insert(insertOffset, insertChar).toString()
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "insert(int offset, int i)"() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        def insertInteger = 42
        def insertOffset = 5
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE, Type.INT_TYPE] as Type[]
        def args = [new IntValue(context, insertOffset),
                    new IntValue(context, insertInteger)] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("insert", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(new StringBuilder(exampleString).insert(insertOffset, insertInteger).toString())))

        then:
        stringBuilderValue.stringValue.concrete == new StringBuilder(exampleString).insert(insertOffset, insertInteger).toString()
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }

    def "replace("() {
        setup:
        prover.push()
        def exampleString = "abcdefghijk"
        def replacementString = "42"
        def startIndex = 1
        def endIndex = 9
        def stringBuilderValue = new StringBuilderValue(context,
                new StringValue(context, exampleString, -1),
                -1)
        def desc = [Type.INT_TYPE, Type.INT_TYPE, Type.getType(String.class)] as Type[]
        def args = [new IntValue(context, startIndex),
                    new IntValue(context, endIndex),
                    new StringValue(context, replacementString, -1)] as Value<?, ?>[]

        when:
        stringBuilderValue.invokeMethod("replace", desc, args)
        prover.addConstraint(smgr.equal((StringFormula) stringBuilderValue.stringValue.formula,
                smgr.makeString(new StringBuilder(exampleString).replace(startIndex, endIndex, replacementString).toString())))

        then:
        stringBuilderValue.stringValue.concrete == new StringBuilder(exampleString).replace(startIndex, endIndex, replacementString).toString()
        !prover.isUnsat()

        cleanup:
        prover.pop()
    }
}
