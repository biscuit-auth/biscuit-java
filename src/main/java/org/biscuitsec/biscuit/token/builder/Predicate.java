package org.biscuitsec.biscuit.token.builder;

import org.biscuitsec.biscuit.datalog.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

public class Predicate implements Cloneable {
    final String name;
    List<Term> terms;

    public Predicate(String name, List<Term> terms) {
        this.name = name;
        this.terms = terms;
    }

    public String getName() {
        return name;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public org.biscuitsec.biscuit.datalog.Predicate convert(SymbolTable symbols) {
        long name = symbols.insert(this.name);
        ArrayList<org.biscuitsec.biscuit.datalog.Term> terms = new ArrayList<>();

        for(Term a: this.terms) {
            terms.add(a.convert(symbols));
        }

        return new org.biscuitsec.biscuit.datalog.Predicate(name, terms);
    }

    public static Predicate convertFrom(org.biscuitsec.biscuit.datalog.Predicate p, SymbolTable symbols) {
        String name = symbols.printSymbol((int) p.name());
        List<Term> terms = new ArrayList<>();
        for(org.biscuitsec.biscuit.datalog.Term t: p.terms()) {
            terms.add(t.toTerm(symbols));
        }

        return new Predicate(name, terms);
    }

    @Override
    public String toString() {
        final List<String> i = terms.stream().map(Object::toString).collect(toList());
        return name+"("+ join(", ", i)+")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Predicate predicate = (Predicate) o;

        if (!Objects.equals(name, predicate.name)) return false;
        return Objects.equals(terms, predicate.terms);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (terms != null ? terms.hashCode() : 0);
        return result;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Predicate clone(){
        String name = this.name;
        List<Term> terms = new ArrayList<Term>(this.terms.size());
        terms.addAll(this.terms);
        return new Predicate(name, terms);
    }
}
