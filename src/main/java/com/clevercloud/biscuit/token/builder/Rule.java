package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.token.builder.constraints.ConstraintBuilder;

import java.util.ArrayList;
import java.util.List;

public class Rule {
    Predicate head;
    List<Predicate> body;
    List<ConstraintBuilder> constraintsBuilders;

    public Rule(Predicate head, List<Predicate> body, List<ConstraintBuilder> constraintsBuilders) {
        this.head = head;
        this.body = body;
        this.constraintsBuilders = constraintsBuilders;
    }

    public com.clevercloud.biscuit.datalog.Rule convert(SymbolTable symbols) {
        com.clevercloud.biscuit.datalog.Predicate head = this.head.convert(symbols);
        ArrayList<com.clevercloud.biscuit.datalog.Predicate> body = new ArrayList<>();
        ArrayList<Constraint> constraints = new ArrayList<>();

        for(Predicate p: this.body) {
            body.add(p.convert(symbols));
        }

        for(ConstraintBuilder cb: this.constraintsBuilders) {
            constraints.add(cb.convert(symbols));
        }

        return new com.clevercloud.biscuit.datalog.Rule(head, body, constraints);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule = (Rule) o;

        if (head != null ? !head.equals(rule.head) : rule.head != null) return false;
        if (body != null ? !body.equals(rule.body) : rule.body != null) return false;
        return constraintsBuilders != null ? constraintsBuilders.equals(rule.constraintsBuilders) : rule.constraintsBuilders == null;
    }

    @Override
    public int hashCode() {
        int result = head != null ? head.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (constraintsBuilders != null ? constraintsBuilders.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return head.toString() + " <- " + body + " @ "+ constraintsBuilders;
    }
}
