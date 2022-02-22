package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.token.builder.Rule;

import java.util.ArrayList;
import java.util.List;

public class Policy {
    public enum Kind {
        Allow,
        Deny,
    }

    public final List<Rule> queries;
    public Kind kind;

    public Policy(List<Rule> queries, Kind kind) {
        this.queries = queries;
        this.kind = kind;
    }

    public Policy(Rule query, Kind kind) {
        ArrayList<Rule> r = new ArrayList<>();
        r.add(query);

        this.queries = r;
        this.kind = kind;
    }

    public com.clevercloud.biscuit.datalog.Check convert(SymbolTable symbols) {
        ArrayList<com.clevercloud.biscuit.datalog.Rule> queries = new ArrayList<>();

        for(Rule q: this.queries) {
            queries.add(q.convert(symbols));
        }
        return new com.clevercloud.biscuit.datalog.Check(queries);
    }

    @Override
    public String toString() {
        switch(this.kind) {
            case Allow:
                return "allow if "+queries;
            case Deny:
                return "deny if "+queries;
        }
        return null;
    }

}
