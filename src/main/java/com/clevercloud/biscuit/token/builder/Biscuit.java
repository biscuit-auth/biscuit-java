package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.Rule;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.Check;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Block;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.clevercloud.biscuit.token.Biscuit.make;
import static com.clevercloud.biscuit.token.builder.Utils.*;

public class Biscuit {
    SecureRandom rng;
    KeyPair root;
    int symbol_start;
    SymbolTable symbols;
    String context;
    List<Fact> facts;
    List<Rule> rules;
    List<Check> checks;
    Option<Integer> root_key_id;

    public Biscuit(final SecureRandom rng, final KeyPair root, SymbolTable base_symbols) {
        this.rng = rng;
        this.root = root;
        this.symbol_start = base_symbols.symbols.size();
        this.symbols = new SymbolTable(base_symbols);
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.root_key_id = Option.none();
    }

    public Biscuit(final SecureRandom rng, final KeyPair root, Option<Integer> root_key_id, SymbolTable base_symbols) {
        this.rng = rng;
        this.root = root;
        this.symbol_start = base_symbols.symbols.size();
        this.symbols = new SymbolTable(base_symbols);
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.root_key_id = root_key_id;
    }

    public Biscuit add_authority_fact(com.clevercloud.biscuit.token.builder.Fact f) throws Error.Language {
        f.validate();
        this.facts.add(f.convert(this.symbols));
        return this;
    }

    public Biscuit add_authority_fact(String s) throws Error.Parser, Error.Language {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Fact>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.fact(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Fact> t = res.get();

        return add_authority_fact(t._2);
    }

    public Biscuit add_authority_rule(com.clevercloud.biscuit.token.builder.Rule rule) {
        this.rules.add(rule.convert(this.symbols));
        return this;
    }

    public Biscuit add_authority_rule(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();



        return add_authority_rule(t._2);
    }

    public Biscuit add_authority_check(com.clevercloud.biscuit.token.builder.Check c) {
        this.checks.add(c.convert(this.symbols));
        return this;
    }

    public Biscuit add_authority_check(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Check>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.check(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Check> t = res.get();

        return add_authority_check(t._2);
    }

    public Biscuit set_context(String context) {
        this.context = context;
        return this;
    }

    public void set_root_key_id(Integer i) {
        this.root_key_id = Option.some(i);
    }

    public com.clevercloud.biscuit.token.Biscuit build() throws Error {
        SymbolTable base_symbols = new SymbolTable();
        SymbolTable symbols = new SymbolTable();

        for (int i = 0; i < this.symbol_start; i++) {
            base_symbols.add(this.symbols.symbols.get(i));
        }

        for (int i = this.symbol_start; i < this.symbols.symbols.size(); i++) {
            symbols.add(this.symbols.symbols.get(i));
        }

        Block authority_block = new com.clevercloud.biscuit.token.Block(symbols, context, this.facts, this.rules, this.checks);
        if (this.root_key_id.isDefined()) {
            return make(this.rng, this.root, this.root_key_id.get(), base_symbols, authority_block);
        } else {
            return make(this.rng, this.root, base_symbols, authority_block);
        }
    }

    public Biscuit add_right(String resource, String right) throws Error.Language {
        return this.add_authority_fact(fact("right", Arrays.asList(string(resource), s(right))));
    }
}
