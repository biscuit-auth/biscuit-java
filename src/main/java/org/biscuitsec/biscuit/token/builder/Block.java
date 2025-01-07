package org.biscuitsec.biscuit.token.builder;


import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SchemaVersion;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.builder.parser.Parser;

import java.util.*;

import static org.biscuitsec.biscuit.datalog.Check.Kind.One;
import static org.biscuitsec.biscuit.token.UnverifiedBiscuit.defaultSymbolTable;
import static org.biscuitsec.biscuit.token.builder.Utils.*;

public class Block {
    final List<Fact> facts;
    final List<Rule> rules;
    final List<Check> checks;
    final List<Scope> scopes;
    String context;

    public Block() {
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.scopes = new ArrayList<>();
        this.context = "";
    }

    public Block addCheck(org.biscuitsec.biscuit.token.builder.Check check) {
        this.checks.add(check);
        return this;
    }

    public Block addCheck(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Check>> res =
                Parser.check(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Check> t = res.get();

        return addCheck(t._2);
    }

    public Block addFact(org.biscuitsec.biscuit.token.builder.Fact f) {
        this.facts.add(f);
        return this;
    }

    public Block addFact(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact>> res =
                Parser.fact(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Fact> t = res.get();

        return addFact(t._2);
    }

    public Block addRule(org.biscuitsec.biscuit.token.builder.Rule rule) {
        this.rules.add(rule);
        return this;
    }

    public Block addRule(String s) throws Error.Parser {
        Either<org.biscuitsec.biscuit.token.builder.parser.Error, Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule>> res =
                Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, org.biscuitsec.biscuit.token.builder.Rule> t = res.get();

        return addRule(t._2);
    }

    public Block addScope(org.biscuitsec.biscuit.token.builder.Scope scope) {
        this.scopes.add(scope);
        return this;
    }

    public org.biscuitsec.biscuit.token.Block build() {
        return build(defaultSymbolTable(), Option.none());
    }

    @SuppressWarnings("unused")
    public org.biscuitsec.biscuit.token.Block build(final Option<PublicKey> externalKey) {
        return build(defaultSymbolTable(), externalKey);
    }

    public org.biscuitsec.biscuit.token.Block build(SymbolTable symbolTable) {
        return build(symbolTable, Option.none());
    }

    public org.biscuitsec.biscuit.token.Block build(SymbolTable symbolTable, final Option<PublicKey> externalKey) {
        if (externalKey.isDefined()) {
            symbolTable = new SymbolTable();
        }
        int symbolStart = symbolTable.currentOffset();
        int publicKeyStart = symbolTable.currentPublicKeyOffset();

        List<org.biscuitsec.biscuit.datalog.Fact> facts = new ArrayList<>();
        for (Fact f : this.facts) {
            facts.add(f.convert(symbolTable));
        }
        List<org.biscuitsec.biscuit.datalog.Rule> rules = new ArrayList<>();
        for (Rule r : this.rules) {
            rules.add(r.convert(symbolTable));
        }
        List<org.biscuitsec.biscuit.datalog.Check> checks = new ArrayList<>();
        for (Check c : this.checks) {
            checks.add(c.convert(symbolTable));
        }
        List<org.biscuitsec.biscuit.datalog.Scope> scopes = new ArrayList<>();
        for (Scope s : this.scopes) {
            scopes.add(s.convert(symbolTable));
        }
        SchemaVersion schemaVersion = new SchemaVersion(facts, rules, checks, scopes);

        SymbolTable blockSymbols = new SymbolTable();

        for (int i = symbolStart; i < symbolTable.symbols.size(); i++) {
            blockSymbols.add(symbolTable.symbols.get(i));
        }

        List<PublicKey> publicKeys = new ArrayList<>();
        for (int i = publicKeyStart; i < symbolTable.currentPublicKeyOffset(); i++) {
            publicKeys.add(symbolTable.publicKeys().get(i));
        }

        return new org.biscuitsec.biscuit.token.Block(blockSymbols, this.context, facts, rules, checks,
                scopes, publicKeys, externalKey, schemaVersion.version());
    }

    public Block checkRight(String right) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();
        queries.add(rule(
                "check_right",
                List.of(s(right)),
                Arrays.asList(
                        pred("resource", List.of(var("resource"))),
                        pred("operation", List.of(s(right))),
                        pred("right", Arrays.asList(var("resource"), s(right)))
                )
        ));
        return this.addCheck(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }

    @SuppressWarnings("unused")
    public Block expirationDate(Date d) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrainedRule(
                "expiration",
                List.of(var("date")),
                List.of(pred("time", List.of(var("date")))),
                List.of(new Expression.Binary(Expression.Op.LessOrEqual, new Expression.Value(var("date")),
                        new Expression.Value(date(d))))
        ));
        return this.addCheck(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }

    @Override
    public int hashCode() {
        int result = context != null ? context.hashCode() : 0;
        result = 31 * result + facts.hashCode();
        result = 31 * result + rules.hashCode();
        result = 31 * result + checks.hashCode();
        result = 31 * result + scopes.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        if (!Objects.equals(context, block.context)) return false;
        if (!Objects.equals(facts, block.facts)) return false;
        if (!Objects.equals(rules, block.rules)) return false;
        if (!Objects.equals(checks, block.checks)) return false;
        return Objects.equals(scopes, block.scopes);
    }

    public Block resourcePrefix(String prefix) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrainedRule(
                "prefix",
                List.of(var("resource")),
                List.of(pred("resource", List.of(var("resource")))),
                List.of(new Expression.Binary(Expression.Op.Prefix, new Expression.Value(var("resource")),
                        new Expression.Value(string(prefix))))
        ));
        return this.addCheck(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }

    @SuppressWarnings("unused")
    public Block resourceSuffix(String suffix) {
        ArrayList<org.biscuitsec.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrainedRule(
                "suffix",
                List.of(var("resource")),
                List.of(pred("resource", List.of(var("resource")))),
                List.of(new Expression.Binary(Expression.Op.Suffix, new Expression.Value(var("resource")),
                        new Expression.Value(string(suffix))))
        ));
        return this.addCheck(new org.biscuitsec.biscuit.token.builder.Check(One, queries));
    }

    @SuppressWarnings("unused")
    public Block setContext(String context) {
        this.context = context;
        return this;
    }
}
