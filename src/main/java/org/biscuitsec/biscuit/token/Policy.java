package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.token.builder.Rule;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Policy {
    public enum Kind {
        ALLOW,
        DENY,
    }

    public final List<Rule> queries;
    public final Kind kind;

    public Policy(List<Rule> queries, Kind kind) {
        this.queries = queries;
        this.kind = kind;
    }

    @SuppressWarnings("unused")
    public Policy(Rule query, Kind kind) {
        ArrayList<Rule> r = new ArrayList<>();
        r.add(query);

        this.queries = r;
        this.kind = kind;
    }

    @Override
    public String toString() {
        final List<String> qs = queries.stream().map(Rule::bodyToString).collect(toList());

        switch(this.kind) {
            case ALLOW:
                return "allow if "+String.join(" or ", qs);
            case DENY:
                return "deny if "+String.join(" or ", qs);
        }
        return null;
    }

}
