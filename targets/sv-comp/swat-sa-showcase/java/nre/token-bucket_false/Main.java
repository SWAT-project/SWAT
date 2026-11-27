// Expected verdict: VIOLATION (no-runtime-exception). Violating inputs (nondet call order): boolean:true int:150
import org.sosy_lab.sv_benchmarks.Verifier;

// A token-bucket rate limiter in front of an API. Each event is a timer tick (refills the bucket)
// or a request with its cost in tokens; requests larger than the bucket are capped and audited.
// It runs for as long as events arrive, then prints its counters.
public class Main {
    static final int CAPACITY = 100;
    static final int REFILL = 10;

    static class Audit {
        int oversized;
        int largest;
    }

    static class Bucket {
        int tokens = CAPACITY;
        int accepted, rejected, ticks;
        Audit audit; // was created lazily; the creation got lost in a refactoring

        void tick() {
            ticks++;
            tokens += REFILL;
            if (tokens > CAPACITY) tokens = CAPACITY;
        }

        void request(int cost) {
            if (cost > CAPACITY) {
                audit.oversized++;
                if (cost > audit.largest) audit.largest = cost;
                cost = CAPACITY;
            }
            if (cost <= tokens) {
                tokens -= cost;
                accepted++;
            } else {
                rejected++;
            }
        }
    }

    public static void main(String[] args) {
        Bucket bucket = new Bucket();
        while (Verifier.nondetBoolean()) { // another event
            int event = Verifier.nondetInt();
            if (event <= 0) {
                bucket.tick();
            } else {
                bucket.request(event);
            }
        }
        System.out.println(bucket.accepted);
        System.out.println(bucket.rejected);
        System.out.println(bucket.ticks);
    }
}
