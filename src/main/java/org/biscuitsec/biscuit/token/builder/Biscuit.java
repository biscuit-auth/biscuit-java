package org.biscuitsec.biscuit.token.builder;

import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SchemaVersion;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.Block;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.biscuitsec.biscuit.token.builder.parser.Parser;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.biscuitsec.biscuit.token.UnverifiedBiscuit.default_symbol_table;

public class Biscuit {
    final SecureRandom rng;
    final KeyPair root;
    String context;
    final List<Fact> facts;
    final List<Rule> rules;
    final List<Check> checks;
    final List<Scope> scopes;
    Option<Integer> rootKeyId;

    public Biscuit(final SecureRandom rng, final KeyPair root) {
        this.rng = rng;
        this.root = root;
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.scopes = new ArrayList<>();
        this.rootKeyId = Option.none();
    }

    public Biscuit(final SecureRandom rng, final KeyPair root, Option<Integer> rootKeyId) {
        this.rng = rng;
        this.root = root;
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.scopes = new ArrayList<>();
        this.rootKeyId = rootKeyId;
    }

    public Biscuit(final SecureRandom rng,
                   final KeyPair root,
                   Option<Integer> rootKeyId,
                   org.biscuitsec.biscuit.token.builder.Block block) {
        this.rng = rng;
        this.root = root;
        this.rootKeyId = rootKeyId;
        this.context = block.context;
        this.facts = block.facts;
        this.rules = block.rules;
        this.checks = block.checks;
        this.scopes = block.scopes;
    }

    public Biscuit addAuthorityFact(org.biscuitsec.biscuit.token.builder.Fact f) throws Error.Language {
        f.validate();
        this.facts.add(f);
        return this;
    }

    public Biscuit addAuthorityFact(String s) throws Error.Parser, Error.Language {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact>> res =
                Parser.fact(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact> t = res.get();

        return addAuthorityFact(t._2);
    }

    public Biscuit addAuthorityRule(org.biscuitsec.biscuit.token.builder.Rule rule) {
        this.rules.add(rule);
        return this;
    }

    @SuppressWarnings("unused")
    public Biscuit addAuthorityRule(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule>> res =
                Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule> t = res.get();

        return addAuthorityRule(t._2);
    }

    public Biscuit addAuthorityCheck(org.biscuitsec.biscuit.token.builder.Check c) {
        this.checks.add(c);
        return this;
    }

    public Biscuit addAuthorityCheck(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Check>> res =
                Parser.check(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Check> t = res.get();

        return addAuthorityCheck(t._2);
    }

    @SuppressWarnings("unused")
    public Biscuit setContext(String context) {
        this.context = context;
        return this;
    }

    @SuppressWarnings("unused")
    public Biscuit addScope(org.biscuitsec.biscuit.token.builder.Scope scope) {
        this.scopes.add(scope);
        return this;
    }

    @SuppressWarnings("unused")
    public void setRootKeyId(Integer rootKeyId) {
        this.rootKeyId = Option.some(rootKeyId);
    }

    public org.biscuitsec.biscuit.token.Biscuit build() throws Error {
        return build(default_symbol_table());
    }

    private org.biscuitsec.biscuit.token.Biscuit build(SymbolTable symbols) throws Error {
        int symbolStart = symbols.currentOffset();
        int publicKeyStart = symbols.currentPublicKeyOffset();

        List<org.biscuitsec.biscuit.datalog.Fact> facts = new ArrayList<>();
        for(Fact f: this.facts) {
            facts.add(f.convert(symbols));
        }
        List<org.biscuitsec.biscuit.datalog.Rule> rules = new ArrayList<>();
        for(Rule r: this.rules) {
            rules.add(r.convert(symbols));
        }
        List<org.biscuitsec.biscuit.datalog.Check> checks = new ArrayList<>();
        for(Check c: this.checks) {
            checks.add(c.convert(symbols));
        }
        List<org.biscuitsec.biscuit.datalog.Scope> scopes = new ArrayList<>();
        for(Scope s: this.scopes) {
            scopes.add(s.convert(symbols));
        }
        SchemaVersion schemaVersion = new SchemaVersion(facts, rules, checks, scopes);

        SymbolTable blockSymbols = new SymbolTable();

        for (int i = symbolStart; i < symbols.symbols.size(); i++) {
            blockSymbols.add(symbols.symbols.get(i));
        }

        List<PublicKey> publicKeys = new ArrayList<>();
        for (int i = publicKeyStart; i < symbols.currentPublicKeyOffset(); i++) {
            publicKeys.add(symbols.publicKeys().get(i));
        }

        Block authority_block = new Block(blockSymbols, context, facts, rules,
                checks, scopes, publicKeys, Option.none(), schemaVersion.version());

        if (this.rootKeyId.isDefined()) {
            return org.biscuitsec.biscuit.token.Biscuit.make(this.rng, this.root, this.rootKeyId.get(), authority_block);
        } else {
            return org.biscuitsec.biscuit.token.Biscuit.make(this.rng, this.root, authority_block);
        }
    }

    public Biscuit addRight(String resource, String right) throws Error.Language {
        return this.addAuthorityFact(Utils.fact("right", Arrays.asList(Utils.string(resource), Utils.s(right))));
    }
}
