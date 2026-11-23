// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:0 int:160 int:2000 int:1000000 int:1
import org.sosy_lab.sv_benchmarks.Verifier;

// Payroll for one employee: computes the monthly payslip (mode PAY) or prints the audit report of
// the last nine months (mode AUDIT). The payslip checks its own figures before it goes to the bank.
public class Main {
    static final int PAY = 0, AUDIT = 1;
    static final int UNDER = 0, REGULAR = 1, OVERTIME = 2;
    static final int MAX_HOURS = 250;
    static final int MIN_RATE = 1300, MAX_RATE = 20000; // cents per hour
    static final int BONUS_CAP = 50000; // cents
    static final int REGULAR_HOURS = 173;

    static int classify(int hours) {
        if (hours < 120) return UNDER;
        if (hours <= REGULAR_HOURS) return REGULAR;
        return OVERTIME;
    }

    static void audit() {
        int[] months = new int[3];
        for (int m = 0; m < 9; m++) {
            months[classify(Verifier.nondetInt())]++; // hours worked, from the time sheets
        }
        System.out.println(months[UNDER]);
        System.out.println(months[REGULAR]);
        System.out.println(months[OVERTIME]);
    }

    static int taxPercent(int gross) {
        if (gross < 100000) return 0;
        if (gross < 400000) return 20;
        return 35;
    }

    static int transferred;

    static void pay(int hours, int rate, int salesCents, int referrals) {
        if (hours < 0 || hours > MAX_HOURS) {
            System.out.println("hours out of range");
            return;
        }
        if (rate < MIN_RATE || rate > MAX_RATE) {
            System.out.println("rate out of range");
            return;
        }
        int base = hours * rate;
        int overtime = 0;
        if (hours > REGULAR_HOURS) overtime = (hours - REGULAR_HOURS) * rate / 2;
        int bonus = 0;
        if (salesCents > 0 && salesCents < 100000000) bonus = salesCents / 20; // 5 percent commission
        if (bonus > BONUS_CAP) bonus = BONUS_CAP;
        if (referrals > 0 && referrals <= 10) bonus += referrals * 5000; // referral bonus, added later
        int gross = base + overtime + bonus;
        int tax = gross / 100 * taxPercent(gross);
        int net = gross - tax;
        assert gross >= base : "negative supplements";
        assert tax <= gross : "tax above gross";
        assert net >= 0 : "negative payout";
        assert bonus <= BONUS_CAP : "bonus above the cap";
        transferred = net;
    }

    public static void main(String[] args) {
        int mode = Verifier.nondetInt();
        if (mode == AUDIT) {
            audit();
            return;
        }
        int hours = Verifier.nondetInt();
        int rate = Verifier.nondetInt();
        int salesCents = Verifier.nondetInt();
        int referrals = Verifier.nondetInt();
        pay(hours, rate, salesCents, referrals);
    }
}
