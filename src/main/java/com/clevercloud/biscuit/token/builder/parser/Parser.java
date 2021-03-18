package com.clevercloud.biscuit.token.builder.parser;

import com.clevercloud.biscuit.token.Policy;
import com.clevercloud.biscuit.token.builder.*;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Either;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.clevercloud.biscuit.token.builder.Utils.s;
import static com.clevercloud.biscuit.token.builder.Utils.var;

public class Parser {
    public static Either<Error, Tuple2<String, Fact>> fact(String s) {
        Either<Error, Tuple2<String, Predicate>> res = fact_predicate(s);
        if (res.isLeft()) {
            return Either.left(res.getLeft());
        } else {
            Tuple2<String, Predicate> t = res.get();
            return Either.right(new Tuple2<>(t._1, new Fact(t._2)));
        }
    }

    public static Either<Error, Tuple2<String, Rule>> rule(String s) {
        Either<Error, Tuple2<String, Predicate>> res0 = predicate(s);
        if (res0.isLeft()) {
            return Either.left(res0.getLeft());
        }

        Tuple2<String, Predicate> t0 = res0.get();
        s = t0._1;
        Predicate head = t0._2;

        s = space(s);
        if (s.length() < 2 || s.charAt(0) != '<' || s.charAt(1) != '-') {
            return Either.left(new Error(s, "rule arrow not found"));
        }

        List<Predicate> predicates = new ArrayList<Predicate>();
        s = s.substring(2);

        Either<Error, Tuple3<String, List<Predicate>, List<Expression>>> bodyRes = rule_body(s);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple3<String, List<Predicate>, List<Expression>> body = bodyRes.get();

        return Either.right(new Tuple2<>(body._1, new Rule(head, body._2, body._3)));
    }

    public static Either<Error, Tuple2<String, Check>> check(String s) {
        String prefix = "check if";
        if(!s.startsWith(prefix)) {
            return Either.left(new Error(s, "missing check prefix"));
        }

        s = s.substring(prefix.length());

        List<Rule> queries = new ArrayList<>();
        Either<Error, Tuple2<String, List<Rule>>> bodyRes = check_body(s);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple2<String, List<Rule>> t = bodyRes.get();

        return Either.right(new Tuple2<>(t._1, new Check(t._2)));
    }

    public static Either<Error, Tuple2<String, Policy>> policy(String s) {
        Policy.Kind p = Policy.Kind.Allow;

        String allow = "allow if";
        String deny = "deny if";
        if(s.startsWith(allow)) {
            s = s.substring(allow.length());
        } else if(s.startsWith(deny)) {
            p = Policy.Kind.Deny;
            s = s.substring(deny.length());
        } else {
            return Either.left(new Error(s, "missing policy prefix"));
        }

        List<Rule> queries = new ArrayList<>();
        Either<Error, Tuple2<String, List<Rule>>> bodyRes = check_body(s);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple2<String, List<Rule>> t = bodyRes.get();

        return Either.right(new Tuple2<>(t._1, new Policy(t._2, p)));
    }

    public static Either<Error, Tuple2<String, List<Rule>>> check_body(String s) {
        List<Rule> queries = new ArrayList<>();
        Either<Error, Tuple3<String, List<Predicate>, List<Expression>>> bodyRes = rule_body(s);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple3<String, List<Predicate>, List<Expression>> body = bodyRes.get();

        s = body._1;
        queries.add(new Rule(new Predicate("query", new ArrayList<>()), body._2, body._3));

        int i = 0;
        while(true) {
            if(s.length() == 0) {
                break;
            }

            s = space(s);

            if(!s.startsWith("or")) {
                break;
            }
            s = s.substring(2);

            Either<Error, Tuple3<String, List<Predicate>, List<Expression>>> bodyRes2 = rule_body(s);
            if (bodyRes2.isLeft()) {
                return Either.left(bodyRes2.getLeft());
            }

            Tuple3<String, List<Predicate>, List<Expression>> body2 = bodyRes2.get();

            s = body2._1;
            queries.add(new Rule(new Predicate("query", new ArrayList<>()), body2._2, body2._3));
        }

        return Either.right(new Tuple2<>(s, queries));
    }

    public static Either<Error, Tuple3<String, List<Predicate>, List<Expression>>> rule_body(String s) {
        List<Predicate> predicates = new ArrayList<Predicate>();
        while(true) {
            s = space(s);

            Either<Error, Tuple2<String, Predicate>> res = predicate(s);
            if (res.isLeft()) {
                break;
            }

            Tuple2<String, Predicate> t = res.get();
            s = t._1;
            predicates.add(t._2);

            s = space(s);

            if(s.length() ==0 || s.charAt(0) != ',') {
                break;
            } else {
                s = s.substring(1);
            }
        }

        //FIXME: handle constraints

        return Either.right(new Tuple3<>(s, predicates, new ArrayList<Expression>()));
    }

    public static Either<Error, Tuple2<String, Predicate>> predicate(String s) {
        Tuple2<String, String> tn = take_while(s, (c) -> Character.isAlphabetic(c) || c == '_');
        String name = tn._1;
        s = tn._2;

        s = space(s);
        if(s.length() ==  0 || s.charAt(0) != '(') {
            return Either.left(new Error(s, "opening parens not found"));
        }
        s = s.substring(1);

        List<Term> terms = new ArrayList<Term>();
        while(true) {

            s = space(s);

            Either<Error, Tuple2<String, Term>> res = atom(s);
            if (res.isLeft()) {
                break;
            }

            Tuple2<String, Term> t = res.get();
            s = t._1;
            terms.add(t._2);

            s = space(s);

            if(s.length() == 0 ||s.charAt(0) != ',') {
                break;
            } else {
                s = s.substring(1);
            }
        }

        s = space(s);
        if (0 == s.length() || s.charAt(0) != ')') {
            return Either.left(new Error(s, "closing parens not found"));
        }
        String remaining = s.substring(1);

        return Either.right(new Tuple2<String, Predicate>(remaining, new Predicate(name, terms)));
    }

    public static Either<Error, Tuple2<String, Predicate>> fact_predicate(String s) {
        Tuple2<String, String> tn = take_while(s, (c) -> Character.isAlphabetic(c) || c == '_');
        String name = tn._1;
        s = tn._2;

        s = space(s);
        if(s.length() ==  0 || s.charAt(0) != '(') {
            return Either.left(new Error(s, "opening parens not found"));
        }
        s = s.substring(1);

        List<Term> terms = new ArrayList<Term>();
        while(true) {

            s = space(s);

            Either<Error, Tuple2<String, Term>> res = fact_atom(s);
            if (res.isLeft()) {
                break;
            }

            Tuple2<String, Term> t = res.get();
            s = t._1;
            terms.add(t._2);

            s = space(s);

            if(s.length() == 0 ||s.charAt(0) != ',') {
                break;
            } else {
                s = s.substring(1);
            }
        }

        s = space(s);
        if (0 == s.length() || s.charAt(0) != ')') {
            return Either.left(new Error(s, "closing parens not found"));
        }
        String remaining = s.substring(1);

        return Either.right(new Tuple2<String, Predicate>(remaining, new Predicate(name, terms)));
    }

    public static Either<Error, Tuple2<String, String>> name(String s) {
        Tuple2<String, String> t = take_while(s, (c) -> Character.isAlphabetic(c) || c == '_');
        String name = t._1;
        String remaining = t._2;

        return Either.right(new Tuple2<String, String>(remaining, name));
    }

    public static Either<Error, Tuple2<String, Term>> atom(String s) {
        Either<Error, Tuple2<String, Term.Symbol>> res1 = symbol(s);
        if(res1.isRight()) {
            Tuple2<String, Term.Symbol> t = res1.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Str>> res2 = string(s);
        if(res2.isRight()) {
            Tuple2<String, Term.Str> t = res2.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Date>> res4 = date(s);
        if(res4.isRight()) {
            Tuple2<String, Term.Date> t = res4.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Integer>> res3 = integer(s);
        if(res3.isRight()) {
            Tuple2<String, Term.Integer> t = res3.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Variable>> res5 = variable(s);
        if(res5.isRight()) {
            Tuple2<String, Term.Variable> t = res5.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        return Either.left(new Error(s, "unrecognized value"));
    }

    public static Either<Error, Tuple2<String, Term>> fact_atom(String s) {
        if(s.length() > 0 && s.charAt(0) == '$') {
            return Either.left(new Error(s, "variables are not allowed in facts"));
        }

        Either<Error, Tuple2<String, Term.Symbol>> res1 = symbol(s);
        if(res1.isRight()) {
            Tuple2<String, Term.Symbol> t = res1.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Str>> res2 = string(s);
        if(res2.isRight()) {
            Tuple2<String, Term.Str> t = res2.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Date>> res4 = date(s);
        if(res4.isRight()) {
            Tuple2<String, Term.Date> t = res4.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Integer>> res3 = integer(s);
        if(res3.isRight()) {
            Tuple2<String, Term.Integer> t = res3.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }


        return Either.left(new Error(s, "unrecognized value"));
    }

    public static Either<Error, Tuple2<String, Term.Symbol>> symbol(String s) {
        if(s.charAt(0) !='#') {
            return Either.left(new Error(s, "not a symbol"));
        }

        Tuple2<String, String> t = take_while(s.substring(1), (c) -> Character.isAlphabetic(c) || c == '_');
        String name = t._1;
        String remaining = t._2;

        return Either.right(new Tuple2<String, Term.Symbol>(remaining, (Term.Symbol) s(name)));
    }

    public static Either<Error, Tuple2<String, Term.Str>> string(String s) {
        if(s.charAt(0) !='"') {
            return Either.left(new Error(s, "not a string"));
        }

        int index = s.length();
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);

            if(c == '\\' && s.charAt(i+1) == '"') {
                i += 1;
                continue;
            }

            if (c == '"') {
                index = i-1;
                break;
            }
        }

        if(index == s.length()) {
            return Either.left(new Error(s, "end of string not found"));
        }

        if (s.charAt(index+1) != '"'){
            return Either.left(new Error(s, "ending double quote not found"));
        }

        String string = s.substring(1, index+1);
        String remaining = s.substring(index+2);

        return Either.right(new Tuple2<String, Term.Str>(remaining, (Term.Str) Utils.string(string)));
    }

    public static Either<Error, Tuple2<String, Term.Integer>> integer(String s) {
        int index = 0;
        if(s.charAt(0) == '-') {
            index += 1;
        }

        int index2 = s.length();
        for (int i = index; i < s.length(); i++) {
            char c = s.charAt(i);

            if(!Character.isDigit(c)) {
                index2 = i;
                break;
            }
        }

        if (index2 == 0) {
            return Either.left(new Error(s, "not an integer"));
        }

        Integer i = Integer.parseInt(s.substring(0, index2));
        String remaining = s.substring(index2);

        return Either.right(new Tuple2<String, Term.Integer>(remaining, (Term.Integer) Utils.integer(i.intValue())));
    }

    public static Either<Error, Tuple2<String, Term.Date>> date(String s) {
        Tuple2<String, String> t = take_while(s, (c) -> c != ' ' && c != ',' && c != ')');

        try {
            Instant i = Instant.parse(t._1);
            String remaining = t._2;
            return Either.right(new Tuple2<String, Term.Date>(remaining, new Term.Date(i.getEpochSecond())));
        } catch (DateTimeParseException e) {
            return Either.left(new Error(s, "not a date"));

        }
    }

    public static Either<Error, Tuple2<String, Term.Variable>> variable(String s) {
        if(s.charAt(0) !='$') {
            return Either.left(new Error(s, "not a variable"));
        }

        Tuple2<String, String> t = take_while(s.substring(1), (c) -> Character.isAlphabetic(c)|| Character.isDigit(c) || c == '_');

        return Either.right(new Tuple2<String, Term.Variable>(t._2, (Term.Variable) var(t._1)));
    }

    public static Either<Error, Tuple2<String, Expression>> expression(String s) {
        return Either.left(new Error(s, "unimplemented"));
    }

    public static String space(String s) {
        int index = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if(c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                break;
            }
            index += 1;
        }

        return s.substring(index);
    }

    public static Tuple2<String, String> take_while(String s, Function<Character, Boolean> f) {
        int index = s.length();
        for (int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);

            if(!f.apply(c)) {
                index = i;
                break;
            }
        }

        return new Tuple2<>(s.substring(0, index), s.substring(index));
    }
}
