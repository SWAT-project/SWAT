// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:361
import org.sosy_lab.sv_benchmarks.Verifier;

// Builds the repayment schedule of a consumer loan: one fixed instalment per month over the term
// the customer chose. Every month is recorded for the yearly statement.
public class Main {
    static final int INSTALMENT = 250; // euros
    static final int MAX_TERM_MONTHS = 480; // raised from 360 for the 40-year product;
    static final int CREDIT_LINE = 90000; // the most the bank lends to one customer

    static int recorded;
    static int[] lastYear = new int[12];

    static void record(int month) {
        recorded++;
        lastYear[month % 12] = month;
    }

    public static void main(String[] args) {
        int term = Verifier.nondetInt(); // months, from the application form
        if (term < 1 || term > MAX_TERM_MONTHS) {
            System.out.println("term out of range");
            return;
        }
        int month = 0;
        int repaid = 0;
        while (month < term) {
            month++;
            repaid += INSTALMENT;
            record(month);
        }
        assert repaid <= CREDIT_LINE : "schedule exceeds the credit line";
    }
}
