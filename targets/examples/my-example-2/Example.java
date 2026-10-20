import de.uzl.its.swat.annotations.Symbolic;

public class Example {

    public static void main(String[] args) {
        test(5, 6);
    }

    public static void test(@Symbolic int val, @Symbolic int distract) {
        if (distract > 0) {
            int sum = 0;
            for (int i = 0; i < 10; i++) {
                if (distract > i) {
                    sum += i;
                }
            }
            System.out.println(sum);
        } else {
            assert val != 3141592;
        }
    }
}
