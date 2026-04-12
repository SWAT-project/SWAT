
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void test() throws Exception{
        System.out.println("[MARKER] 1");
        int i = Verifier.nondetInt();
        float fa[] = new float[10];
        fa[i] = 42.0f;
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




