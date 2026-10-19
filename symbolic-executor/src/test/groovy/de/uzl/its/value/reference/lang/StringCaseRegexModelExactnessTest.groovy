package de.uzl.its.value.reference.lang

import de.uzl.its.swat.symbolic.invoke.java.lang.CharacterInvocation
import de.uzl.its.swat.symbolic.invoke.java.lang.StringCallBranches
import de.uzl.its.swat.symbolic.invoke.java.lang.StringInvocation
import de.uzl.its.swat.symbolic.value.PlaceHolder
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.StringBuilderValue
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue
import de.uzl.its.swat.symbolic.value.reference.array.ObjectArrayValue
import de.uzl.its.swat.symbolic.value.reference.lang.CaseMapping
import de.uzl.its.swat.symbolic.value.reference.lang.JavaRegex
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.objectweb.asm.Type
import org.sosy_lab.java_smt.api.*
import spock.lang.Specification
import spock.lang.Unroll

import static java.lang.Thread.currentThread

/**
 * L0: the case mappings, ignore-case comparisons, simple regular expressions, code point methods,
 * StringBuffer and the other String models added after StringModelExactnessTest are exact. As
 * there: a model is applied to symbolic strings and ints whose concrete values are the sample, the
 * call's branches (index checks, pins) are taken as they hold for the sample, and with the
 * variables fixed to the sample the solver must not find a result other than Java's.
 */
class StringCaseRegexModelExactnessTest extends Specification {

    SolverContext context
    FormulaManager fmgr
    BooleanFormulaManager bmgr
    IntegerFormulaManager imgr
    StringFormulaManager smgr
    BitvectorFormulaManager bvmgr

    static final Type STRING = Type.getType(String)
    static final Type LOCALE = Type.getType(Locale)
    static final Type INT = Type.INT_TYPE
    static final Type BOOLEAN = Type.BOOLEAN_TYPE

    static final String DESERET_UPPER = "𐐀" // U+10400
    static final String DESERET_LOWER = "𐐨" // U+10428

    /** Strings with the chars whose case mappings are special: 'ß', 'İ', 'ı', 'ſ', 'µ', 'ÿ', title case 'ǅ', 'ŉ', a ligature, Greek, surrogates. */
    static final List<String> CASE_SAMPLES = ["", "a", "Ab", "aB", "ß", "SS", "ss", "İ", "i", "I", "ı",
                                              "ſ", "s", "µ", "Μ", "μ", "ÿ", "Ÿ", "Ǆ",
                                              "ǅ", "ǆ", "ŉ", "ﬀ", "σ", "ς", "ΐ",
                                              "x" + DESERET_UPPER, "x" + DESERET_LOWER, "\uD801", "\uDC00\uD801",
                                              "Ⴀ", "ⴀ", "<bad/>", "<BAD/>", "W1E!+O", "w1e!+o"]

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
    }

    def cleanup() {
        context.close()
        ThreadHandler.removeThreadContext(currentThread().id)
    }

    // ---- helpers ----

    protected StringFormula chars(String s) {
        StringFormula f = smgr.makeString("")
        for (char c : s.toCharArray()) {
            f = smgr.concat(f, smgr.fromCodePoint(imgr.makeNumber((int) c)))
        }
        return f
    }

    /** A symbolic string whose concrete value is the sample (the pinned models use it). */
    protected StringValue symbolicString(String name, String concrete) {
        return new StringValue(context, concrete, smgr.makeVariable(name), -1)
    }

    protected IntValue symbolicInt(String name, int concrete) {
        return new IntValue(context, concrete, bvmgr.makeVariable(32, name))
    }

    protected BooleanFormula fix(StringValue v) {
        return smgr.equal(v.formula, chars(v.concrete))
    }

    protected BooleanFormula fix(IntValue v) {
        return bvmgr.equal(v.formula, bvmgr.makeBitvector(32, v.concrete))
    }

    /** The call's branches as they hold for the concrete values. */
    protected List<BooleanFormula> branches(Value<?, ?> receiver, String name, String desc, List<Value<?, ?>> args) {
        return StringCallBranches.of(receiver, name, desc, args as Value<?, ?>[]).collect {
            it.holds() ? it.condition() : bmgr.not(it.condition())
        }
    }

    protected boolean precisionLoss() {
        return ThreadHandler.getSymbolicTraceHandler(currentThread().id).symbolicTrace.symbolicPrecisionLoss
    }

    protected static boolean ascii(String... strings) {
        return strings.every { it.chars().allMatch { c -> c < 128 } }
    }

    protected boolean onlyJavasResult(List<BooleanFormula> fixed, BooleanFormula other) {
        context.newProverEnvironment().withCloseable { p ->
            ThreadHandler.getSymbolicTraceHandler(currentThread().id).getConstraints().each { p.addConstraint(it) }
            fixed.each { p.addConstraint(it) }
            p.addConstraint(other)
            return p.isUnsat()
        }
    }

    protected boolean satisfiable(List<BooleanFormula> constraints) {
        context.newProverEnvironment().withCloseable { p ->
            constraints.each { p.addConstraint(it) }
            return !p.isUnsat()
        }
    }

    protected boolean exactInt(List<BooleanFormula> fixed, IntValue result, int java) {
        return result.concrete == java && onlyJavasResult(fixed, bmgr.not(imgr.equal(result.asIntegerFormula(), imgr.makeNumber(java))))
    }

    protected boolean exactChar(List<BooleanFormula> fixed, CharValue result, char java) {
        return result.concrete == java && onlyJavasResult(fixed, bmgr.not(imgr.equal(result.asIntegerFormula(), imgr.makeNumber((int) java))))
    }

    protected boolean exactBoolean(List<BooleanFormula> fixed, BooleanValue result, boolean java) {
        return result.concrete == java && onlyJavasResult(fixed, java ? bmgr.not(result.formula) : result.formula)
    }

    protected boolean exactString(List<BooleanFormula> fixed, StringValue result, String java) {
        return result.concrete == java && onlyJavasResult(fixed, bmgr.not(smgr.equal(result.formula, chars(java))))
    }

    protected Value<?, ?> call(Value<?, ?> receiver, String name, List<Type> desc, List<Value<?, ?>> args) {
        return ((ObjectValue) receiver).invokeMethod(name, desc as Type[], args as Value<?, ?>[])
    }

    protected ObjectValue locale(Locale l) {
        def v = new ObjectValue(context, "java/util/Locale", new IntValue(context, 0), -1)
        v.concrete = l
        return v
    }

    // ---- toLowerCase / toUpperCase ----

    @Unroll
    def "toLowerCase and toUpperCase are exact: '#sample' #localeName"() {
        given:
        def s = symbolicString("s", sample)
        def args = l == null ? [] : [locale(l)]
        def desc = l == null ? [] : [LOCALE]
        def javaDesc = l == null ? "()Ljava/lang/String;" : "(Ljava/util/Locale;)Ljava/lang/String;"

        when:
        def lower = call(s, "toLowerCase", desc, args)
        def upper = call(s, "toUpperCase", desc, args)
        def fixedLower = [fix(s)] + branches(s, "toLowerCase", javaDesc, args)
        def fixedUpper = [fix(s)] + branches(s, "toUpperCase", javaDesc, args)

        then:
        // Only ASCII strings are modelled, the others are separated by a branch.
        !ascii(sample) || exactString(fixedLower, (StringValue) lower, sample.toLowerCase(l ?: Locale.getDefault()))
        !ascii(sample) || exactString(fixedUpper, (StringValue) upper, sample.toUpperCase(l ?: Locale.getDefault()))
        ascii(sample) || (lower instanceof PlaceHolder && upper instanceof PlaceHolder)

        where:
        [sample, l] << [CASE_SAMPLES + ["Hello World", "aİbßc"], [null, Locale.ROOT, Locale.ENGLISH, Locale.UK]].combinations()
        localeName = l == null ? "default" : l.toString()
    }

    def "toLowerCase separates the ASCII strings, the others are not modelled"() {
        given:
        def s = symbolicString("s", "a\u00c4")

        when:
        def r = call(s, "toLowerCase", [LOCALE], [locale(Locale.ROOT)])
        def b = StringCallBranches.of(s, "toLowerCase", "(Ljava/util/Locale;)Ljava/lang/String;", [locale(Locale.ROOT)] as Value<?, ?>[])

        then:
        r instanceof PlaceHolder
        b.size() == 1
        !b[0].holds()
        !b[0].check()
        onlyJavasResult([fix(s)], b[0].condition())
        satisfiable([b[0].condition(), smgr.equal(s.formula, chars("aB"))])
    }

    def "toLowerCase pins the length"() {
        given:
        def s = symbolicString("s", "Ab")

        when:
        def b = branches(s, "toLowerCase", "()Ljava/lang/String;", [])

        then:
        onlyJavasResult(b, bmgr.not(imgr.equal(smgr.length(s.formula), imgr.makeNumber(2))))
        satisfiable(b + [smgr.equal(s.formula, chars("xy"))])
    }

    def "toLowerCase with the Turkish locale or an unknown one is not modelled"() {
        given:
        def s = symbolicString("s", "I")
        def unknown = new ObjectValue(context, "java/util/Locale", new IntValue(context, 0), -1)

        expect:
        call(s, "toLowerCase", [LOCALE], [locale(new Locale("tr"))]) instanceof PlaceHolder
        call(s, "toUpperCase", [LOCALE], [locale(new Locale("lt"))]) instanceof PlaceHolder
        call(s, "toLowerCase", [LOCALE], [unknown]) instanceof PlaceHolder
        StringCallBranches.of(s, "toLowerCase", "(Ljava/util/Locale;)Ljava/lang/String;", [locale(new Locale("tr"))] as Value<?, ?>[]).isEmpty()
    }

    // ---- equalsIgnoreCase / regionMatches(true, ...) / compareToIgnoreCase ----

    @Unroll
    def "equalsIgnoreCase is exact, a concrete and a symbolic string: '#a' '#b'"() {
        given:
        def s = symbolicString("s", a)
        def t = new StringValue(context, b, -1)

        when:
        def r1 = call(s, "equalsIgnoreCase", [STRING], [t])
        def r2 = call(t, "equalsIgnoreCase", [STRING], [s])

        then:
        // Against a concrete string with a surrogate the comparison is modelled for ASCII strings only.
        b.chars().anyMatch { Character.isSurrogate((char) it) } ? (r1 instanceof PlaceHolder && r2 instanceof PlaceHolder) :
                (exactBoolean([fix(s)] + branches(s, "equalsIgnoreCase", "(Ljava/lang/String;)Z", [t]), (BooleanValue) r1, a.equalsIgnoreCase(b))
                && exactBoolean([fix(s)] + branches(t, "equalsIgnoreCase", "(Ljava/lang/String;)Z", [s]), (BooleanValue) r2, b.equalsIgnoreCase(a)))
        !precisionLoss()

        where:
        [a, b] << [CASE_SAMPLES, ["", "a", "SS", "ß", "i", "ı", "µ", "ǅ", "x" + DESERET_LOWER, "<bad/>"]].combinations()
    }

    @Unroll
    def "equalsIgnoreCase is exact, two symbolic strings: '#a' '#b'"() {
        given:
        def s = symbolicString("s", a)
        def t = symbolicString("t", b)

        when:
        def r = call(s, "equalsIgnoreCase", [STRING], [t])
        def fixed = [fix(s), fix(t)] + branches(s, "equalsIgnoreCase", "(Ljava/lang/String;)Z", [t])

        then:
        ascii(a, b) ? exactBoolean(fixed, (BooleanValue) r, a.equalsIgnoreCase(b)) : r instanceof PlaceHolder

        where:
        [a, b] << ([["", "a", "A", "ab", "AB", "ß", "Ǆ", "ǆ", "ı", "I", DESERET_UPPER, DESERET_LOWER,
                     "\uD801𐐀", "𐐨X", "𐐨\uD801"], ["", "a", "AB", "ǅ", "i",
                     DESERET_LOWER, "𐐨X", "\uD801𐐀"]].combinations()
                + [["\uD801𐐀", "𐐨X"], ["x𐐀", "x𐐨"]])
    }

    def "equalsIgnoreCase(null) is false"() {
        expect:
        !((BooleanValue) call(symbolicString("s", "a"), "equalsIgnoreCase", [STRING], [new ObjectValue(context, 0)])).concrete
    }

    @Unroll
    def "regionMatches(#ic, ...) is exact: '#a' #to '#b' #oo #len"() {
        given:
        def s = symbolicString("s", a)
        def t = symbolicString("t", b)
        def ignoreCase = new BooleanValue(context, ic, bmgr.makeVariable("ic"))
        def toffset = symbolicInt("to", to)
        def ooffset = symbolicInt("oo", oo)
        def length = symbolicInt("len", len)
        def args = [ignoreCase, toffset, t, ooffset, length]

        when:
        def r = call(s, "regionMatches", [BOOLEAN, INT, STRING, INT, INT], args)
        def fixed = [fix(s), fix(t), fix(toffset), fix(ooffset), fix(length), ic ? ignoreCase.formula : bmgr.not(ignoreCase.formula)] +
                branches(s, "regionMatches", "(ZILjava/lang/String;II)Z", args)

        then:
        (r instanceof PlaceHolder && !ascii(a, b) && StringValue.regionInBounds(a, to, b, oo, len))
                || exactBoolean(fixed, (BooleanValue) r, a.regionMatches(ic, to, b, oo, len))

        where:
        [a, b, ic, to, oo, len] << [["", "aBc", "x" + DESERET_UPPER], ["ABC", "bc", "X" + DESERET_LOWER], [true, false],
                                    [-1, 1], [0, 1], [-1, 2, 5]].combinations()
    }

    @Unroll
    def "compareToIgnoreCase is exact: '#a' '#b'"() {
        given:
        def s = symbolicString("s", a)
        def t = symbolicString("t", b)

        when:
        def r = call(s, "compareToIgnoreCase", [STRING], [t])
        def fixed = [fix(s), fix(t)] + branches(s, "compareToIgnoreCase", "(Ljava/lang/String;)I", [t])

        then:
        ascii(a, b) ? exactInt(fixed, (IntValue) r, a.compareToIgnoreCase(b)) : r instanceof PlaceHolder

        where:
        [a, b] << [["", "a", "B", "abc", "ß", "ÿ", "Ÿ", "ǅ", "x" + DESERET_UPPER, "x\uD801", "Ā"],
                   ["", "A", "b", "ABD", "Ÿ", "x" + DESERET_LOWER, "x\uDC28", "xa", "ā"]].combinations()
    }

    def "Boolean.parseBoolean is exact"() {
        given:
        def s = symbolicString("s", sample)

        expect:
        exactBoolean([fix(s)], (BooleanValue) de.uzl.its.swat.symbolic.invoke.java.lang.BooleanInvocation.invokeStaticMethod(
                "parseBoolean", [s] as Value<?, ?>[], [STRING] as Type[]), Boolean.parseBoolean(sample))

        where:
        sample << ["", "true", "TRUE", "tRuE", "true ", "ırue", "tru"]
    }

    // ---- regular expressions ----

    static final List<String> REGEXES = ["[A-Z][a-zA-Z]*", "[a-zA-z]+(['-][a-zA-Z]+)*", "\\d+\\s+([a-zA-Z]+|[a-zA-Z]+\\s[a-zA-Z]+)",
                                         "([a-zA-Z]+|[a-zA-Z]+\\s[a-zA-Z]+)", "\\d{5}", "[1-9]\\d{2}-[1-9]\\d{2}-\\d{4}", "a{2,}b?",
                                         "(?:ab|c)*?", "\\w*\\*", "", "[-a]+", "[a-]", "x{0}", "a{1,3}"]

    @Unroll
    def "matches(#regex) is exact: '#sample'"() {
        given:
        def s = symbolicString("s", sample)

        when:
        def r = call(s, "matches", [STRING], [new StringValue(context, regex, -1)])

        then:
        exactBoolean([fix(s)], (BooleanValue) r, sample.matches(regex))

        where:
        [regex, sample] << [REGEXES, ["", "a", "Ab", "O'Neil-Smith", "12 Main St", "12  Main", "Lake City", "12345", "1234",
                                      "234-567-8901", "aab", "aaab", "abcab", "x_1*", "a-", "-", "😀", "a\uD83D"]].combinations()
    }

    def "regular expressions outside the simple ones are not translated"() {
        expect:
        ["a.b", "[^a]", '^a', 'a$', "\\D", "\\bx", "(a)\\1", "a*+", "(?i)a", "(?=a)", "\\p{L}", "[a&&b]", "a{2", "\\Qa\\E", "\\u0041"].every {
            JavaRegex.toRegex(smgr, it) == null
        }
    }

    @Unroll
    def "replaceFirst with a literal regex is exact, replaceAll a precision loss: '#sample' #regex"() {
        given:
        def s = symbolicString("s", sample)
        def args = [new StringValue(context, regex, -1), new StringValue(context, "^", -1)]

        when:
        def first = call(s, "replaceFirst", [STRING, STRING], args)
        def all = call(s, "replaceAll", [STRING, STRING], args)

        then:
        exactString([fix(s)], (StringValue) first, sample.replaceFirst(regex, "^"))
        exactString([fix(s)], (StringValue) all, sample.replaceAll(regex, "^"))
        precisionLoss()

        where:
        [sample, regex] << [["", "a*b", "**", "Automatic x Automatic"], ["\\*", "Automatic", "b"]].combinations()
    }

    def "replaceAll/replaceFirst with a regex or a replacement that is not literal are not modelled"() {
        given:
        def s = symbolicString("s", "ab")

        expect:
        call(s, "replaceAll", [STRING, STRING], [new StringValue(context, "\\w+", -1), new StringValue(context, "x", -1)]) instanceof PlaceHolder
        call(s, "replaceAll", [STRING, STRING], [new StringValue(context, "a", -1), new StringValue(context, "\$1", -1)]) instanceof PlaceHolder
        call(s, "replaceAll", [STRING, STRING], [new StringValue(context, "", -1), new StringValue(context, "x", -1)]) instanceof PlaceHolder
    }

    // ---- code points, repeat, join, new String(char[], int, int) ----

    @Unroll
    def "codePointAt/codePointBefore/codePointCount are exact: '#sample' #i #j"() {
        given:
        def s = symbolicString("s", sample)
        def x = symbolicInt("x", i)
        def y = symbolicInt("y", j)

        when:
        Integer at = orNull { sample.codePointAt(i) }
        Integer before = orNull { sample.codePointBefore(i) }
        Integer count = orNull { sample.codePointCount(i, j) }

        then:
        at == null || exactInt([fix(s), fix(x)] + branches(s, "codePointAt", "(I)I", [x]),
                (IntValue) call(s, "codePointAt", [INT], [x]), at)
        before == null || exactInt([fix(s), fix(x)] + branches(s, "codePointBefore", "(I)I", [x]),
                (IntValue) call(s, "codePointBefore", [INT], [x]), before)
        count == null || exactInt([fix(s), fix(x), fix(y)] + branches(s, "codePointCount", "(II)I", [x, y]),
                (IntValue) call(s, "codePointCount", [INT, INT], [x, y]), count)

        where:
        [sample, i, j] << [["", "a", "a😀b", "\uDE00\uD83D", "\uD83D😀"], [-1, 0, 1, 2, 3], [0, 2, 3, 4]].combinations()
    }

    /** Java's result, or null if it throws (the index checks are tested elsewhere; the model is not used then). */
    protected static Integer orNull(Closure<Integer> java) {
        try {
            return java()
        } catch (IndexOutOfBoundsException ignored) {
            return null
        }
    }

    def "the index check of codePointBefore holds iff Java does not throw"() {
        given:
        def s = symbolicString("s", sample)
        def x = symbolicInt("x", i)
        def b = StringCallBranches.of(s, "codePointBefore", "(I)I", [x] as Value<?, ?>[])
        boolean javaThrows = false
        try { sample.codePointBefore(i) } catch (IndexOutOfBoundsException ignored) { javaThrows = true }

        expect:
        b.size() == 1
        b[0].holds() == !javaThrows
        onlyJavasResult([fix(s), fix(x)], javaThrows ? b[0].condition() : bmgr.not(b[0].condition()))

        where:
        [sample, i] << [["", "ab"], [-1, 0, 1, 2, 3]].combinations()
    }

    @Unroll
    def "repeat(#n) is exact: '#sample'"() {
        given:
        def s = symbolicString("s", sample)
        def count = symbolicInt("n", n)

        expect:
        exactString([fix(s), fix(count)] + branches(s, "repeat", "(I)Ljava/lang/String;", [count]),
                (StringValue) call(s, "repeat", [INT], [count]), sample.repeat(n))

        where:
        [sample, n] << [["", "ab"], [0, 1, 3]].combinations()
    }

    def "String.join of an array is exact"() {
        given:
        def s = symbolicString("s", "x")
        def array = new ObjectArrayValue(context, "java/lang/CharSequence", new IntValue(context, 3))
        array.storeElement(new IntValue(context, 0), s)
        array.storeElement(new IntValue(context, 1), new ObjectValue(context, 0))
        array.storeElement(new IntValue(context, 2), new StringBuilderValue(context, new StringValue(context, "y", -1), -1))

        when:
        def r = StringInvocation.invokeStaticMethod("join", [new StringValue(context, ", ", -1), array] as Value<?, ?>[],
                [Type.getType(CharSequence), Type.getType(CharSequence[])] as Type[])

        then:
        exactString([fix(s)], (StringValue) r, String.join(", ", "x", null, "y"))
    }

    @Unroll
    def "new String(char[], #offset, #count) is exact"() {
        given:
        def array = new CharArrayValue(context, "hello".toCharArray(), -1)
        def o = symbolicInt("o", offset)
        def c = symbolicInt("c", count)
        def s = new StringValue(context, "", -1)
        def args = [array, o, c]
        def b = StringCallBranches.of(s, "<init>", "([CII)V", args as Value<?, ?>[])
        boolean javaThrows = false
        try { new String("hello".toCharArray(), offset, count) } catch (IndexOutOfBoundsException ignored) { javaThrows = true }

        if (!javaThrows) call(s, "<init>", [Type.getType(char[]), INT, INT], args)

        expect:
        b[0].holds() == !javaThrows
        onlyJavasResult([fix(o), fix(c)], javaThrows ? b[0].condition() : bmgr.not(b[0].condition()))
        javaThrows || exactString([fix(o), fix(c)] + b*.condition(), s, new String("hello".toCharArray(), offset, count))

        where:
        [offset, count] << [[-1, 0, 2, 5], [-1, 0, 3, 6]].combinations()
    }

    // ---- StringBuilder.reverse, StringBuffer ----

    @Unroll
    def "reverse of a symbolic builder is exact, surrogate pairs kept in order: '#sample'"() {
        given:
        def s = symbolicString("s", sample)
        def sb = new StringBuilderValue(context, s, -1)
        def fixed = [fix(s)] + branches(sb, "reverse", "()Ljava/lang/StringBuilder;", [])

        when:
        call(sb, "reverse", [], [])

        then:
        exactString(fixed, sb.getContent(), new StringBuilder(sample).reverse().toString())

        where:
        sample << ["", "a", "abc", "a😀b", "\uDE00\uD83D", "😀😁", "\uDE00\uD83D\uD83D"]
    }

    def "a StringBuffer is modelled as a StringBuilder"() {
        given:
        def s = symbolicString("s", "ab")
        def sb = new StringBuilderValue(context, "java/lang/StringBuffer", null, -1)

        when:
        call(sb, "<init>", [STRING], [s])
        call(sb, "append", [STRING], [new StringValue(context, "!", -1)])
        call(sb, "insert", [INT, STRING], [new IntValue(context, 0), new StringValue(context, "<", -1)])
        def str = call(sb, "toString", [], [])

        then:
        sb.className == "java/lang/StringBuffer"
        sb.isSymbolic()
        exactString([fix(s)], (StringValue) str, "<ab!")
        exactString([fix(s)], (StringValue) StringInvocation.invokeStaticMethod("valueOf", [sb] as Value<?, ?>[], [Type.getType(Object)] as Type[]), "<ab!")
    }

    // ---- Character.digit / forDigit ----

    @Unroll
    def "Character.digit(char, int) with a symbolic radix is exact: #c #radix"() {
        given:
        def ch = new CharValue(context, c as char, bvmgr.makeVariable(16, "c"))
        def r = symbolicInt("r", radix)
        def fixed = [bvmgr.equal(ch.formula, bvmgr.makeBitvector(16, (int) c)), fix(r)]

        expect:
        exactInt(fixed, (IntValue) CharacterInvocation.invokeStaticMethod("digit", [ch, r] as Value<?, ?>[],
                [Type.CHAR_TYPE, INT] as Type[], null), Character.digit(c as char, radix))

        where:
        [c, radix] << [['0', '7', 'a', 'Z', 'Ａ', '٣', '-'], [-1, 0, 2, 8, 10, 16, 36, 37]].combinations()
    }

    def "Character.digit(c, r) is digit(c, 36) if that is below r, for every char and radix"() {
        expect:
        (0..0xFFFF).every { int c ->
            int d = Character.digit((char) c, 36)
            (Character.MIN_RADIX..Character.MAX_RADIX).every { int r -> Character.digit((char) c, r) == (d < r ? d : -1) }
        }
    }

    @Unroll
    def "Character.forDigit is exact: #d #radix"() {
        given:
        def digit = symbolicInt("d", d)
        def r = symbolicInt("r", radix)

        expect:
        exactChar([fix(digit), fix(r)], (CharValue) CharacterInvocation.invokeStaticMethod("forDigit", [digit, r] as Value<?, ?>[],
                [INT, INT] as Type[], null), Character.forDigit(d, radix))

        where:
        [d, radix] << [[-1, 0, 7, 9, 10, 15, 35, 36], [1, 2, 10, 16, 36, 37]].combinations()
    }
}
