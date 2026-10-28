import de.uzl.its.swat.annotations.Symbolic;

public class Example {

    public static void main(String[] args) {
        test(5, 6);
    }

    public static void test(@Symbolic int val, @Symbolic int distract) {
        if (distract > 0) {
            for (int i = 0; i < 100; i++) {
                if (i < val) {
                    System.out.println(i);
                }
            }
        } else {
            assert val != 3141592;
        }
    }
}
