package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.List;

public class Fact {
    Predicate predicate;

    public Fact(String name, List<Atom> ids) {
        this.predicate = new Predicate(name, ids);
    }

    public com.clevercloud.biscuit.datalog.Fact convert(SymbolTable symbols) {
        return new com.clevercloud.biscuit.datalog.Fact(this.predicate.convert(symbols));
    }
}
