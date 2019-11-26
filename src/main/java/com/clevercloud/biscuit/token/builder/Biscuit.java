package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.Rule;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Block;
import io.vavr.control.Either;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.clevercloud.biscuit.token.builder.Utils.*;

public class Biscuit {
    SecureRandom rng;
    KeyPair root;
    int symbol_start;
    SymbolTable symbols;
    String context;
    List<Fact> facts;
    List<Rule> rules;
    List<Rule> caveats;

    public Biscuit(final SecureRandom rng, final KeyPair root, SymbolTable base_symbols) {
        this.rng = rng;
        this.root = root;
        this.symbol_start = base_symbols.symbols.size();
        this.symbols = new SymbolTable(base_symbols);
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.caveats = new ArrayList<>();
    }

    public void add_authority_fact(com.clevercloud.biscuit.token.builder.Fact f) {
        Atom.Symbol authority_symbol = new Atom.Symbol("authority");
        if(f.predicate.ids.isEmpty() || !(f.predicate.ids.get(0).equals(authority_symbol))) {
            ArrayList<Atom> ids = new ArrayList<>();
            ids.add(authority_symbol);
            for(Atom id: f.predicate.ids) {
                ids.add(id);
            }
            f.predicate.ids = ids;
        }

        this.facts.add(f.convert(this.symbols));
    }

    public void add_authority_rule(com.clevercloud.biscuit.token.builder.Rule rule) {
        Atom.Symbol authority_symbol = new Atom.Symbol("authority");
        if(rule.head.ids.isEmpty() || !(rule.head.ids.get(0).equals(authority_symbol))) {
            rule.head.ids.add(0, authority_symbol);
        }

        this.rules.add(rule.convert(this.symbols));
    }

    public void add_authority_caveat(com.clevercloud.biscuit.token.builder.Rule rule) {
        this.caveats.add(rule.convert(this.symbols));
    }

    public  void set_context(String context) {
        this.context = context;
    }

    public Either<Error, com.clevercloud.biscuit.token.Biscuit> build() {
        SymbolTable base_symbols = new SymbolTable();
        SymbolTable symbols = new SymbolTable();

        for(int i = 0; i < this.symbol_start; i++) {
            base_symbols.add(this.symbols.symbols.get(i));
        }

        for(int i = this.symbol_start; i < this.symbols.symbols.size(); i++) {
            symbols.add(this.symbols.symbols.get(i));
        }

        Block authority_block = new com.clevercloud.biscuit.token.Block(0, symbols, context, this.facts, this.rules, this.caveats);
        return com.clevercloud.biscuit.token.Biscuit.make(this.rng, this.root, base_symbols, authority_block);
    }

    public void add_right(String resource, String right) {
            this.add_authority_fact(fact("right", Arrays.asList(s("authority"), string(resource), s(right))));
    }
}
