package org.biscuitsec.biscuit.token.builder;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.FailedCheck;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Rule implements Cloneable {
    final Predicate head;
    final List<Predicate> body;
    final Option<Map<String, Option<Term>>> variables;
    final List<Scope> scopes;
    List<Expression> expressions;

    public Rule(Predicate head, List<Predicate> body, List<Expression> expressions, List<Scope> scopes) {
        Map<String, Option<Term>> variables = new HashMap<>();
        this.head = head;
        this.body = body;
        this.expressions = expressions;
        this.scopes = scopes;
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

    public void applyVariables() {
        this.variables.forEach(
                vars -> {
                    this.head.terms = this.head.terms.stream().flatMap(t -> {
                        if (t instanceof Term.Variable) {
                            Option<Term> term = vars.getOrDefault(((Term.Variable) t).value, Option.none());
                            return term.map(Stream::of).getOrElse(Stream.of(t));
                        } else return Stream.of(t);
                    }).collect(toList());
                    for (Predicate p : this.body) {
                        p.terms = p.terms.stream().flatMap(t -> {
                            if (t instanceof Term.Variable) {
                                Option<Term> term = vars.getOrDefault(((Term.Variable) t).value, Option.none());
                                return term.map(Stream::of).getOrElse(Stream.of(t));
                            } else return Stream.of(t);
                        }).collect(toList());
                    }
                    this.expressions = this.expressions.stream().flatMap(
                            e -> {
                                if (e instanceof Expression.Value) {
                                    Expression.Value ev = (Expression.Value) e;
                                    if (ev.value instanceof Term.Variable) {
                                        Option<Term> t = vars.getOrDefault(((Term.Variable) ev.value).value, Option.none());
                                        if (t.isDefined()) {
                                            return Stream.of(new Expression.Value(t.get()));
                                        }
                                    }
                                }
                                return Stream.of(e);
                            }).collect(toList());
                });
    }

    public String bodyToString() {
        Rule r = this.clone();
        r.applyVariables();
        String res = "";

        if (!r.body.isEmpty()) {
            final List<String> b = r.body.stream().map(Predicate::toString).collect(toList());
            res += String.join(", ", b);
        }

        if (!r.expressions.isEmpty()) {
            if (!r.body.isEmpty()) {
                res += ", ";
            }
            final List<String> e = r.expressions.stream().map(Object::toString).collect(toList());
            res += String.join(", ", e);
        }

        if (!r.scopes.isEmpty()) {
            if (!r.body.isEmpty() || !r.expressions.isEmpty()) {
                res += " ";
            }
            final List<String> e = r.scopes.stream().map(Scope::toString).collect(toList());
            res += "trusting " + String.join(", ", e);
        }

        return res;
    }

    public org.biscuitsec.biscuit.datalog.Rule convert(SymbolTable symbolTable) {
        Rule r = this.clone();
        r.applyVariables();
        org.biscuitsec.biscuit.datalog.Predicate head = r.head.convert(symbolTable);
        ArrayList<org.biscuitsec.biscuit.datalog.Predicate> body = new ArrayList<>();
        ArrayList<org.biscuitsec.biscuit.datalog.expressions.Expression> expressions = new ArrayList<>();
        ArrayList<org.biscuitsec.biscuit.datalog.Scope> scopes = new ArrayList<>();


        for (Predicate p : r.body) {
            body.add(p.convert(symbolTable));
        }

        for (Expression e : r.expressions) {
            expressions.add(e.convert(symbolTable));
        }

        for (Scope s : r.scopes) {
            scopes.add(s.convert(symbolTable));
        }

        return new org.biscuitsec.biscuit.datalog.Rule(head, body, expressions, scopes);
    }

    public static Rule convertFrom(org.biscuitsec.biscuit.datalog.Rule r, SymbolTable symbolTable) {
        Predicate head = Predicate.convertFrom(r.head(), symbolTable);

        ArrayList<Predicate> body = new ArrayList<>();
        ArrayList<Expression> expressions = new ArrayList<>();
        ArrayList<Scope> scopes = new ArrayList<>();


        for (org.biscuitsec.biscuit.datalog.Predicate p : r.body()) {
            body.add(Predicate.convertFrom(p, symbolTable));
        }

        for (org.biscuitsec.biscuit.datalog.expressions.Expression e : r.expressions()) {
            expressions.add(Expression.convertFrom(e, symbolTable));
        }

        for (org.biscuitsec.biscuit.datalog.Scope s : r.scopes()) {
            scopes.add(Scope.convertFrom(s, symbolTable));
        }

        return new Rule(head, body, expressions, scopes);
    }

    @Override
    public int hashCode() {
        int result = head != null ? head.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (expressions != null ? expressions.hashCode() : 0);
        result = 31 * result + (scopes != null ? scopes.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule = (Rule) o;

        if (!Objects.equals(head, rule.head)) return false;
        if (!Objects.equals(body, rule.body)) return false;
        if (!Objects.equals(scopes, rule.scopes)) return false;
        return Objects.equals(expressions, rule.expressions);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Rule clone() {
        Predicate head = this.head.clone();
        List<Predicate> body = new ArrayList<>(this.body);
        List<Expression> expressions = new ArrayList<>(this.expressions);
        List<Scope> scopes = new ArrayList<>(this.scopes);
        return new Rule(head, body, expressions, scopes);
    }

    @Override
    public String toString() {
        Rule r = this.clone();
        r.applyVariables();
        return r.head.toString() + " <- " + bodyToString();
    }

    @SuppressWarnings("unused")
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

    public Either<String, Rule> validateVariables() {
        Set<String> freeVariables = this.head.terms.stream().flatMap(t -> {
            if (t instanceof Term.Variable) {
                return Stream.of(((Term.Variable) t).value);
            } else return Stream.empty();
        }).collect(Collectors.toSet());

        for (Expression e : this.expressions) {
            e.gatherVariables(freeVariables);
        }
        if (freeVariables.isEmpty()) {
            return Either.right(this);
        }

        for (Predicate p : this.body) {
            for (Term term : p.terms) {
                if (term instanceof Term.Variable) {
                    freeVariables.remove(((Term.Variable) term).value);
                    if (freeVariables.isEmpty()) {
                        return Either.right(this);
                    }
                }
            }
        }

        return Either.left("rule head or expressions contains variables that are not used in predicates of the rule's body: " + freeVariables);
    }
}
