package de.uzl.its.swat.instrument;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.coverage.InstrCoverage;
import de.uzl.its.swat.metadata.ClassDepot;

import java.util.*;

/** An object to keep track of (classId, methodId, instructionId) tuples during instrumentation. */
public class GlobalStateForInstrumentation {
    public static GlobalStateForInstrumentation instance = new GlobalStateForInstrumentation();
    private boolean activeInstrumentation = false;
    private static long invokeId = 0;

    private long iid = 0;
    private long mid = 0;
    private long cid = 0;

    // Getting the id returns the result of all three ids merged into one.
    // NOTE
    // ToDo (Flo): Be aware of truncation errors. -> Changed to long: "quick fix"

    // keep track of seen instruction‐IDs to detect duplicates
    private final Set<Long> seenInstructionIds = new HashSet<>();

    // Be aware, long is signed, thus we do not use the most significant bit
    private static final int BIT_LEN_LONG = 64;
    // private static final int EMPTY_LEADING_BITS = 10;
    private static final int FOR_BITS = 6;
    private static final int SWITCH_BITS = 4;
    private static final int CBITS = 19; // CID occupies the upper 20 bits
    private static final int MBITS = 11; // MID occupies the next 11 bits
    private static final int IBITS = BIT_LEN_LONG - FOR_BITS - SWITCH_BITS - CBITS - MBITS;

    private static final int MAX_SWITCH_CASES = (1 << SWITCH_BITS) - 1;
    private static final int MAX_FOR_ITERATIONS = (1 << FOR_BITS) - 1;

    /** Increment iid and get the complete id */
    public long incAndGetId() {
        iid++;
        long id = getId();
        registerInstructionId(id);
        InstrCoverage.numInstructions += 1;
        return id;
    }

    /** Thread‐safe registration; throws on duplicate */
    private synchronized void registerInstructionId(long id) {
        if (!seenInstructionIds.add(id)) {
            throw new IllegalStateException(
                    "Duplicate instruction ID detected! id=" + id +
                            "  (thread=" + Thread.currentThread().getName() + ")");
        }
    }

    public long incAndGetInvokeId() {
        invokeId++;
        return invokeId;
    }

    public synchronized void setActiveInstrumentation(boolean active) {
        SWATAssert.enforce(active != activeInstrumentation,
                "Global instrumentation state cannot handle parallel instrumentation.");
        this.activeInstrumentation = active;
    }

    /**
     * Getter for the iid
     *
     * @return iid
     */
    public long getId() {
        return (cid << (BIT_LEN_LONG - FOR_BITS - SWITCH_BITS - CBITS))
                + (mid << (BIT_LEN_LONG - FOR_BITS - SWITCH_BITS - CBITS - MBITS))
                + iid;
    }


    public static long extractCid(long iid) {
        long mask = (1L << CBITS) - 1;
        long shift = (IBITS + MBITS);

        return (iid >> shift) & mask;
    }

    public static long extractMid(long iid) {
        long mask = (1L << MBITS) - 1;
        long shift = IBITS;
        return (iid >> shift) & mask;
    }


    public void setMid(long mid) {
        this.mid = mid;
        this.iid = 0;
    }

    public void setCid(long cid) {
        if (cid >= ClassDepot.getRuntimeInstance().getClassCounter()) {
            System.out.println("ERROR: Setting cid larger than classCounter!");
            System.out.println("cid: " + cid);
            System.out.println("classCounter: " + ClassDepot.getRuntimeInstance().getClassCounter());
            new ErrorHandler().raiseException("Cannot set cid larger than classCounter!");
        }

        this.iid = 0;
        this.mid = 0;
        this.cid = cid;
    }

    /**
     * Creates a unique instruction ID for switch case values using the 9 empty leading bits.
     * This ensures that each switch case gets a unique IID while preserving the bit format
     * for proper class ID extraction.
     *
     * @param iid The original instruction ID
     * @param caseValue The switch case value
     * @param minValue The minimum value of the switch (inst.min)
     * @return A unique instruction ID for this switch case
     */
    public static long createSwitchCaseIid(long iid, int caseValue, int minValue) {
        long caseIndex = caseValue - minValue; // Zero-based index
        if (caseIndex > MAX_SWITCH_CASES) {
            // Handle overflow - shouldn't happen in normal switch statements
            // but protect against huge switch tables
            SWATAssert.check(true,
                    "Counter overflow for  switch instruction with iid {}: current count is {}, but should be less than "
                            + (MAX_SWITCH_CASES + 1) + ".",
                    iid, caseIndex);
            caseIndex = caseIndex % (MAX_SWITCH_CASES + 1);
        }
        // Use the 9 empty leading bits to store the case index
        return iid | (caseIndex << (BIT_LEN_LONG - FOR_BITS - SWITCH_BITS));
    }

    public static long createLoopIid(long iid, int currentCnt) {
        // Use the 9 empty leading bits for the execution counter
        // Maximum counter value: 2^9 - 1 = 511
        if (currentCnt > MAX_FOR_ITERATIONS) {
            // Handle overflow - could log warning or wrap around
            SWATAssert.check(true,
                    "Counter overflow for iid {}: current count is {}, but should be less than "
                            + (MAX_FOR_ITERATIONS + 1) + "." ,
                    iid, currentCnt);
            currentCnt = currentCnt % (MAX_FOR_ITERATIONS + 1);
        }

        // Pack the counter into the leading 9 bits
        return iid | (((long)currentCnt) << (BIT_LEN_LONG - FOR_BITS));
    }
}
