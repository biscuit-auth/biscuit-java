package com.clevercloud.biscuit.token.builder.parser;

import com.clevercloud.biscuit.token.builder.*;
import com.clevercloud.biscuit.token.builder.constraints.ConstraintBuilder;
import io.vavr.Tuple2;
import io.vavr.control.Either;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.clevercloud.biscuit.token.builder.Utils.s;
import static com.clevercloud.biscuit.token.builder.Utils.var;

public class Parser {
    public static Either<Error, Tuple2<String, Fact>> fact(String s) {
        Either<Error, Tuple2<String, Predicate>> res = predicate(s);
        if (res.isLeft()) {
            return Either.left(res.getLeft());
        } else {
            Tuple2<String, Predicate> t = res.get();
            return Either.right(new Tuple2<>(t._1, new Fact(t._2)));
        }
    }

    public static Either<Error, Tuple2<String, Rule>> rule(String s) {
        if(s.charAt(0) != '*') {
            return Either.left(new Error(s, "rule * prefix not found"));
        }
        Either<Error, Tuple2<String, Predicate>> res0 = predicate(s.substring(1));
        if (res0.isLeft()) {
            return Either.left(res0.getLeft());
        }

        Tuple2<String, Predicate> t0 = res0.get();
        s = t0._1;
        Predicate head = t0._2;

        int index2 = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                index2 = i;
                break;
            }
        }
        if (index2 == s.length() || s.charAt(index2) != '<' || s.charAt(index2+1) != '-') {
            return Either.left(new Error(s, "rule arrow not found"));
        }

        List<Predicate> predicates = new ArrayList<Predicate>();
        s = s.substring(index2+2);
        while(true) {
            int index_loop = s.length();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            s = s.substring(index_loop);

            Either<Error, Tuple2<String, Predicate>> res = predicate(s);
            if (res.isLeft()) {
                break;
            }

            Tuple2<String, Predicate> t = res.get();
            s = t._1;
            predicates.add(t._2);

            index_loop = s.length();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            
            if(index_loop == s.length() || s.charAt(index_loop) != ',') {
                s = s.substring(index_loop);
                break;
            } else {
                s = s.substring(index_loop + 1);
            }
        }

        //FIXME: handle constraints

        return Either.right(new Tuple2<>(s, Utils.rule(head.getName(), head.getIds(), predicates)));
    }

    public static Either<Error, Tuple2<String, Caveat>> caveat(String s) {
        return Either.left(new Error(s, "unimplemented"));
    }

    public static Either<Error, Tuple2<String, Predicate>> predicate(String s) {
        int index = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!(Character.isAlphabetic(c) || c == '_')) {
                if (i == 0) {
                    return Either.left(new Error(s, "empty name"));
                } else {
                    index = i;
                    break;
                }
            }
        }

        if (index == s.length()) {
            return Either.left(new Error(s, "end of name not found"));
        }

        String name = s.substring(0, index);

        int index2 = s.length();
        for (int i = index; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                index2 = i;
                break;
            }
        }
        if (index2 == s.length() || s.charAt(index2) != '(') {
            return Either.left(new Error(s, "opening parens not found"));
        }

        List<Term> terms = new ArrayList<Term>();
        s = s.substring(index2+1);
        while(true) {
            int index_loop = s.length();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            s = s.substring(index_loop);

            Either<Error, Tuple2<String, Term>> res = atom(s);
            if (res.isLeft()) {
                break;
            }

            Tuple2<String, Term> t = res.get();
            s = t._1;
            terms.add(t._2);

            index_loop = s.length();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    index_loop = i;
                    break;
                }
            }
            if(s.charAt(index_loop) != ',') {
                s = s.substring(index_loop);
                break;
            } else {
                s = s.substring(index_loop + 1);
            }
        }

        index = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                index = i;
                break;
            }
        }

        if (index == s.length() || s.charAt(index) != ')') {
            return Either.left(new Error(s, "closing parens not found"));
        }

        String remaining = s.substring(index+1);
        return Either.right(new Tuple2<String, Predicate>(remaining, new Predicate(name, terms)));
    }

    public static Either<Error, Tuple2<String, String>> name(String s) {
        int index = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!(Character.isAlphabetic(c) || c == '_')) {
                index = i;
                break;
            }
        }

        if(index == 0) {
            return Either.left(new Error(s, "empty name"));
        }
        String name = s.substring(0, index);
        String remaining = s.substring(index);
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

        Either<Error, Tuple2<String, Term.Integer>> res3 = integer(s);
        if(res3.isRight()) {
            Tuple2<String, Term.Integer> t = res3.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Date>> res4 = date(s);
        if(res4.isRight()) {
            Tuple2<String, Term.Date> t = res4.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        Either<Error, Tuple2<String, Term.Variable>> res5 = variable(s);
        if(res5.isRight()) {
            Tuple2<String, Term.Variable> t = res5.get();
            return Either.right(new Tuple2<String, Term>(t._1, t._2));
        }

        return Either.left(new Error(s, "unrecognized value"));
    }

    public static Either<Error, Tuple2<String, Term.Symbol>> symbol(String s) {
        if(s.charAt(0) !='#') {
            return Either.left(new Error(s, "not a symbol"));
        }

        int index = s.length();
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!(Character.isAlphabetic(c) && c != '_')) {
                index = i;
                break;
            }
        }

        if(index == 1) {
            return Either.left(new Error(s, "empty symbol"));
        }
        String name = s.substring(1, index);
        String remaining = s.substring(index);
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
        int index = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == ',' || c == ')') {
                index = i;
                break;
            }
        }

        try {
            Instant i = Instant.parse(s.substring(0, index));
            String remaining = s.substring(index);
            return Either.right(new Tuple2<String, Term.Date>(remaining, new Term.Date(i.getEpochSecond())));
        } catch (DateTimeParseException e) {
            return Either.left(new Error(s, "not a date"));

        }
    }

    public static Either<Error, Tuple2<String, Term.Variable>> variable(String s) {
        if(s.charAt(0) !='$') {
            return Either.left(new Error(s, "not a variable"));
        }

        int index = s.length();
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);

            if(!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
                index = i;
                break;
            }
        }

        String name = s.substring(1, index);
        String remaining = s.substring(index);
        return Either.right(new Tuple2<String, Term.Variable>(remaining, (Term.Variable) var(name)));
    }

    public static Either<Error, Tuple2<String, ConstraintBuilder>> constraint(String s) {
        return Either.left(new Error(s, "unimplemented"));
    }
}
