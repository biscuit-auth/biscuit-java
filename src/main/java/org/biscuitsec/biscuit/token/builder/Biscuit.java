package org.biscuitsec.biscuit.token.builder;

import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.*;
import org.biscuitsec.biscuit.datalog.Check;
import org.biscuitsec.biscuit.datalog.Fact;
import org.biscuitsec.biscuit.datalog.Rule;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.Block;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.biscuitsec.biscuit.datalog.Scope;
import org.biscuitsec.biscuit.token.builder.parser.Parser;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Biscuit {
    SecureRandom rng;
    KeyPair root;
    int symbol_start;
    int publicKeyStart;
    SymbolTable symbols;
    String context;
    List<Fact> facts;
    List<Rule> rules;
    List<Check> checks;
    List<Scope> scopes;
    Option<Integer> root_key_id;

    public Biscuit(final SecureRandom rng, final KeyPair root, SymbolTable base_symbols) {
        this.rng = rng;
        this.root = root;
        this.symbol_start = base_symbols.currentOffset();
        this.publicKeyStart = base_symbols.currentPublicKeyOffset();
        this.symbols = new SymbolTable(base_symbols);
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.scopes = new ArrayList<>();
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
        this.scopes = new ArrayList<>();
        this.root_key_id = root_key_id;
    }

    public Biscuit add_authority_fact(org.biscuitsec.biscuit.token.builder.Fact f) throws Error.Language {
        f.validate();
        this.facts.add(f.convert(this.symbols));
        return this;
    }

    public Biscuit add_authority_fact(String s) throws Error.Parser, Error.Language {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact>> res =
                Parser.fact(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact> t = res.get();

        return add_authority_fact(t._2);
    }

    public Biscuit add_authority_rule(org.biscuitsec.biscuit.token.builder.Rule rule) {
        this.rules.add(rule.convert(this.symbols));
        return this;
    }

    public Biscuit add_authority_rule(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule>> res =
                Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule> t = res.get();

        return add_authority_rule(t._2);
    }

    public Biscuit add_authority_check(org.biscuitsec.biscuit.token.builder.Check c) {
        this.checks.add(c.convert(this.symbols));
        return this;
    }

    public Biscuit add_authority_check(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Check>> res =
                Parser.check(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Check> t = res.get();

        return add_authority_check(t._2);
    }

    public Biscuit set_context(String context) {
        this.context = context;
        return this;
    }

    public Biscuit add_scope(org.biscuitsec.biscuit.token.builder.Scope scope) {
        this.scopes.add(scope.convert(this.symbols));
        return this;
    }

    public void set_root_key_id(Integer i) {
        this.root_key_id = Option.some(i);
    }

    public org.biscuitsec.biscuit.token.Biscuit build() throws Error {
        SymbolTable base_symbols = new SymbolTable();
        SymbolTable symbols = new SymbolTable();

        for (int i = 0; i < this.symbol_start; i++) {
            base_symbols.add(this.symbols.symbols.get(i));
        }

        for (int i = this.symbol_start; i < this.symbols.symbols.size(); i++) {
            symbols.add(this.symbols.symbols.get(i));
        }

        List<PublicKey> publicKeys = new ArrayList<>();
        for (int i = this.publicKeyStart; i < this.symbols.currentPublicKeyOffset(); i++) {
            publicKeys.add(this.symbols.publicKeys().get(i));
        }

        SchemaVersion schemaVersion = new SchemaVersion(this.facts, this.rules, this.checks, this.scopes);

        Block authority_block = new Block(symbols, context, this.facts, this.rules,
                this.checks, scopes, publicKeys, Option.none(), schemaVersion.version());

        if (this.root_key_id.isDefined()) {
            return org.biscuitsec.biscuit.token.Biscuit.make(this.rng, this.root, this.root_key_id.get(), base_symbols, authority_block);
        } else {
            return org.biscuitsec.biscuit.token.Biscuit.make(this.rng, this.root, base_symbols, authority_block);
        }
    }

    public Biscuit add_right(String resource, String right) throws Error.Language {
        return this.add_authority_fact(Utils.fact("right", Arrays.asList(Utils.string(resource), Utils.s(right))));
    }
}
