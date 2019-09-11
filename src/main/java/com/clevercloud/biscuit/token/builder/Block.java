package com.clevercloud.biscuit.token.builder;


import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.Rule;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.DateConstraint;
import com.clevercloud.biscuit.datalog.constraints.StrConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Block {
    long index;
    int symbol_start;
    SymbolTable symbols;
    List<Fact> facts;
    List<Rule> caveats;

    public Block(long index, SymbolTable base_symbols) {
        this.index = index;
        this.symbol_start = base_symbols.symbols.size();
        this.symbols = new SymbolTable(base_symbols);
        this.facts = new ArrayList<>();
        this.caveats = new ArrayList<>();
    }

    public ID symbol_add(String s) {
        return this.symbols.add(s);
    }

    public long symbol_insert(String s) {
        return this.symbols.insert(s);
    }

    public void add_fact(com.clevercloud.biscuit.token.builder.Fact f) {
        this.facts.add(f.convert(this.symbols));
    }

    public void add_caveat(com.clevercloud.biscuit.token.builder.Rule caveat) {
        this.caveats.add(caveat.convert(this.symbols));
    }

    public com.clevercloud.biscuit.token.Block build() {
        SymbolTable symbols = new SymbolTable();

        for(int i = this.symbol_start; i < this.symbols.symbols.size(); i++) {
            symbols.add(this.symbols.symbols.get(i));
        }

        return new com.clevercloud.biscuit.token.Block(this.index, symbols, this.facts, this.caveats);
    }

    public Boolean add_right(String resource, String right) {
        if(this.index != 0) {
            return false;
        } else {
            this.add_fact(fact("right", Arrays.asList(s("authority"), string(resource), s(right))));
            return true;
        }
    }

    public void check_right(String right) {
        this.add_caveat(rule(
                "check_right",
                Arrays.asList(s(right)),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var(0))),
                        pred("operation", Arrays.asList(s("ambient"), s(right))),
                        pred("right", Arrays.asList(s("authority"), var(0), s(right)))
                )
        ));
    }

    public void resource_prefix(String prefix) {
        this.add_caveat(constrained_rule(
                "prefix",
                Arrays.asList(var(0)),
                Arrays.asList(pred("resource", Arrays.asList(s("ambient"), var(0)))),
                Arrays.asList(
                        new Constraint(0, new ConstraintKind.Str(new StrConstraint.Prefix(prefix))))
        ));
    }

    public void resource_suffix(String suffix) {
        this.add_caveat(constrained_rule(
                "suffix",
                Arrays.asList(var(0)),
                Arrays.asList(pred("resource", Arrays.asList(s("ambient"), var(0)))),
                Arrays.asList(new Constraint(0, new ConstraintKind.Str(new StrConstraint.Suffix(suffix))))
        ));
    }

    public void expiration_date(Date d) {
        this.add_caveat(constrained_rule(
                "expiration",
                Arrays.asList(var(0)),
                Arrays.asList(pred("time", Arrays.asList(s("ambient"), var(0)))),
                Arrays.asList(new Constraint(0, new ConstraintKind.Date(new DateConstraint.Before(d.getTime() / 1000))))
        ));
    }

    public static com.clevercloud.biscuit.token.builder.Fact fact(String name, List<Atom> ids) {
        return new com.clevercloud.biscuit.token.builder.Fact(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Predicate pred(String name, List<Atom> ids) {
        return new com.clevercloud.biscuit.token.builder.Predicate(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Rule rule(String head_name, List<Atom> head_ids,
                                                                  List<com.clevercloud.biscuit.token.builder.Predicate> predicates) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, new ArrayList<>());
    }

    public static com.clevercloud.biscuit.token.builder.Rule constrained_rule(String head_name, List<Atom> head_ids,
                                                                              List<com.clevercloud.biscuit.token.builder.Predicate> predicates,
                                                                              List<Constraint> constraints) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, constraints);
    }

    public static Atom integer(long i) {
        return new Atom.Integer(i);
    }

    public static Atom string(String s) {
        return new Atom.Str(s);
    }

    public static Atom s(String str) {
        return new Atom.Symbol(str);
    }

    public static Atom date(Date d) {
        return new Atom.Date(d.getTime() / 1000);
    }

    public static Atom var(int i) {
        return new Atom.Variable(i);
    }
}
