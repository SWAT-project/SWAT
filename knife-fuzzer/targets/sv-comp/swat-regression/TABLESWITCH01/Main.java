
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void test(){
        int i = Verifier.nondetInt();
        switch (i){
            case 0:
                System.out.println("0");
                break;
            case 1: System.out.println("1");
                break;
            case 3 :
                System.out.println(2);
                assert false;
                break;
            default:
                System.out.println("default");
                break;
        }
    }

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        test();
    }
}




