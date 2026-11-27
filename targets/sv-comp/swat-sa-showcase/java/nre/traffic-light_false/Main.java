// Expected verdict: VIOLATION (no-runtime-exception). Violating inputs (nondet call order): boolean:true int:1 boolean:true int:0 boolean:true int:0
import org.sosy_lab.sv_benchmarks.Verifier;

// The controller of a traffic light with a pedestrian phase: north-south green, yellow, east-west
// green, yellow, and a walk phase after the north-south yellow when someone pressed the button.
// The phases form a cycle of objects, each with its duration and its successor.
public class Main {
    static final int NS_GREEN = 0, NS_YELLOW = 1, EW_GREEN = 2, EW_YELLOW = 3, WALK = 4;

    static class Phase {
        final int id;
        final int seconds;
        Phase next;

        Phase(int id, int seconds) {
            this.id = id;
            this.seconds = seconds;
        }
    }

    static int waited;

    static boolean request() { return true; }
    static boolean served() { return false; }

    static void hold(int seconds) {
        waited = seconds;
    }

    static void run() {
        Phase nsGreen = new Phase(NS_GREEN, 30);
        Phase nsYellow = new Phase(NS_YELLOW, 5);
        Phase ewGreen = new Phase(EW_GREEN, 40);
        Phase ewYellow = new Phase(EW_YELLOW, 5);
        Phase walkPhase = new Phase(WALK, 20);
        nsGreen.next = nsYellow;
        nsYellow.next = ewGreen;
        ewGreen.next = ewYellow;
        ewYellow.next = nsGreen; // the walk phase (added later) has no successor yet

        Phase phase = nsGreen;
        boolean walk = false;
        while (Verifier.nondetBoolean()) { // the next tick of the timer
            int button = Verifier.nondetInt(); // the pedestrian button's sensor reading
            if (button > 0) walk = request();
            hold(phase.seconds);
            if (phase.id == WALK) {
                walk = served();
                phase = phase.next;
            } else if (phase.id == NS_YELLOW && walk) {
                phase = walkPhase;
            } else {
                phase = phase.next;
            }
            hold(phase.seconds);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
