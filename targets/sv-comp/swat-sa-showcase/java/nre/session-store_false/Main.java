// Expected verdict: VIOLATION (no-runtime-exception). Violating inputs (nondet call order): int:0 int:8 int:-5
import org.sosy_lab.sv_benchmarks.Verifier;

// An in-memory session store behind a command interface: one command (op code and an argument)
// per request. Sessions are kept in a fixed table indexed by the session id.
public class Main {
    static final int OPEN = 0, TOUCH = 1, STATS = 2, CONFIG = 3, PURGE = 4, PING = 5, AUDIT = 6,
            EXPORT = 7, LOOKUP = 8;

    static int[] slots;
    static int hits, misses, opened, purged, audited, exported, ttl = 300;
    static boolean verbose;

    static void log(String what) {
        if (verbose) System.out.println(what);
    }

    static int slotOf(int sessionId) {
        return sessionId % slots.length;
    }

    static void open(int sessionId) {
        slots[slotOf(sessionId)] = sessionId;
        opened++;
        log("opened");
    }

    static void touch(int age) {
        if (age < 0) { log("clock skew"); return; }
        if (age > ttl) log("expired");
        if (age > ttl / 2) log("renewing");
        if (age == 0) log("fresh");
        if (age > 86400) log("stale for a day");
        hits++;
    }

    static void stats(int detail) {
        if (detail > 0) System.out.println(hits);
        if (detail > 1) System.out.println(misses);
        if (detail > 2) System.out.println(opened);
        if (detail > 3) System.out.println(purged);
        if (detail < 0) log("negative detail");
    }

    static void config(int value) {
        if (value == 1) verbose = true;
        if (value == 2) verbose = false;
        if (value >= 60 && value <= 86400) ttl = value;
        if (value > 86400) log("ttl too long");
        if (value < 0) log("negative ttl");
    }

    static void purge(int olderThan) {
        if (olderThan < 0) return;
        if (olderThan < ttl) log("purging live sessions");
        if (olderThan > 3600) log("long horizon");
        if (olderThan % 60 != 0) log("not a whole minute");
        if (olderThan == 0) log("purging everything");
        purged++;
    }

    static void ping(int token) {
        if (token == 42) log("pong");
        if (token < 0) log("negative token");
        if (token > 1000) log("large token");
        if (token % 2 == 0) log("even");
    }

    static void audit(int level) {
        if (level > 0) log("audit 1");
        if (level > 1) log("audit 2");
        if (level > 2) log("audit 3");
        if (level == 7) log("full audit");
        audited++;
    }

    static void export(int format) {
        if (format < 0 || format > 3) { log("unknown format"); return; }
        if (format == 0) log("csv");
        if (format == 1) log("json");
        if (format == 2) log("xml");
        if (opened > 100) log("large export");
        exported++;
    }

    static void lookup(int sessionId) {
        if (slots[slotOf(sessionId)] == sessionId) {
            hits++;
            log("hit");
        } else {
            misses++;
            log("miss");
        }
    }

    public static void main(String[] args) {
        int capacity = Verifier.nondetInt();
        if (capacity < 1 || capacity > 1024) capacity = 64;
        slots = new int[capacity];
        int op = Verifier.nondetInt();
        int arg = Verifier.nondetInt();
        if (op == OPEN) open(arg);
        else if (op == TOUCH) touch(arg);
        else if (op == STATS) stats(arg);
        else if (op == CONFIG) config(arg);
        else if (op == PURGE) purge(arg);
        else if (op == PING) ping(arg);
        else if (op == AUDIT) audit(arg);
        else if (op == EXPORT) export(arg);
        else if (op == LOOKUP) lookup(arg);
    }
}
