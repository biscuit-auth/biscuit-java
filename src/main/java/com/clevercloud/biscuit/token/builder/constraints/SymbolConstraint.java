package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class SymbolConstraint {
    abstract public ConstraintKind.Symbol convert(SymbolTable symbols);

    public static class InSet extends SymbolConstraint {
        Set<String> value;

        public InSet(Set<String> value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Symbol convert(SymbolTable symbols) {
            Set<Long> ids = this.value.stream().map(string -> symbols.insert(string)).collect(Collectors.toSet());
            return new ConstraintKind.Symbol(new com.clevercloud.biscuit.datalog.constraints.SymbolConstraint.InSet(ids));
        }

        @Override
        public String toString() {
            return "in \""+value+"\"";
        }
    }

    public static class NotInSet extends SymbolConstraint {
        Set<String> value;

        public NotInSet(Set<String> value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Symbol convert(SymbolTable symbols) {
            Set<Long> ids = this.value.stream().map(string -> symbols.insert(string)).collect(Collectors.toSet());
            return new ConstraintKind.Symbol(new com.clevercloud.biscuit.datalog.constraints.SymbolConstraint.NotInSet(ids));
        }

        @Override
        public String toString() {
            return "not in \""+value+"\"";
        }
    }

}
