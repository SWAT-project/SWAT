
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void test(){
        System.out.println("[MARKER] 1");
        int i = Verifier.nondetInt();
        int j = Verifier.nondetInt();
        if(i > 5){
            return;
        } else if(j > 5){
            return;
        }
            for(int k = 0; k<i; k++){
                System.out.println("[MARKER] 2");
            }
            for(int k = 0; k<j; k++){
                System.out.println("[MARKER] 3");
            }
            assert i != 3 && j != 3;
    }

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        test();
    }
}




