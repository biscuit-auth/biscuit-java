package com.clevercloud.biscuit.token;

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
public class Authorizer {
    Biscuit token;
    List<Check> checks;
    List<List<com.clevercloud.biscuit.datalog.Check>> token_checks;
    List<Policy> policies;
    World world;
    SymbolTable symbols;

    private Authorizer(Biscuit token, World w) {
        this.token = token;
        this.world = w;
        this.symbols = new SymbolTable(this.token.symbols);
        this.checks = new ArrayList<>();
        this.policies = new ArrayList<>();
        this.token_checks = this.token.checks();
    }

    /**
     * Creates an empty authorizer
     * <p>
     * used to apply policies when unauthenticated (no token)
     * and to preload a authorizer that is cloned for each new request
     */
    public Authorizer() {
        this.world = new World();
        this.symbols = Biscuit.default_symbol_table();
        this.checks = new ArrayList<>();
        this.policies = new ArrayList<>();
        this.token_checks = new ArrayList<>();
    }

    Authorizer(Biscuit token, List<Check> checks, List<Policy> policies,
               List<List<com.clevercloud.biscuit.datalog.Check>> token_checks, World world, SymbolTable symbols) {
        this.token = token;
        this.checks = checks;
        this.policies = policies;
        this.token_checks = token_checks;
        this.world = world;
        this.symbols = symbols;
    }

    /**
     * Creates a authorizer for a token
     * <p>
     * also checks that the token is valid for this root public key
     *
     * @param token
     * @param root
     * @return
     */
    static public Authorizer make(Biscuit token) {
        return new Authorizer(token, new World());
    }

    public Authorizer clone() {
        return new Authorizer(this.token, new ArrayList<>(this.checks), new ArrayList<>(this.policies),
                new ArrayList<>(this.token_checks), new World(this.world), new SymbolTable(this.symbols));
    }

    public Authorizer add_token(Biscuit token) throws Error.FailedLogic {
        if (this.token != null) {
            throw new Error.FailedLogic(new LogicError.AuthorizerNotEmpty());
        }

        this.token = token;

        return this;
    }

    public Authorizer add_fact(Fact fact) {
        world.add_fact(fact.convert(symbols));
        return this;
    }

    public Authorizer add_fact(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, Fact>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.fact(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Fact> t = res.get();

        return this.add_fact(t._2);
    }

    public Authorizer add_rule(Rule rule) {
        world.add_rule(rule.convert(symbols));
        return this;
    }

    public Authorizer add_rule(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();

        return add_rule(t._2);
    }

    public Authorizer add_check(Check check) {
        this.checks.add(check);
        return this;
    }

    public Authorizer add_check(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Check>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.check(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Check> t = res.get();

        return add_check(t._2);
    }

    public Authorizer set_time() throws Error.Language {
        world.add_fact(fact("time", Arrays.asList(date(new Date()))).convert(symbols));
        return this;
    }

    public List<String> get_revocation_ids() throws Error.TooManyFacts, Error.TooManyIterations, Error.Timeout {
        ArrayList<String> ids = new ArrayList<>();

        final Rule getRevocationIds = rule(
                "revocation_id",
                Arrays.asList(var("id")),
                Arrays.asList(pred("revocation_id", Arrays.asList(var("id"))))
        );

        this.query(getRevocationIds).stream().forEach(fact -> {
            fact.terms().stream().forEach(id -> {
                if (id instanceof Term.Str) {
                    ids.add(((Term.Str) id).getValue());
                }
            });
        });

        return ids;
    }

    public Authorizer allow() {
        ArrayList<Rule> q = new ArrayList<>();

        q.add(constrained_rule(
                "allow",
                new ArrayList<>(),
                new ArrayList<>(),
                Arrays.asList(new Expression.Value(new Term.Bool(true)))
        ));

        this.policies.add(new Policy(q, Policy.Kind.Allow));
        return this;
    }

    public Authorizer deny() {
        ArrayList<Rule> q = new ArrayList<>();

        q.add(constrained_rule(
                "deny",
                new ArrayList<>(),
                new ArrayList<>(),
                Arrays.asList(new Expression.Value(new Term.Bool(true)))
        ));

        this.policies.add(new Policy(q, Policy.Kind.Deny));
        return this;
    }

    public Authorizer add_policy(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.Policy>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.policy(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.Policy> t = res.get();

        this.policies.add(t._2);
        return this;
    }

    public Set<Fact> query(Rule query) throws Error.TooManyFacts, Error.TooManyIterations, Error.Timeout {
        return this.query(query, new RunLimits());
    }

    public Set<Fact> query(String s) throws Error.Parser, Error.TooManyFacts, Error.TooManyIterations, Error.Timeout {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();

        return query(t._2);
    }

    public Set<Fact> query(Rule query, RunLimits limits) throws Error.TooManyFacts, Error.TooManyIterations, Error.Timeout {
        world.run(limits, symbols);

        Set<com.clevercloud.biscuit.datalog.Fact> facts = world.query_rule(query.convert(symbols), symbols);
        Set<Fact> s = new HashSet();

        for (com.clevercloud.biscuit.datalog.Fact f : facts) {
            s.add(Fact.convert_from(f, symbols));
        }

        return s;
    }

    public Set<Fact> query(String s, RunLimits limits) throws Error.Parser, Error.TooManyFacts, Error.TooManyIterations, Error.Timeout {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();

        return query(t._2, limits);
    }

    public Long authorize() throws Error.Timeout, Error.FailedLogic, Error.TooManyFacts, Error.TooManyIterations, LogicError.InvalidBlockRule {
        return this.authorize(new RunLimits());
    }

    public Long authorize(RunLimits limits) throws Error.Timeout, Error.FailedLogic, Error.TooManyFacts, Error.TooManyIterations, LogicError.InvalidBlockRule {
        Instant timeLimit = Instant.now().plus(limits.maxTime);
        List<com.clevercloud.biscuit.datalog.Check> authority_checks = new ArrayList<>();
        Option<Either<Integer, Integer>> policy_result = Option.none();
        if (token != null) {
            for (com.clevercloud.biscuit.datalog.Fact fact : token.authority.facts) {
                com.clevercloud.biscuit.datalog.Fact converted_fact = Fact.convert_from(fact, token.symbols).convert(this.symbols);
                world.add_fact(converted_fact);
            }

            List<byte[]> revocation_ids = token.revocation_ids;
            Long revocation_id_sym = this.symbols.get("revocation_id").get();
            for (int i = 0; i < revocation_ids.size(); i++) {
                List<com.clevercloud.biscuit.datalog.Term> terms = Arrays.asList(new com.clevercloud.biscuit.datalog.Term.Integer(i), new com.clevercloud.biscuit.datalog.Term.Bytes(revocation_ids.get(i)));
                this.world.facts().add(new com.clevercloud.biscuit.datalog.Fact(revocation_id_sym.longValue(), terms));
            }
            for (com.clevercloud.biscuit.datalog.Rule rule : token.authority.rules) {
                com.clevercloud.biscuit.token.builder.Rule _rule = Rule.convert_from(rule, token.symbols);
                com.clevercloud.biscuit.datalog.Rule converted_rule = _rule.convert(this.symbols);

                Either<String,Rule> res = _rule.validate_variables();
                if(res.isLeft()){
                    throw new LogicError.InvalidBlockRule(0, token.symbols.print_rule(converted_rule));
                }
            }

            for (com.clevercloud.biscuit.datalog.Check check : token.authority.checks) {
                com.clevercloud.biscuit.datalog.Check converted_check = Check.convert_from(check, token.symbols).convert(this.symbols);
                authority_checks.add(converted_check);
            }
        }
        token_checks.add(authority_checks);

        world.run(limits, symbols);

        ArrayList<FailedCheck> errors = new ArrayList<>();

        for (int j = 0; j < this.checks.size(); j++) {
            com.clevercloud.biscuit.datalog.Check c = this.checks.get(j).convert(symbols);
            boolean successful = false;

            for (int k = 0; k < c.queries().size(); k++) {
                boolean res = world.test_rule(c.queries().get(k), symbols);

                if (Instant.now().compareTo(timeLimit) >= 0) {
                    throw new Error.Timeout();
                }

                if (res) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedAuthorizer(j, symbols.print_check(c)));
            }
        }

        for (int j = 0; j < authority_checks.size(); j++) {
            boolean successful = false;
            com.clevercloud.biscuit.datalog.Check c = authority_checks.get(j);

            for (int k = 0; k < c.queries().size(); k++) {
                boolean res = world.test_rule(c.queries().get(k), symbols);

                if (Instant.now().compareTo(timeLimit) >= 0) {
                    throw new Error.Timeout();
                }

                if (res) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedBlock(0, j, symbols.print_check(authority_checks.get(j))));
            }
        }
        if (token != null) {
            for (int i = 0; i < token.blocks.size(); i++) {
                Block b = token.blocks.get(i);

                for (com.clevercloud.biscuit.datalog.Fact fact : b.facts) {
                    com.clevercloud.biscuit.datalog.Fact converted_fact = Fact.convert_from(fact, token.symbols).convert(this.symbols);
                    world.add_fact(converted_fact);
                }

                for (com.clevercloud.biscuit.datalog.Rule rule : b.rules) {
                    com.clevercloud.biscuit.datalog.Rule converted_rule = Rule.convert_from(rule, token.symbols).convert(this.symbols);
                    world.add_rule(converted_rule);
                }

                List<com.clevercloud.biscuit.datalog.Check> block_checks = new ArrayList<>();
                for (com.clevercloud.biscuit.datalog.Check check : b.checks) {
                    com.clevercloud.biscuit.datalog.Check converted_check = Check.convert_from(check, token.symbols).convert(this.symbols);
                    block_checks.add(converted_check);
                }
                token_checks.add(block_checks);

                world.run(limits, symbols);

                for (int j = 0; j < block_checks.size(); j++) {
                    boolean successful = false;
                    com.clevercloud.biscuit.datalog.Check c = block_checks.get(j);

                    for (int k = 0; k < c.queries().size(); k++) {
                        boolean res = world.test_rule(c.queries().get(k), symbols);

                        if (Instant.now().compareTo(timeLimit) >= 0) {
                            throw new Error.Timeout();
                        }

                        if (res) {
                            successful = true;
                            break;
                        }
                    }

                    if (!successful) {
                        errors.add(new FailedCheck.FailedBlock(i + 1, j, symbols.print_check(block_checks.get(j))));
                    }
                }
            }
        }
        policies_test:
        for (int i = 0; i < this.policies.size(); i++) {
            com.clevercloud.biscuit.datalog.Check c = this.policies.get(i).convert(symbols);

            for (int k = 0; k < c.queries().size(); k++) {
                boolean res = world.test_rule(c.queries().get(k), symbols);

                if (Instant.now().compareTo(timeLimit) >= 0) {
                    throw new Error.Timeout();
                }

                if (res) {
                    if (this.policies.get(i).kind == Policy.Kind.Allow) {
                        policy_result = Option.some(Right(Integer.valueOf(i)));
                    } else {
                        policy_result = Option.some(Left(Integer.valueOf(i)));
                    }
                    break policies_test;
                }
            }
        }
        if (policy_result.isDefined()) {
            Either<Integer, Integer> e = policy_result.get();
            if (e.isRight()) {
                if (errors.isEmpty()) {
                    return Long.valueOf(e.get().longValue());
                } else {
                    throw new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(e.get().intValue()), errors));
                }
            } else {
                throw new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Deny(e.getLeft().intValue()), errors));
            }
        } else {
            throw new Error.FailedLogic(new LogicError.NoMatchingPolicy(errors));
        }
    }

    public String print_world() {
        final List<String> facts = this.world.facts().stream().map((f) -> this.symbols.print_fact(f)).collect(Collectors.toList());
        final List<String> rules = this.world.rules().stream().map((r) -> this.symbols.print_rule(r)).collect(Collectors.toList());

        List<String> checks = new ArrayList<>();

        for (int j = 0; j < this.checks.size(); j++) {
            checks.add("Authorizer[" + j + "]: " + this.checks.get(j).toString());
        }

        if (this.token != null) {
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
