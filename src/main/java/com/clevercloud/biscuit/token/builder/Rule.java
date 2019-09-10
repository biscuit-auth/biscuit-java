package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;

import java.util.ArrayList;
import java.util.List;

public class Rule {
    Predicate head;
    List<Predicate> body;
    List<Constraint> constraints;

    public Rule(Predicate head, List<Predicate> body, List<Constraint> constraints) {
        this.head = head;
        this.body = body;
        this.constraints = constraints;
    }

    public com.clevercloud.biscuit.datalog.Rule convert(SymbolTable symbols) {
        com.clevercloud.biscuit.datalog.Predicate head = this.head.convert(symbols);
        ArrayList<com.clevercloud.biscuit.datalog.Predicate> body = new ArrayList<>();
        ArrayList<Constraint> constraints = new ArrayList<>(this.constraints);

        for(Predicate p: this.body) {
            body.add(p.convert(symbols));
        }

        return new com.clevercloud.biscuit.datalog.Rule(head, body, constraints);
    }
}
