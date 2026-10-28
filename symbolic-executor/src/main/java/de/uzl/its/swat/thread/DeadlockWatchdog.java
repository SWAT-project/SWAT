package de.uzl.its.swat.thread;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ends a run whose application threads are deadlocked.
 *
 * <p>A target can deadlock (the SV-COMP no-deadlock tasks are made to): its threads then wait
 * forever without using CPU, so neither the target nor the explorer's CPU budget ever ends the run,
 * and only the harness' wall-clock timeout kills it, before the explorer reports anything. This
 * watchdog checks twice a second whether every live application thread is BLOCKED or WAITING (not
 * TIMED_WAITING) and has used no CPU since the previous check; if that holds for {@link
 * #STUCK_MS}, no application thread can ever wake another, and the execution has ended. It then
 * reports the deadlock on stdout and ends the JVM normally, so that the agent's shutdown hook sends
 * the trace of whatever execution is still active (see Intrinsics.terminateOnShutdown).
 *
 * <p>Application threads are those of the main thread group and its subgroups, except the JVM's
 * DestroyJavaVM thread, this one and the workers of SWAT's asynchronous log appenders.
 */
public final class DeadlockWatchdog implements Runnable {

    /** How long every application thread must be stuck before the run is ended. */
    static final long STUCK_MS = 3000;
    private static final long PERIOD_MS = 500;
    /** Marker the explorer looks for in the target's output. */
    public static final String MARKER = "[SWAT] Deadlock: every application thread waits forever";

    private DeadlockWatchdog() {}

    public static void start() {
        Thread t = new Thread(new DeadlockWatchdog(), "swat-deadlock-watchdog");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        ThreadGroup main = Thread.currentThread().getThreadGroup();
        Map<Long, String> previous = null;
        long stuckSince = -1;
        while (true) {
            try {
                Thread.sleep(PERIOD_MS);
            } catch (InterruptedException e) {
                return;
            }
            Map<Long, String> state = stuckState(mx, main);
            long now = System.currentTimeMillis();
            if (state == null || !Objects.equals(state, previous)) {
                previous = state;
                stuckSince = state == null ? -1 : now;
                continue;
            }
            if (now - stuckSince >= STUCK_MS) {
                System.out.println(MARKER + ": " + state.values());
                System.out.flush();
                System.exit(0);
                return;
            }
        }
    }

    /**
     * The application threads with their state and CPU time, if every one of them waits without a
     * timeout; null if some application thread can still make progress (or there is none).
     */
    private static Map<Long, String> stuckState(ThreadMXBean mx, ThreadGroup main) {
        Map<Long, String> state = new HashMap<>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t == Thread.currentThread() || !t.isAlive() || !inGroup(t, main)
                    || t.getName().equals("DestroyJavaVM") || t.getName().startsWith("AsyncAppender-Worker")) {
                continue;
            }
            Thread.State s = t.getState();
            if (s != Thread.State.BLOCKED && s != Thread.State.WAITING) {
                return null;
            }
            long cpu = mx.isThreadCpuTimeSupported() ? mx.getThreadCpuTime(t.getId()) : -1;
            state.put(t.getId(), t.getName() + " " + s + " " + cpu);
        }
        return state.isEmpty() ? null : state;
    }

    private static boolean inGroup(Thread t, ThreadGroup group) {
        for (ThreadGroup g = t.getThreadGroup(); g != null; g = g.getParent()) {
            if (g == group) {
                return true;
            }
        }
        return false;
    }
}
