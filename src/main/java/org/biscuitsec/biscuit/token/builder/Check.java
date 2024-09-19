package org.biscuitsec.biscuit.token.builder;

import org.biscuitsec.biscuit.datalog.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.biscuitsec.biscuit.datalog.Check.Kind.One;

public class Check {
    final List<Rule> queries;
    private final org.biscuitsec.biscuit.datalog.Check.Kind kind;

    public Check(org.biscuitsec.biscuit.datalog.Check.Kind kind, List<Rule> queries) {
        this.kind = kind;
        this.queries = queries;
    }

    public Check(org.biscuitsec.biscuit.datalog.Check.Kind kind, Rule query) {
        this.kind = kind;

        ArrayList<Rule> r = new ArrayList<>();
        r.add(query);
        queries = r;
    }

    public org.biscuitsec.biscuit.datalog.Check convert(SymbolTable symbols) {
        ArrayList<org.biscuitsec.biscuit.datalog.Rule> queries = new ArrayList<>();

        for (Rule q : this.queries) {
            queries.add(q.convert(symbols));
        }
        return new org.biscuitsec.biscuit.datalog.Check(this.kind, queries);
    }

    public static Check convertFrom(org.biscuitsec.biscuit.datalog.Check r, SymbolTable symbols) {
        ArrayList<Rule> queries = new ArrayList<>();

        for (org.biscuitsec.biscuit.datalog.Rule q : r.queries()) {
            queries.add(Rule.convert_from(q, symbols));
        }

        return new Check(r.kind(), queries);
    }

    @Override
    public int hashCode() {
        return queries != null ? queries.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Check check = (Check) o;

        return Objects.equals(queries, check.queries);
    }

    @Override
    public String toString() {
        final List<String> qs = queries.stream().map(Rule::bodyToString).collect(toList());

        if (kind == One) {
            return "check if " + String.join(" or ", qs);
        } else {
            return "check all " + String.join(" or ", qs);
        }
    }
}
