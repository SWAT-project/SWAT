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

    public static boolean test(int x, float f, String s) {
        System.out.println("========= Marker 0 ");
        int y = Verifier.nondetInt();
        int z = (int) x + y / 2;
        if (z == (int) f){
            System.out.println("========= Marker 1 ");
            byte b = Verifier.nondetByte();
            if (s.equals("foo") && b == 42) {
                System.out.println("========= Marker 2 ");
                assert false;
                return false;
            }
        }

        return true;
    }

    public static void main(String[] args) {
        System.out.println("========= Starting Main");
        int x = Verifier.nondetInt();
        float y = Verifier.nondetFloat();
        String s = Verifier.nondetString();
        assert (test(x, y, s));
    }
}




