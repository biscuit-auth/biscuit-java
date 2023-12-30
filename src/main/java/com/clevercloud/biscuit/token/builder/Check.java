package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.clevercloud.biscuit.datalog.Check.Kind.One;

public class Check {
    private final com.clevercloud.biscuit.datalog.Check.Kind kind;
    List<Rule> queries;

    public Check(com.clevercloud.biscuit.datalog.Check.Kind kind, List<Rule> queries) {
        this.kind = kind;
        this.queries = queries;
    }

    public Check(com.clevercloud.biscuit.datalog.Check.Kind kind, Rule query) {
        this.kind = kind;

        ArrayList<Rule> r = new ArrayList<>();
        r.add(query);
        queries = r;
    }

    public com.clevercloud.biscuit.datalog.Check convert(SymbolTable symbols) {
        ArrayList<com.clevercloud.biscuit.datalog.Rule> queries = new ArrayList<>();

        for(Rule q: this.queries) {
            queries.add(q.convert(symbols));
        }
        return new com.clevercloud.biscuit.datalog.Check(this.kind, queries);
    }

    public static Check convert_from(com.clevercloud.biscuit.datalog.Check r, SymbolTable symbols) {
        ArrayList<Rule> queries = new ArrayList<>();

        for(com.clevercloud.biscuit.datalog.Rule q: r.queries()) {
            queries.add(Rule.convert_from(q, symbols));
        }

        return new Check(r.kind(), queries);
    }

    @Override
    public String toString() {
        final List<String> qs = queries.stream().map((q) -> {
            final List<String> b = q.body.stream().map((pred) -> pred.toString()).collect(Collectors.toList());
            String res = String.join(", ", b);

            if(!q.expressions.isEmpty()) {
                final List<String> e = q.expressions.stream().map((expression) -> expression.toString()).collect(Collectors.toList());
                res += ", "+ String.join(", ", e);
            }

            return res;
        }).collect(Collectors.toList());

        if(kind == One) {
            return "check if " + String.join(" or ", qs);
        } else {
            return "check all " + String.join(" or ", qs);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Check check = (Check) o;

        return queries != null ? queries.equals(check.queries) : check.queries == null;
    }

    @Override
    public int hashCode() {
        return queries != null ? queries.hashCode() : 0;
    }
}
