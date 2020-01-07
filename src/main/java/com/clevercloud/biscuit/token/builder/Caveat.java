package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;

import java.util.ArrayList;
import java.util.List;

public class Caveat {
    List<Rule> queries;

    public Caveat(List<Rule> queries) {
        this.queries = queries;
    }
    public Caveat(Rule query) {
        ArrayList<Rule> r = new ArrayList<>();
        r.add(query);
        queries = r;
    }


    public com.clevercloud.biscuit.datalog.Caveat convert(SymbolTable symbols) {
        ArrayList<com.clevercloud.biscuit.datalog.Rule> queries = new ArrayList<>();

        for(Rule q: this.queries) {
            queries.add(q.convert(symbols));
        }
        return new com.clevercloud.biscuit.datalog.Caveat(queries);
    }

    /*public static Caveat convert_from(com.clevercloud.biscuit.datalog.Caveat f, SymbolTable symbols) {
        ArrayList<Rule> queries = new ArrayList<>();
        for(com.clevercloud.biscuit.datalog.Rule q: f.queries()) {
            queries.add(Rule.convert_from(q, symbols));
        }
        return new Caveat(queries);
    }*/

    @Override
    public String toString() {
        return "caveat("+queries+")";
    }
}
