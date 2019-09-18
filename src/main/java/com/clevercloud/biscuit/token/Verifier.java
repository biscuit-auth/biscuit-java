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
    List<Rule> caveats;

    private Verifier(Biscuit token) {
        this.token = token;
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.caveats = new ArrayList<>();
    }

    /**
     * Creates a verifier for a token
     *
     * also checks that the token is valid for this root public key
     * @param token
     * @param root
     * @return
     */
    static public Either<Error, Verifier> make(Biscuit token, PublicKey root) {
        Either<Error, Void> res  = token.check_root_key(root);
        if(res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        return Right(new Verifier(token));
    }

    public void add_fact(Fact fact) {
        this.facts.add(fact);
    }

    public void add_rule(Rule rule) {
        this.rules.add(rule);
    }

    public void add_caveat(Rule caveat) {
        this.caveats.add(caveat);
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
        this.caveats.add(constrained_rule(
                "revocation_check",
                Arrays.asList((var(0))),
                Arrays.asList(pred("revocation_id", Arrays.asList(var(0)))),
                Arrays.asList(new Constraint(0, new ConstraintKind.Int(new IntConstraint.NotInSet(new HashSet(ids)))))
        ));
    }

    public Either<Error, Void> verify() {
        SymbolTable symbols = new SymbolTable(this.token.symbols);

        ArrayList<com.clevercloud.biscuit.datalog.Fact> ambient_facts = new ArrayList<>();
        for(Fact fact: this.facts) {
            ambient_facts.add(fact.convert(symbols));
        }

        ArrayList<com.clevercloud.biscuit.datalog.Rule> ambient_rules = new ArrayList<>();
        for(Rule rule: this.rules) {
            ambient_rules.add(rule.convert(symbols));
        }

        ArrayList<com.clevercloud.biscuit.datalog.Rule> ambient_caveats = new ArrayList<>();
        for(Rule caveat: this.caveats) {
            ambient_caveats.add(caveat.convert(symbols));
        }
        
        return this.token.check(symbols, ambient_facts, ambient_rules, ambient_caveats);
    }
}
