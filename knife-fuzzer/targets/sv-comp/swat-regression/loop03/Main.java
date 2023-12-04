
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void test(){
        System.out.println("[MARKER] 1");
        int i = Verifier.nondetInt();
        int j = Verifier.nondetInt();
        if(i > 5){
            return;
        } else if(j > 7){
            return;
        }
        int kcnt = 0;
        int lcnt = 0;
        for(int k = 0; k<i; ++k){
            System.out.println("[MARKER] 2");
            kcnt++;
            for(int l = 0; l<j; ++l){
                System.out.println("[MARKER] 3");
                lcnt++;
            }
        }
        assert i != 3 && j != 4;
    }

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        test();
    }
}




