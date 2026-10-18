package de.uzl.its.swat.symbolic.value.primitive.numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

/**
 * Converts Integer-sorted formulas to bit-vectors without mixing the two sorts where possible.
 *
 * <p>{@link #toBitvector} returns a formula that is equal to {@code (_ int_to_bv width) f} in every
 * model, but built over bit-vectors where {@code f} allows it:
 *
 * <ul>
 *   <li>numerals become bit-vector constants (modulo 2^width),
 *   <li>{@code ite}, {@code +}, {@code -}, {@code *} and unary minus are rebuilt over bit-vectors
 *       ({@code int_to_bv} is a ring homomorphism modulo 2^width),
 *   <li>{@code bv2int} (signed or unsigned) of a bit-vector is replaced by that bit-vector,
 *       extracted or extended to the width.
 * </ul>
 *
 * Anything else (string lengths, {@code str.to_code}, division, ...) keeps its {@code int_to_bv}.
 *
 * <p>Z3 handles {@code int_to_bv} badly: a query over {@code int_to_bv(ite(fp.lt x c, 1, -1))}
 * (a DCMP result) may not return at all, while the same query over bit-vector constants is decided
 * at once.
 */
public final class BitvectorEncoding {

    /** Deeper Integer terms keep their int_to_bv rather than risk a deep recursion. */
    private static final int MAX_DEPTH = 64;

    private BitvectorEncoding() {}

    /**
     * Returns a bit-vector of the given width equal to {@code (_ int_to_bv width) f}.
     *
     * @param fmgr the formula manager
     * @param f the Integer-sorted formula
     * @param width the bit width of the result
     * @return the bit-vector formula
     */
    public static BitvectorFormula toBitvector(FormulaManager fmgr, IntegerFormula f, int width) {
        return new Lowering(fmgr, width).lower(f, 0);
    }

    /**
     * Returns the three-way comparison result {@code eq ? 0 : (gt ? 1 : -1)} as a bit-vector of the
     * given width, as LCMP/FCMP/DCMP and the compare methods produce it.
     *
     * @param fmgr the formula manager
     * @param eq the equality condition
     * @param gt the greater-than condition
     * @param width the bit width
     * @return the bit-vector formula
     */
    public static BitvectorFormula compareResult(
            FormulaManager fmgr, BooleanFormula eq, BooleanFormula gt, int width) {
        BitvectorFormulaManager bvmgr = fmgr.getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        return bmgr.ifThenElse(
                eq,
                bvmgr.makeBitvector(width, 0),
                bmgr.ifThenElse(gt, bvmgr.makeBitvector(width, 1), bvmgr.makeBitvector(width, -1)));
    }

    /**
     * Returns {@code Double.compare(a, b)} (or {@code Float.compare}) as a 32-bit bit-vector: NaN is
     * equal to itself and greater than every other value, and -0.0 is less than 0.0.
     *
     * @param fmgr the formula manager
     * @param a the first value
     * @param b the second value
     * @return the bit-vector formula, -1, 0 or 1
     */
    public static BitvectorFormula javaCompare(FormulaManager fmgr, FloatingPointFormula a, FloatingPointFormula b) {
        BitvectorFormulaManager bvmgr = fmgr.getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        FloatingPointFormulaManager fpmgr = fmgr.getFloatingPointFormulaManager();
        BitvectorFormula minus = bvmgr.makeBitvector(32, -1);
        BitvectorFormula zero = bvmgr.makeBitvector(32, 0);
        BitvectorFormula one = bvmgr.makeBitvector(32, 1);
        BooleanFormula aNaN = fpmgr.isNaN(a);
        BooleanFormula bNaN = fpmgr.isNaN(b);
        BooleanFormula aNeg = fpmgr.isNegative(a);
        BooleanFormula bNeg = fpmgr.isNegative(b);
        // Equal in FP terms but not as bits: -0.0 and 0.0
        BitvectorFormula equal = bmgr.ifThenElse(
                bmgr.and(aNeg, bmgr.not(bNeg)),
                minus,
                bmgr.ifThenElse(bmgr.and(bNeg, bmgr.not(aNeg)), one, zero));
        BitvectorFormula ordered = bmgr.ifThenElse(
                fpmgr.lessThan(a, b), minus, bmgr.ifThenElse(fpmgr.greaterThan(a, b), one, equal));
        return bmgr.ifThenElse(
                aNaN,
                bmgr.ifThenElse(bNaN, zero, one),
                bmgr.ifThenElse(bNaN, minus, ordered));
    }

    private static final class Lowering {
        private final FormulaManager fmgr;
        private final BitvectorFormulaManager bvmgr;
        private final BooleanFormulaManager bmgr;
        private final int width;
        private final BigInteger modulus;
        private final Map<Formula, BitvectorFormula> done = new HashMap<>();

        Lowering(FormulaManager fmgr, int width) {
            this.fmgr = fmgr;
            this.bvmgr = fmgr.getBitvectorFormulaManager();
            this.bmgr = fmgr.getBooleanFormulaManager();
            this.width = width;
            this.modulus = BigInteger.ONE.shiftLeft(width);
        }

        BitvectorFormula lower(IntegerFormula f, int depth) {
            BitvectorFormula known = done.get(f);
            if (known != null) {
                return known;
            }
            BitvectorFormula result =
                    depth > MAX_DEPTH ? bvmgr.makeBitvector(width, f) : fmgr.visit(f, visitor(f, depth));
            done.put(f, result);
            return result;
        }

        private DefaultFormulaVisitor<BitvectorFormula> visitor(IntegerFormula f, int depth) {
            return new DefaultFormulaVisitor<>() {
                @Override
                protected BitvectorFormula visitDefault(Formula formula) {
                    return bvmgr.makeBitvector(width, f);
                }

                @Override
                public BitvectorFormula visitConstant(Formula formula, Object value) {
                    if (value instanceof BigInteger v) {
                        return bvmgr.makeBitvector(width, v.mod(modulus));
                    }
                    return visitDefault(formula);
                }

                @Override
                public BitvectorFormula visitFunction(
                        Formula formula, List<Formula> args, FunctionDeclaration<?> decl) {
                    switch (decl.getKind()) {
                        case ITE:
                            if (args.size() == 3
                                    && args.get(0) instanceof BooleanFormula cond
                                    && args.get(1) instanceof IntegerFormula a
                                    && args.get(2) instanceof IntegerFormula b) {
                                return bmgr.ifThenElse(cond, lower(a, depth + 1), lower(b, depth + 1));
                            }
                            break;
                        case ADD:
                        case SUB:
                        case MUL:
                            if (args.size() >= 2 && allInteger(args)) {
                                BitvectorFormula acc = lower((IntegerFormula) args.get(0), depth + 1);
                                for (Formula arg : args.subList(1, args.size())) {
                                    BitvectorFormula next = lower((IntegerFormula) arg, depth + 1);
                                    acc = switch (decl.getKind()) {
                                        case ADD -> bvmgr.add(acc, next);
                                        case SUB -> bvmgr.subtract(acc, next);
                                        default -> bvmgr.multiply(acc, next);
                                    };
                                }
                                return acc;
                            }
                            break;
                        case UMINUS:
                            if (args.size() == 1 && args.get(0) instanceof IntegerFormula a) {
                                return bvmgr.negate(lower(a, depth + 1));
                            }
                            break;
                        case UBV_TO_INT:
                        case SBV_TO_INT:
                            if (args.size() == 1 && args.get(0) instanceof BitvectorFormula bv) {
                                return resize(bv, decl.getKind() == FunctionDeclarationKind.SBV_TO_INT);
                            }
                            break;
                        default:
                            break;
                    }
                    return visitDefault(formula);
                }
            };
        }

        /** int_to_bv(bv2int(bv)) at this width: bv itself, its low bits, or bv extended. */
        private BitvectorFormula resize(BitvectorFormula bv, boolean signed) {
            int length = bvmgr.getLength(bv);
            if (length == width) {
                return bv;
            }
            if (length > width) {
                return bvmgr.extract(bv, width - 1, 0);
            }
            return bvmgr.extend(bv, width - length, signed);
        }

        private static boolean allInteger(List<Formula> args) {
            for (Formula arg : args) {
                if (!(arg instanceof IntegerFormula)) {
                    return false;
                }
            }
            return true;
        }
    }
}
