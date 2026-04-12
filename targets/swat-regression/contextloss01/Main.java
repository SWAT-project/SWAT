/* Copyright, TU Dortmund 2020 Malte Mues
 * contributed-by: Malte Mues (mail.mues@gmail.com)
 *
 * This benchmark task is a modificaiton of the following original Benchmark:
 * Origin of the benchmark:
 *     license: MIT (see /java/jayhorn-recursive/LICENSE)
 *     repo: https://github.com/jayhorn/cav_experiments.git
 *     branch: master
 *     root directory: benchmarks/recursive
 * The benchmark was taken from the repo: 24 January 2018
 *
 * Following the original license model, modifications are as well licensed  under the
 * MIT license.
 */

import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static boolean test(int x) {
        return 0 == Integer.highestOneBit(x);
    }

    public static void main(String[] args) {

        int x = Verifier.nondetInt();
        assert test(x);
    }
}




