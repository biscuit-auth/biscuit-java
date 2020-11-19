package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class SymbolConstraint implements ConstraintBuilder {
    abstract public Constraint convert(SymbolTable symbols);

    public static class InSet extends SymbolConstraint {
        String id;
        Set<String> value;

        public InSet(String id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            Set<Long> ids = this.value.stream().map(string -> symbols.insert(string)).collect(Collectors.toSet());
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Symbol(new com.clevercloud.biscuit.datalog.constraints.SymbolConstraint.InSet(ids)));
        }

        @Override
        public String toString() {
            return "in \""+value+"\"";
        }
    }

    public static class NotInSet extends SymbolConstraint {
        String id;
        Set<String> value;

        public NotInSet(String id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            Set<Long> ids = this.value.stream().map(string -> symbols.insert(string)).collect(Collectors.toSet());
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Symbol(new com.clevercloud.biscuit.datalog.constraints.SymbolConstraint.NotInSet(ids)));
        }

        @Override
        public String toString() {
            return "not in \""+value+"\"";
        }
    }

}
