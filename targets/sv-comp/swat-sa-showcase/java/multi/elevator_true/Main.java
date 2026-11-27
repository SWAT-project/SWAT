// Expected verdict: SAFE. the door only opens while the car stands, and the car never leaves floors 0..5; the loop is unbounded, but its state at the head (floor, direction, door) takes only 36 values.
// Paired VIOLATION inputs (must not fail here): boolean:true int:3 boolean:true int:-1
import org.sosy_lab.sv_benchmarks.Verifier;

// The controller of an elevator in a six-floor building. Each event is a requested floor, or the
// door button (-1); the car moves one floor per event.
public class Main {
    static final int FLOORS = 6;
    static final int DOOR_BUTTON = -1;
    static final int DOWN = -1, STOP = 0, UP = 1;

    static int up() { return UP; }
    static int down() { return DOWN; }
    static int stop() { return STOP; }
    static boolean open() { return true; }
    static boolean close() { return false; }

    static int move(int floor, int dir) {
        int next = floor;
        if (dir == UP && floor < FLOORS - 1) next = floor + 1;
        else if (dir == DOWN && floor > 0) next = floor - 1;
        assert next >= 0 && next < FLOORS : "left the shaft";
        return next;
    }

    static void run() {
        int floor = 0;
        int dir = STOP;
        boolean doorOpen = false;
        while (Verifier.nondetBoolean()) { // another event
            int request = Verifier.nondetInt();
            if (request == DOOR_BUTTON) {
                if (dir == STOP) doorOpen = open();
            } else if (request > floor && request < FLOORS) {
                doorOpen = close();
                dir = up();
            } else if (request < floor && request >= 0) {
                doorOpen = close();
                dir = down();
            } else {
                dir = stop();
            }
            floor = move(floor, dir);
            assert !(doorOpen && dir != STOP) : "moving with the door open";
        }
    }

    public static void main(String[] args) {
        run();
    }
}
