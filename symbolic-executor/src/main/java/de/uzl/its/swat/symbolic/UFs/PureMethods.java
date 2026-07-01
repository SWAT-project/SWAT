package de.uzl.its.swat.symbolic.UFs;

import java.util.Set;
import org.objectweb.asm.Type;

/**
 * Whitelist of pure, deterministic, side-effect-free JDK methods that SWAT does NOT model, plus the
 * descriptive naming scheme for the generic uninterpreted functions that model their returns. A
 * whitelisted method's result is modeled as {@code pure_<Class>_<method>[_<argTypes>](inputs)}
 * instead of being concretized - preserving the relational fact (equal inputs => equal outputs)
 * soundly, since an axiom-free UF over-approximates any deterministic function.
 *
 * <p>Membership is a soundness precondition: only genuinely pure and deterministic methods may
 * appear here. Exclude locale-dependent (no-arg {@code toLowerCase}/{@code toUpperCase}),
 * environment/property readers, argument-mutating, identity/{@code intern}, and nondeterministic
 * (random/time) methods. String and all primitive return types are supported (the UF's return sort
 * is the method's return type); the method must also be UNMODELED by SWAT, otherwise the UF never
 * fires.
 */
public final class PureMethods {
    private PureMethods() {}

    /**
     * Keys are {@code owner + "/" + name + desc} (descriptor included to disambiguate overloads).
     * Every entry is pure, deterministic, side-effect-free, and UNMODELED by SWAT - absent from its
     * Invocation handler, or present only as a StringValue stub - so the generic UF actually fires.
     * The boxed types' toString-family is intentionally absent (already modeled, so a UF would never
     * fire). Covers String and primitive returns across
     * Math/StrictMath/Character/Integer/Byte/Float/Double/Objects and the String methods.
     */
    private static final Set<String> WHITELIST =
            Set.of(
                    // String/primitive returns on String - UF-firing (StringValue stubs), pure + locale-independent:
                    "java/lang/String/codePointAt(I)I",
                    "java/lang/String/codePointBefore(I)I",
                    "java/lang/String/codePointCount(II)I",
                    "java/lang/String/compareTo(Ljava/lang/String;)I",
                    "java/lang/String/compareToIgnoreCase(Ljava/lang/String;)I",
                    "java/lang/String/hashCode()I",
                    "java/lang/String/indent(I)Ljava/lang/String;",
                    "java/lang/String/isBlank()Z",
                    "java/lang/String/isEmpty()Z",
                    "java/lang/String/lastIndexOf(I)I",
                    "java/lang/String/lastIndexOf(II)I",
                    "java/lang/String/lastIndexOf(Ljava/lang/String;)I",
                    "java/lang/String/lastIndexOf(Ljava/lang/String;I)I",
                    "java/lang/String/matches(Ljava/lang/String;)Z",
                    "java/lang/String/offsetByCodePoints(II)I",
                    "java/lang/String/regionMatches(ILjava/lang/String;II)Z",
                    "java/lang/String/regionMatches(ZILjava/lang/String;II)Z",
                    "java/lang/String/repeat(I)Ljava/lang/String;",
                    "java/lang/String/replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                    "java/lang/String/replaceFirst(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                    "java/lang/String/strip()Ljava/lang/String;",
                    "java/lang/String/stripIndent()Ljava/lang/String;",
                    "java/lang/String/stripLeading()Ljava/lang/String;",
                    "java/lang/String/stripTrailing()Ljava/lang/String;",
                    "java/lang/String/translateEscapes()Ljava/lang/String;",
                    "java/lang/String/trim()Ljava/lang/String;",
                    // Pure, unmodeled, value-typed methods from the java.lang/util purity audit:
                    "java/lang/Math/IEEEremainder(DD)D",
                    "java/lang/Math/absExact(I)I",
                    "java/lang/Math/absExact(J)J",
                    "java/lang/Math/acos(D)D",
                    "java/lang/Math/addExact(II)I",
                    "java/lang/Math/addExact(JJ)J",
                    "java/lang/Math/asin(D)D",
                    "java/lang/Math/atan(D)D",
                    "java/lang/Math/atan2(DD)D",
                    "java/lang/Math/cbrt(D)D",
                    "java/lang/Math/ceil(D)D",
                    "java/lang/Math/copySign(DD)D",
                    "java/lang/Math/copySign(FF)F",
                    "java/lang/Math/cosh(D)D",
                    "java/lang/Math/decrementExact(I)I",
                    "java/lang/Math/decrementExact(J)J",
                    "java/lang/Math/exp(D)D",
                    "java/lang/Math/expm1(D)D",
                    "java/lang/Math/floor(D)D",
                    "java/lang/Math/floorDiv(II)I",
                    "java/lang/Math/floorDiv(JI)J",
                    "java/lang/Math/floorDiv(JJ)J",
                    "java/lang/Math/floorMod(II)I",
                    "java/lang/Math/floorMod(JI)I",
                    "java/lang/Math/floorMod(JJ)J",
                    "java/lang/Math/fma(DDD)D",
                    "java/lang/Math/fma(FFF)F",
                    "java/lang/Math/getExponent(D)I",
                    "java/lang/Math/getExponent(F)I",
                    "java/lang/Math/hypot(DD)D",
                    "java/lang/Math/incrementExact(I)I",
                    "java/lang/Math/incrementExact(J)J",
                    "java/lang/Math/log(D)D",
                    "java/lang/Math/log10(D)D",
                    "java/lang/Math/log1p(D)D",
                    "java/lang/Math/multiplyExact(II)I",
                    "java/lang/Math/multiplyExact(JI)J",
                    "java/lang/Math/multiplyExact(JJ)J",
                    "java/lang/Math/multiplyFull(II)J",
                    "java/lang/Math/multiplyHigh(JJ)J",
                    "java/lang/Math/negateExact(I)I",
                    "java/lang/Math/negateExact(J)J",
                    "java/lang/Math/nextAfter(DD)D",
                    "java/lang/Math/nextAfter(FD)F",
                    "java/lang/Math/nextDown(D)D",
                    "java/lang/Math/nextDown(F)F",
                    "java/lang/Math/nextUp(D)D",
                    "java/lang/Math/nextUp(F)F",
                    "java/lang/Math/pow(DD)D",
                    "java/lang/Math/rint(D)D",
                    "java/lang/Math/scalb(DI)D",
                    "java/lang/Math/scalb(FI)F",
                    "java/lang/Math/signum(D)D",
                    "java/lang/Math/signum(F)F",
                    "java/lang/Math/sinh(D)D",
                    "java/lang/Math/subtractExact(II)I",
                    "java/lang/Math/subtractExact(JJ)J",
                    "java/lang/Math/tan(D)D",
                    "java/lang/Math/tanh(D)D",
                    "java/lang/Math/toDegrees(D)D",
                    "java/lang/Math/toIntExact(J)I",
                    "java/lang/Math/toRadians(D)D",
                    "java/lang/Math/ulp(D)D",
                    "java/lang/Math/ulp(F)F",
                    "java/lang/StrictMath/IEEEremainder(DD)D",
                    "java/lang/StrictMath/absExact(I)I",
                    "java/lang/StrictMath/absExact(J)J",
                    "java/lang/StrictMath/acos(D)D",
                    "java/lang/StrictMath/addExact(II)I",
                    "java/lang/StrictMath/addExact(JJ)J",
                    "java/lang/StrictMath/asin(D)D",
                    "java/lang/StrictMath/atan(D)D",
                    "java/lang/StrictMath/atan2(DD)D",
                    "java/lang/StrictMath/cbrt(D)D",
                    "java/lang/StrictMath/ceil(D)D",
                    "java/lang/StrictMath/copySign(DD)D",
                    "java/lang/StrictMath/copySign(FF)F",
                    "java/lang/StrictMath/cosh(D)D",
                    "java/lang/StrictMath/decrementExact(I)I",
                    "java/lang/StrictMath/decrementExact(J)J",
                    "java/lang/StrictMath/exp(D)D",
                    "java/lang/StrictMath/expm1(D)D",
                    "java/lang/StrictMath/floor(D)D",
                    "java/lang/StrictMath/floorDiv(II)I",
                    "java/lang/StrictMath/floorDiv(JI)J",
                    "java/lang/StrictMath/floorDiv(JJ)J",
                    "java/lang/StrictMath/floorMod(II)I",
                    "java/lang/StrictMath/floorMod(JI)I",
                    "java/lang/StrictMath/floorMod(JJ)J",
                    "java/lang/StrictMath/fma(DDD)D",
                    "java/lang/StrictMath/fma(FFF)F",
                    "java/lang/StrictMath/getExponent(D)I",
                    "java/lang/StrictMath/getExponent(F)I",
                    "java/lang/StrictMath/hypot(DD)D",
                    "java/lang/StrictMath/incrementExact(I)I",
                    "java/lang/StrictMath/incrementExact(J)J",
                    "java/lang/StrictMath/log(D)D",
                    "java/lang/StrictMath/log10(D)D",
                    "java/lang/StrictMath/log1p(D)D",
                    "java/lang/StrictMath/multiplyExact(II)I",
                    "java/lang/StrictMath/multiplyExact(JI)J",
                    "java/lang/StrictMath/multiplyExact(JJ)J",
                    "java/lang/StrictMath/multiplyFull(II)J",
                    "java/lang/StrictMath/multiplyHigh(JJ)J",
                    "java/lang/StrictMath/negateExact(I)I",
                    "java/lang/StrictMath/negateExact(J)J",
                    "java/lang/StrictMath/nextAfter(DD)D",
                    "java/lang/StrictMath/nextAfter(FD)F",
                    "java/lang/StrictMath/nextDown(D)D",
                    "java/lang/StrictMath/nextDown(F)F",
                    "java/lang/StrictMath/nextUp(D)D",
                    "java/lang/StrictMath/nextUp(F)F",
                    "java/lang/StrictMath/pow(DD)D",
                    "java/lang/StrictMath/rint(D)D",
                    "java/lang/StrictMath/scalb(DI)D",
                    "java/lang/StrictMath/scalb(FI)F",
                    "java/lang/StrictMath/signum(D)D",
                    "java/lang/StrictMath/signum(F)F",
                    "java/lang/StrictMath/sinh(D)D",
                    "java/lang/StrictMath/subtractExact(II)I",
                    "java/lang/StrictMath/subtractExact(JJ)J",
                    "java/lang/StrictMath/tan(D)D",
                    "java/lang/StrictMath/tanh(D)D",
                    "java/lang/StrictMath/toDegrees(D)D",
                    "java/lang/StrictMath/toIntExact(J)I",
                    "java/lang/StrictMath/toRadians(D)D",
                    "java/lang/StrictMath/ulp(D)D",
                    "java/lang/StrictMath/ulp(F)F",
                    "java/lang/Character/digit(CI)I",
                    "java/lang/Character/digit(II)I",
                    "java/lang/Character/forDigit(II)C",
                    "java/lang/Character/getDirectionality(C)B",
                    "java/lang/Character/getDirectionality(I)B",
                    "java/lang/Character/getNumericValue(C)I",
                    "java/lang/Character/getNumericValue(I)I",
                    "java/lang/Character/getType(C)I",
                    "java/lang/Character/getType(I)I",
                    "java/lang/Character/hashCode(C)I",
                    "java/lang/Character/highSurrogate(I)C",
                    "java/lang/Character/isAlphabetic(I)Z",
                    "java/lang/Character/isDefined(C)Z",
                    "java/lang/Character/isDefined(I)Z",
                    "java/lang/Character/isDigit(C)Z",
                    "java/lang/Character/isDigit(I)Z",
                    "java/lang/Character/isHighSurrogate(C)Z",
                    "java/lang/Character/isISOControl(C)Z",
                    "java/lang/Character/isISOControl(I)Z",
                    "java/lang/Character/isIdentifierIgnorable(C)Z",
                    "java/lang/Character/isIdentifierIgnorable(I)Z",
                    "java/lang/Character/isIdeographic(I)Z",
                    "java/lang/Character/isJavaIdentifierPart(C)Z",
                    "java/lang/Character/isJavaIdentifierPart(I)Z",
                    "java/lang/Character/isJavaIdentifierStart(C)Z",
                    "java/lang/Character/isJavaIdentifierStart(I)Z",
                    "java/lang/Character/isJavaLetter(C)Z",
                    "java/lang/Character/isJavaLetterOrDigit(C)Z",
                    "java/lang/Character/isLetter(C)Z",
                    "java/lang/Character/isLetter(I)Z",
                    "java/lang/Character/isLetterOrDigit(C)Z",
                    "java/lang/Character/isLetterOrDigit(I)Z",
                    "java/lang/Character/isLowSurrogate(C)Z",
                    "java/lang/Character/isLowerCase(C)Z",
                    "java/lang/Character/isLowerCase(I)Z",
                    "java/lang/Character/isMirrored(C)Z",
                    "java/lang/Character/isMirrored(I)Z",
                    "java/lang/Character/isSpace(C)Z",
                    "java/lang/Character/isSpaceChar(C)Z",
                    "java/lang/Character/isSpaceChar(I)Z",
                    "java/lang/Character/isSurrogate(C)Z",
                    "java/lang/Character/isSurrogatePair(CC)Z",
                    "java/lang/Character/isTitleCase(C)Z",
                    "java/lang/Character/isTitleCase(I)Z",
                    "java/lang/Character/isUnicodeIdentifierPart(C)Z",
                    "java/lang/Character/isUnicodeIdentifierPart(I)Z",
                    "java/lang/Character/isUnicodeIdentifierStart(C)Z",
                    "java/lang/Character/isUnicodeIdentifierStart(I)Z",
                    "java/lang/Character/isUpperCase(C)Z",
                    "java/lang/Character/isUpperCase(I)Z",
                    "java/lang/Character/isWhitespace(C)Z",
                    "java/lang/Character/isWhitespace(I)Z",
                    "java/lang/Character/lowSurrogate(I)C",
                    "java/lang/Character/reverseBytes(C)C",
                    "java/lang/Character/toLowerCase(C)C",
                    "java/lang/Character/toLowerCase(I)I",
                    "java/lang/Character/toString(C)Ljava/lang/String;",
                    "java/lang/Character/toString(I)Ljava/lang/String;",
                    "java/lang/Character/toTitleCase(C)C",
                    "java/lang/Character/toTitleCase(I)I",
                    "java/lang/Character/toUpperCase(C)C",
                    "java/lang/Character/toUpperCase(I)I",
                    "java/lang/Integer/bitCount(I)I",
                    "java/lang/Byte/compareUnsigned(BB)I",
                    "java/lang/Byte/hashCode(B)I",
                    "java/lang/Float/hashCode(F)I",
                    "java/lang/Float/intBitsToFloat(I)F",
                    "java/lang/Float/parseFloat(Ljava/lang/String;)F",
                    "java/lang/Float/toHexString(F)Ljava/lang/String;",
                    "java/lang/Float/toString(F)Ljava/lang/String;",
                    "java/lang/Double/doubleToLongBits(D)J",
                    "java/lang/Double/doubleToRawLongBits(D)J",
                    "java/lang/Double/hashCode(D)I",
                    "java/lang/Double/longBitsToDouble(J)D",
                    "java/lang/Double/max(DD)D",
                    "java/lang/Double/min(DD)D",
                    "java/lang/Double/parseDouble(Ljava/lang/String;)D",
                    "java/lang/Double/sum(DD)D",
                    "java/lang/Double/toHexString(D)Ljava/lang/String;",
                    "java/lang/Double/toString(D)Ljava/lang/String;",
                    "java/util/Objects/checkFromIndexSize(III)I",
                    "java/util/Objects/checkFromIndexSize(JJJ)J",
                    "java/util/Objects/checkFromToIndex(III)I",
                    "java/util/Objects/checkFromToIndex(JJJ)J",
                    "java/util/Objects/checkIndex(II)I",
                    "java/util/Objects/checkIndex(JJ)J");

    public static boolean isWhitelisted(String owner, String name, String desc) {
        return WHITELIST.contains(owner + "/" + name + desc);
    }

    /**
     * Descriptive, SMT-safe UF name {@code pure_<SimpleClass>_<method>[_<argSimpleTypes>]}, e.g.
     * {@code pure_String_trim}, {@code pure_String_substring_int_int}. The {@code pure_} prefix is the
     * precision-loss exemption's recognizer; arg types disambiguate overloads.
     */
    public static String ufName(String owner, String name, String desc) {
        StringBuilder sb = new StringBuilder("pure_");
        sb.append(simpleName(owner)).append('_').append(name);
        for (Type t : Type.getArgumentTypes(desc)) {
            sb.append('_').append(simpleTypeName(t));
        }
        return sb.toString();
    }

    private static String simpleName(String internalOwner) {
        int slash = internalOwner.lastIndexOf('/');
        return slash >= 0 ? internalOwner.substring(slash + 1) : internalOwner;
    }

    private static String simpleTypeName(Type t) {
        String cn = t.getClassName().replace("[]", "Array");
        int dot = cn.lastIndexOf('.');
        return dot >= 0 ? cn.substring(dot + 1) : cn;
    }
}
