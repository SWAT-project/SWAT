import de.uzl.its.swat.annotations.Symbolic;

public class Example2 {
    @Symbolic String s = "Random string";


    public static void main(String[] args) {
        String result = new Example2().test();
        System.out.println(result);
    }

    public String test() {
        return s.equals("Hello world") ? "Path 1" : "Path 2";
    }
}

