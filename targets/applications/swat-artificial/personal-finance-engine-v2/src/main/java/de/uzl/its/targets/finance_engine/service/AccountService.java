package de.uzl.its.targets.finance_engine.service;

import de.uzl.its.targets.finance_engine.util.CsvParser;
import org.springframework.stereotype.Service;
import de.uzl.its.targets.finance_engine.model.*;
import java.util.*;
import java.time.LocalDate;

@Service
public class AccountService {
    private Map<String, Account> accounts = new HashMap<String, Account>();
    private int idSeq = 1;

    public AccountService() {
        Account a = new Account("acc-1", "user-1", "checking", "USD");
        // CHANGED: add metadata entry instead of a map put
        a.metadata.add(new MetadataEntry("ownerName", "Felix"));
        a.transactions.add(new Transaction("t1", LocalDate.now().minusDays(2), "Starbucks", "coffee", 450, "USD", "latte"));
        a.transactions.add(new Transaction("t2", LocalDate.now().minusDays(10), "Netflix", "entertainment", 1299, "USD", "monthly subs"));
        accounts.put(a.id, a);
    }

    public Account createAccount(Account request, boolean verify) {
        String id = "acc-" + (++idSeq);
        request.id = id;
        if (request.transactions == null) request.transactions = new ArrayList<Transaction>();
        if (request.metadata == null) request.metadata = new ArrayList<MetadataEntry>();
        accounts.put(id, request);
        if (verify) { if (request.currency == null) request.currency = "USD"; }
        return request;
    }

    public Account get(String id) { return accounts.get(id); }

    public List<Transaction> getTransactions(String accountId) {
        Account a = accounts.get(accountId);
        if (a == null) return new ArrayList<Transaction>();
        return a.transactions;
    }

    // NOTE: signature unchanged; controller converts DTO list -> Map<Integer,String>
    public void importCsv(String accountId, String csvContent, Map<Integer,String> mapping, String strategy) {
        List<List<String>> rows = CsvParser.parse(csvContent);
        int start = 0;
        if (rows.size() > 0) {
            boolean headerLike = false;
            List<String> first = rows.get(0);
            for (int i = 0; i < first.size(); i++) {
                String fl = first.get(i).toLowerCase();
                if (fl.indexOf("date") >= 0 || fl.indexOf("amount") >= 0 || fl.indexOf("merchant") >= 0) { headerLike = true; break; }
            }
            if (headerLike) start = 1;
        }
        Account a = accounts.get(accountId);
        if (a == null) return;
        List<Transaction> parsed = new ArrayList<Transaction>();
        for (int r = start; r < rows.size(); ++r) {
            List<String> row = rows.get(r);
            Transaction tx = new Transaction();
            tx.id = accountId + "-imp-" + r;
            StringBuilder raw = new StringBuilder();
            for (int i = 0; i < row.size(); i++) { if (i>0) raw.append(","); raw.append(row.get(i)); }
            tx.raw = raw.toString();

            // mapping: index -> field
            for (Map.Entry<Integer,String> e : mapping.entrySet()) {
                int col = e.getKey();
                String field = e.getValue();
                String val = "";
                if (col >= 0 && col < row.size()) val = row.get(col);
                if (field.equals("date")) {
                    try { tx.date = LocalDate.parse(val); } catch (Exception ex) { tx.date = LocalDate.now(); }
                } else if (field.equals("merchant")) tx.merchant = val;
                else if (field.equals("category")) tx.category = val;
                else if (field.equals("amount")) {
                    try { double d = Double.parseDouble(val); tx.amountMinor = Math.round(d * 100.0); }
                    catch (Exception ex) {
                        String num = val.replaceAll("[^0-9.-]", "");
                        try { double d = Double.parseDouble(num); tx.amountMinor = Math.round(d * 100.0); } catch (Exception ex2) { tx.amountMinor = 0; }
                    }
                } else if (field.equals("currency")) tx.currency = val;
            }
            parsed.add(tx);
        }
        if (strategy == null) strategy = "append";
        if (strategy.equalsIgnoreCase("replace")) {
            a.transactions.clear();
            for (int i = 0; i < parsed.size(); i++) a.transactions.add(parsed.get(i));
        } else if (strategy.equalsIgnoreCase("merge")) {
            for (int i = 0; i < parsed.size(); i++) {
                Transaction tx = parsed.get(i);
                boolean found = false;
                for (int j = 0; j < a.transactions.size(); j++) {
                    if (a.transactions.get(j).raw.equals(tx.raw)) { found = true; break; }
                }
                if (!found) a.transactions.add(tx);
            }
        } else {
            for (int i = 0; i < parsed.size(); i++) a.transactions.add(parsed.get(i));
        }
    }

    public Transaction addTransaction(String accountId, Transaction tx) {
        Account a = accounts.get(accountId);
        if (a == null) return null;
        if (tx.id == null) tx.id = accountId + "-t-" + (a.transactions.size()+1);
        a.transactions.add(tx);
        return tx;
    }
}