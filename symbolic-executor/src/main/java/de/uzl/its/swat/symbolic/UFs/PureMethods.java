package de.uzl.its.swat.symbolic.UFs;

import java.util.Set;
import org.objectweb.asm.Type;

/**
 * Whitelist of pure, deterministic, side-effect-free JDK methods that SWAT does NOT model, plus the
 * descriptive naming scheme for the generic uninterpreted functions that model their unmodeled
 * returns (G4). A whitelisted method's result is modeled as {@code pure_<Class>_<method>[_<argTypes>]
 * (inputs)} instead of being concretized (G2) - preserving the relational fact (equal inputs =>
 * equal outputs) soundly, since an axiom-free UF over-approximates any deterministic function.
 *
 * <p>Membership is the soundness precondition: only genuinely pure + deterministic methods may
 * appear here. Exclude locale-dependent (no-arg {@code toLowerCase}/{@code toUpperCase}),
 * environment/property readers, argument-mutating, identity/{@code intern}, and nondeterministic
 * (random/time) methods. v1 starter set is tiny and hand-audited (String returns only); it is later
 * scaled by a per-class survey of {@code java.lang}.
 */
public final class PureMethods {
    private PureMethods() {}

    /**
     * Keys are {@code owner + "/" + name + desc} (descriptor included to disambiguate overloads).
     * Entries are pure, deterministic, side-effect-free, String-RETURNING, and UNMODELED by SWAT (so
     * the generic UF actually fires). Curated from the java.lang purity survey
     * (docs/heap-redesign-g4-whitelist-survey.md); the boxed types' toString-family is intentionally
     * absent (already modeled -> a UF would never fire), and cross-class static String methods
     * (Float/Double/Character.toString) are a documented backlog pending static-invoke test support.
     */
    private static final Set<String> WHITELIST =
            Set.of(
                    "java/lang/String/trim()Ljava/lang/String;",
                    "java/lang/String/strip()Ljava/lang/String;",
                    "java/lang/String/stripLeading()Ljava/lang/String;",
                    "java/lang/String/stripTrailing()Ljava/lang/String;",
                    "java/lang/String/substring(I)Ljava/lang/String;",
                    "java/lang/String/substring(II)Ljava/lang/String;",
                    "java/lang/String/repeat(I)Ljava/lang/String;",
                    "java/lang/String/replace(CC)Ljava/lang/String;",
                    "java/lang/String/indent(I)Ljava/lang/String;");

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
