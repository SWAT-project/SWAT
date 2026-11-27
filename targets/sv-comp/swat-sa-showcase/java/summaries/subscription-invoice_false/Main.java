// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:990
import org.sosy_lab.sv_benchmarks.Verifier;

// Computes the invoice of a 1000-day service contract. Days in the free trial cost nothing; after
// it, weekdays and weekend days have different rates. New customers get a welcome credit.
public class Main {
    static final int CONTRACT_DAYS = 1000;
    static final int WEEKDAY_CENTS = 30;
    static final int WEEKEND_CENTS = 10;
    static final int WELCOME_CREDIT_CENTS = 500;
    static final int MAX_TRIAL_WITH_CREDIT = 90;

    public static void main(String[] args) {
        int trialDays = Verifier.nondetInt(); // from the campaign the customer signed up with
        int total = 0;
        for (int day = 0; day < CONTRACT_DAYS; day++) {
            if (day >= trialDays) {
                if (day % 7 < 5) {
                    total += WEEKDAY_CENTS;
                } else {
                    total += WEEKEND_CENTS;
                }
            }
        }
        total -= WELCOME_CREDIT_CENTS;
        assert total >= 0 : "negative invoice";
    }
}
