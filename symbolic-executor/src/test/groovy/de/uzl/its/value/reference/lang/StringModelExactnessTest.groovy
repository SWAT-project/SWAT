package de.uzl.its.value.reference.lang

import de.uzl.its.swat.symbolic.invoke.java.lang.StringCallBranches
import de.uzl.its.swat.symbolic.value.PlaceHolder
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.StringBuilderValue
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.objectweb.asm.Type
import org.sosy_lab.java_smt.api.*
import spock.lang.Specification
import spock.lang.Unroll

import static java.lang.Thread.currentThread

/**
 * L0: the String and StringBuilder models are exact. A model is applied to a symbolic string (and
 * symbolic ints); then, with the variables fixed to a sample, the solver must not find a result
 * other than Java's. A model that is too strict there would make the solver call a feasible path
 * infeasible: a false SAFE.
 */
class StringModelExactnessTest extends Specification {

    SolverContext context
    ProverEnvironment prover
    FormulaManager fmgr
    BooleanFormulaManager bmgr
    IntegerFormulaManager imgr
    StringFormulaManager smgr
    BitvectorFormulaManager bvmgr

    static final Type STRING = Type.getType(String)
    static final Type CHAR_SEQUENCE = Type.getType(CharSequence)
    static final Type OBJECT = Type.getType(Object)
    static final Type INT = Type.INT_TYPE

    static final List<String> SAMPLES = ["", "a", "abc", "aXa", "97", "xaxax", "a😀b", " \t pad \n", "aaaaaaaaaaaaaaa"]

    def setup() {
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
        ThreadHandler.addThreadContext(currentThread().id, "Test-Thread", -2)
        context = ThreadHandler.getSolverContext(currentThread().id)
        prover = context.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS)
        fmgr = context.formulaManager
        bmgr = fmgr.booleanFormulaManager
        imgr = fmgr.integerFormulaManager
        smgr = fmgr.stringFormulaManager
        bvmgr = fmgr.bitvectorFormulaManager
    }

    def cleanup() {
        prover.close()
        context.close()
        ThreadHandler.removeThreadContext(currentThread().id)
    }

    // ---- helpers ----

    /** A Java string as the solver sees it: one SMT char per UTF-16 char (makeString would join surrogate pairs). */
    private StringFormula chars(String s) {
        StringFormula f = smgr.makeString("")
        for (char c : s.toCharArray()) {
            f = smgr.concat(f, smgr.fromCodePoint(imgr.makeNumber((int) c)))
        }
        return f
    }

    private StringValue symbolicString(String name, String concrete) {
        return new StringValue(context, concrete, smgr.makeVariable(name), -1)
    }

    private IntValue symbolicInt(String name, int concrete) {
        return new IntValue(context, concrete, bvmgr.makeVariable(32, name))
    }

    private BooleanFormula fixString(StringValue v, String sample) {
        return smgr.equal(v.formula, chars(sample))
    }

    private BooleanFormula fixInt(IntValue v, int sample) {
        return bvmgr.equal(v.formula, bvmgr.makeBitvector(32, sample))
    }

    private boolean precisionLoss() {
        return ThreadHandler.getSymbolicTraceHandler(currentThread().id).symbolicTrace.symbolicPrecisionLoss
    }

    /** Whether, under the given fixing constraints (and the definitions of the UFs), `other` is unsatisfiable. */
    private boolean onlyJavasResult(List<BooleanFormula> fixed, BooleanFormula other) {
        // A fresh prover per query: after a push, Z3's incremental mode gets stuck on some of them.
        context.newProverEnvironment().withCloseable { p ->
            ThreadHandler.getSymbolicTraceHandler(currentThread().id).getConstraints().each { p.addConstraint(it) }
            fixed.each { p.addConstraint(it) }
            p.addConstraint(other)
            return p.isUnsat()
        }
    }

    /**
     * Whether, under the given fixing constraints, Java's result is possible: the direction that
     * matters for soundness (the converse, that it is the only one, Z3 cannot always show for UFs).
     */
    private boolean allowsJava(List<BooleanFormula> fixed, BooleanFormula java) {
        return !onlyJavasResult(fixed, java)
    }

    private boolean exactInt(List<BooleanFormula> fixed, IntValue result, int java) {
        return onlyJavasResult(fixed, bmgr.not(imgr.equal(result.asIntegerFormula(), imgr.makeNumber(java))))
    }

    private boolean exactBoolean(List<BooleanFormula> fixed, BooleanValue result, boolean java) {
        return onlyJavasResult(fixed, java ? bmgr.not(result.formula) : result.formula)
    }

    private boolean exactString(List<BooleanFormula> fixed, StringFormula result, String java) {
        return onlyJavasResult(fixed, bmgr.not(smgr.equal(result, chars(java))))
    }

    private Value<?, ?> call(Value<?, ?> receiver, String name, List<Type> desc, List<Value<?, ?>> args) {
        return ((ObjectValue) receiver).invokeMethod(name, desc as Type[], args as Value<?, ?>[])
    }

    // ---- String ----

    @Unroll
    def "indexOf(int ch) and indexOf(int ch, int from) search for the char, not its digits: #sample, #ch, #from"() {
        given:
        def s = symbolicString("s", "fixed")
        def c = symbolicInt("ch", 97)
        def f = symbolicInt("from", 0)
        def fixed = [fixString(s, sample), fixInt(c, ch), fixInt(f, from)]

        when:
        def one = (IntValue) call(s, "indexOf", [INT], [c])
        def two = (IntValue) call(s, "indexOf", [INT, INT], [c, f])

        then:
        exactInt(fixed, one, sample.indexOf(ch))
        exactInt(fixed, two, sample.indexOf(ch, from))

        where:
        [sample, ch, from] << [["", "a", "aXa", "97", "a\uD83D\uDE00b"], [(int) 'a', (int) '9', -1, 0x1F600, 0xD83D, 0x110000],
                               [-3, 0, 2]].combinations()
    }

    def "indexOf('a') on a concrete char has the Java result concretely"() {
        expect:
        ((IntValue) call(new StringValue(context, "xab", -1), "indexOf", [INT], [new IntValue(context, (int) 'a')])).concrete == 1
        ((IntValue) call(new StringValue(context, "x97a", -1), "indexOf", [INT, INT], [new IntValue(context, (int) 'a'), new IntValue(context, 0)])).concrete == 3
    }

    @Unroll
    def "lastIndexOf(int ch, int from) is exact, with the char pinned: #sample, #ch, #from"() {
        given:
        def s = symbolicString("s", "fixed")
        def c = symbolicInt("ch", ch)
        def f = symbolicInt("from", 0)
        def pins = StringCallBranches.of(s, "lastIndexOf", "(II)I", [c, f] as Value<?, ?>[])
        def fixed = [fixString(s, sample), fixInt(f, from)] + pins*.condition()

        when:
        def r = (IntValue) call(s, "lastIndexOf", [INT, INT], [c, f])

        then:
        exactInt(fixed, r, sample.lastIndexOf(ch, from))

        where:
        [sample, ch, from] << [["", "a", "aXa", "a\uD83D\uDE00b"], [(int) 'a', -1, 0x1F600, 0x110000], [-1, 0, 2, 100]].combinations()
    }

    @Unroll
    def "lastIndexOf(String str, int from) is exact: #sample, #target, #from"() {
        given:
        def s = symbolicString("s", "fixed")
        def t = symbolicString("t", "x")
        def f = symbolicInt("from", 0)
        def fixed = [fixString(s, sample), fixString(t, target), fixInt(f, from)]

        when:
        def r = (IntValue) call(s, "lastIndexOf", [STRING, INT], [t, f])

        then:
        exactInt(fixed, r, sample.lastIndexOf(target, from))

        where:
        [sample, target, from] << [["", "aXa", "xaxax"], ["", "a", "xa"], [-1, 0, 3, 100]].combinations()
    }

    @Unroll
    def "indexOf(String str, int from) is exact: #sample, #target, #from"() {
        given:
        def s = symbolicString("s", "fixed")
        def t = symbolicString("t", "x")
        def f = symbolicInt("from", 0)
        def fixed = [fixString(s, sample), fixString(t, target), fixInt(f, from)]

        when:
        def r = (IntValue) call(s, "indexOf", [STRING, INT], [t, f])

        then:
        exactInt(fixed, r, sample.indexOf(target, from))

        where:
        [sample, target, from] << [["", "aXa", "xaxax"], ["", "a", "xa"], [-1, 0, 3, 100]].combinations()
    }

    @Unroll
    def "startsWith(String prefix, int toffset) is false for an offset out of range: #sample, #prefix, #offset"() {
        given:
        def s = symbolicString("s", "fixed")
        def p = symbolicString("p", "x")
        def o = symbolicInt("o", 0)
        def fixed = [fixString(s, sample), fixString(p, prefix), fixInt(o, offset)]

        when:
        def r = (BooleanValue) call(s, "startsWith", [STRING, INT], [p, o])

        then:
        exactBoolean(fixed, r, sample.startsWith(prefix, offset))

        where:
        [sample, prefix, offset] << [["", "abc"], ["", "a", "abc"], [-1, 0, 1, 3, 4]].combinations()
    }

    def "startsWith(String prefix, int toffset) has the concrete result of the offset"() {
        expect:
        ((BooleanValue) call(new StringValue(context, "tested", -1), "startsWith", [STRING, INT],
                [new StringValue(context, "ste", -1), new IntValue(context, 2)])).concrete
    }

    @Unroll
    def "replace is exact up to ten occurrences, and precision loss: #sample, #target -> #replacement"() {
        given:
        def s = symbolicString("s", "fixed")
        def fixed = [fixString(s, sample)]

        when:
        def r = (StringValue) call(s, "replace", [CHAR_SEQUENCE, CHAR_SEQUENCE],
                [new StringValue(context, target, -1), new StringValue(context, replacement, -1)])

        then:
        precisionLoss()
        exactString(fixed, r.formula, sample.replace(target, replacement))

        where:
        [sample, target, replacement] << [["", "aaaaaaaaa", "xaxax"], ["a", "aa", "xa"], ["", "b"]].combinations()
    }

    def "replace(char, char) of more than ten occurrences is not exact, and precision loss"() {
        given:
        def s = symbolicString("s", "fixed")
        def sample = "a" * 25

        when:
        def r = (StringValue) call(s, "replace", [Type.CHAR_TYPE, Type.CHAR_TYPE],
                [new CharValue(context, 'a' as char), new CharValue(context, 'c' as char)])

        then:
        precisionLoss()
        !exactString([fixString(s, sample)], r.formula, sample.replace('a' as char, 'c' as char))
    }

    def "replace on concrete strings is concrete"() {
        when:
        def r = (StringValue) call(new StringValue(context, "abab", -1), "replace", [Type.CHAR_TYPE, Type.CHAR_TYPE],
                [new CharValue(context, 'a' as char), new CharValue(context, 'c' as char)])

        then:
        r.concrete == "cbcb"
        !precisionLoss()
    }

    def "replace with an empty or a symbolic target is not modelled exactly"() {
        when:
        call(symbolicString("s", "ab"), "replace", [CHAR_SEQUENCE, CHAR_SEQUENCE],
                [new StringValue(context, "", -1), new StringValue(context, "-", -1)])

        then:
        precisionLoss()
    }

    @Unroll
    def "trim() is exact: '#sample'"() {
        given:
        def s = symbolicString("s", "fixed")

        when:
        def r = (StringValue) call(s, "trim", [], [])

        then:
        exactString([fixString(s, sample)], r.formula, sample.trim())

        where:
        sample << SAMPLES + ["   ", "\u0000a\u0000", " a b "]
    }

    @Unroll
    def "compareTo(String) allows the Java result: #a vs #b"() {
        given:
        def s = symbolicString("s", "fixed")
        def t = symbolicString("t", "fixed")

        when:
        def r = (IntValue) call(s, "compareTo", [STRING], [t])

        then:
        allowsJava([fixString(s, a), fixString(t, b)], imgr.equal(r.asIntegerFormula(), imgr.makeNumber(a.compareTo(b))))
        // (That it is the only one, Z3 does not show within reasonable time for some samples.)

        where:
        // (Some samples, e.g. "B" vs "abd", take Z3 minutes even to satisfy.)
        [a, b] << [["", "a", "ab"], ["", "a", "abd"]].combinations()
    }

    @Unroll
    def "regionMatches(int, String, int, int) is exact: #toffset #ooffset #len"() {
        given:
        def s = symbolicString("s", "fixed")
        def o = symbolicString("o", "fixed")
        def t = symbolicInt("t", 0)
        def oo = symbolicInt("oo", 0)
        def l = symbolicInt("l", 0)
        def fixed = [fixString(s, "abcab"), fixString(o, "xab"), fixInt(t, toffset), fixInt(oo, ooffset), fixInt(l, len)]

        when:
        def r = (BooleanValue) call(s, "regionMatches", [INT, STRING, INT, INT], [t, o, oo, l])

        then:
        exactBoolean(fixed, r, "abcab".regionMatches(toffset, "xab", ooffset, len))

        where:
        [toffset, ooffset, len] << [[-1, 0, 3], [-1, 1, 2], [-1, 0, 2]].combinations()
    }

    def "equals(StringBuilder) and equals(null) are false"() {
        given:
        def s = new StringValue(context, "ab", -1)
        def sb = new StringBuilderValue(context, new StringValue(context, "ab", -1), -1)

        expect:
        call(s, "equals", [OBJECT], [sb]) == PlaceHolder.instance
        !((BooleanValue) call(s, "equals", [OBJECT], [new ObjectValue(context, ObjectValue.ADDRESS_NULL)])).concrete
    }

    def "contentEquals(StringBuilder) compares the chars"() {
        given:
        def s = symbolicString("s", "ab")
        def sb = new StringBuilderValue(context, new StringValue(context, "ab", -1), -1)

        when:
        def r = (BooleanValue) call(s, "contentEquals", [CHAR_SEQUENCE], [sb])

        then:
        r.concrete
        exactBoolean([fixString(s, "ab")], r, true)
        exactBoolean([fixString(s, "abc")], r, false)
    }

    def "getChars copies to the destination offset"() {
        given:
        def s = new StringValue(context, "abcdef", -1)
        def dst = new CharArrayValue(context, new IntValue(context, 4), -1)

        when:
        call(s, "getChars", [INT, INT, Type.getType(char[]), INT],
                [new IntValue(context, 2), new IntValue(context, 4), dst, new IntValue(context, 1)])

        then:
        (0..3).collect { dst.getElement(new IntValue(context, it)).concrete } == ['\u0000', 'c', 'd', '\u0000'].collect { it as char }
    }

    def "toCharArray of the empty string is an empty array"() {
        expect:
        ((CharArrayValue) call(new StringValue(context, "", -1), "toCharArray", [], [])).size.concrete == 0
    }

    def "new String(char[]) takes the chars of the array"() {
        given:
        def s = new StringValue(context, "", -1)
        def array = new CharArrayValue(context, "hi".toCharArray(), -1)

        when:
        call(s, "<init>", [Type.getType(char[])], [array])

        then:
        s.concrete == "hi"
        exactString([], s.formula, "hi")
    }

    @Unroll
    def "long #v and short #v as a String keep their sign"() {
        given:
        def l = new LongValue(context, (long) v, bvmgr.makeVariable(64, "l"))

        when:
        def r = l.asStringValue()

        then:
        exactString([bvmgr.equal(l.formula, bvmgr.makeBitvector(64, (long) v))], r.formula, String.valueOf((long) v))

        where:
        v << [0L, 7L, -7L, Long.MIN_VALUE, Long.MAX_VALUE]
    }

    // ---- StringBuilder ----

    private StringBuilderValue builder(StringValue chars) {
        return new StringBuilderValue(context, chars, -1)
    }

    @Unroll
    def "setLength(#n) truncates or pads a symbolic builder: '#sample'"() {
        given:
        def s = symbolicString("s", "fixed")
        def sb = builder(s)

        when:
        call(sb, "setLength", [INT], [new IntValue(context, n)])

        then:
        def java = new StringBuilder(sample)
        java.setLength(n)
        exactString([fixString(s, sample)], sb.content.formula, java.toString())

        where:
        [sample, n] << [["", "ab", "abcdef"], [0, 1, 3, 5]].combinations()
    }

    def "substring does not change the builder, and substring(start, end) takes end - start chars"() {
        given:
        def s = symbolicString("s", "abcdefgh")
        def sb = builder(s)

        when:
        def sub = (StringValue) call(sb, "substring", [INT, INT], [new IntValue(context, 2), new IntValue(context, 5)])
        def rest = (StringValue) call(sb, "substring", [INT], [new IntValue(context, 6)])

        then:
        sub.concrete == "cde"
        rest.concrete == "gh"
        sb.content.concrete == "abcdefgh"
        exactString([fixString(s, "abcdefgh")], sub.formula, "cde")
        exactString([fixString(s, "abcdefgh")], sb.content.formula, "abcdefgh")
    }

    def "a builder made from a String does not change that String"() {
        given:
        def s = new StringValue(context, "abc", -1)
        def sb = builder(s)

        when:
        call(sb, "deleteCharAt", [INT], [new IntValue(context, 0)])
        call(sb, "insert", [INT, STRING], [new IntValue(context, 0), new StringValue(context, "x", -1)])

        then:
        s.concrete == "abc"
        exactString([], s.formula, "abc")
        sb.content.concrete == "xbc"
    }

    @Unroll
    def "replace(#start, #end, str) replaces that range: '#sample'"() {
        given:
        def s = symbolicString("s", "fixed")
        def sb = builder(s)

        when:
        call(sb, "replace", [INT, INT, STRING], [new IntValue(context, start), new IntValue(context, end), new StringValue(context, "XY", -1)])

        then:
        exactString([fixString(s, sample)], sb.content.formula, new StringBuilder(sample).replace(start, end, "XY").toString())

        where:
        [sample, start, end] << [["abab", "abcdef"], [0, 1, 2], [2, 3, 10]].combinations()
    }

    @Unroll
    def "delete(#start, #end) is exact: '#sample'"() {
        given:
        def s = symbolicString("s", "fixed")
        def sb = builder(s)

        when:
        call(sb, "delete", [INT, INT], [new IntValue(context, start), new IntValue(context, end)])

        then:
        exactString([fixString(s, sample)], sb.content.formula, new StringBuilder(sample).delete(start, end).toString())

        where:
        [sample, start, end] << [["abab", "abcdef"], [0, 1, 2], [2, 3, 10]].combinations()
    }

    def "indexOf(String, int) of a builder uses the from index"() {
        given:
        def s = symbolicString("s", "xaxa")
        def f = symbolicInt("f", 2)

        when:
        def r = (IntValue) call(builder(s), "indexOf", [STRING, INT], [new StringValue(context, "a", -1), f])

        then:
        r.concrete == 3
        exactInt([fixString(s, "xaxa"), fixInt(f, 2)], r, 3)
        exactInt([fixString(s, "xaxa"), fixInt(f, -4)], r, 1)
    }

    def "setCharAt is modelled"() {
        given:
        def s = symbolicString("s", "Hello")
        def sb = builder(s)

        when:
        call(sb, "setCharAt", [INT, Type.CHAR_TYPE], [new IntValue(context, 1), new CharValue(context, 'a' as char)])

        then:
        sb.content.concrete == "Hallo"
        exactString([fixString(s, "Hello")], sb.content.formula, "Hallo")
        exactString([fixString(s, "xy")], sb.content.formula, "xa")
    }

    def "append(Object) appends a String, a builder and null"() {
        given:
        def sb = builder(new StringValue(context, "a", -1))

        when:
        call(sb, "append", [OBJECT], [new StringValue(context, "b", -1)])
        call(sb, "append", [CHAR_SEQUENCE], [builder(new StringValue(context, "c", -1))])
        call(sb, "append", [OBJECT], [new ObjectValue(context, ObjectValue.ADDRESS_NULL)])
        call(sb, "append", [Type.DOUBLE_TYPE], [new de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue(context, 1.5d)])

        then:
        sb.content.concrete == "abcnull1.5"
        !sb.unknown
    }

    def "after a change that is not modelled the chars are unknown, until resynced"() {
        given:
        def sb = builder(symbolicString("s", "ab"))
        def unmodelled = new ObjectValue(context, "x/Unknown", new IntValue(context, 0), -1)

        when:
        call(sb, "insert", [INT, OBJECT], [new IntValue(context, 0), unmodelled])

        then:
        sb.unknown
        sb.symbolic
        call(sb, "length", [], []) == PlaceHolder.instance
        call(sb, "toString", [], []) == PlaceHolder.instance

        when:
        sb.resync("ba")

        then:
        !sb.unknown
        ((StringValue) call(sb, "toString", [], [])).concrete == "ba"
    }

    def "the capacity is the constructor's until the builder may grow"() {
        given:
        def s = symbolicString("s", "abc")
        def sb = builder(null)
        call(sb, "<init>", [STRING], [s])

        when:
        def capacity = (IntValue) call(sb, "capacity", [], [])

        then:
        capacity.concrete == 19
        exactInt([fixString(s, "hello")], capacity, 21)

        when:
        call(sb, "append", [STRING], [new StringValue(context, "x", -1)])

        then:
        call(sb, "capacity", [], []) == PlaceHolder.instance
    }

    def "a builder with symbolic chars is symbolic"() {
        expect:
        builder(symbolicString("s", "ab")).symbolic
        !builder(new StringValue(context, "ab", -1)).symbolic
    }

    // ---- branches of the library calls ----

    private boolean branchHoldsExactly(List<StringCallBranches.Branch> branches, List<BooleanFormula> fixed, boolean javaThrows) {
        // The call returns normally iff all of its index checks hold.
        BooleanFormula all = bmgr.and(branches*.condition())
        return onlyJavasResult(fixed, javaThrows ? all : bmgr.not(all))
    }

    private static boolean throwsIn(Closure c) {
        try {
            c()
            return false
        } catch (IndexOutOfBoundsException ignored) {
            return true
        }
    }

    @Unroll
    def "the index checks of String.#name hold iff Java does not throw: '#sample' #a #b"() {
        given:
        def s = symbolicString("s", "fixed")
        def x = symbolicInt("x", a)
        def y = symbolicInt("y", b)
        def args = desc.startsWith("(II") ? [x, y] : [x]
        def fixed = [fixString(s, sample), fixInt(x, a), fixInt(y, b)]

        when:
        def branches = StringCallBranches.of(s, name, desc, args as Value<?, ?>[])

        then:
        !branches.isEmpty()
        branchHoldsExactly(branches, fixed, throwsIn(java.curry(sample, a, b)))

        where:
        [sample, a, b, call] << [["", "abc"], [-1, 0, 3, 4], [-1, 2, 4],
                                 [["charAt", "(I)C", { String str, int i, int j -> str.charAt(i) }],
                                  ["substring", "(I)Ljava/lang/String;", { String str, int i, int j -> str.substring(i) }],
                                  ["substring", "(II)Ljava/lang/String;", { String str, int i, int j -> str.substring(i, j) }]]].combinations()
        name = call[0]
        desc = call[1]
        java = call[2]
    }

    @Unroll
    def "the index checks of StringBuilder.#name hold iff Java does not throw: '#sample' #a #b"() {
        given:
        def s = symbolicString("s", "fixed")
        def x = symbolicInt("x", a)
        def y = symbolicInt("y", b)
        def fixed = [fixString(s, sample), fixInt(x, a), fixInt(y, b)]
        def args = desc.startsWith("(II") ? [x, y] : desc.startsWith("(IC") ? [x, new CharValue(context, 'z' as char)]
                : desc.startsWith("(ILjava/lang/String;") ? [x, new StringValue(context, "q", -1)] : [x]
        if (desc.startsWith("(IILjava/lang/String;")) args = [x, y, new StringValue(context, "q", -1)]

        when:
        def branches = StringCallBranches.of(builder(s), name, desc, args as Value<?, ?>[])

        then:
        !branches.isEmpty()
        branchHoldsExactly(branches, fixed, throwsIn(java.curry(new StringBuilder(sample), a, b)))

        where:
        [sample, a, b, call] << [["", "abc"], [-1, 0, 3, 4], [-1, 2, 4],
                                 [["charAt", "(I)C", { StringBuilder str, int i, int j -> str.charAt(i) }],
                                  ["setCharAt", "(IC)V", { StringBuilder str, int i, int j -> str.setCharAt(i, 'z' as char) }],
                                  ["deleteCharAt", "(I)Ljava/lang/StringBuilder;", { StringBuilder str, int i, int j -> str.deleteCharAt(i) }],
                                  ["delete", "(II)Ljava/lang/StringBuilder;", { StringBuilder str, int i, int j -> str.delete(i, j) }],
                                  ["replace", "(IILjava/lang/String;)Ljava/lang/StringBuilder;", { StringBuilder str, int i, int j -> str.replace(i, j, "q") }],
                                  ["insert", "(ILjava/lang/String;)Ljava/lang/StringBuilder;", { StringBuilder str, int i, int j -> str.insert(i, "q") }],
                                  ["substring", "(II)Ljava/lang/String;", { StringBuilder str, int i, int j -> str.substring(i, j) }],
                                  ["setLength", "(I)V", { StringBuilder str, int i, int j -> str.setLength(i) }]]].combinations()
        name = call[0]
        desc = call[1]
        java = call[2]
    }

    def "a model that uses a concrete length pins it"() {
        given:
        def s = symbolicString("s", "abc")

        when:
        def branches = StringCallBranches.of(s, "toCharArray", "()[C", [] as Value<?, ?>[])

        then:
        branches.size() == 1
        branches[0].holds()
        onlyJavasResult([branches[0].condition()], bmgr.not(imgr.equal(smgr.length(s.formula), imgr.makeNumber(3))))
    }
}
