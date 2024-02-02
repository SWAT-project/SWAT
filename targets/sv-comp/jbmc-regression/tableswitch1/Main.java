/*
 * Origin of the benchmark:
 *     license: 4-clause BSD (see /java/jbmc-regression/LICENSE)
 *     repo: https://github.com/diffblue/cbmc.git
 *     branch: develop
 *     directory: regression/cbmc-java/tableswitch1
 * The benchmark was taken from the repo: 24 January 2018
 */
import org.sosy_lab.sv_benchmarks.Verifier;

class Main {
  public static void main(String[] args) {
    int i, j;

    i = Verifier.nondetInt();

    switch (i) {
      case -1:
        j = 0;
        break;
      case 0:
        j = 1;
        break;
      case 1:
        j = 2;
        break;
      case 2:
        j = 3;
        break;
      case 3:
        j = 4;
        break;
      case 4:
        j = 5;
        break;
      case 5:
        j = 6;
        break;
      case 6:
        j = 7;
        break;
      case 7:
        j = 8;
        break;
      case 8:
        j = 9;
        break;
      case 9:
        j = 10;
        break;
      case 10:
        j = 11;
        break;
      case 11:
        j = 12;
        break;
      default:
        j = 1000;
    }

    if (i >= -1 && i <= 11) assert j == i + 1;
    else assert j == 1000;
  }
}
