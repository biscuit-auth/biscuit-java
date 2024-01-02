package com.clevercloud.biscuit.datalog;

import java.util.Set;

public class AuthorizedWorld extends World {

    public AuthorizedWorld(FactSet facts) {
        super(facts);
    }

    /*public final Set<Fact> queryAll(final Rule rule, SymbolTable symbols) {
        return this.query_rule(rule, symbols);
    }*/
}
