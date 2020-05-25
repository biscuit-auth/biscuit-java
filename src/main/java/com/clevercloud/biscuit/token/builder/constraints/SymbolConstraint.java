package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class SymbolConstraint {
    abstract public Constraint convert(SymbolTable symbols);

    public static class InSet extends SymbolConstraint {
        long id;
        Set<String> value;

        public InSet(long id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            Set<Long> ids = this.value.stream().map(string -> symbols.insert(string)).collect(Collectors.toSet());
            return new Constraint(this.id, new ConstraintKind.Symbol(new com.clevercloud.biscuit.datalog.constraints.SymbolConstraint.InSet(ids)));
        }

        @Override
        public String toString() {
            return "in \""+value+"\"";
        }
    }

    public static class NotInSet extends SymbolConstraint {
        long id;
        Set<String> value;

        public NotInSet(long id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            Set<Long> ids = this.value.stream().map(string -> symbols.insert(string)).collect(Collectors.toSet());
            return new Constraint(this.id, new ConstraintKind.Symbol(new com.clevercloud.biscuit.datalog.constraints.SymbolConstraint.NotInSet(ids)));
        }

        @Override
        public String toString() {
            return "not in \""+value+"\"";
        }
    }

}
