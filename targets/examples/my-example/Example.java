import de.uzl.its.swat.annotations.Symbolic;

public class Example {

    public static void main(String[] args) {
        test(5);
    }

    public static void test(@Symbolic int val) {
        int sum = 0;
        // for (int i = 0; i < 100; i++) {
        //     if (val > i) {
        //         sum += i;
        //     }
        // }
        for (int i = 0; i < 100; i += 10) {
            if (val > i) {
                sum += i;
            }
            if (val > i + 1) {
                sum += i + 1;
            }
            if (val > i + 2) {
                sum += i + 2;
            }
            if (val > i + 3) {
                sum += i + 3;
            }
            if (val > i + 4) {
                sum += i + 4;
            }
            if (val > i + 5) {
                sum += i + 5;
            }
            if (val > i + 6) {
                sum += i + 6;
            }
            if (val > i + 7) {
                sum += i + 7;
            }
            if (val > i + 8) {
                sum += i + 8;
            }
            if (val > i + 9) {
                sum += i + 9;
            }
        }
        assert val != 3141592;
    }
}
