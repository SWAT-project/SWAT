#!/usr/bin/env python3
"""Writes the swat-sa-showcase tasks: java/<suite>/<task>_{true,false}/Main.java and the .yml next to it.

Each task is one template with a few placeholders that differ between the SAFE (_true) and the
VIOLATION (_false) variant, so the two stay identical apart from the bug. The first line of every
Main.java states the verdict and the inputs that check_labels.py replays (see README.md).

    ./gen.py            (then ./build.sh and ./check_labels.py)
"""

from pathlib import Path

JAVA_DIR = Path(__file__).resolve().parent.parent / 'java'

YML = """format_version: '2.0'
input_files:
  - ../common/
  - {task}/
properties:
  - property_file: ../properties/{prop}.prp
    expected_verdict: {verdict}
options:
  language: Java
"""

TASKS = []
PROP_NOTE = {'valid-assert': '', 'no-runtime-exception': ' (no-runtime-exception)'}


def task(suite, name, witness, safe_reason, template, false, true, prop='valid-assert'):
    TASKS.append((suite, name, witness, safe_reason, template, false, true, prop))


# ---------------------------------------------------------------------------------------------
# pruning: reachability pruning over a command dispatcher with exact receivers
# (swat-benchmarks dispatcher/ifchain and virtual-dispatch/local)

task('pruning', 'bank-cli',
     'int:12 int:1490 int:0',
     'Withdraw rejects amount + fee > balance + overdraft, so the balance stays >= -overdraft; '
     'amount <= 100000 rules out overflow.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// A command-line front end for one bank account: each invocation runs a single command (an op code
// and two arguments) against the account as loaded from storage, then saves it.
public class Main {
    static final int BALANCE = 0, DEPOSIT = 1, STATEMENT = 2, FREEZE = 3, UNFREEZE = 4, SET_OVERDRAFT = 5,
            INTEREST = 6, STANDING_ORDER = 7, ROUND_UP = 8, CARD_LIMIT = 9, EXPORT = 10, CLOSE = 11,
            WITHDRAW = 12;
    static final int OK = 0, REJECTED = 1, UNKNOWN_COMMAND = 2;
    static final int MAX_WITHDRAWAL = 100000;

    static class Account {
        int balance = 1000; // cents
        int overdraft = 500;
        boolean frozen;
        int standingOrders;
        int cardLimit = 2000;
        boolean roundUp;
        boolean closed;
    }

    static int events;

    static void event(String what) {
        events++;
        System.out.println(what);
    }

    static int reject(String why) {
        event(why);
        return REJECTED;
    }

    interface Command {
        int run(Account acc, int x, int y);
    }

    static class Balance implements Command {
        public int run(Account acc, int format, int currency) {
            int shown = acc.balance;
            if (format == 1) event("in whole units");
            if (currency == 1) event("converted to USD");
            if (currency == 2) event("converted to GBP");
            if (shown < 100) event("low balance");
            System.out.println(shown);
            return OK;
        }
    }

    static class Deposit implements Command {
        public int run(Account acc, int amount, int channel) {
            if (amount <= 0) return reject("invalid amount");
            if (amount >= 1000000) event("report: large cash deposit");
            if (channel == 1) event("deposit at ATM");
            if (channel == 2) event("deposit by transfer");
            acc.balance += amount;
            return OK;
        }
    }

    static class Statement implements Command {
        public int run(Account acc, int fromDay, int toDay) {
            if (fromDay < 0 || toDay > 365) return reject("days out of range");
            if (fromDay > toDay) return reject("empty period");
            if (toDay - fromDay > 90) event("long statement, sent by mail");
            if (fromDay == 0) event("includes opening balance");
            return OK;
        }
    }

    static class Freeze implements Command {
        public int run(Account acc, int reason, int days) {
            if (reason == 1) event("frozen: card lost");
            if (reason == 2) event("frozen: fraud suspected");
            if (days > 30) event("long freeze, notify branch");
            if (days < 0) return reject("negative duration");
            acc.frozen = true;
            return OK;
        }
    }

    static class Unfreeze implements Command {
        public int run(Account acc, int pin, int unused) {
            if (pin < 0 || pin > 9999) return reject("malformed PIN");
            if (pin != 4711) return reject("wrong PIN");
            acc.frozen = false;
            event("unfrozen");
            return OK;
        }
    }

    static class SetOverdraft implements Command {
        public int run(Account acc, int limit, int approvedBy) {
            if (limit < 0) return reject("negative overdraft");
            if (limit > 5000 && approvedBy == 0) return reject("needs approval");
            if (limit > 20000) event("high overdraft, report to risk");
            if (limit < acc.overdraft) event("overdraft lowered");
            acc.overdraft = limit;
            return OK;
        }
    }

    static class Interest implements Command {
        public int run(Account acc, int ratePermille, int months) {
            if (ratePermille < 0 || ratePermille > 200) return reject("rate out of range");
            if (months < 1 || months > 12) return reject("months out of range");
            int interest = acc.balance / 1000 * ratePermille * months / 12;
            if (interest > 100) event("interest above 1.00");
            if (months == 12) event("yearly interest");
            if (ratePermille == 0) event("no interest");
            acc.balance += interest;
            return OK;
        }
    }

    static class StandingOrder implements Command {
        public int run(Account acc, int amount, int dayOfMonth) {
            if (amount <= 0) return reject("invalid amount");
            if (dayOfMonth < 1 || dayOfMonth > 28) return reject("invalid day");
            if (amount > acc.balance) event("warning: order exceeds balance");
            if (acc.standingOrders >= 10) return reject("too many orders");
            if (dayOfMonth == 1) event("first of the month");
            acc.standingOrders++;
            return OK;
        }
    }

    static class RoundUp implements Command {
        public int run(Account acc, int enable, int savingsAccount) {
            if (enable == 1) acc.roundUp = true;
            if (enable == 0) acc.roundUp = false;
            if (savingsAccount <= 0) event("no savings account given");
            if (savingsAccount > 99999999) return reject("invalid savings account");
            event(acc.roundUp ? "round-up on" : "round-up off");
            return OK;
        }
    }

    static class CardLimit implements Command {
        public int run(Account acc, int limit, int online) {
            if (limit < 0) return reject("negative limit");
            if (limit > 10000) return reject("limit too high");
            if (online == 0) event("online payments disabled");
            if (limit > acc.cardLimit) event("limit raised");
            if (limit < acc.cardLimit) event("limit lowered");
            acc.cardLimit = limit;
            return OK;
        }
    }

    static class Export implements Command {
        public int run(Account acc, int format, int year) {
            if (format < 0 || format > 2) return reject("unknown format");
            if (year < 2000 || year > 2030) return reject("year out of range");
            if (format == 0) event("CSV");
            if (format == 1) event("PDF");
            if (year < 2020) event("from the archive");
            return OK;
        }
    }

    static class Close implements Command {
        public int run(Account acc, int confirm, int payoutAccount) {
            if (confirm != 1) return reject("not confirmed");
            if (acc.balance < 0) return reject("settle the overdraft first");
            if (payoutAccount <= 0) return reject("no payout account");
            if (acc.balance > 0) event("paying out the balance");
            if (acc.standingOrders > 0) event("cancelling standing orders");
            acc.closed = true;
            return OK;
        }
    }

    static class Withdraw implements Command {
        public int run(Account acc, int amount, int unused) {
            if (acc.frozen || acc.closed) return reject("account locked");
            if (amount <= 0 || amount > MAX_WITHDRAWAL) return reject("invalid amount");
            int fee = amount / 100; // 1 percent
            if (@@CHECK@@ > acc.balance + acc.overdraft) return reject("insufficient funds");
            acc.balance -= amount + fee;
            assert acc.balance >= -acc.overdraft : "overdraft exceeded";
            return OK;
        }
    }

    static int execute(Account acc, int op, int x, int y) {
        if (op == BALANCE) return new Balance().run(acc, x, y);
        else if (op == DEPOSIT) return new Deposit().run(acc, x, y);
        else if (op == STATEMENT) return new Statement().run(acc, x, y);
        else if (op == FREEZE) return new Freeze().run(acc, x, y);
        else if (op == UNFREEZE) return new Unfreeze().run(acc, x, y);
        else if (op == SET_OVERDRAFT) return new SetOverdraft().run(acc, x, y);
        else if (op == INTEREST) return new Interest().run(acc, x, y);
        else if (op == STANDING_ORDER) return new StandingOrder().run(acc, x, y);
        else if (op == ROUND_UP) return new RoundUp().run(acc, x, y);
        else if (op == CARD_LIMIT) return new CardLimit().run(acc, x, y);
        else if (op == EXPORT) return new Export().run(acc, x, y);
        else if (op == CLOSE) return new Close().run(acc, x, y);
        else if (op == WITHDRAW) return new Withdraw().run(acc, x, y);
        return UNKNOWN_COMMAND;
    }

    public static void main(String[] args) {
        int op = Verifier.nondetInt();
        int x = Verifier.nondetInt();
        int y = Verifier.nondetInt();
        Account acc = new Account();
        int status = execute(acc, op, x, y);
        System.out.println(status);
    }
}
''',
     false={'CHECK': 'amount'},
     true={'CHECK': 'amount + fee'})

# ---------------------------------------------------------------------------------------------
# pruning: handlers of implicit runtime exceptions (swat-benchmarks error-handling/bounds, arith)

task('pruning', 'order-import',
     'int:0 int:5 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:0 int:1',
     'every discount in DISCOUNT_PCT is at most 20 percent, so the unit price stays positive.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// Imports one order line from a partner's feed. Each field is a code into one of the shop's tables;
// a bad code fails the lookup (index out of range) or the instalment split (division by zero). The
// importer then logs the bad field and skips the line. A line that imports cleanly is booked.
public class Main {
    static final int[] PRICE_CENTS = {199, 349, 1299, 89, 2499, 599, 4999, 129};
    static final int[] DISCOUNT_PCT = {0, 5, 10, 15, 20, @@DISCOUNT@@}; // the last one: spring campaign
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
''',
     false={'DISCOUNT': '120'},
     true={'DISCOUNT': '12'})

# ---------------------------------------------------------------------------------------------
# pruning: dead calls of a helper that is also live elsewhere (swat-benchmarks dead-callees/shared)

task('pruning', 'sensor-monitor',
     'int:0 int:900 int:3 int:61',
     'both escalation steps stop at MAX_LEVEL, and a level outside 0..MAX_LEVEL is rejected first.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// The monitoring agent of one sensor. In report mode it classifies the last 8 logged readings for
// the daily histogram. Otherwise it classifies the current reading and, if it is critical,
// escalates the sensor's open alarm.
public class Main {
    static final int LOW = 0, NORMAL = 1, HIGH = 2;
    static final int REPORT = 1;
    static final int HISTORY = 8;
    static final int MAX_LEVEL = 3; // 0: logged, 1: on-call engineer, 2: team lead, 3: site manager

    static int[] histogram = new int[3];

    static int classify(int reading) {
        if (reading < 50) return LOW;
        if (reading < 800) return NORMAL;
        return HIGH;
    }

    static void report() {
        for (int i = 0; i < HISTORY; i++) {
            int reading = Verifier.nondetInt(); // from the sensor's log
            histogram[classify(reading)]++;
        }
        System.out.println(histogram[LOW]);
        System.out.println(histogram[NORMAL]);
        System.out.println(histogram[HIGH]);
    }

    static int notified;

    // Raises the alarm's level the longer nobody acknowledges it.
    static int escalate(int level, int unackedMinutes) {
        if (level < 0 || level > MAX_LEVEL) return -1; // corrupt alarm record
        if (unackedMinutes > 15 && level < MAX_LEVEL) level++;
        if (unackedMinutes > 60@@CAP@@) level++;
        assert level <= MAX_LEVEL : "escalated past the top level";
        notified = level;
        return level;
    }

    public static void main(String[] args) {
        int mode = Verifier.nondetInt();
        if (mode == REPORT) {
            report();
            return;
        }
        int current = Verifier.nondetInt();
        if (classify(current) == HIGH) {
            int level = Verifier.nondetInt(); // the open alarm's level, from the alarm store
            int unackedMinutes = Verifier.nondetInt();
            System.out.println(escalate(level, unackedMinutes));
        }
    }
}
''',
     false={'CAP': ''},
     true={'CAP': ' && level < MAX_LEVEL'})

# ---------------------------------------------------------------------------------------------
# slicing: a loop whose input-dependent branches only log (swat-benchmarks relevance/loop-helper,
# scratch-array, example)

task('slicing', 'backup-resume',
     'int:2147483600',
     'resumeFrom is clamped to 0..BLOCKS, so resumeFrom + CHECKPOINT_INTERVAL cannot overflow.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// Resumes an interrupted backup of a 1000-block volume from the last checkpoint: blocks before it
// are skipped (and logged), the others are copied. Afterwards the next checkpoint is scheduled.
public class Main {
    static final int BLOCKS = 1000;
    static final int CHECKPOINT_INTERVAL = 64;

    static int skipped, copied, verboseLines;
    static int[] recent = new int[16]; // the last blocks written, for the status page

    static void logSkip(int block, int resumeFrom) {
        skipped++;
        if (resumeFrom - block <= 3) verboseLines++; // the last few skipped blocks are logged in full
    }

    static void copy(int block) {
        recent[block % recent.length] = block;
        copied++;
        if (block % 100 == 0) System.out.println(block);
    }

    public static void main(String[] args) {
        int resumeFrom = Verifier.nondetInt(); // from the checkpoint file
        if (resumeFrom < 0) resumeFrom = 0;@@CLAMP@@
        for (int block = 0; block < BLOCKS; block++) {
            if (block < resumeFrom) {
                logSkip(block, resumeFrom);
            } else {
                copy(block);
            }
        }
        System.out.println(copied);
        int nextCheckpoint = resumeFrom + CHECKPOINT_INTERVAL;
        assert nextCheckpoint > resumeFrom : "checkpoint counter overflow";
        System.out.println(nextCheckpoint);
    }
}
''',
     false={'CLAMP': ''},
     true={'CLAMP': '\n        if (resumeFrom > BLOCKS) resumeFrom = BLOCKS; // the backup had finished'})

# ---------------------------------------------------------------------------------------------
# summaries: a diamond with a nested condition in a loop (swat-benchmarks loop-summary/count-below,
# count-even)

task('summaries', 'subscription-invoice',
     'int:990',
     'the credit is only granted for trials of at most 90 days; then at least 910 days are charged, '
     'at 10 cents or more each.',
     '''\
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
        @@CREDIT@@
        assert total >= 0 : "negative invoice";
    }
}
''',
     false={'CREDIT': 'total -= WELCOME_CREDIT_CENTS;'},
     true={'CREDIT': 'if (trialDays <= MAX_TRIAL_WITH_CREDIT) total -= WELCOME_CREDIT_CENTS; // not combined with long trials'})

# ---------------------------------------------------------------------------------------------
# summaries: a counting loop up to an input bound whose body also logs (swat-benchmarks
# loop-summary/count-up-log, count-up)

task('summaries', 'loan-schedule',
     'int:361',
     'the term is at most 360 months, so at most 360 * 250 = CREDIT_LINE is repaid.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// Builds the repayment schedule of a consumer loan: one fixed instalment per month over the term
// the customer chose. Every month is recorded for the yearly statement.
public class Main {
    static final int INSTALMENT = 250; // euros
    static final int MAX_TERM_MONTHS = @@MAX_TERM@@;
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
''',
     false={'MAX_TERM': '480; // raised from 360 for the 40-year product'},
     true={'MAX_TERM': '360;'})

# ---------------------------------------------------------------------------------------------
# loop-states: loop-head state matching with a dead input local, in a callee (swat-benchmarks
# loop-liveness/dead-input, loop-frames/callee-loop)

task('loop-states', 'thermostat',
     'boolean:true int:100 boolean:true int:300 boolean:false',
     'every transition sets mode and relay together; the loop is unbounded, but its state at the head '
     '(mode, relay, the display) only takes two values.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// The control loop of a two-state thermostat with hysteresis: it switches the heater on below
// 18.0 degrees and off above 22.0 degrees, for as long as the sensor delivers samples.
public class Main {
    static final int IDLE = 0, HEATING = 1;
    static final int ON_BELOW = 180, OFF_ABOVE = 220; // tenths of a degree

    static int shownMode; // what the front panel shows

    static boolean relay(boolean on) {
        // drives the heater's relay
        return on;
    }

    static void display(int mode) {
        shownMode = mode;
    }

    static void control() {
        int mode = IDLE;
        boolean relayOn = false;
        while (Verifier.nondetBoolean()) { // the sensor has another sample
            int temp = Verifier.nondetInt();
            if (mode == IDLE && temp < ON_BELOW) {
                relayOn = relay(true);
                mode = HEATING;
                display(mode);
            } else if (mode == HEATING && temp > OFF_ABOVE) {@@OFF@@
                mode = IDLE;
                display(mode);
            }
        }
        assert relayOn == (mode == HEATING) : "relay and mode disagree";
    }

    public static void main(String[] args) {
        control();
    }
}
''',
     false={'OFF': ''},
     true={'OFF': '\n                relayOn = relay(false);'})


# ---------------------------------------------------------------------------------------------
# slicing: many independent input-dependent branches that only feed logging (swat-benchmarks
# irrelevant-prefix/count: 2^n paths without slicing)

task('slicing', 'feature-flags',
     'int:70000',
     'a port outside 1..65535 is replaced by the default before the check.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// Starts a server from its configuration: a listening port and 16 feature flags. The enabled
// features are registered and logged; then the server binds to the port.
public class Main {
    static final int FLAGS = 16;
    static final int DEFAULT_PORT = 8080;

    static boolean[] enabled = new boolean[FLAGS];
    static int enabledCount, warnings, bound;

    static void enable(int flag, int value) {
        enabled[flag] = true;
        enabledCount++;
        if (value != 1) warnings++; // flags should be 0 or 1; other values count as on
        System.out.println(flag);
    }

    static void bind(int port) {
        bound = port;
    }

    public static void main(String[] args) {
        int port = Verifier.nondetInt();
        int[] flags = new int[FLAGS];
        for (int i = 0; i < FLAGS; i++) {
            flags[i] = Verifier.nondetInt();
        }
        for (int i = 0; i < FLAGS; i++) {
            if (flags[i] != 0) {
                enable(i, flags[i]);
            }
        }
        System.out.println(enabledCount);
        @@PORT@@
        assert port > 0 && port <= 65535 : "invalid port";
        bind(port);
    }
}
''',
     false={'PORT': 'if (port == 0) port = DEFAULT_PORT; // unset'},
     true={'PORT': 'if (port <= 0 || port > 65535) port = DEFAULT_PORT; // unset or invalid'})

# ---------------------------------------------------------------------------------------------
# multi: several asserts, only one of which can fail

task('multi', 'payroll',
     'int:0 int:160 int:2000 int:1000000 int:1',
     'the referral bonus is added before the cap; the other three asserts hold because hours and '
     'rate are validated (no overflow) and the tax is at most 35 percent.',
     '''\
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
        if (salesCents > 0 && salesCents < 100000000) bonus = salesCents / 20; // 5 percent commission@@BEFORE_CAP@@
        if (bonus > BONUS_CAP) bonus = BONUS_CAP;@@AFTER_CAP@@
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
''',
     false={'BEFORE_CAP': '',
            'AFTER_CAP': '\n        if (referrals > 0 && referrals <= 10) bonus += referrals * 5000; // referral bonus, added later'},
     true={'BEFORE_CAP': '\n        if (referrals > 0 && referrals <= 10) bonus += referrals * 5000; // referral bonus',
           'AFTER_CAP': ''})

task('multi', 'elevator',
     'boolean:true int:3 boolean:true int:-1',
     'the door only opens while the car stands, and the car never leaves floors 0..5; the loop is '
     'unbounded, but its state at the head (floor, direction, door) takes only 36 values.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// The controller of an elevator in a six-floor building. Each event is a requested floor, or the
// door button (-1); the car moves one floor per event.
public class Main {
    static final int FLOORS = 6;
    static final int DOOR_BUTTON = -1;
    static final int DOWN = -1, STOP = 0, UP = 1;

    static int up() { return UP; }
    static int down() { return DOWN; }
    static int stop() { return STOP; }
    static boolean open() { return true; }
    static boolean close() { return false; }

    static int move(int floor, int dir) {
        int next = floor;
        if (dir == UP && floor < FLOORS - 1) next = floor + 1;
        else if (dir == DOWN && floor > 0) next = floor - 1;
        assert next >= 0 && next < FLOORS : "left the shaft";
        return next;
    }

    static void run() {
        int floor = 0;
        int dir = STOP;
        boolean doorOpen = false;
        while (Verifier.nondetBoolean()) { // another event
            int request = Verifier.nondetInt();
            if (request == DOOR_BUTTON) {
                @@DOOR@@
            } else if (request > floor && request < FLOORS) {
                doorOpen = close();
                dir = up();
            } else if (request < floor && request >= 0) {
                doorOpen = close();
                dir = down();
            } else {
                dir = stop();
            }
            floor = move(floor, dir);
            assert !(doorOpen && dir != STOP) : "moving with the door open";
        }
    }

    public static void main(String[] args) {
        run();
    }
}
''',
     false={'DOOR': 'doorOpen = open();'},
     true={'DOOR': 'if (dir == STOP) doorOpen = open();'})

task('multi', 'meter-billing',
     'int:665 int:1',
     'the goodwill credit is only granted to meters installed on the first day; then at least 576 '
     'slots are measured, at 24 cents or more each. The other asserts hold because billingDays is '
     'validated.',
     '''\
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
        if (@@CREDIT_IF@@) bill -= GOODWILL_CENTS;
        assert peak + offPeak <= SLOTS : "more slots than in a week";
        assert standing <= MAX_DAYS * STANDING_CENTS : "standing charge too high";
        assert bill >= 0 : "negative bill";
    }
}
''',
     false={'CREDIT_IF': 'installSlot > 0'},
     true={'CREDIT_IF': 'installSlot > 0 && installSlot <= SLOTS_PER_DAY'})

# ---------------------------------------------------------------------------------------------
# nre: no-runtime-exception

task('nre', 'token-bucket',
     'boolean:true int:150',
     'every object is created before it is used, and nothing indexes or divides by a variable; '
     'the loop is unbounded and its state (the token count) symbolic.',
     '''\
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
        Audit audit@@AUDIT_INIT@@

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
''',
     false={'AUDIT_INIT': '; // was created lazily; the creation got lost in a refactoring'},
     true={'AUDIT_INIT': ' = new Audit();'},
     prop='no-runtime-exception')

task('nre', 'session-store',
     'int:0 int:8 int:-5',
     'slotOf masks the sign bit, so the index is in 0..slots.length - 1, and slots.length >= 1.',
     '''\
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
        return @@INDEX@@ % slots.length;
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
''',
     false={'INDEX': 'sessionId'},
     true={'INDEX': '(sessionId & 0x7fffffff)'},
     prop='no-runtime-exception')

task('nre', 'average-order',
     'int:1000',
     'the cutoff is at most 999, so at least the last order counts and the divisor is >= 1.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// The average order size over the last 1000 orders of a shop, ignoring the orders before a
// cutoff (e.g. the ones placed before a price change). Shops with large averages get a badge.
public class Main {
    static final int ORDERS = 1000;

    public static void main(String[] args) {
        int cutoff = Verifier.nondetInt();
        if (cutoff < 0) cutoff = 0;@@CLAMP@@
        int counted = 0;
        int items = 0;
        for (int i = 0; i < ORDERS; i++) {
            if (i >= cutoff) {
                counted++;
                items += 3; // the shop's orders: 3 items each
            }
        }
        if (items / counted > 2) {
            System.out.println("badge: large orders");
        }
    }
}
''',
     false={'CLAMP': ''},
     true={'CLAMP': '\n        if (cutoff > ORDERS - 1) cutoff = ORDERS - 1;'},
     prop='no-runtime-exception')

task('nre', 'traffic-light',
     'boolean:true int:1 boolean:true int:0 boolean:true int:0',
     'every phase has a next phase; the loop is unbounded, but its state at the head (the current '
     'phase, the walk request) takes only 10 values.',
     '''\
import org.sosy_lab.sv_benchmarks.Verifier;

// The controller of a traffic light with a pedestrian phase: north-south green, yellow, east-west
// green, yellow, and a walk phase after the north-south yellow when someone pressed the button.
// The phases form a cycle of objects, each with its duration and its successor.
public class Main {
    static final int NS_GREEN = 0, NS_YELLOW = 1, EW_GREEN = 2, EW_YELLOW = 3, WALK = 4;

    static class Phase {
        final int id;
        final int seconds;
        Phase next;

        Phase(int id, int seconds) {
            this.id = id;
            this.seconds = seconds;
        }
    }

    static int waited;

    static boolean request() { return true; }
    static boolean served() { return false; }

    static void hold(int seconds) {
        waited = seconds;
    }

    static void run() {
        Phase nsGreen = new Phase(NS_GREEN, 30);
        Phase nsYellow = new Phase(NS_YELLOW, 5);
        Phase ewGreen = new Phase(EW_GREEN, 40);
        Phase ewYellow = new Phase(EW_YELLOW, 5);
        Phase walkPhase = new Phase(WALK, 20);
        nsGreen.next = nsYellow;
        nsYellow.next = ewGreen;
        ewGreen.next = ewYellow;
        ewYellow.next = nsGreen;@@WALK_NEXT@@

        Phase phase = nsGreen;
        boolean walk = false;
        while (Verifier.nondetBoolean()) { // the next tick of the timer
            int button = Verifier.nondetInt(); // the pedestrian button's sensor reading
            if (button > 0) walk = request();
            hold(phase.seconds);
            if (phase.id == WALK) {
                walk = served();
                phase = phase.next;
            } else if (phase.id == NS_YELLOW && walk) {
                phase = walkPhase;
            } else {
                phase = phase.next;
            }
            hold(phase.seconds);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
''',
     false={'WALK_NEXT': ' // the walk phase (added later) has no successor yet'},
     true={'WALK_NEXT': '\n        walkPhase.next = ewGreen;'},
     prop='no-runtime-exception')


def main():
    for suite, name, witness, safe_reason, template, false, true, prop in TASKS:
        for verdict, subst in (('false', false), ('true', true)):
            src = template
            for key, value in subst.items():
                src = src.replace(f'@@{key}@@', value)
            assert '@@' not in src, (name, verdict)
            if verdict == 'false':
                head = f'// Expected verdict: VIOLATION{PROP_NOTE[prop]}. Violating inputs (nondet call order): {witness}\n'
            else:
                head = (f'// Expected verdict: SAFE{PROP_NOTE[prop]}. {safe_reason}\n'
                        f'// Paired VIOLATION inputs (must not fail here): {witness}\n')
            task_name = f'{name}_{verdict}'
            d = JAVA_DIR / suite / task_name
            d.mkdir(parents=True, exist_ok=True)
            (d / 'Main.java').write_text(head + src)
            (JAVA_DIR / suite / f'{task_name}.yml').write_text(YML.format(task=task_name, verdict=verdict, prop=prop))
    print(f'Wrote {2 * len(TASKS)} tasks.')


if __name__ == '__main__':
    main()
