package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.RunLimits;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.World;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.*;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.clevercloud.biscuit.token.builder.Utils.*;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Token verification class
 */
public class Verifier {
    Biscuit token;
    List<Check> checks;
    List<List<com.clevercloud.biscuit.datalog.Check>> token_checks;
    List<Policy> policies;
    World world;
    SymbolTable symbols;

    private Verifier(Biscuit token, World w) {
        this.token = token;
        this.world = w;
        this.symbols = new SymbolTable(this.token.symbols);
        this.checks = new ArrayList<>();
        this.policies = new ArrayList<>();
        this.token_checks = this.token.checks();
    }

    /**
     * Creates an empty verifier
     *
     * used to apply policies when unauthenticated (no token)
     * and to preload a verifier that is cloned for each new request
     */
    public Verifier() {
        this.world = new World();
        this.symbols = Biscuit.default_symbol_table();
        this.checks = new ArrayList<>();
        this.policies = new ArrayList<>();
        this.token_checks = new ArrayList<>();
    }

    Verifier(Biscuit token, List<Check> checks, List<Policy> policies,
             List<List<com.clevercloud.biscuit.datalog.Check>> token_checks, World world, SymbolTable symbols) {
        this.token = token;
        this.checks = checks;
        this.policies = policies;
        this.token_checks = token_checks;
        this.world = world;
        this.symbols = symbols;
    }

    /**
     * Creates a verifier for a token
     *
     * also checks that the token is valid for this root public key
     * @param token
     * @param root
     * @return
     */
    static public Either<Error, Verifier> make(Biscuit token, Option<PublicKey> root) {
        if(!token.is_sealed()) {
            Either<Error, Void> res = token.check_root_key(root.get());
            if (res.isLeft()) {
                Error e = res.getLeft();
                return Left(e);
            }
        }

        Either<Error, World> res = token.generate_world();
        if (res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        return Right(new Verifier(token, res.get()));
    }

    public Verifier clone() {
        return new Verifier(this.token, new ArrayList<>(this.checks), new ArrayList<>(this.policies),
                new ArrayList<>(this.token_checks), new World(this.world), new SymbolTable(this.symbols));
    }

    public Either<Error, Void> add_token(Biscuit token, Option<PublicKey> root) {
        if(!token.is_sealed()) {
            Either<Error, Void> res = token.check_root_key(root.get());
            if (res.isLeft()) {
                Error e = res.getLeft();
                return Left(e);
            }
        }

        if(this.token != null) {
            return Either.left(new Error.FailedLogic(new LogicError.VerifierNotEmpty()));
        }

        long authority_index = symbols.get("authority").get();
        long ambient_index = symbols.get("ambient").get();

        for(com.clevercloud.biscuit.datalog.Fact fact: token.authority.facts) {
            if (fact.predicate().ids().get(0).equals(new ID.Symbol(ambient_index))) {
                return Left(new Error.FailedLogic(new LogicError.InvalidAuthorityFact(symbols.print_fact(fact))));
            }

            com.clevercloud.biscuit.datalog.Fact converted_fact = Fact.convert_from(fact, token.symbols).convert(this.symbols);
            world.add_fact(converted_fact);
        }

        for(com.clevercloud.biscuit.datalog.Rule rule: token.authority.rules) {
            com.clevercloud.biscuit.datalog.Rule converted_rule = Rule.convert_from(rule, token.symbols).convert(this.symbols);
            world.add_rule(converted_rule);
        }

        List<com.clevercloud.biscuit.datalog.Check> authority_checks = new ArrayList<>();
        for(com.clevercloud.biscuit.datalog.Check check: token.authority.checks) {
            com.clevercloud.biscuit.datalog.Check converted_check = Check.convert_from(check, token.symbols).convert(this.symbols);
            authority_checks.add(converted_check);
        }
        token_checks.add(authority_checks);

        for(int i = 0; i < token.blocks.size(); i++) {
            Block b = token.blocks.get(i);
            if (b.index != i + 1) {
                return Left(new Error.InvalidBlockIndex(1 + token.blocks.size(), token.blocks.get(i).index));
            }

            for (com.clevercloud.biscuit.datalog.Fact fact : b.facts) {
                if (fact.predicate().ids().get(0).equals(new ID.Symbol(authority_index)) ||
                        fact.predicate().ids().get(0).equals(new ID.Symbol(ambient_index))) {
                    return Left(new Error.FailedLogic(new LogicError.InvalidBlockFact(i, symbols.print_fact(fact))));
                }

                com.clevercloud.biscuit.datalog.Fact converted_fact = Fact.convert_from(fact, token.symbols).convert(this.symbols);
                world.add_fact(converted_fact);
            }

            for (com.clevercloud.biscuit.datalog.Rule rule : b.rules) {
                com.clevercloud.biscuit.datalog.Rule converted_rule = Rule.convert_from(rule, token.symbols).convert(this.symbols);
                world.add_rule(converted_rule);
            }

            List<com.clevercloud.biscuit.datalog.Check> block_checks = new ArrayList<>();
            for(com.clevercloud.biscuit.datalog.Check check: b.checks) {
                com.clevercloud.biscuit.datalog.Check converted_check = Check.convert_from(check, token.symbols).convert(this.symbols);
                block_checks.add(converted_check);
            }
            token_checks.add(block_checks);
        }

        List<byte[]> revocation_ids = token.revocation_identifiers();
        long rev = symbols.get("revocation_id").get();
        for(int i = 0; i < revocation_ids.size(); i++) {
            byte[] id = revocation_ids.get(i);
            world.add_fact(new com.clevercloud.biscuit.datalog.Fact(new com.clevercloud.biscuit.datalog.Predicate(rev, Arrays.asList(new ID.Integer(i), new ID.Bytes(id)))));
        }

        return Right(null);
    }

    public void add_fact(Fact fact) {
        world.add_fact(fact.convert(symbols));
    }

    public Either<Error, Void> add_fact(String s) {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, Fact>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.fact(s);

        if (res.isLeft()) {
            return Either.left(new Error.Parser(res.getLeft()));
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Fact> t = res.get();

        add_fact(t._2);

        return Either.right(null);
    }

    public void add_rule(Rule rule) {
        world.add_rule(rule.convert(symbols));
    }

    public Either<Error, Void> add_rule(String s) {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            return Either.left(new Error.Parser(res.getLeft()));
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();

        add_rule(t._2);

        return Either.right(null);
    }

    public void add_check(Check check) {
        this.checks.add(check);
        world.add_check(check.convert(symbols));
    }

    public Either<Error, Void> add_check(String s) {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Check>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.check(s);

        if (res.isLeft()) {
            return Either.left(new Error.Parser(res.getLeft()));
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Check> t = res.get();

        add_check(t._2);

        return Either.right(null);
    }

    public void add_resource(String resource) {
        world.add_fact(fact("resource", Arrays.asList(s("ambient"), string(resource))).convert(symbols));
    }

    public void add_operation(String operation) {
        world.add_fact(fact("operation", Arrays.asList(s("ambient"), s(operation))).convert(symbols));
    }

    public void set_time() {

        world.add_fact(fact("time", Arrays.asList(s("ambient"), date(new Date()))).convert(symbols));
    }

    public void revocation_check(List<Long> ids) {
        ArrayList<Rule> q = new ArrayList<>();

        q.add(constrained_rule(
                "revocation_check",
                Arrays.asList((var("id"))),
                Arrays.asList(pred("revocation_id", Arrays.asList(var("id")))),
                Arrays.asList(
                        new Expression.Unary(
                                Expression.Op.Negate,
                                new Expression.Binary(
                                        Expression.Op.Contains,
                                        new Expression.Value(var("id")),
                                        new Expression.Value(new Term.Set(new HashSet(ids)))
                                )
                        )
                )
        ));

        this.checks.add(new Check(q));
    }

    public Either<Error, List<String>> get_revocation_ids() {
        ArrayList<String> ids = new ArrayList<>();

        final Rule getRevocationIds = rule(
                "revocation_id",
                Arrays.asList(var("id")),
                Arrays.asList(pred("revocation_id", Arrays.asList(var("id"))))
        );

        Either<Error, Set<Fact>> queryRes = this.query(getRevocationIds);
        if (queryRes.isLeft()) {
            Error e = queryRes.getLeft();
            return Left(e);
        }

        queryRes.get().stream().forEach(fact -> {
            fact.ids().stream().forEach(id -> {
                if (id instanceof Term.Str) {
                    ids.add((((Term.Str) id).value()));
                }
            });
        });

        return Right(ids);
    }

    public void allow() {
        ArrayList<Rule> q = new ArrayList<>();

        q.add(constrained_rule(
                "allow",
                new ArrayList<>(),
                new ArrayList<>(),
                Arrays.asList(new Expression.Value(new Term.Bool(true)))
        ));

        this.policies.add(new Policy(q, Policy.Kind.Allow));
    }

    public void deny() {
        ArrayList<Rule> q = new ArrayList<>();

        q.add(constrained_rule(
                "deny",
                new ArrayList<>(),
                new ArrayList<>(),
                Arrays.asList(new Expression.Value(new Term.Bool(true)))
        ));

        this.policies.add(new Policy(q, Policy.Kind.Deny));
    }

    public Either<Error, Void> add_policy(String s) {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.Policy>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.policy(s);

        if (res.isLeft()) {
            return Either.left(new Error.Parser(res.getLeft()));
        }

        Tuple2<String, com.clevercloud.biscuit.token.Policy> t = res.get();

        this.policies.add(t._2);
        return Either.right(null);
    }

    public Either<Error, Set<Fact>> query(Rule query) {
        return this.query(query, new RunLimits());
    }

    public Either<Error, Set<Fact>> query(String s) {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            return Either.left(new Error.Parser(res.getLeft()));
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();

        return query(t._2);
    }

    public Either<Error, Set<Fact>> query(Rule query, RunLimits limits) {
        Either<Error, Void> runRes = world.run(limits);
        if (runRes.isLeft()) {
            Error e = runRes.getLeft();
            return Left(e);
        }

        Set<com.clevercloud.biscuit.datalog.Fact> facts = world.query_rule(query.convert(symbols));
        Set<Fact> s = new HashSet();

        for(com.clevercloud.biscuit.datalog.Fact f: facts) {
            s.add(Fact.convert_from(f, symbols));
        }

        return Right(s);
    }

    public Either<Error, Set<Fact>> query(String s, RunLimits limits) {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            return Either.left(new Error.Parser(res.getLeft()));
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();

        return query(t._2, limits);
    }

    public Either<Error, Long> verify() {
        return this.verify(new RunLimits());
    }

    public Either<Error, Long> verify(RunLimits limits) {
        Instant timeLimit = Instant.now().plus(limits.maxTime);

        if(this.symbols.get("authority").isEmpty() || this.symbols.get("ambient").isEmpty()) {
            return Left(new Error.MissingSymbols());
        }

        Either<Error, Void> runRes = world.run(limits);
        if (runRes.isLeft()) {
            Error e = runRes.getLeft();
            return Left(e);
        }

        SymbolTable symbols = new SymbolTable(this.symbols);

        ArrayList<FailedCheck> errors = new ArrayList<>();

        for (int j = 0; j < this.checks.size(); j++) {
            com.clevercloud.biscuit.datalog.Check c = this.checks.get(j).convert(symbols);
            boolean successful = false;

            for(int k = 0; k < c.queries().size(); k++) {
                boolean res = world.test_rule(c.queries().get(k));

                if(Instant.now().compareTo(timeLimit) >= 0) {
                    return Left(new Error.Timeout());
                }

                if (res) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedVerifier(j, symbols.print_check(c)));
            }
        }


        for (int i = 0; i < this.token_checks.size(); i++) {
            List<com.clevercloud.biscuit.datalog.Check> checks = this.token_checks.get(i);

            for (int j = 0; j < checks.size(); j++) {
                boolean successful = false;
                com.clevercloud.biscuit.datalog.Check c = checks.get(j);

                for (int k = 0; k < c.queries().size(); k++) {
                    boolean res = world.test_rule(c.queries().get(k));

                    if (Instant.now().compareTo(timeLimit) >= 0) {
                        return Left(new Error.Timeout());
                    }

                    if (res) {
                        successful = true;
                        break;
                    }
                }

                if (!successful) {
                    errors.add(new FailedCheck.FailedBlock(i, j, symbols.print_check(checks.get(j))));
                }
            }
        }

        if(errors.isEmpty()) {
            for (int i = 0; i < this.policies.size(); i++) {
                com.clevercloud.biscuit.datalog.Check c = this.policies.get(i).convert(symbols);
                boolean successful = false;

                for(int k = 0; k < c.queries().size(); k++) {
                    boolean res = world.test_rule(c.queries().get(k));

                    if(Instant.now().compareTo(timeLimit) >= 0) {
                        return Left(new Error.Timeout());
                    }

                    if (res) {
                        if(this.policies.get(i).kind == Policy.Kind.Deny) {
                            return Left(new Error.FailedLogic(new LogicError.Denied(i)));
                        } else {
                            return Right(Long.valueOf(i));
                        }
                    }
                }
            }

            return Left(new Error.FailedLogic(new LogicError.NoMatchingPolicy()));
        } else {
            return Left(new Error.FailedLogic(new LogicError.FailedChecks(errors)));
        }
    }

    public String print_world() {
        final List<String> facts = this.world.facts().stream().map((f) -> this.symbols.print_fact(f)).collect(Collectors.toList());
        final List<String> rules = this.world.rules().stream().map((r) -> this.symbols.print_rule(r)).collect(Collectors.toList());

        List<String> checks = new ArrayList<>();

        for (int j = 0; j < this.checks.size(); j++) {
            checks.add("Verifier["+j+"]: "+this.checks.get(j).toString());
        }

        if(this.token != null) {
            for (int j = 0; j < this.token.authority.checks.size(); j++) {
                checks.add("Block[0][" + j + "]: " + this.symbols.print_check(this.token.authority.checks.get(j)));
            }

            for (int i = 0; i < this.token.blocks.size(); i++) {
                Block b = this.token.blocks.get(i);

                for (int j = 0; j < b.checks.size(); j++) {
                    checks.add("Block[" + i + "][" + j + "]: " + this.symbols.print_check(b.checks.get(j)));
                }
            }
        }

        StringBuilder b = new StringBuilder();
        b.append("World {\n\tfacts: [\n\t\t");
        b.append(String.join(",\n\t\t", facts));
        b.append("\n\t],\n\trules: [\n\t\t");
        b.append(String.join(",\n\t\t", rules));
        b.append("\n\t],\n\tchecks: [\n\t\t");
        b.append(String.join(",\n\t\t", checks));
        b.append("\n\t]\n}");

        return b.toString();
    }
}
