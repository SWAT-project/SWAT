package de.uzl.its.swat.symbolic.invoke.java.lang

import java.util.function.DoubleUnaryOperator
import spock.lang.Specification

class SampledMathSpec extends Specification {

    private static Set<Long> bits(double[] xs) {
        xs.collect { Double.doubleToRawLongBits(it) } as Set
    }

    def "the samples include the edges of the double and integral ranges"() {
        when:
        double[] samples = SampledMath.samples({ x -> Math.sin(x) } as DoubleUnaryOperator,
                { t -> Math.asin(t) } as DoubleUnaryOperator)

        then:
        bits(samples).containsAll(bits([Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE,
                                        Double.MAX_VALUE, -Double.MAX_VALUE, Double.MIN_VALUE,
                                        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN] as double[]))
        bits(samples).size() == samples.length  // each sample once
    }

    def "a power that overflows only at an extreme exponent is in the table"() {
        // Math.pow(-log(0.5), (int) (1 / beta)) for a tiny negative beta (sv-benchmarks
        // WeibullGenerator_exceptionless): the int cast gives Integer.MIN_VALUE, the power +Infinity.
        given:
        double base = -Math.log(1.0 - 0.5)
        DoubleUnaryOperator g = { v -> Math.pow(base, v) } as DoubleUnaryOperator
        DoubleUnaryOperator inverse = { t -> Math.log(t) / Math.log(base) } as DoubleUnaryOperator

        when:
        double[] samples = SampledMath.samples(g, inverse)

        then:
        samples.any { g.applyAsDouble(it) == Double.POSITIVE_INFINITY }
        samples.any { g.applyAsDouble(it) == 0.0d }
    }
}
