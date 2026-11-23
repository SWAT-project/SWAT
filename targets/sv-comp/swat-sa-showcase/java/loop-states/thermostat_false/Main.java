// Expected verdict: VIOLATION. Violating inputs (nondet call order): boolean:true int:100 boolean:true int:300 boolean:false
import org.sosy_lab.sv_benchmarks.Verifier;

// The control loop of a two-state thermostat with hysteresis: it switches the heater on below
// 18.0 degrees and off above 22.0 degrees, for as long as the sensor delivers samples.
public class Main {
    static final int IDLE = 0, HEATING = 1;
    static final int ON_BELOW = 180, OFF_ABOVE = 220; // tenths of a degree

    static int shownMode; // what the front panel shows

    static boolean relay(boolean on) {
        // drives the heater's relay
        return on;
    }

    static void display(int mode) {
        shownMode = mode;
    }

    static void control() {
        int mode = IDLE;
        boolean relayOn = false;
        while (Verifier.nondetBoolean()) { // the sensor has another sample
            int temp = Verifier.nondetInt();
            if (mode == IDLE && temp < ON_BELOW) {
                relayOn = relay(true);
                mode = HEATING;
                display(mode);
            } else if (mode == HEATING && temp > OFF_ABOVE) {
                mode = IDLE;
                display(mode);
            }
        }
        assert relayOn == (mode == HEATING) : "relay and mode disagree";
    }

    public static void main(String[] args) {
        control();
    }
}
