// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:2147483600
import org.sosy_lab.sv_benchmarks.Verifier;

// Resumes an interrupted backup of a 1000-block volume from the last checkpoint: blocks before it
// are skipped (and logged), the others are copied. Afterwards the next checkpoint is scheduled.
public class Main {
    static final int BLOCKS = 1000;
    static final int CHECKPOINT_INTERVAL = 64;

    static int skipped, copied, verboseLines;
    static int[] recent = new int[16]; // the last blocks written, for the status page

    static void logSkip(int block, int resumeFrom) {
        skipped++;
        if (resumeFrom - block <= 3) verboseLines++; // the last few skipped blocks are logged in full
    }

    static void copy(int block) {
        recent[block % recent.length] = block;
        copied++;
        if (block % 100 == 0) System.out.println(block);
    }

    public static void main(String[] args) {
        int resumeFrom = Verifier.nondetInt(); // from the checkpoint file
        if (resumeFrom < 0) resumeFrom = 0;
        for (int block = 0; block < BLOCKS; block++) {
            if (block < resumeFrom) {
                logSkip(block, resumeFrom);
            } else {
                copy(block);
            }
        }
        System.out.println(copied);
        int nextCheckpoint = resumeFrom + CHECKPOINT_INTERVAL;
        assert nextCheckpoint > resumeFrom : "checkpoint counter overflow";
        System.out.println(nextCheckpoint);
    }
}
