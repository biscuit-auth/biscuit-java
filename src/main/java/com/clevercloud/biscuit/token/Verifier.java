package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.IntConstraint;
import com.clevercloud.biscuit.token.builder.Fact;
import com.clevercloud.biscuit.token.builder.Rule;

import java.util.*;

import static com.clevercloud.biscuit.token.builder.Block.*;

public class Verifier {
    List<Fact> facts;
    List<Rule> rules;
    List<Rule> caveats;

    public Verifier() {
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.caveats = new ArrayList<>();
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

    public void resource(String resource) {
        this.facts.add(fact("resource", Arrays.asList(s("ambient"), string(resource))));
    }

    public void operation(String operation) {
        this.facts.add(fact("operation", Arrays.asList(s("ambient"), string(operation))));
    }

    public void time() {
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

}
