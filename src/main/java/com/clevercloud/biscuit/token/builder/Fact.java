package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.List;

public class Fact {
    Predicate predicate;

    public Fact(String name, List<Atom> ids) {
        this.predicate = new Predicate(name, ids);
    }

    public Fact(Predicate p) {
        this.predicate = p;
    }

    public com.clevercloud.biscuit.datalog.Fact convert(SymbolTable symbols) {
        return new com.clevercloud.biscuit.datalog.Fact(this.predicate.convert(symbols));
    }

    public static Fact convert_from(com.clevercloud.biscuit.datalog.Fact f, SymbolTable symbols) {
        return new Fact(Predicate.convert_from(f.predicate(), symbols));
    }

    @Override
    public String toString() {
        return "fact("+predicate+")";
    }

    public String name() {
        return this.predicate.name;
    }

    public List<Atom> ids() { return this.predicate.ids; }
}
