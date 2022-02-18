package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import com.google.protobuf.MapEntry;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Fact {
    Predicate predicate;
    Option<Map<String, Option<Term>>> variables;

    public Fact(String name, List<Term> terms) {
        Map<String,Option<Term>> variables = new HashMap<String, Option<Term>>();
        for(Term term: terms){
            if(term instanceof Term.Variable){
                variables.put(((Term.Variable)term).value,Option.none());
            }
        }
        this.predicate = new Predicate(name, terms);
        this.variables = Option.some(variables);
    }

    public Fact(Predicate p) {
        this.predicate = p;
        this.variables = Option.none();
    }

    public Either<Error,Void> validate(){
        if(this.variables.isEmpty()){
            return Either.right(null);
        } else {
            List<String> invalid_variables = variables.get().entrySet().stream().flatMap(
                    e -> {
                        if(e.getValue().isEmpty()){
                            return Stream.of(e.getKey());
                        } else {
                            return Stream.empty();
                        }
                    }).collect(Collectors.toList());
            if (invalid_variables.isEmpty()){
                return Either.right(null);
            } else {
                return Either.left(new Error.Language(new FailedCheck.LanguageError.Builder(invalid_variables)));
            }
        }
    }

    public com.clevercloud.biscuit.datalog.Fact convert(SymbolTable symbols) {
        return new com.clevercloud.biscuit.datalog.Fact(this.predicate.convert(symbols));
    }

    public static Fact convert_from(com.clevercloud.biscuit.datalog.Fact f, SymbolTable symbols) {
        return new Fact(Predicate.convert_from(f.predicate(), symbols));
    }

    @Override
    public String toString() {
        return "fact("+predicate+")";
    }

    public String name() {
        return this.predicate.name;
    }

    public List<Term> terms() { return this.predicate.terms; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fact fact = (Fact) o;

        return predicate != null ? predicate.equals(fact.predicate) : fact.predicate == null;
    }

    @Override
    public int hashCode() {
        return predicate != null ? predicate.hashCode() : 0;
    }
}
