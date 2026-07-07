import de.uzl.its.swat.annotations.Symbolic;

public class Example {

    public static void main(String[] args) {
        test(5, 6);
    }

    public static void test(@Symbolic int val, @Symbolic int distract) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            if (distract > i) {
                sum += i;
            }
        }
        assert val != 3141592;
    }
}
// for (int i = 0; i < 100; i += 10) {
//     if (distract > i) {
//         sum += i;
//     }
//     if (distract > i + 1) {
//         sum += i + 1;
//     }
//     if (distract > i + 2) {
//         sum += i + 2;
//     }
//     if (distract > i + 3) {
//         sum += i + 3;
//     }
//     if (distract > i + 4) {
//         sum += i + 4;
//     }
//     if (distract > i + 5) {
//         sum += i + 5;
//     }
//     if (distract > i + 6) {
//         sum += i + 6;
//     }
//     if (distract > i + 7) {
//         sum += i + 7;
//     }
//     if (distract > i + 8) {
//         sum += i + 8;
//     }
//     if (distract > i + 9) {
//         sum += i + 9;
//     }
// }
