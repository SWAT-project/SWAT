package de.uzl.its.swat.staticcheck;

import java.util.*;

/**
 * The library (JDK) methods {@link ExceptionFreedomCheck} lets a program call.
 *
 * <p>Every method listed here throws no RuntimeException for any arguments that meet its
 * condition, and never calls back into application code: it calls no method of an argument or
 * receiver that an application class could override. A call to any other library method fails the
 * check. Entries are matched by owner, name and descriptor as written in the call instruction.
 */
final class JdkModel {

    enum Condition {
        NONE,
        /** Every reference argument is non-null. */
        NONNULL_ARGS,
        /**
         * Every reference argument is null or of a final library type whose toString() is one of the
         * listed safe methods (the method converts its argument with String.valueOf).
         */
        STRING_ARGS
    }

    enum Returns {
        /** Nothing known beyond the declared return type. */
        DECLARED,
        /** A non-null object of exactly the declared return type. */
        NONNULL_EXACT,
        /** The receiver (StringBuilder.append). */
        RECEIVER,
        /** A copy of the receiver array (same runtime type and length). */
        CLONE
    }

    static final class Entry {
        final Condition condition;
        final Returns returns;
        /** The receiver must be exactly of the owner class: subclasses could override the method. */
        final boolean exactReceiver;

        Entry(Condition condition, Returns returns, boolean exactReceiver) {
            this.condition = condition;
            this.returns = returns;
            this.exactReceiver = exactReceiver;
        }
    }

    /** Final classes: no subclass can override their methods. */
    private static final Set<String> FINAL_CLASSES = Set.of(
            "java/lang/String", "java/lang/StringBuilder", "java/lang/StringBuffer", "java/lang/Integer",
            "java/lang/Long", "java/lang/Short", "java/lang/Byte", "java/lang/Character", "java/lang/Boolean",
            "java/lang/Double", "java/lang/Float", "java/lang/Math", "java/lang/StrictMath", "java/lang/System",
            "java/lang/Class");

    /** Library types whose toString() neither throws nor calls application code. */
    static final Set<String> SAFE_TO_STRING = Set.of(
            "java/lang/String", "java/lang/Integer", "java/lang/Long", "java/lang/Short", "java/lang/Byte",
            "java/lang/Character", "java/lang/Boolean", "java/lang/Double", "java/lang/Float",
            "java/lang/StringBuilder", "java/lang/StringBuffer");

    /** Math/StrictMath methods that never throw, for every overload. */
    private static final Set<String> MATH = Set.of(
            "abs", "max", "min", "sqrt", "cbrt", "pow", "exp", "expm1", "log", "log10", "log1p", "sin", "cos",
            "tan", "asin", "acos", "atan", "atan2", "sinh", "cosh", "tanh", "hypot", "ceil", "floor", "rint",
            "round", "signum", "toRadians", "toDegrees", "ulp", "copySign", "getExponent", "nextUp", "nextDown",
            "nextAfter", "scalb", "IEEEremainder", "random", "fma");

    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    /** Methods declared by java/lang/Object and java/lang/Enum, the library superclasses we know. */
    private static final Map<String, Set<String>> DECLARED = new HashMap<>();

    /** Known library supertypes, for assignability. */
    private static final Map<String, List<String>> SUPERTYPES = new HashMap<>();

    private static void add(String owner, Condition c, Returns r, String... methods) {
        for (String m : methods) ENTRIES.put(owner + "." + m, new Entry(c, r, false));
    }

    private static void addExact(String owner, Condition c, Returns r, String... methods) {
        for (String m : methods) ENTRIES.put(owner + "." + m, new Entry(c, r, true));
    }

    static {
        Condition none = Condition.NONE, nonNull = Condition.NONNULL_ARGS, str = Condition.STRING_ARGS;
        Returns decl = Returns.DECLARED, fresh = Returns.NONNULL_EXACT, self = Returns.RECEIVER;

        DECLARED.put("java/lang/Object", Set.of("<init>()V", "getClass()Ljava/lang/Class;", "hashCode()I",
                "equals(Ljava/lang/Object;)Z", "clone()Ljava/lang/Object;", "toString()Ljava/lang/String;",
                "notify()V", "notifyAll()V", "wait()V", "wait(J)V", "wait(JI)V", "finalize()V"));
        DECLARED.put("java/lang/Enum", Set.of("<init>(Ljava/lang/String;I)V", "name()Ljava/lang/String;",
                "ordinal()I", "toString()Ljava/lang/String;", "equals(Ljava/lang/Object;)Z", "hashCode()I",
                "clone()Ljava/lang/Object;", "compareTo(Ljava/lang/Enum;)I", "getDeclaringClass()Ljava/lang/Class;",
                "finalize()V"));

        SUPERTYPES.put("java/lang/String", List.of("java/lang/Object", "java/io/Serializable",
                "java/lang/Comparable", "java/lang/CharSequence"));
        SUPERTYPES.put("java/lang/Enum", List.of("java/lang/Object", "java/io/Serializable", "java/lang/Comparable"));
        for (String n : List.of("Integer", "Long", "Short", "Byte", "Double", "Float"))
            SUPERTYPES.put("java/lang/" + n, List.of("java/lang/Number", "java/lang/Comparable"));
        SUPERTYPES.put("java/lang/Number", List.of("java/lang/Object", "java/io/Serializable"));
        SUPERTYPES.put("java/lang/Character", List.of("java/lang/Object", "java/io/Serializable", "java/lang/Comparable"));
        SUPERTYPES.put("java/lang/Boolean", List.of("java/lang/Object", "java/io/Serializable", "java/lang/Comparable"));
        SUPERTYPES.put("java/lang/AssertionError", List.of("java/lang/Error"));
        SUPERTYPES.put("java/lang/Error", List.of("java/lang/Throwable"));
        SUPERTYPES.put("java/lang/Exception", List.of("java/lang/Throwable"));
        SUPERTYPES.put("java/lang/Throwable", List.of("java/lang/Object", "java/io/Serializable"));

        // Object and Enum: the implementations an application class may inherit. The final methods
        // are safe for every receiver; the others only as the implementation a class inherits.
        add("java/lang/Object", none, decl, "<init>()V", "hashCode()I", "equals(Ljava/lang/Object;)Z");
        add("java/lang/Object", none, Returns.DECLARED, "getClass()Ljava/lang/Class;");
        add("java/lang/Enum", none, decl, "<init>(Ljava/lang/String;I)V", "name()Ljava/lang/String;", "ordinal()I",
                "toString()Ljava/lang/String;", "equals(Ljava/lang/Object;)Z", "hashCode()I",
                "getDeclaringClass()Ljava/lang/Class;");

        add("java/lang/Class", none, decl, "desiredAssertionStatus()Z");

        // Throwables: constructing one is safe as long as the object is exactly the library class (a
        // subclass could override fillInStackTrace). Throwing it is checked separately.
        addExact("java/lang/AssertionError", none, decl, "<init>()V", "<init>(Z)V", "<init>(C)V", "<init>(I)V",
                "<init>(J)V", "<init>(F)V", "<init>(D)V", "<init>(Ljava/lang/String;Ljava/lang/Throwable;)V");
        addExact("java/lang/AssertionError", str, decl, "<init>(Ljava/lang/Object;)V");
        for (String t : List.of("java/lang/Error", "java/lang/Exception", "java/lang/RuntimeException"))
            addExact(t, none, decl, "<init>()V", "<init>(Ljava/lang/String;)V");

        // Boxes and their static helpers (no parsing: that throws NumberFormatException).
        for (String[] b : new String[][] {{"Integer", "I"}, {"Long", "J"}, {"Short", "S"}, {"Byte", "B"}}) {
            String o = "java/lang/" + b[0], p = b[1];
            add(o, none, fresh, "valueOf(" + p + ")L" + o + ";", "toString(" + p + ")Ljava/lang/String;",
                    "toString()Ljava/lang/String;");
            add(o, none, decl, "compare(" + p + p + ")I", "hashCode(" + p + ")I", "hashCode()I",
                    "equals(Ljava/lang/Object;)Z", "intValue()I", "longValue()J", "shortValue()S", "byteValue()B",
                    "floatValue()F", "doubleValue()D");
            add(o, nonNull, decl, "compareTo(L" + o + ";)I");
        }
        for (String o : List.of("java/lang/Integer", "java/lang/Long")) {
            String p = o.endsWith("Integer") ? "I" : "J";
            add(o, none, decl, "signum(" + p + ")I", "bitCount(" + p + ")I", "reverse(" + p + ")" + p,
                    "reverseBytes(" + p + ")" + p, "highestOneBit(" + p + ")" + p, "lowestOneBit(" + p + ")" + p,
                    "numberOfLeadingZeros(" + p + ")I", "numberOfTrailingZeros(" + p + ")I",
                    "rotateLeft(" + p + "I)" + p, "rotateRight(" + p + "I)" + p, "sum(" + p + p + ")" + p,
                    "max(" + p + p + ")" + p, "min(" + p + p + ")" + p, "compareUnsigned(" + p + p + ")I");
            add(o, none, fresh, "toString(" + p + "I)Ljava/lang/String;", "toBinaryString(" + p + ")Ljava/lang/String;",
                    "toHexString(" + p + ")Ljava/lang/String;", "toOctalString(" + p + ")Ljava/lang/String;");
        }
        add("java/lang/Integer", none, decl, "toUnsignedLong(I)J");
        for (String[] b : new String[][] {{"Double", "D", "J"}, {"Float", "F", "I"}}) {
            String o = "java/lang/" + b[0], p = b[1], bits = b[2];
            String toBits = p.equals("D") ? "doubleToLongBits" : "floatToIntBits";
            String toRaw = p.equals("D") ? "doubleToRawLongBits" : "floatToRawIntBits";
            String fromBits = p.equals("D") ? "longBitsToDouble" : "intBitsToFloat";
            add(o, none, fresh, "valueOf(" + p + ")L" + o + ";", "toString(" + p + ")Ljava/lang/String;",
                    "toHexString(" + p + ")Ljava/lang/String;", "toString()Ljava/lang/String;");
            add(o, none, decl, "isNaN(" + p + ")Z", "isInfinite(" + p + ")Z", "isFinite(" + p + ")Z",
                    "compare(" + p + p + ")I", toBits + "(" + p + ")" + bits, toRaw + "(" + p + ")" + bits,
                    fromBits + "(" + bits + ")" + p, "hashCode(" + p + ")I", "sum(" + p + p + ")" + p,
                    "max(" + p + p + ")" + p, "min(" + p + p + ")" + p, "isNaN()Z", "isInfinite()Z", "hashCode()I",
                    "equals(Ljava/lang/Object;)Z", "intValue()I", "longValue()J", "shortValue()S", "byteValue()B",
                    "floatValue()F", "doubleValue()D");
            add(o, nonNull, decl, "compareTo(L" + o + ";)I");
        }
        add("java/lang/Boolean", none, fresh, "valueOf(Z)Ljava/lang/Boolean;", "toString(Z)Ljava/lang/String;",
                "toString()Ljava/lang/String;");
        add("java/lang/Boolean", none, decl, "booleanValue()Z", "compare(ZZ)I", "hashCode(Z)I", "hashCode()I",
                "equals(Ljava/lang/Object;)Z", "logicalAnd(ZZ)Z", "logicalOr(ZZ)Z", "logicalXor(ZZ)Z",
                "parseBoolean(Ljava/lang/String;)Z");
        add("java/lang/Character", none, fresh, "valueOf(C)Ljava/lang/Character;", "toString(C)Ljava/lang/String;",
                "toString()Ljava/lang/String;");
        add("java/lang/Character", none, decl, "charValue()C", "compare(CC)I", "hashCode(C)I", "hashCode()I",
                "equals(Ljava/lang/Object;)Z", "digit(CI)I", "digit(II)I", "forDigit(II)C", "getNumericValue(C)I",
                "getNumericValue(I)I");
        for (String pred : List.of("isDigit", "isLetter", "isLetterOrDigit", "isWhitespace", "isSpaceChar",
                "isUpperCase", "isLowerCase", "isAlphabetic", "isJavaIdentifierStart", "isJavaIdentifierPart",
                "isISOControl", "isTitleCase", "isDefined")) {
            add("java/lang/Character", none, decl, pred + "(C)Z", pred + "(I)Z");
        }
        add("java/lang/Character", none, decl, "toUpperCase(C)C", "toLowerCase(C)C", "toUpperCase(I)I",
                "toLowerCase(I)I");

        // String (final): methods that neither index nor parse.
        add("java/lang/String", none, decl, "length()I", "isEmpty()Z", "hashCode()I", "equals(Ljava/lang/Object;)Z",
                "equalsIgnoreCase(Ljava/lang/String;)Z", "indexOf(I)I", "indexOf(II)I", "lastIndexOf(I)I",
                "lastIndexOf(II)I");
        add("java/lang/String", none, fresh, "toString()Ljava/lang/String;", "intern()Ljava/lang/String;",
                "trim()Ljava/lang/String;", "toLowerCase()Ljava/lang/String;", "toUpperCase()Ljava/lang/String;",
                "replace(CC)Ljava/lang/String;", "valueOf(I)Ljava/lang/String;", "valueOf(J)Ljava/lang/String;",
                "valueOf(F)Ljava/lang/String;", "valueOf(D)Ljava/lang/String;", "valueOf(Z)Ljava/lang/String;",
                "valueOf(C)Ljava/lang/String;", "<init>()V");
        add("java/lang/String", nonNull, decl, "compareTo(Ljava/lang/String;)I",
                "compareToIgnoreCase(Ljava/lang/String;)I", "startsWith(Ljava/lang/String;)Z",
                "startsWith(Ljava/lang/String;I)Z", "endsWith(Ljava/lang/String;)Z", "indexOf(Ljava/lang/String;)I",
                "indexOf(Ljava/lang/String;I)I", "lastIndexOf(Ljava/lang/String;)I", "<init>(Ljava/lang/String;)V",
                "<init>([C)V");
        add("java/lang/String", nonNull, fresh, "concat(Ljava/lang/String;)Ljava/lang/String;",
                "valueOf([C)Ljava/lang/String;");
        add("java/lang/String", none, Returns.NONNULL_EXACT, "toCharArray()[C");
        add("java/lang/String", str, fresh, "valueOf(Ljava/lang/Object;)Ljava/lang/String;");

        for (String sb : List.of("java/lang/StringBuilder", "java/lang/StringBuffer")) {
            String l = "L" + sb + ";";
            add(sb, none, decl, "<init>()V", "length()I");
            add(sb, nonNull, decl, "<init>(Ljava/lang/String;)V");
            add(sb, none, self, "append(Z)" + l, "append(C)" + l, "append(I)" + l, "append(J)" + l,
                    "append(F)" + l, "append(D)" + l, "append(Ljava/lang/String;)" + l, "reverse()" + l);
            add(sb, str, self, "append(Ljava/lang/Object;)" + l, "append(Ljava/lang/CharSequence;)" + l);
            add(sb, nonNull, self, "append([C)" + l);
            add(sb, none, fresh, "toString()Ljava/lang/String;");
        }

        // System.out / System.err (exactly java.io.PrintStream, see FactInterpreter).
        String ps = "java/io/PrintStream";
        addExact(ps, none, decl, "println()V", "println(Z)V", "println(C)V", "println(I)V", "println(J)V",
                "println(F)V", "println(D)V", "println(Ljava/lang/String;)V", "print(Z)V", "print(C)V", "print(I)V",
                "print(J)V", "print(F)V", "print(D)V", "print(Ljava/lang/String;)V", "flush()V");
        addExact(ps, str, decl, "println(Ljava/lang/Object;)V", "print(Ljava/lang/Object;)V");
        addExact(ps, nonNull, decl, "println([C)V", "print([C)V");

        add("java/lang/System", none, decl, "currentTimeMillis()J", "nanoTime()J",
                "identityHashCode(Ljava/lang/Object;)I", "exit(I)V");
        add("java/lang/System", none, fresh, "lineSeparator()Ljava/lang/String;");
    }

    /** The model of a library method, or null if calls to it fail the check. */
    static Entry lookup(String owner, String name, String desc) {
        if (owner.equals("java/lang/Math") || owner.equals("java/lang/StrictMath")) {
            return MATH.contains(name) ? new Entry(Condition.NONE, Returns.DECLARED, false) : null;
        }
        if (owner.startsWith("[") && name.equals("clone") && desc.equals("()Ljava/lang/Object;")) {
            return new Entry(Condition.NONE, Returns.CLONE, false);
        }
        return ENTRIES.get(owner + "." + name + desc);
    }

    /** No class can override this library method: its class is final, or the method is. */
    static boolean cannotBeOverridden(String owner, String name, String desc) {
        if (FINAL_CLASSES.contains(owner) || owner.startsWith("[")) return true;
        if (owner.equals("java/lang/Object")) return name.equals("getClass") || name.equals("<init>");
        if (owner.equals("java/lang/Enum")) {
            return !name.equals("toString");
        }
        return name.equals("<init>");
    }

    /** Whether we know every method the class (with its library superclasses) declares. */
    static boolean knowsMethodsOf(String owner) {
        return DECLARED.containsKey(owner);
    }

    /** Whether the library class or one of its library superclasses declares the method. */
    static boolean declares(String owner, String name, String desc) {
        return declaringClass(owner, name, desc) != null;
    }

    /** The class among the known library classes that declares the method an instance of owner inherits. */
    static String declaringClass(String owner, String name, String desc) {
        for (String c = owner; c != null; c = c.equals("java/lang/Enum") ? "java/lang/Object" : null) {
            Set<String> d = DECLARED.get(c);
            if (d != null && d.contains(name + desc)) return c;
        }
        return null;
    }

    static List<String> knownSupertypes(String type) {
        return SUPERTYPES.getOrDefault(type, List.of("java/lang/Object"));
    }

    static boolean isFinalClass(String owner) {
        return FINAL_CLASSES.contains(owner);
    }
}
