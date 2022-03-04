package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Rule implements Cloneable {
    Predicate head;
    List<Predicate> body;
    List<Expression> expressions;
    Option<Map<String, Option<Term>>> variables;

    public Rule(Predicate head, List<Predicate> body, List<Expression> expressions) {
        Map<String, Option<Term>> variables = new HashMap<>();
        this.head = head;
        this.body = body;
        this.expressions = expressions;
        for (Term t : head.terms) {
            if (t instanceof Term.Variable) {
                variables.put(((Term.Variable) t).value, Option.none());
            }
        }
        for (Predicate p : body) {
            for (Term t : p.terms) {
                if (t instanceof Term.Variable) {
                    variables.put(((Term.Variable) t).value, Option.none());
                }
            }
        }
        for (Expression e : expressions) {
            if (e instanceof Expression.Value) {
                Expression.Value ev = (Expression.Value) e;
                if (ev.value instanceof Term.Variable)
                    variables.put(((Term.Variable) ev.value).value, Option.none());
            }
        }
        this.variables = Option.some(variables);
    }

    @Override
    public Rule clone() {
        Predicate head = this.head.clone();
        List<Predicate> body = new ArrayList<>();
        body.addAll(this.body);
        List<Expression> expressions = new ArrayList<>();
        expressions.addAll(this.expressions);
        return new Rule(head, body, expressions);
    }

    public void set(String name, Term term) throws Error.Language {
        if (this.variables.isDefined()) {
            Option<Option<Term>> t = Option.of(this.variables.get().get(name));
            if (t.isDefined()) {
                this.variables.get().put(name, Option.some(term));
            } else {
                throw new Error.Language(new FailedCheck.LanguageError.UnknownVariable("name"));
            }
        } else {
            throw new Error.Language(new FailedCheck.LanguageError.UnknownVariable("name"));
        }
    }

    public void apply_variables() {
        this.variables.forEach(
                _variables -> {
                    this.head.terms = this.head.terms.stream().flatMap(t -> {
                        if (t instanceof Term.Variable) {
                            Option<Term> term = _variables.getOrDefault(((Term.Variable) t).value, Option.none());
                            return term.map(_t -> Stream.of(_t)).getOrElse(Stream.of(t));
                        } else return Stream.of(t);
                    }).collect(Collectors.toList());
                    for (Predicate p : this.body) {
                        p.terms = p.terms.stream().flatMap(t -> {
                            if (t instanceof Term.Variable) {
                                Option<Term> term = _variables.getOrDefault(((Term.Variable) t).value, Option.none());
                                return term.map(_t -> Stream.of(_t)).getOrElse(Stream.of(t));
                            } else return Stream.of(t);
                        }).collect(Collectors.toList());
                    }
                    this.expressions = this.expressions.stream().flatMap(
                            e -> {
                                if (e instanceof Expression.Value) {
                                    Expression.Value ev = (Expression.Value) e;
                                    if (ev.value instanceof Term.Variable) {
                                        Option<Term> t = _variables.getOrDefault(((Term.Variable) ev.value).value, Option.none());
                                        if (t.isDefined()) {
                                            return Stream.of(new Expression.Value(t.get()));
                                        }
                                    }
                                }
                                return Stream.of(e);
                            }).collect(Collectors.toList());
                });
    }

    public Either<String, Rule> validate_variables() {
        Set<String> head_variables = this.head.terms.stream().flatMap(t -> {
            if (t instanceof Term.Variable) {
                return Stream.of(((Term.Variable) t).value);
            } else return Stream.empty();
        }).collect(Collectors.toSet());
        for (Predicate p : this.body) {
            for (Term term : p.terms) {
                if (term instanceof Term.Variable) {
                    head_variables.remove(((Term.Variable) term).value);
                    if (head_variables.isEmpty()) {
                        return Either.right(this);
                    }
                }
            }
        }
        return Either.left("rule head contains variables that are not used in predicates of the rule's body: " + head_variables.toString());
    }

    public com.clevercloud.biscuit.datalog.Rule convert(SymbolTable symbols) {
        Rule r = this.clone();
        r.apply_variables();
        com.clevercloud.biscuit.datalog.Predicate head = r.head.convert(symbols);
        ArrayList<com.clevercloud.biscuit.datalog.Predicate> body = new ArrayList<>();
        ArrayList<com.clevercloud.biscuit.datalog.expressions.Expression> expressions = new ArrayList<>();

        for (Predicate p : r.body) {
            body.add(p.convert(symbols));
        }

        for (Expression e : r.expressions) {
            expressions.add(e.convert(symbols));
        }

        return new com.clevercloud.biscuit.datalog.Rule(head, body, expressions);
    }

    public static Rule convert_from(com.clevercloud.biscuit.datalog.Rule r, SymbolTable symbols) {
        Predicate head = Predicate.convert_from(r.head(), symbols);

        ArrayList<Predicate> body = new ArrayList<>();
        ArrayList<Expression> expressions = new ArrayList<>();

        for (com.clevercloud.biscuit.datalog.Predicate p : r.body()) {
            body.add(Predicate.convert_from(p, symbols));
        }

        for (com.clevercloud.biscuit.datalog.expressions.Expression e : r.expressions()) {
            expressions.add(Expression.convert_from(e, symbols));
        }

        return new Rule(head, body, expressions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule = (Rule) o;

        if (head != null ? !head.equals(rule.head) : rule.head != null) return false;
        if (body != null ? !body.equals(rule.body) : rule.body != null) return false;
        return expressions != null ? expressions.equals(rule.expressions) : rule.expressions == null;
    }

    @Override
    public int hashCode() {
        int result = head != null ? head.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (expressions != null ? expressions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        Rule r = this.clone();
        r.apply_variables();
        final List<String> b = r.body.stream().map((pred) -> pred.toString()).collect(Collectors.toList());
        String res = r.head.toString() + " <- " + String.join(", ", b);

        if (!r.expressions.isEmpty()) {
            final List<String> e = r.expressions.stream().map((expression) -> expression.toString()).collect(Collectors.toList());
            res += ", " + String.join(", ", e);
        }

        return res;
    }
}
