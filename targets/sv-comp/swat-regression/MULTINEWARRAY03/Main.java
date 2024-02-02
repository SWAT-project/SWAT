
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        int i = Verifier.nondetInt();
        int j = Verifier.nondetInt();
        int k = Verifier.nondetInt();

        boolean val = Verifier.nondetBoolean();

        boolean arr[][][] = new boolean[3][3][3];
        try {
            arr[i][j][k] = val;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Index out of bounds in Main::main");
        }
        assert arr[1][2][1] != true;
        System.out.println("[MARKER] End Main");

    }
}




