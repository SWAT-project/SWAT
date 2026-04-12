package de.uzl.its.targets.finance_engine.util;

import de.uzl.its.targets.finance_engine.model.Transaction;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A tiny expression engine with recursive-descent parsing.
 * Supports:
 *  - ||, &&, !
 *  - ==, !=, >, <, >=, <=
 *  - +, -, *, / (integer math; safe division by zero -> 0)
 *  - parentheses ()
 *  - identifiers: amount, amountMinor, category, merchant, date (yyyy-MM-dd)
 *  - functions:
 *      param(name) -> number
 *      contains(field, substr) -> bool
 *      regex(field, pattern) -> bool  (caps input length to 200)
 *      SUM(expr), COUNT(expr), AVG(expr)  -> aggregates over allTx using amountMinor
 *      RULE(id) -> bool (deferred to RuleResolver)
 *
 * No Streams/Optionals/Comparators used.
 */
public class SimpleExpressionEvaluatorV2 {

    public interface RuleResolver {
        boolean resolve(
                String ruleId,
                Transaction current,
                List<Transaction> allTx,
                Map<String,String> params,
                Set<String> visiting,
                int depth
        );
    }

    private static class Tok {
        String type; // "num","str","id","op","lp","rp","comma","eof"
        String text;
        Tok(String type, String text) { this.type = type; this.text = text; }
    }

    private List<Tok> toks;
    private int pos;
    private Transaction cur;
    private List<Transaction> all;
    private Map<String,String> params;
    private RuleResolver resolver;

    public static boolean evaluate(
            String expr,
            Transaction current,
            List<Transaction> allTx,
            Map<String,String> params,
            RuleResolver resolver
    ) {
        try {
            SimpleExpressionEvaluatorV2 ev = new SimpleExpressionEvaluatorV2(expr, current, allTx, params, resolver);
            boolean v = ev.parseOr();
            ev.expect("eof");
            return v;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private SimpleExpressionEvaluatorV2(
            String expr,
            Transaction current,
            List<Transaction> allTx,
            Map<String,String> params,
            RuleResolver resolver
    ) {
        this.cur = current;
        this.all = allTx;
        this.params = params;
        this.resolver = resolver;
        this.toks = tokenize(expr == null ? "" : expr);
        this.pos = 0;
    }

    private void expect(String t) {
        Tok k = peek();
        if (!k.type.equals(t)) throw new RuntimeException("expected " + t + " got " + k.type);
        next();
    }

    private Tok peek() { return (pos < toks.size()) ? toks.get(pos) : new Tok("eof",""); }
    private Tok next() { Tok k = peek(); pos++; return k; }

    private boolean parseOr() {
        boolean v = parseAnd();
        while (peek().type.equals("op") && peek().text.equals("||")) { next(); boolean r = parseAnd(); v = v || r; }
        return v;
    }

    private boolean parseAnd() {
        boolean v = parseNot();
        while (peek().type.equals("op") && peek().text.equals("&&")) { next(); boolean r = parseNot(); v = v && r; }
        return v;
    }

    private boolean parseNot() {
        if (peek().type.equals("op") && peek().text.equals("!")) { next(); return !parseNot(); }
        return parseCompare();
    }

    private boolean parseCompare() {
        Value a = parseSum();
        if (isComp(peek())) {
            String op = next().text;
            Value b = parseSum();
            if (a.isString || b.isString) {
                String as = a.toStringVal();
                String bs = b.toStringVal();
                if (op.equals("==")) return as.equals(bs);
                if (op.equals("!=")) return !as.equals(bs);
                // attempt numeric fallback
                try {
                    long an = Long.parseLong(as);
                    long bn = Long.parseLong(bs);
                    if (op.equals(">"))  return an > bn;
                    if (op.equals("<"))  return an < bn;
                    if (op.equals(">=")) return an >= bn;
                    if (op.equals("<=")) return an <= bn;
                } catch (Exception ex) { return false; }
                return false;
            } else {
                long an = a.num;
                long bn = b.num;
                if (op.equals("==")) return an == bn;
                if (op.equals("!=")) return an != bn;
                if (op.equals(">"))  return an > bn;
                if (op.equals("<"))  return an < bn;
                if (op.equals(">=")) return an >= bn;
                if (op.equals("<=")) return an <= bn;
                return false;
            }
        }
        // treat non-comp as truthy if numeric != 0 or non-empty string
        if (a.isString) return a.str != null && a.str.length() > 0;
        return a.num != 0;
    }

    private boolean isComp(Tok t) {
        if (!t.type.equals("op")) return false;
        String s = t.text;
        return s.equals("==") || s.equals("!=") || s.equals(">") || s.equals("<") || s.equals(">=") || s.equals("<=");
    }

    private Value parseSum() {
        Value v = parseTerm();
        while (peek().type.equals("op") && (peek().text.equals("+") || peek().text.equals("-"))) {
            String op = next().text;
            Value r = parseTerm();
            if (v.isString || r.isString) { // string concat only for +
                if (op.equals("+")) v = Value.str(v.toStringVal() + r.toStringVal());
                else v = Value.num(0);
            } else {
                if (op.equals("+")) v = Value.num(v.num + r.num);
                else v = Value.num(v.num - r.num);
            }
        }
        return v;
    }

    private Value parseTerm() {
        Value v = parseFactor();
        while (peek().type.equals("op") && (peek().text.equals("*") || peek().text.equals("/"))) {
            String op = next().text;
            Value r = parseFactor();
            if (v.isString || r.isString) {
                v = Value.num(0);
            } else {
                if (op.equals("*")) v = Value.num(v.num * r.num);
                else v = Value.num(r.num == 0 ? 0 : v.num / r.num);
            }
        }
        return v;
    }

    /**
     * Resolve param('name') to a numeric long.
     * Looks up the key in `params` (case-insensitive fallback).
     * If not found, tries to parse the provided name itself as a number (so param('10') works).
     * Returns 0 on any parse error.
     */
    private long paramNum(String name) {
        if (name == null) return 0L;

        String v = null;
        if (params != null) {
            v = params.get(name);
            if (v == null) {
                // Case-insensitive fallback
                for (Map.Entry<String, String> e : params.entrySet()) {
                    String k = e.getKey();
                    if (k != null && k.equalsIgnoreCase(name)) {
                        v = e.getValue();
                        break;
                    }
                }
            }
        }

        if (v == null) v = name; // allow literals like param('10')

        try {
            if (v.indexOf('.') >= 0) {
                return Math.round(Double.parseDouble(v));
            }
            return Long.parseLong(v);
        } catch (Exception ex) {
            return 0L;
        }
    }

    private Value parseFactor() {
        Tok k = peek();
        if (k.type.equals("num")) { next(); return Value.num(parseLongSafe(k.text)); }
        if (k.type.equals("str")) { next(); return Value.str(unquote(k.text)); }
        if (k.type.equals("lp"))  { next(); boolean b = parseOr(); expect("rp"); return Value.num(b ? 1 : 0); }
        if (k.type.equals("id")) {
            String id = next().text;
            if (peek().type.equals("lp")) {
                next(); // '('
                if (id.equalsIgnoreCase("param")) {
                    String name = null;
                    if (peek().type.equals("str") || peek().type.equals("id")) { name = unquote(next().text); }
                    while (!peek().type.equals("rp")) next();
                    expect("rp");
                    return Value.num(paramNum(name));
                } else if (id.equalsIgnoreCase("contains")) {
                    String field = readArgAsString();
                    expect("comma");
                    String substr = readArgAsString();
                    expect("rp");
                    if (field == null) field = "";
                    if (substr == null) substr = "";
                    return Value.num(field.toLowerCase().indexOf(substr.toLowerCase()) >= 0 ? 1 : 0);
                } else if (id.equalsIgnoreCase("regex")) {
                    String field = readArgAsString();
                    expect("comma");
                    String pattern = readArgAsString();
                    expect("rp");
                    if (field == null) field = "";
                    if (pattern == null) pattern = "";
                    String sub = field.length() > 200 ? field.substring(0,200) : field;
                    try {
                        boolean m = Pattern.compile(pattern).matcher(sub).find();
                        return Value.num(m ? 1 : 0);
                    } catch (PatternSyntaxException ex) {
                        return Value.num(0);
                    }
                } else if (id.equalsIgnoreCase("SUM") || id.equalsIgnoreCase("COUNT") || id.equalsIgnoreCase("AVG")) {
                    String expr = readArgAsRawUntilRp();
                    expect("rp");
                    long sum = 0;
                    long cnt = 0;
                    for (int i = 0; i < all.size(); i++) {
                        Transaction t = all.get(i);
                        boolean match = SimpleExpressionEvaluatorV2.evaluate(expr, t, all, params, resolver);
                        if (match) {
                            sum += t.amountMinor;
                            cnt += 1;
                        }
                    }
                    if (id.equalsIgnoreCase("SUM"))   return Value.num(sum);
                    if (id.equalsIgnoreCase("COUNT")) return Value.num(cnt);
                    if (cnt == 0) return Value.num(0);
                    return Value.num(sum / cnt);
                } else if (id.equalsIgnoreCase("RULE")) {
                    String rid = null;
                    if (peek().type.equals("str") || peek().type.equals("id")) { rid = unquote(next().text); }
                    while (!peek().type.equals("rp")) next();
                    expect("rp");
                    if (rid == null) return Value.num(0);
                    Set<String> visiting = new HashSet<String>();
                    boolean ok = resolver != null && resolver.resolve(rid, cur, all, params, visiting, 0);
                    return Value.num(ok ? 1 : 0);
                } else {
                    while (!peek().type.equals("rp")) next();
                    expect("rp");
                    return Value.num(0);
                }
            } else {
                if (id.equals("amount") || id.equals("amountMinor")) return Value.num(cur.amountMinor);
                if (id.equals("category")) return Value.str(cur.category == null ? "" : cur.category);
                if (id.equals("merchant")) return Value.str(cur.merchant == null ? "" : cur.merchant);
                if (id.equals("date"))     return Value.str(cur.date == null ? "" : cur.date.toString());
                if (id.equalsIgnoreCase("true"))  return Value.num(1);
                if (id.equalsIgnoreCase("false")) return Value.num(0);
                return Value.num(0);
            }
        }
        // fallback: consume one token
        next();
        return Value.num(0);
    }

    private String readArgAsString() {
        Tok t = peek();
        if (t.type.equals("str") || t.type.equals("id")) { next(); return unquote(t.text); }
        if (t.type.equals("num")) { next(); return t.text; }
        return "";
    }

    private String readArgAsRawUntilRp() {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        while (!peek().type.equals("rp") || depth > 0) {
            Tok t = next();
            if (t.type.equals("lp")) depth++;
            if (t.type.equals("rp")) depth--;
            sb.append(t.text);
            if (peek().type.equals("eof")) break;
        }
        return sb.toString();
    }

    private static class Value {
        long num;
        String str;
        boolean isString;
        static Value num(long n) { Value v = new Value(); v.num = n; v.isString = false; return v; }
        static Value str(String s) { Value v = new Value(); v.str = s; v.isString = true; return v; }
        String toStringVal() { return isString ? (str == null ? "" : str) : Long.toString(num); }
    }

    // Tokenizer
    private List<Tok> tokenize(String s) {
        List<Tok> out = new ArrayList<Tok>();
        int i = 0; int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(') { out.add(new Tok("lp","(")); i++; continue; }
            if (c == ')') { out.add(new Tok("rp",")")); i++; continue; }
            if (c == ',') { out.add(new Tok("comma",",")); i++; continue; }
            // operators (2-char)
            if (i+1 < n) {
                String two = s.substring(i,i+2);
                if (two.equals("&&") || two.equals("||") || two.equals("==") || two.equals("!=") || two.equals(">=") || two.equals("<=")) {
                    out.add(new Tok("op", two)); i+=2; continue;
                }
            }
            // single-char ops
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '!' || c == '>' || c == '<') {
                out.add(new Tok("op", String.valueOf(c))); i++; continue;
            }
            // quoted string
            if (c == '\'' || c == '\"') {
                char q = c; i++;
                StringBuilder sb = new StringBuilder();
                while (i < n && s.charAt(i) != q) { sb.append(s.charAt(i)); i++; }
                if (i < n && s.charAt(i) == q) i++;
                out.add(new Tok("str", "'" + sb.toString() + "'"));
                continue;
            }
            // number
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < n && Character.isDigit(s.charAt(i))) { sb.append(s.charAt(i)); i++; }
                out.add(new Tok("num", sb.toString()));
                continue;
            }
            // identifier
            if (Character.isLetter(c) || c == '_' ) {
                StringBuilder sb = new StringBuilder();
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i)=='_')) { sb.append(s.charAt(i)); i++; }
                out.add(new Tok("id", sb.toString()));
                continue;
            }
            // unknown char -> skip
            i++;
        }
        out.add(new Tok("eof",""));
        return out;
    }

    private String unquote(String s) {
        if (s == null) return "";
        if (s.length() >= 2 && (
                (s.charAt(0) == '\'' && s.charAt(s.length()-1) == '\'') ||
                        (s.charAt(0) == '\"' && s.charAt(s.length()-1) == '\"')
        )) {
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    private long parseLongSafe(String s) {
        if (params != null && params.containsKey(s)) {
            try { return Long.parseLong(params.get(s)); } catch (Exception ex) {}
        }
        try { return Long.parseLong(s); } catch (Exception ex) { return 0L; }
    }
}