// Expected verdict: SAFE. the goodwill credit is only granted to meters installed on the first day; then at least 576 slots are measured, at 24 cents or more each. The other asserts hold because billingDays is validated.
// Paired VIOLATION inputs (must not fail here): int:665 int:1
import org.sosy_lab.sv_benchmarks.Verifier;

// The weekly electricity bill from a smart meter's 15-minute slots (7 days, 672 slots). Slots
// before the meter was installed are not charged; day-shift slots (8:00-16:00) cost more. A daily
// standing charge is added, and late installations get a goodwill credit.
public class Main {
    static final int SLOTS = 672;
    static final int SLOTS_PER_DAY = 96;
    static final int SLOTS_PER_SHIFT = 32, DAY_SHIFT = 1; // three 8-hour shifts per day
    static final int PEAK_CENTS = 45, OFF_PEAK_CENTS = 24; // per slot
    static final int STANDING_CENTS = 40; // per day
    static final int MAX_DAYS = 31;
    static final int GOODWILL_CENTS = 400;

    public static void main(String[] args) {
        int installSlot = Verifier.nondetInt();
        int billingDays = Verifier.nondetInt();
        if (billingDays < 1 || billingDays > MAX_DAYS) billingDays = 7;

        int peak = 0;
        int offPeak = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slot >= installSlot) {
                if (slot % SLOTS_PER_DAY / SLOTS_PER_SHIFT == DAY_SHIFT) {
                    peak++;
                } else {
                    offPeak++;
                }
            }
        }
        int day = 0;
        int standing = 0;
        while (day < billingDays) {
            day++;
            standing += STANDING_CENTS;
        }
        int bill = peak * PEAK_CENTS + offPeak * OFF_PEAK_CENTS + standing;
        if (installSlot > 0 && installSlot <= SLOTS_PER_DAY) bill -= GOODWILL_CENTS;
        assert peak + offPeak <= SLOTS : "more slots than in a week";
        assert standing <= MAX_DAYS * STANDING_CENTS : "standing charge too high";
        assert bill >= 0 : "negative bill";
    }
}
