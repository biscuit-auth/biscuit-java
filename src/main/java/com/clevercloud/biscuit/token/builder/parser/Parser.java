package com.clevercloud.biscuit.token.builder.parser;

import com.clevercloud.biscuit.token.Policy;
import com.clevercloud.biscuit.token.builder.*;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Either;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.clevercloud.biscuit.token.builder.Utils.s;
import static com.clevercloud.biscuit.token.builder.Utils.var;

public class Parser {

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Fact>> fact(String s) {
        return fact(s, 0, s.length());
    }


    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Fact>> fact(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Predicate>> res = fact_predicate(s, offset, end_offset);
        if (res.isLeft()) {
            return Either.left(res.getLeft());
        } else {
            Tuple2<Tuple2<Integer, Integer>, Predicate> t = res.get();
            return Either.right(new Tuple2<>(t._1, new Fact(t._2)));
        }
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Rule>> rule(String s) {
        return rule(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Rule>> rule(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Predicate>> res0 = predicate(s, offset, end_offset);
        if (res0.isLeft()) {
            return Either.left(res0.getLeft());
        }

        Tuple2<Tuple2<Integer, Integer>, Predicate> t0 = res0.get();
        Tuple2<Integer, Integer> rTuple = t0._1;
        Predicate head = t0._2;

        int rOffset = space(s, rTuple._1, rTuple._2);
        if ((rTuple._2 - rOffset) < 2 || s.charAt(rOffset) != '<' || s.charAt(rOffset + 1) != '-') {

            return Either.left(new Error(s.substring(rOffset, rTuple._2), "rule arrow not found"));
        }

        List<Predicate> predicates = new ArrayList<Predicate>();
        rOffset += 2;

        Either<Error, Tuple3<Tuple2<Integer, Integer>, List<Predicate>, List<Expression>>> bodyRes = rule_body(s, rOffset, rTuple._2);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple3<Tuple2<Integer, Integer>, List<Predicate>, List<Expression>> body = bodyRes.get();

        return Either.right(new Tuple2<>(body._1, new Rule(head, body._2, body._3)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Check>> check(String s) {
        return check(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Check>> check(String s, int offset, int end_offset) {
        String prefix = "check if";
        if (!s.startsWith(prefix, offset)) {
            return Either.left(new Error(s.substring(offset, end_offset), "missing check prefix"));
        }

        int rOffset = offset + prefix.length();

        List<Rule> queries = new ArrayList<>();
        Either<Error, Tuple2<Tuple2<Integer, Integer>, List<Rule>>> bodyRes = check_body(s, rOffset, end_offset);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple2<Tuple2<Integer, Integer>, List<Rule>> t = bodyRes.get();

        return Either.right(new Tuple2<>(t._1, new Check(t._2)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Policy>> policy(String s, int offset, int end_offset) {
        Policy.Kind p = Policy.Kind.Allow;

        String allow = "allow if";
        String deny = "deny if";
        int rOffset = offset;
        if (s.startsWith(allow, offset)) {
            rOffset = offset + allow.length();
        } else if (s.startsWith(deny, offset)) {
            p = Policy.Kind.Deny;
            rOffset = offset + allow.length();
        } else {
            return Either.left(new Error(s.substring(offset, end_offset), "missing policy prefix"));
        }

        List<Rule> queries = new ArrayList<>();
        Either<Error, Tuple2<Tuple2<Integer, Integer>, List<Rule>>> bodyRes = check_body(s);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple2<Tuple2<Integer, Integer>, List<Rule>> t = bodyRes.get();

        return Either.right(new Tuple2<>(t._1, new Policy(t._2, p)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, List<Rule>>> check_body(String s) {
        return check_body(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, List<Rule>>> check_body(String s, int offset, int end_offset) {
        List<Rule> queries = new ArrayList<>();
        Either<Error, Tuple3<Tuple2<Integer, Integer>, List<Predicate>, List<Expression>>> bodyRes = rule_body(s, offset, end_offset);
        if (bodyRes.isLeft()) {
            return Either.left(bodyRes.getLeft());
        }

        Tuple3<Tuple2<Integer, Integer>, List<Predicate>, List<Expression>> body = bodyRes.get();

        Tuple2<Integer, Integer> rTuple = body._1;

        queries.add(new Rule(new Predicate("query", new ArrayList<>()), body._2, body._3));

        int i = 0;
        int rOffset = rTuple._1;
        while (true) {
            if ((rTuple._2 - rOffset) == 0) {
                break;
            }

            rOffset = space(s, rOffset, rTuple._2);

            if (!s.startsWith("or", rOffset)) {
                break;
            }
            rOffset = rOffset + 2;

            Either<Error, Tuple3<Tuple2<Integer, Integer>, List<Predicate>, List<Expression>>> bodyRes2 = rule_body(s, rOffset, rTuple._2);
            if (bodyRes2.isLeft()) {
                return Either.left(bodyRes2.getLeft());
            }

            Tuple3<Tuple2<Integer, Integer>, List<Predicate>, List<Expression>> body2 = bodyRes2.get();

            rTuple = body2._1;
            rOffset = rTuple._1;
            queries.add(new Rule(new Predicate("query", new ArrayList<>()), body2._2, body2._3));
        }

        return Either.right(new Tuple2<>(new Tuple2<>(rOffset, rTuple._2), queries));
    }

    public static Either<Error, Tuple3<Tuple2<Integer, Integer>, List<Predicate>, List<Expression>>> rule_body(String s, int offset, int end_offset) {
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<Expression> expressions = new ArrayList<>();

        int rOffset = offset;
        Tuple2<Integer, Integer> rTuple = new Tuple2<Integer, Integer>(rOffset, end_offset);
        while (true) {
            rOffset = space(s, rOffset, end_offset);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Predicate>> res = predicate(s, rOffset, rTuple._2);
            if (res.isRight()) {
                Tuple2<Tuple2<Integer, Integer>, Predicate> t = res.get();
                rTuple = t._1;
                rOffset = rTuple._1;
                predicates.add(t._2);
            } else {
                Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res2 = expression(s, rOffset, rTuple._2);
                if (res2.isRight()) {
                    Tuple2<Tuple2<Integer, Integer>, Expression> t2 = res2.get();
                    rTuple = t2._1;
                    rOffset = rTuple._1;
                    expressions.add(t2._2);
                } else {
                    break;
                }
            }

            rOffset = space(s, rOffset, rTuple._2);

            if ((rTuple._2 - rOffset) == 0 || s.charAt(rOffset) != ',') {
                break;
            } else {
                rOffset = rOffset + 1;
            }
        }

        //FIXME: handle constraints

        return Either.right(new Tuple3<>(new Tuple2<>(rOffset, rTuple._2), predicates, expressions));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Predicate>> predicate(String s, int offset, int end_offset) {
        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> tn = take_while(s, offset, end_offset, (c) -> Character.isAlphabetic(c) || c == '_');
        String name = s.substring(tn._1._1, tn._1._2);
        Tuple2<Integer, Integer> rTuple = tn._2;

        int rOffset = space(s, rTuple._1, rTuple._2);
        if ((rTuple._2 - rOffset) == 0 || s.charAt(rOffset) != '(') {
            return Either.left(new Error(s.substring(rOffset, rTuple._2), "opening parens not found"));
        }
        rOffset += 1;

        List<Term> terms = new ArrayList<Term>();
        while (true) {

            rOffset = space(s, rOffset, rTuple._2);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Term>> res = term(s, rOffset, rTuple._2);
            if (res.isLeft()) {
                break;
            }

            Tuple2<Tuple2<Integer, Integer>, Term> t = res.get();
            rTuple = t._1;

            terms.add(t._2);

            rOffset = space(s, rTuple._1, rTuple._2);

            if ((rTuple._2 - rOffset) == 0 || s.charAt(rOffset) != ',') {
                break;
            } else {
                rOffset += 1;
            }
        }

        rOffset = space(s, rOffset, rTuple._2);
        if ((rTuple._2 - rOffset) == 0 || s.charAt(rOffset) != ')') {
            return Either.left(new Error(s.substring(rOffset, rTuple._2), "closing parens not found"));
        }

        return Either.right(new Tuple2<Tuple2<Integer, Integer>, Predicate>(new Tuple2<>(rOffset + 1, rTuple._2), new Predicate(name, terms)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Predicate>> fact_predicate(String s, int offset, int end_offset) {
        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> tn = take_while(s, offset, end_offset, (c) -> Character.isAlphabetic(c) || c == '_');
        String name = s.substring(tn._1._1, tn._1._2);
        Tuple2<Integer, Integer> rTuple = tn._2;

        int rOffset = space(s, rTuple._1, rTuple._2);
        if ((rTuple._2 - rOffset) == 0 || s.charAt(rOffset) != '(') {
            return Either.left(new Error(s.substring(rOffset, rTuple._2), "opening parens not found"));
        }
        rOffset += 1;

        List<Term> terms = new ArrayList<Term>();
        while (true) {

            rOffset = space(s, rOffset, rTuple._2);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Term>> res = fact_term(s, rOffset, rTuple._2);
            if (res.isLeft()) {
                break;
            }

            Tuple2<Tuple2<Integer, Integer>, Term> t = res.get();
            rTuple = t._1;
            rOffset = rTuple._1;
            terms.add(t._2);

            rOffset = space(s, rOffset, rTuple._2);

            if ((rTuple._2 - rOffset) == 0 || s.charAt(rOffset) != ',') {
                break;
            } else {
                rOffset += 1;
            }
        }

        rOffset = space(s, rOffset, rTuple._2);
        if ((rTuple._2 - rOffset) == 0 || s.charAt(rOffset) != ')') {
            return Either.left(new Error(s.substring(rOffset, rTuple._2), "closing parens not found"));
        }
        rTuple = new Tuple2<>(rOffset + 1, rTuple._2);

        return Either.right(new Tuple2<Tuple2<Integer, Integer>, Predicate>(rTuple, new Predicate(name, terms)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, String>> name(String s) {
        return name(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, String>> name(String s, int offset, int end_offset) {
        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> t = take_while(s, offset, end_offset, (c) -> Character.isAlphabetic(c) || c == '_');
        String name = s.substring(t._1._1, t._1._2);

        return Either.right(new Tuple2<Tuple2<Integer, Integer>, String>(t._2, name));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term>> term(String s) {
        return term(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term>> term(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Symbol>> res1 = symbol(s, offset, end_offset);
        if (res1.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Symbol> t = res1.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Variable>> res5 = variable(s, offset, end_offset);
        if (res5.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Variable> t = res5.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Str>> res2 = string(s, offset, end_offset);
        if (res2.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Str> t = res2.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Bool>> res6 = bool(s, offset, end_offset);
        if (res6.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Bool> t = res6.get();
            return Either.right(new Tuple2<>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Date>> res4 = date(s, offset, end_offset);
        if (res4.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Date> t = res4.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Integer>> res3 = integer(s, offset, end_offset);
        if (res3.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Integer> t = res3.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        return Either.left(new Error(s.substring(offset, end_offset), "unrecognized value"));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term>> fact_term(String s, int offset, int end_offset) {
        if ((end_offset - offset) == 0 && s.charAt(offset) == '$') {
            return Either.left(new Error(s.substring(offset, end_offset), "variables are not allowed in facts"));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Symbol>> res1 = symbol(s, offset, end_offset);
        if (res1.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Symbol> t = res1.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Str>> res2 = string(s, offset, end_offset);
        if (res2.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Str> t = res2.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Bool>> res6 = bool(s, offset, end_offset);
        if (res6.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Bool> t = res6.get();
            return Either.right(new Tuple2<>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Date>> res4 = date(s, offset, end_offset);
        if (res4.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Date> t = res4.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Integer>> res3 = integer(s, offset, end_offset);
        if (res3.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term.Integer> t = res3.get();
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term>(t._1, t._2));
        }


        return Either.left(new Error(s.substring(offset, end_offset), "unrecognized value"));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Symbol>> symbol(String s) {
        return symbol(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Symbol>> symbol(String s, int offset, int end_offset) {
        if (s.charAt(offset) != '#') {
            return Either.left(new Error(s.substring(offset, end_offset), "not a symbol"));
        }

        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> t = take_while(s, offset + 1, end_offset, (c) -> Character.isAlphabetic(c) || c == '_');
        String name = s.substring(t._1._1, t._1._2);

        return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term.Symbol>(t._2, (Term.Symbol) s(name)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Str>> string(String s) {
        return string(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Str>> string(String s, int offset, int end_offset) {
        if (s.charAt(offset) != '"') {
            return Either.left(new Error(s.substring(offset, end_offset), "not a string"));
        }

        int index = end_offset;
        for (int i = offset + 1; i < end_offset; i++) {
            char c = s.charAt(i);

            if (c == '\\' && s.charAt(i + 1) == '"') {
                i += 1;
                continue;
            }

            if (c == '"') {
                index = i - 1;
                break;
            }
        }

        if (index == end_offset) {
            return Either.left(new Error(s.substring(offset, end_offset), "end of string not found"));
        }

        if (s.charAt(index + 1) != '"') {
            return Either.left(new Error(s.substring(offset, end_offset), "ending double quote not found"));
        }

        String string = s.substring(offset + 1, index + 1);
        Tuple2<Integer, Integer> remaining = new Tuple2<>(index + 2, end_offset);

        return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term.Str>(remaining, (Term.Str) Utils.string(string)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Integer>> integer(String s) {
        return integer(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Integer>> integer(String s, int offset, int end_offset) {
        int index = offset;
        if (s.charAt(offset) == '-') {
            index += 1;
        }

        int index2 = end_offset;
        for (int i = index; i < end_offset; i++) {
            char c = s.charAt(i);

            if (!Character.isDigit(c)) {
                index2 = i;
                break;
            }
        }

        if (index2 == offset) {
            return Either.left(new Error(s.substring(offset, end_offset), "not an integer"));
        }

        Integer i = Integer.parseInt(s.substring(offset, index2));

        return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term.Integer>(new Tuple2<>(index2, end_offset), (Term.Integer) Utils.integer(i.intValue())));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Date>> date(String s) {
        return date(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Date>> date(String s, int offset, int end_offset) {
        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> t = take_while(s, offset, end_offset, (c) -> c != ' ' && c != ',' && c != ')');

        try {
            OffsetDateTime d = OffsetDateTime.parse(s.substring(t._1._1, t._1._2));
            Tuple2<Integer, Integer> remaining = t._2;
            return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term.Date>(remaining, new Term.Date(d.toEpochSecond())));
        } catch (DateTimeParseException e) {
            return Either.left(new Error(s.substring(offset, end_offset), "not a date"));

        }
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Variable>> variable(String s) {
        return variable(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Variable>> variable(String s, int offset, int end_offset) {
        if (s.charAt(offset) != '$') {
            return Either.left(new Error(s.substring(offset, end_offset), "not a variable"));
        }

        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> t = take_while(s, offset + 1, end_offset, (c) -> Character.isAlphabetic(c) || Character.isDigit(c) || c == '_');

        return Either.right(new Tuple2<Tuple2<Integer, Integer>, Term.Variable>(t._2, (Term.Variable) var(s.substring(t._1._1, t._1._2))));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Term.Bool>> bool(String s, int offset, int end_offset) {
        boolean b;
        int rOffset = offset;
        if (s.startsWith("true", offset)) {
            b = true;
            rOffset = offset + 4;
        } else if (s.startsWith("false", offset)) {
            b = false;
            rOffset = offset + 5;
        } else {
            return Either.left(new Error(s.substring(offset, end_offset), "not a boolean"));
        }

        return Either.right(new Tuple2<>(new Tuple2<>(rOffset, end_offset), new Term.Bool(b)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expression(String s) {
        return ExpressionParser.parse(s, 0, s.length());
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expression(String s, int offset, int end_offset) {
        return ExpressionParser.parse(s, offset, end_offset);
    }

    public static int space(String s, int offset, int end_offset) {
        int index = offset;
        for (int i = offset; i < end_offset; i++) {
            char c = s.charAt(i);

            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                break;
            }
            index += 1;
        }

        return index;
    }

    public static Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> take_while(String s, int offset, int end_offset, Function<Character, Boolean> f) {
        int index = end_offset;
        for (int i = offset; i < s.length(); i++) {
            Character c = s.charAt(i);

            if (!f.apply(c)) {
                index = i;
                break;
            }
        }

        return new Tuple2<>(new Tuple2<>(offset, index), new Tuple2<>(index, end_offset));
    }
}
