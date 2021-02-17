package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.token.builder.constraints.ConstraintBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {
    public static com.clevercloud.biscuit.token.builder.Fact fact(String name, List<Term> ids) {
        return new com.clevercloud.biscuit.token.builder.Fact(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Predicate pred(String name, List<Term> ids) {
        return new com.clevercloud.biscuit.token.builder.Predicate(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Rule rule(String head_name, List<Term> head_ids,
                                                                  List<com.clevercloud.biscuit.token.builder.Predicate> predicates) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, new ArrayList<>());
    }

    public static com.clevercloud.biscuit.token.builder.Rule constrained_rule(String head_name, List<Term> head_ids,
                                                                              List<com.clevercloud.biscuit.token.builder.Predicate> predicates,
                                                                              List<Expression> expressions) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, expressions);
    }

    public static com.clevercloud.biscuit.token.builder.Caveat caveat(com.clevercloud.biscuit.token.builder.Rule rule) {
        return new com.clevercloud.biscuit.token.builder.Caveat(rule);
    }

    public static Term integer(long i) {
        return new Term.Integer(i);
    }

    public static Term string(String s) {
        return new Term.Str(s);
    }

    public static Term s(String str) {
        return new Term.Symbol(str);
    }

    public static Term date(Date d) {
        return new Term.Date(d.getTime() / 1000);
    }

    public static Term var(String name) {
        return new Term.Variable(name);
    }
}
