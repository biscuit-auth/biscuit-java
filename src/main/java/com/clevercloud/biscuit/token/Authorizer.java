package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.datalog.AuthorizedWorld;
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

    private Authorizer(Biscuit token, World w) throws Error.FailedLogic {
        this.token = token;
        this.world = w;
        this.symbols = new SymbolTable(this.token.symbols);
        this.checks = new ArrayList<>();
        this.policies = new ArrayList<>();
        this.token_checks = this.token.checks();
        update_on_token();
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

    private Authorizer(Biscuit token, List<Check> checks, List<Policy> policies,
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
    static public Authorizer make(Biscuit token) throws Error.FailedLogic {
        return new Authorizer(token, new World());
    }

    public Authorizer clone() {
        return new Authorizer(this.token, new ArrayList<>(this.checks), new ArrayList<>(this.policies),
                new ArrayList<>(this.token_checks), new World(this.world), new SymbolTable(this.symbols));
    }

    public void update_on_token() throws Error.FailedLogic {
        if (token != null) {
            for (com.clevercloud.biscuit.datalog.Fact fact : token.authority.facts) {
                com.clevercloud.biscuit.datalog.Fact converted_fact = Fact.convert_from(fact, token.symbols).convert(this.symbols);
                world.add_fact(converted_fact);
            }
            for (com.clevercloud.biscuit.datalog.Rule rule : token.authority.rules) {
                com.clevercloud.biscuit.token.builder.Rule _rule = Rule.convert_from(rule, token.symbols);
                com.clevercloud.biscuit.datalog.Rule converted_rule = _rule.convert(this.symbols);

                Either<String,Rule> res = _rule.validate_variables();
                if(res.isLeft()){
                    throw new Error.FailedLogic(new LogicError.InvalidBlockRule(0, token.symbols.print_rule(converted_rule)));
                }
            }
        }
    }

    public Authorizer add_token(Biscuit token) throws Error.FailedLogic {
        if (this.token != null) {
            throw new Error.FailedLogic(new LogicError.AuthorizerNotEmpty());
        }

        this.token = token;
        update_on_token();
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

        Policy policy = new Policy(q, Policy.Kind.Allow);
        this.policies.add(policy);
        world.add_policy(policy);
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

        Policy policy = new Policy(q, Policy.Kind.Deny);
        this.policies.add(policy);
        world.add_policy(policy);
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
        world.add_policy(t._2);
        return this;
    }

    public Authorizer add_policy(Policy p) {
        this.policies.add(p);
        world.add_policy(p);
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
        Set<Fact> s = new HashSet<>();

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

    public Tuple2<Long, AuthorizedWorld> authorize() throws Error.Timeout, Error.FailedLogic, Error.TooManyFacts, Error.TooManyIterations {
        return this.authorize(new RunLimits());
    }

    public Tuple2<Long, AuthorizedWorld> authorize(RunLimits limits) throws Error.Timeout, Error.FailedLogic, Error.TooManyFacts, Error.TooManyIterations {
        Instant timeLimit = Instant.now().plus(limits.maxTime);
        List<FailedCheck> errors = new LinkedList<>();
        Option<Either<Integer, Integer>> policy_result = Option.none();

        if (token != null) {
            for (com.clevercloud.biscuit.datalog.Fact fact : token.authority.facts) {
                com.clevercloud.biscuit.datalog.Fact converted_fact = Fact.convert_from(fact, token.symbols).convert(this.symbols);
                world.add_fact(converted_fact);
            }
            for (com.clevercloud.biscuit.datalog.Rule rule : token.authority.rules) {
                com.clevercloud.biscuit.token.builder.Rule _rule = Rule.convert_from(rule, token.symbols);
                com.clevercloud.biscuit.datalog.Rule converted_rule = _rule.convert(this.symbols);

                Either<String,Rule> res = _rule.validate_variables();
                if(res.isLeft()){
                    throw new Error.FailedLogic(new LogicError.InvalidBlockRule(0, token.symbols.print_rule(converted_rule)));
                }
            }
        }

        world.run(limits, symbols);
        world.clearRules();
        AuthorizedWorld authorizedWorld = new AuthorizedWorld(world.facts());

        for (int i = 0; i < this.checks.size(); i++) {
            com.clevercloud.biscuit.datalog.Check c = this.checks.get(i).convert(symbols);
            boolean successful = false;

            for (int j = 0; j < c.queries().size(); j++) {
                boolean res = world.query_match(c.queries().get(j), symbols);

                if (Instant.now().compareTo(timeLimit) >= 0) {
                    throw new Error.Timeout();
                }

                if (res) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedAuthorizer(i, symbols.print_check(c)));
            }
        }

        if (token != null) {

            for (int j = 0; j < token.authority.checks.size(); j++) {
                boolean successful = false;

                Check c = Check.convert_from(token.authority.checks.get(j), symbols);
                com.clevercloud.biscuit.datalog.Check check = c.convert(symbols);

                for (int k = 0; k < check.queries().size(); k++) {
                    boolean res = world.query_match(check.queries().get(k), symbols);

                    if (Instant.now().compareTo(timeLimit) >= 0) {
                        throw new Error.Timeout();
                    }

                    if (res) {
                        successful = true;
                        break;
                    }
                }

                if (!successful) {
                    errors.add(new FailedCheck.FailedBlock(0, j, symbols.print_check(check)));
                }
            }
        }

        policies_test:
        for (int i = 0; i < this.policies.size(); i++) {
            Policy policy = this.policies.get(i);

            for (int j = 0; j < policy.queries.size(); j++) {
                Rule query = policy.queries.get(j);
                boolean res = world.query_match(query.convert(symbols), symbols);

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

        if (token != null) {
            for (int i = 0; i < token.blocks.size(); i++) {
                Block b = token.blocks.get(i);

                World blockWorld = new World(world);

                for (com.clevercloud.biscuit.datalog.Fact fact : b.facts) {
                    com.clevercloud.biscuit.datalog.Fact converted_fact = Fact.convert_from(fact, token.symbols).convert(this.symbols);
                    blockWorld.add_fact(converted_fact);
                }

                for (com.clevercloud.biscuit.datalog.Rule rule : b.rules) {
                    com.clevercloud.biscuit.token.builder.Rule _rule = Rule.convert_from(rule, token.symbols);
                    com.clevercloud.biscuit.datalog.Rule converted_rule = _rule.convert(this.symbols);

                    Either<String,Rule> res = _rule.validate_variables();
                    if(res.isLeft()){
                        throw new Error.FailedLogic(new LogicError.InvalidBlockRule(0, token.symbols.print_rule(converted_rule)));
                    }
                    blockWorld.add_rule(converted_rule);
                }

                blockWorld.run(limits, symbols);
                authorizedWorld.add_facts(blockWorld.facts());
                blockWorld.clearRules();

                for (int j = 0; j < b.checks.size(); j++) {
                    boolean successful = false;

                    Check c = Check.convert_from(b.checks.get(j),symbols);
                    com.clevercloud.biscuit.datalog.Check check = c.convert(symbols);

                    for (int k = 0; k < check.queries().size(); k++) {
                        boolean res = blockWorld.query_match(check.queries().get(k), symbols);

                        if (Instant.now().compareTo(timeLimit) >= 0) {
                            throw new Error.Timeout();
                        }

                        if (res) {
                            successful = true;
                            break;
                        }
                    }

                    if (!successful) {
                        errors.add(new FailedCheck.FailedBlock(i + 1, j, symbols.print_check(check)));
                    }
                }
            }
        }

        if (policy_result.isDefined()) {
            Either<Integer, Integer> e = policy_result.get();
            if (e.isRight()) {
                if (errors.isEmpty()) {
                    return new Tuple2<>(Long.valueOf(e.get().longValue()), authorizedWorld);
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
        final List<String> policies = this.world.policies().stream().map((p) -> this.symbols.print_policy(p)).collect(Collectors.toList());

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
        b.append("\n\t],\n\tpolicies: [\n\t\t");
        b.append(String.join(",\n\t\t", policies));
        b.append("\n\t]\n}");

        return b.toString();
    }
}
