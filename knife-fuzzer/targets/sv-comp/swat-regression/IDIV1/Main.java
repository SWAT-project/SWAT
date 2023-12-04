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

    public static boolean test() {
        System.out.println("========= Marker 0 ");
        int y = Verifier.nondetInt();
        try{
            y = 10 / y;
        } catch (Exception e){
            System.out.println("========= Marker 1 ");
            y = 0;
        }
        if(y != 42){
            System.out.println("========= Marker 2 ");
            return true;
        }else{
            System.out.println("========= Marker 3 ");
            assert false;
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("========= Starting Main");
        test();
    }
}




