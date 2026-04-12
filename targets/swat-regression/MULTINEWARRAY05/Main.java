
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void main(String[] args) {
        System.out.println("[MARKER] Starting Main");
        int i = Verifier.nondetInt();
        int j = Verifier.nondetInt();

        String val = Verifier.nondetString();

        String arr[][] = new String[3][3];
        for (int k = 0; k < 3; k++) {
            for (int l = 0; l < 3; l++) {
                arr[k][l] = "";
            }
        }
        try {
            arr[i][j] = val;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Index out of bounds in Main::main");
        }
        assert !(arr[1][2].equals("magic"));
        System.out.println("[MARKER] End Main");

    }
}




