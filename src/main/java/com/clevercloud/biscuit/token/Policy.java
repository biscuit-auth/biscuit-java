package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.token.builder.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                return "allow if "+ queries;
            case Deny:
                return "deny if " + queries;
        }
        return null;
    }

    public String print(SymbolTable symbolTable) {
        String formattedQueries = queries.stream().map((q) -> symbolTable.print_rule(q.convert(symbolTable))).collect(Collectors.joining());
        switch (this.kind) {
            case Allow:
                return "allow if " + formattedQueries;
            case Deny:
                return "deny if " + formattedQueries;
        }
        return null;
    }

}
