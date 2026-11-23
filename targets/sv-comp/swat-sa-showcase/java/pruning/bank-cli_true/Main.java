// Expected verdict: SAFE. Withdraw rejects amount + fee > balance + overdraft, so the balance stays >= -overdraft; amount <= 100000 rules out overflow.
// Paired VIOLATION inputs (must not fail here): int:12 int:1490 int:0
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
            if (amount + fee > acc.balance + acc.overdraft) return reject("insufficient funds");
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
