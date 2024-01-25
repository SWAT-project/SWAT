
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        int i = Verifier.nondetInt();
        int j = Verifier.nondetInt();
        double val = Verifier.nondetDouble();

        double arr[][] = new double[3][3];
        try {
            arr[i][j] = val;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Index out of bounds in Main::main");
        }
        assert arr[1][2] != 42.42;

    }
}




