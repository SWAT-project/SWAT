import de.uzl.its.swat.annotations.Symbolic;

public class Example1 {

    public static void main(String[] args) {
        String result = test("Random string");
        System.out.println(result);
    }

    public static String test(@Symbolic String s) {
        if (s.equals("Hello world")) {
            return "Path 1";
        } else {
            return "Path 2";
        }
    }
}
