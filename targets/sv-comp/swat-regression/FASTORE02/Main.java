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

    public static void test() throws Exception{
        System.out.println("[MARKER] 1");
        int i = Verifier.nondetInt();
        float f = Verifier.nondetFloat();
        float fa[] = new float[10];
        fa[i] = f;
        assert fa[5] != 42.0f;
    }

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        try {
            test();
        } catch (Exception e) {
            System.out.println("[MARKER] 2");
        }
    }
}




