// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:0 int:900 int:3 int:61
import org.sosy_lab.sv_benchmarks.Verifier;

// The monitoring agent of one sensor. In report mode it classifies the last 8 logged readings for
// the daily histogram. Otherwise it classifies the current reading and, if it is critical,
// escalates the sensor's open alarm.
public class Main {
    static final int LOW = 0, NORMAL = 1, HIGH = 2;
    static final int REPORT = 1;
    static final int HISTORY = 8;
    static final int MAX_LEVEL = 3; // 0: logged, 1: on-call engineer, 2: team lead, 3: site manager

    static int[] histogram = new int[3];

    static int classify(int reading) {
        if (reading < 50) return LOW;
        if (reading < 800) return NORMAL;
        return HIGH;
    }

    static void report() {
        for (int i = 0; i < HISTORY; i++) {
            int reading = Verifier.nondetInt(); // from the sensor's log
            histogram[classify(reading)]++;
        }
        System.out.println(histogram[LOW]);
        System.out.println(histogram[NORMAL]);
        System.out.println(histogram[HIGH]);
    }

    static int notified;

    // Raises the alarm's level the longer nobody acknowledges it.
    static int escalate(int level, int unackedMinutes) {
        if (level < 0 || level > MAX_LEVEL) return -1; // corrupt alarm record
        if (unackedMinutes > 15 && level < MAX_LEVEL) level++;
        if (unackedMinutes > 60) level++;
        assert level <= MAX_LEVEL : "escalated past the top level";
        notified = level;
        return level;
    }

    public static void main(String[] args) {
        int mode = Verifier.nondetInt();
        if (mode == REPORT) {
            report();
            return;
        }
        int current = Verifier.nondetInt();
        if (classify(current) == HIGH) {
            int level = Verifier.nondetInt(); // the open alarm's level, from the alarm store
            int unackedMinutes = Verifier.nondetInt();
            System.out.println(escalate(level, unackedMinutes));
        }
    }
}
