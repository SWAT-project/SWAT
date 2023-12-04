
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {

    public static void main(String[] args) {

        int x = 3;
        int y = 5;
        int[][] int_array = new int[x][y];

        for (int i = 0; i < x; ++i) for (int j = 0; j < y; ++j) int_array[i][j] = i + j;

        assert int_array[2][4] == 6;
    }
}




