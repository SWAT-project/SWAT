package de.uzl.its.swat.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/* Code generated with ChatGPT5 */
public final class AbortTimer {
    public enum State { IDLE, RUNNING, PAUSED, EXPIRED }

    private final ReentrantLock lock = new ReentrantLock();
    private final long initialNanos;

    private long remainingNanos;  // valid when PAUSED/IDLE; when RUNNING derived from deadline
    private long deadlineNano;    // valid when RUNNING
    private State state = State.IDLE;

    public AbortTimer(long duration, TimeUnit unit) {
        if (duration <= 0) throw new IllegalArgumentException("duration must be > 0");
        this.initialNanos = unit.toNanos(duration);
        this.remainingNanos = this.initialNanos;
    }

    /** Start the countdown if not already running. Uses current remaining time. */
    public void start() {
        lock.lock();
        try {
            if (state == State.RUNNING) return;
            if (state == State.EXPIRED && remainingNanos == 0) return; // already expired and not reset
            long now = System.nanoTime();
            deadlineNano = now + remainingNanos;
            state = State.RUNNING;
        } finally { lock.unlock(); }
    }

    /** Pause the timer (freeze remaining). No-op if not running. */
    public void stop() {
        lock.lock();
        try {
            tickLocked(); // update state if it already expired
            if (state == State.RUNNING) {
                long now = System.nanoTime();
                remainingNanos = Math.max(0L, deadlineNano - now);
                state = (remainingNanos == 0L) ? State.EXPIRED : State.PAUSED;
            }
        } finally { lock.unlock(); }
    }

    /** Resume after stop() from the frozen remaining time. */
    public void restart() {
        lock.lock();
        try {
            if (state != State.PAUSED) return;
            long now = System.nanoTime();
            deadlineNano = now + remainingNanos;
            state = State.RUNNING;
        } finally { lock.unlock(); }
    }

    /** Reset to the original duration and go back to IDLE (not running). */
    public void reset() {
        lock.lock();
        try {
            remainingNanos = initialNanos;
            state = State.IDLE;
        } finally { lock.unlock(); }
    }

    /** Hard reset and immediately start. */
    public void resetAndStart() {
        lock.lock();
        try {
            remainingNanos = initialNanos;
            long now = System.nanoTime();
            deadlineNano = now + remainingNanos;
            state = State.RUNNING;
        } finally { lock.unlock(); }
    }

    /** Returns true if the countdown has reached zero. */
    public boolean isExpired() {
        lock.lock();
        try {
            tickLocked();
            return state == State.EXPIRED;
        } finally { lock.unlock(); }
    }

    /** Current state (IDLE/RUNNING/PAUSED/EXPIRED). */
    public State state() {
        lock.lock();
        try {
            tickLocked();
            return state;
        } finally { lock.unlock(); }
    }

    /** Remaining time in the requested unit (0 if expired). */
    public long remaining(TimeUnit unit) {
        lock.lock();
        try {
            tickLocked();
            long nanos;
            if (state == State.RUNNING) {
                long now = System.nanoTime();
                nanos = Math.max(0L, deadlineNano - now);
            } else if (state == State.PAUSED || state == State.IDLE) {
                nanos = remainingNanos;
            } else {
                nanos = 0L;
            }
            return unit.convert(nanos, TimeUnit.NANOSECONDS);
        } finally { lock.unlock(); }
    }

    // Update state to EXPIRED if a running timer has passed its deadline.
    private void tickLocked() {
        if (state == State.RUNNING && System.nanoTime() >= deadlineNano) {
            remainingNanos = 0L;
            state = State.EXPIRED;
        }
    }
}

