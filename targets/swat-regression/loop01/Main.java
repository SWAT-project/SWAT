
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void test(){
        System.out.println("[MARKER] 1");
        int i = Verifier.nondetInt();
        for(int j = 0; j<i; j++){
            System.out.println("[MARKER] 2");
            if (j == 8) {
                System.out.println("[MARKER] 3");
                assert false;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        test();
    }
}




