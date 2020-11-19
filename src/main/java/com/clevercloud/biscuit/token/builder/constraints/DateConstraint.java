package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

public abstract class DateConstraint implements ConstraintBuilder {
    abstract public Constraint convert(SymbolTable symbols);

    public static class Before extends DateConstraint {
        String id;
        long value;

        public Before(String id, long value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Date(new com.clevercloud.biscuit.datalog.constraints.DateConstraint.Before(this.value)));
        }

        @Override
        public String toString() {
            return "<= " + this.value;
        }
    }

    public static class After extends DateConstraint {
        String id;
        long value;

        public After(String id, long value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Date(new com.clevercloud.biscuit.datalog.constraints.DateConstraint.After(this.value)));
        }

        @Override
        public String toString() {
            return ">= " + this.value;
        }
    }
}
