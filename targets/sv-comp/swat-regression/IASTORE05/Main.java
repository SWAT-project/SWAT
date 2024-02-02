
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

        public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        int i = Verifier.nondetInt();

        int arr[] = new int[3];
        try {
            arr[i] = 42;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Index out of bounds in Main::main");
        }
        assert arr[1] != 42;
    }
}




