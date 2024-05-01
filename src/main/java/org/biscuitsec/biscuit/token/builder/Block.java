package org.biscuitsec.biscuit.token.builder;


import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.*;
import org.biscuitsec.biscuit.datalog.Check;
import org.biscuitsec.biscuit.datalog.Fact;
import org.biscuitsec.biscuit.datalog.Rule;
import org.biscuitsec.biscuit.error.Error;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.biscuitsec.biscuit.datalog.Scope;
import org.biscuitsec.biscuit.token.builder.parser.Parser;

import static org.biscuitsec.biscuit.datalog.Check.Kind.One;
import static org.biscuitsec.biscuit.token.builder.Utils.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Block {
    long index;
    int symbol_start;
    int publicKeyStart;
    SymbolTable symbols;
    String context;
    List<Fact> facts;
    List<Rule> rules;
    List<Check> checks;
    List<Scope> scopes;
    List<PublicKey> publicKeys;
    Option<PublicKey> externalKey;

    public Block(long index, SymbolTable base_symbols) {
        this.index = index;
        this.symbol_start = base_symbols.currentOffset();
        this.publicKeyStart = base_symbols.currentPublicKeyOffset();
        this.symbols = new SymbolTable(base_symbols);
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.scopes = new ArrayList<>();
        this.publicKeys = new ArrayList<>();
        this.externalKey = Option.none();
    }

    public Block setExternalKey(Option<PublicKey> externalKey) {
        this.externalKey = externalKey;
        return this;
    }

    public Block addPublicKey(PublicKey publicKey) {
        this.publicKeys.add(publicKey);
        return this;
    }

    public Block addPublicKeys(List<PublicKey> publicKeys) {
        this.publicKeys.addAll(publicKeys);
        return this;
    }

    public Block setPublicKeys(List<PublicKey> publicKeys) {
        this.publicKeys = publicKeys;
        return this;
    }

    public Block addSymbol(String symbol) {
        this.symbols.add(symbol);
        return this;
    }

    public Block add_fact(org.biscuitsec.biscuit.token.builder.Fact f) {
        this.facts.add(f.convert(this.symbols));
        return this;
    }

    public Block add_fact(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact>> res =
                Parser.fact(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact> t = res.get();

        return add_fact(t._2);
    }

    public Block add_rule(org.biscuitsec.biscuit.token.builder.Rule rule) {
        this.rules.add(rule.convert(this.symbols));
        return this;
    }

    public Block add_rule(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule>> res =
                Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule> t = res.get();

        return add_rule(t._2);
    }

    public Block add_check(org.biscuitsec.biscuit.token.builder.Check check) {
        this.checks.add(check.convert(this.symbols));
        return this;
    }

    public Block add_check(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Check>> res =
                Parser.check(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Check> t = res.get();

        return add_check(t._2);
    }

    public Block add_scope(org.biscuitsec.biscuit.token.builder.Scope scope) {
        this.scopes.add(scope.convert(this.symbols));
        return this;
    }

    public Block set_context(String context) {
        this.context = context;
        return this;
    }

    public org.biscuitsec.biscuit.token.Block build() {
        SymbolTable symbols = new SymbolTable();

        for (int i = this.symbol_start; i < this.symbols.symbols.size(); i++) {
            symbols.add(this.symbols.symbols.get(i));
        }

        List<PublicKey> publicKeys = new ArrayList<>();
        for (int i = this.publicKeyStart; i < this.symbols.currentPublicKeyOffset(); i++) {
            publicKeys.add(this.symbols.publicKeys().get(i));
        }

        SchemaVersion schemaVersion = new SchemaVersion(this.facts, this.rules, this.checks, this.scopes);

        return new org.biscuitsec.biscuit.token.Block(symbols, this.context, this.facts, this.rules, this.checks,
                this.scopes, publicKeys, this.externalKey, schemaVersion.version());
    }

    public Block check_right(String right) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();
        queries.add(rule(
                "check_right",
                Arrays.asList(s(right)),
                Arrays.asList(
                        pred("resource", Arrays.asList(var("resource"))),
                        pred("operation", Arrays.asList(s(right))),
                        pred("right", Arrays.asList(var("resource"), s(right)))
                )
        ));
        return this.add_check(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }

    public Block resource_prefix(String prefix) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "prefix",
                Arrays.asList(var("resource")),
                Arrays.asList(pred("resource", Arrays.asList(var("resource")))),
                Arrays.asList(new Expression.Binary(Expression.Op.Prefix, new Expression.Value(var("resource")),
                        new Expression.Value(string(prefix))))
        ));
        return this.add_check(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }

    public Block resource_suffix(String suffix) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "suffix",
                Arrays.asList(var("resource")),
                Arrays.asList(pred("resource", Arrays.asList(var("resource")))),
                Arrays.asList(new Expression.Binary(Expression.Op.Suffix, new Expression.Value(var("resource")),
                        new Expression.Value(string(suffix))))
        ));
        return this.add_check(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }

    public Block expiration_date(Date d) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "expiration",
                Arrays.asList(var("date")),
                Arrays.asList(pred("time", Arrays.asList(var("date")))),
                Arrays.asList(new Expression.Binary(Expression.Op.LessOrEqual, new Expression.Value(var("date")),
                        new Expression.Value(date(d))))
        ));
        return this.add_check(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }
}
