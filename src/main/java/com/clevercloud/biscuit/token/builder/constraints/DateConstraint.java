package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

public abstract class DateConstraint {
    abstract public Constraint convert(SymbolTable symbols);

    public static class Before extends DateConstraint {
        long id;
        String value;

        public Before(long id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Date(new com.clevercloud.biscuit.datalog.constraints.DateConstraint.Before(symbols.insert(this.value))));
        }

        @Override
        public String toString() {
            return "<= " + this.value;
        }
    }

    public static class After extends DateConstraint {
        long id;
        String value;

        public After(long id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Date(new com.clevercloud.biscuit.datalog.constraints.DateConstraint.After(symbols.insert(this.value))));
        }

        @Override
        public String toString() {
            return ">= " + this.value;
        }
    }
}
