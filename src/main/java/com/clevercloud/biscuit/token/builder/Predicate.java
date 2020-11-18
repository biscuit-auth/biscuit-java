package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class Predicate {
    String name;
    List<Atom> ids;

    public Predicate(String name, List<Atom> ids) {
        this.name = name;
        this.ids = ids;
    }

    public com.clevercloud.biscuit.datalog.Predicate convert(SymbolTable symbols) {
        long name = symbols.insert(this.name);
        ArrayList<ID> ids = new ArrayList<ID>();

        for(Atom a: this.ids) {
            ids.add(a.convert(symbols));
        }

        return new com.clevercloud.biscuit.datalog.Predicate(name, ids);
    }

    public static Predicate convert_from(com.clevercloud.biscuit.datalog.Predicate p, SymbolTable symbols) {
        String name = symbols.print_symbol((int) p.name());
        List<Atom> ids = new ArrayList<>();
        for(com.clevercloud.biscuit.datalog.ID i: p.ids()) {
            ids.add(i.toAtom(symbols));
        }

        return new Predicate(name, ids);
    }

    @Override
    public String toString() {
        return ""+name+"("+ids+")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Predicate predicate = (Predicate) o;

        if (name != null ? !name.equals(predicate.name) : predicate.name != null) return false;
        return ids != null ? ids.equals(predicate.ids) : predicate.ids == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (ids != null ? ids.hashCode() : 0);
        return result;
    }
}
