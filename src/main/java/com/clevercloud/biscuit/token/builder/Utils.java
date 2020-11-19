package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.token.builder.constraints.ConstraintBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {
    public static com.clevercloud.biscuit.token.builder.Fact fact(String name, List<Atom> ids) {
        return new com.clevercloud.biscuit.token.builder.Fact(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Predicate pred(String name, List<Atom> ids) {
        return new com.clevercloud.biscuit.token.builder.Predicate(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Rule rule(String head_name, List<Atom> head_ids,
                                                                  List<com.clevercloud.biscuit.token.builder.Predicate> predicates) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, new ArrayList<>());
    }

    public static com.clevercloud.biscuit.token.builder.Rule constrained_rule(String head_name, List<Atom> head_ids,
                                                                              List<com.clevercloud.biscuit.token.builder.Predicate> predicates,
                                                                              List<ConstraintBuilder> constraints) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, constraints);
    }

    public static com.clevercloud.biscuit.token.builder.Caveat caveat(com.clevercloud.biscuit.token.builder.Rule rule) {
        return new com.clevercloud.biscuit.token.builder.Caveat(rule);
    }

    public static Atom integer(long i) {
        return new Atom.Integer(i);
    }

    public static Atom string(String s) {
        return new Atom.Str(s);
    }

    public static Atom s(String str) {
        return new Atom.Symbol(str);
    }

    public static Atom date(Date d) {
        return new Atom.Date(d.getTime() / 1000);
    }

    public static Atom var(String name) {
        return new Atom.Variable(name);
    }
}
