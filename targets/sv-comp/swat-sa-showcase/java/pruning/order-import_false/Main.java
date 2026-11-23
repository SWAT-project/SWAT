// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:0 int:5 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:1
import org.sosy_lab.sv_benchmarks.Verifier;

// Imports one order line from a partner's feed. Each field is a code into one of the shop's tables;
// a bad code fails the lookup (index out of range) or the instalment split (division by zero). The
// importer then logs the bad field and skips the line. A line that imports cleanly is booked.
public class Main {
    static final int[] PRICE_CENTS = {199, 349, 1299, 89, 2499, 599, 4999, 129};
    static final int[] DISCOUNT_PCT = {0, 5, 10, 15, 20, 120}; // the last one: spring campaign
    static final int[] TAX_PCT = {0, 7, 19};
    static final int[] SHIPPING_CENTS = {0, 495, 995, 1995};
    static final int[] CARRIER_SURCHARGE_CENTS = {0, 150, 300};
    static final int[] WAREHOUSE_DAYS = {1, 2, 2, 5};
    static final int[] PACKAGING_CENTS = {0, 99, 249};
    static final int[] GIFT_WRAP_CENTS = {0, 299};
    static final int[] PRIORITY_DAYS_SAVED = {0, 1, 2};
    static final int[] REGION_DAYS = {0, 1, 3, 7};
    static final int[] INSURANCE_CENTS = {0, 120, 480};

    static final int F_ITEM = 0, F_DISCOUNT = 1, F_TAX = 2, F_ZONE = 3, F_CARRIER = 4, F_WAREHOUSE = 5,
            F_PACKAGING = 6, F_GIFT = 7, F_PRIORITY = 8, F_REGION = 9, F_INSURANCE = 10, F_INSTALMENTS = 11;

    static int[] rejectedByField = new int[12];
    static int negativeCodes, hugeCodes, unknownCodes;

    // Logs a field that failed to import.
    static void reject(int field, int code) {
        rejectedByField[field]++;
        if (code < 0) negativeCodes++;
        else if (code > 1000) hugeCodes++;
        else unknownCodes++;
        System.out.println(field);
    }

    public static void main(String[] args) {
        int item = Verifier.nondetInt();
        int discountCode = Verifier.nondetInt();
        int taxClass = Verifier.nondetInt();
        int zone = Verifier.nondetInt();
        int carrier = Verifier.nondetInt();
        int warehouse = Verifier.nondetInt();
        int packaging = Verifier.nondetInt();
        int giftWrap = Verifier.nondetInt();
        int priority = Verifier.nondetInt();
        int region = Verifier.nondetInt();
        int insurance = Verifier.nondetInt();
        int instalments = Verifier.nondetInt();

        int price, discount, tax, shipping, surcharge, days, pack, wrap, saved, regionDays, insure;
        try { price = PRICE_CENTS[item]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_ITEM, item); return; }
        try { discount = DISCOUNT_PCT[discountCode]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_DISCOUNT, discountCode); return; }
        try { tax = TAX_PCT[taxClass]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_TAX, taxClass); return; }
        try { shipping = SHIPPING_CENTS[zone]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_ZONE, zone); return; }
        try { surcharge = CARRIER_SURCHARGE_CENTS[carrier]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_CARRIER, carrier); return; }
        try { days = WAREHOUSE_DAYS[warehouse]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_WAREHOUSE, warehouse); return; }
        try { pack = PACKAGING_CENTS[packaging]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_PACKAGING, packaging); return; }
        try { wrap = GIFT_WRAP_CENTS[giftWrap]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_GIFT, giftWrap); return; }
        try { saved = PRIORITY_DAYS_SAVED[priority]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_PRIORITY, priority); return; }
        try { regionDays = REGION_DAYS[region]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_REGION, region); return; }
        try { insure = INSURANCE_CENTS[insurance]; }
        catch (ArrayIndexOutOfBoundsException e) { reject(F_INSURANCE, insurance); return; }

        int unit = price - price * discount / 100;
        int gross = unit + unit * tax / 100 + shipping + surcharge + pack + wrap + insure;
        int perInstalment;
        try { perInstalment = gross / instalments; }
        catch (ArithmeticException e) { reject(F_INSTALMENTS, instalments); return; }
        book(unit, perInstalment, days + regionDays - saved);
    }

    static int booked;

    // Books the line. A negative unit price would turn the order into a payout.
    static void book(int unit, int perInstalment, int deliveryDays) {
        assert unit >= 0 : "negative unit price";
        booked++;
        System.out.println(perInstalment);
        System.out.println(deliveryDays);
    }
}
