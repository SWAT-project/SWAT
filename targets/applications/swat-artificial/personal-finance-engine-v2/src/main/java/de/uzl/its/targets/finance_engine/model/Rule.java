package de.uzl.its.targets.finance_engine.model;

public class Rule {
    public String id;
    public String expr; // DSL expression
    public Rule() {}
    public Rule(String id, String expr) { this.id = id; this.expr = expr; }
}
