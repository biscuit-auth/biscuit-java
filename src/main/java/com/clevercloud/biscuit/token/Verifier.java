package com.clevercloud.biscuit.token;

import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.IntConstraint;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.builder.Fact;
import com.clevercloud.biscuit.token.builder.Rule;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.*;

import static com.clevercloud.biscuit.token.builder.Utils.*;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Token verification class
 */
public class Verifier {
    Biscuit token;
    List<Fact> facts;
    List<Rule> rules;
    List<Rule> authority_caveats;
    List<Rule> block_caveats;
    HashMap<String, Rule> queries;

    private Verifier(Biscuit token) {
        this.token = token;
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.authority_caveats = new ArrayList<>();
        this.block_caveats = new ArrayList<>();
        this.queries = new HashMap<>();
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

        return Right(new Verifier(token));
    }

    public void add_fact(Fact fact) {
        this.facts.add(fact);
    }

    public void add_rule(Rule rule) {
        this.rules.add(rule);
    }

    public void add_authority_caveat(Rule caveat) {
        this.authority_caveats.add(caveat);
    }

    public void add_block_caveat(Rule caveat) {
        this.block_caveats.add(caveat);
    }

    public void add_query(String name, Rule query) {
        this.queries.put(name, query);
    }

    public void add_resource(String resource) {
        this.facts.add(fact("resource", Arrays.asList(s("ambient"), string(resource))));
    }

    public void add_operation(String operation) {
        this.facts.add(fact("operation", Arrays.asList(s("ambient"), s(operation))));
    }

    public void set_time() {
        ArrayList<Fact> facts = new ArrayList<>();
        for(Fact f: this.facts) {
            if(!f.name().equals("time")) {
                facts.add(f);
            }
        }

        this.facts = facts;

        this.facts.add(fact("time", Arrays.asList(s("ambient"), date(new Date()))));
    }

    public void revocation_check(List<Long> ids) {
        this.block_caveats.add(constrained_rule(
                "revocation_check",
                Arrays.asList((var(0))),
                Arrays.asList(pred("revocation_id", Arrays.asList(var(0)))),
                Arrays.asList(new Constraint(0, new ConstraintKind.Int(new IntConstraint.NotInSet(new HashSet(ids)))))
        ));
    }

    public Either<Error, HashMap<String, HashMap<Long, Set<Fact>>>> verify() {
        if(this.token.symbols.get("authority").isEmpty() || this.token.symbols.get("ambient").isEmpty()) {
            return Left(new Error().new MissingSymbols());
        }
        SymbolTable symbols = new SymbolTable(this.token.symbols);

        ArrayList<com.clevercloud.biscuit.datalog.Fact> ambient_facts = new ArrayList<>();
        for(Fact fact: this.facts) {
            ambient_facts.add(fact.convert(symbols));
        }

        ArrayList<com.clevercloud.biscuit.datalog.Rule> ambient_rules = new ArrayList<>();
        for(Rule rule: this.rules) {
            ambient_rules.add(rule.convert(symbols));
        }

        ArrayList<com.clevercloud.biscuit.datalog.Rule> authority_caveats = new ArrayList<>();
        for(Rule caveat: this.authority_caveats) {
            authority_caveats.add(caveat.convert(symbols));
        }

        ArrayList<com.clevercloud.biscuit.datalog.Rule> block_caveats = new ArrayList<>();
        for(Rule caveat: this.block_caveats) {
            block_caveats.add(caveat.convert(symbols));
        }

        HashMap<String, com.clevercloud.biscuit.datalog.Rule> queries = new HashMap<>();
        for(String name: this.queries.keySet()) {
            queries.put(name, this.queries.get(name).convert(symbols));
        }

        Either<Error, HashMap<String, HashMap<Long, Set<com.clevercloud.biscuit.datalog.Fact>>>> res =
                this.token.check(symbols, ambient_facts, ambient_rules, authority_caveats, block_caveats, queries);
        if(res.isLeft()) {
            return Left(res.getLeft());
        } else {
            HashMap<String, HashMap<Long, Set<com.clevercloud.biscuit.datalog.Fact>>> query_results = res.get();
            HashMap<String, HashMap<Long, Set<Fact>>> results = new HashMap();

            for(String key: query_results.keySet()) {
                HashMap<Long, Set<Fact>> h = new HashMap();

                for(Long block_id: query_results.get(key).keySet()) {
                    Set<Fact> s = new HashSet();

                    for(com.clevercloud.biscuit.datalog.Fact f: query_results.get(key).get(block_id)) {
                        s.add(Fact.convert_from(f, symbols));
                    }

                    h.put(block_id, s);
                }

                results.put(key, h);
            }

            return Right(results);
        }
    }
}
