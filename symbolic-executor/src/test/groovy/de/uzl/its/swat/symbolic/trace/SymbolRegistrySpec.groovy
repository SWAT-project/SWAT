package de.uzl.its.swat.symbolic.trace

import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.array.IntArrayValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.objectweb.asm.Type
import org.sosy_lab.java_smt.api.FormulaManager
import org.sosy_lab.java_smt.api.FormulaType
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Specification

import static java.lang.Thread.currentThread

class SymbolRegistrySpec extends Specification {

    SolverContext context
    FormulaManager fmgr

    def setup() {
        ThreadHandler.init()
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
        ThreadHandler.addThreadContext(currentThread().id, "Test-Thread", -2)
        context = ThreadHandler.getSolverContext(currentThread().id)
        fmgr = context.formulaManager
    }

    def cleanup() {
        context.close()
        ThreadHandler.removeThreadContext(currentThread().id)
    }

    private boolean tracePrecisionLoss() {
        ThreadHandler.getSymbolicTraceHandler(currentThread().id).symbolicTrace.symbolicPrecisionLoss
    }

    def "an input and an allocated array's base symbol are precise"() {
        given:
        IntValue i = new IntValue(context, 1)
        String name = i.MAKE_SYMBOLIC(7L)
        ThreadHandler.getSymbolicTraceHandler(currentThread().id).addInput(name, i)
        IntArrayValue arr = new IntArrayValue(context, new IntValue(context, 3), 1)

        when:
        def read = arr.getElement(i)
        def f = fmgr.bitvectorFormulaManager.equal(read.formula, fmgr.bitvectorFormulaManager.makeBitvector(32, 0))
        def base = fmgr.extractVariables(arr.formula).keySet()

        then:
        SymbolRegistry.kindOf(name) == SymbolRegistry.Kind.INPUT
        base.size() == 1
        SymbolRegistry.kindOf(base[0]) == SymbolRegistry.Kind.ARRAY_BASE
        // outside the input namespace, so an input array [I_<n> cannot share it
        !(base[0] ==~ /\[I_\d+/)
        !DTOBuilder.hasImpreciseSymbol(fmgr, f)
    }

    def "an exact String model function is precise"() {
        given:
        StringValue s = new StringValue(context, " a ", 1)
        s.MAKE_SYMBOLIC(3L)

        when:
        StringValue t = s.invokeMethod("trim", [] as Type[], [] as IntValue[]) as StringValue
        def f = fmgr.stringFormulaManager.equal(t.formula, fmgr.stringFormulaManager.makeString("a"))

        then:
        fmgr.extractVariablesAndUFs(f).keySet().containsAll(["trimStart", "trimEnd"])
        !DTOBuilder.hasImpreciseSymbol(fmgr, f)
        !tracePrecisionLoss()
    }

    def "an approximate model function is a precision loss"() {
        given:
        DoubleValue d = new DoubleValue(context, 0.5d)
        d.MAKE_SYMBOLIC(4L)

        when:
        def sin = ThreadHandler.getUFHandler(currentThread().id).getSinCosUF().applySin(d.formula)
        def f = fmgr.floatingPointFormulaManager.lessThan(sin, fmgr.floatingPointFormulaManager.makeNumber(0.1d, DoubleValue.precision))

        then:
        SymbolRegistry.kindOf("sin") == SymbolRegistry.Kind.APPROXIMATE_MODEL_FUNCTION
        DTOBuilder.hasImpreciseSymbol(fmgr, f)
        tracePrecisionLoss()
    }

    def "an undeclared function is a precision loss"() {
        given:
        def uf = fmgr.UFManager.declareUF("someUndeclaredFunction", FormulaType.IntegerType, FormulaType.IntegerType)
        IntValue i = new IntValue(context, 1)
        i.MAKE_SYMBOLIC(5L)

        when:
        def call = fmgr.UFManager.callUF(uf, i.asIntegerFormula())
        def f = fmgr.integerFormulaManager.equal(call, fmgr.integerFormulaManager.makeNumber(0))

        then:
        DTOBuilder.hasImpreciseSymbol(fmgr, f)
    }

    def "an input or approximate declaration is not weakened by a later one"() {
        when:
        SymbolRegistry.declare("Spec_input_0", SymbolRegistry.Kind.INPUT)
        SymbolRegistry.declare("Spec_input_0", SymbolRegistry.Kind.VALUE)
        SymbolRegistry.declare("specApprox", SymbolRegistry.Kind.APPROXIMATE_MODEL_FUNCTION)
        SymbolRegistry.declare("specApprox", SymbolRegistry.Kind.MODEL_FUNCTION)

        then:
        SymbolRegistry.kindOf("Spec_input_0") == SymbolRegistry.Kind.INPUT
        !SymbolRegistry.isPrecise("specApprox")
    }
}
