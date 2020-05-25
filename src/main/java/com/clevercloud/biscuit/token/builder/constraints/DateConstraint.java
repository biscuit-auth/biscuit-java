package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

public abstract class DateConstraint {
    abstract public ConstraintKind.Date convert(SymbolTable symbols);

    public static class Before extends DateConstraint {
        String value;

        public Before(String value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Date convert(SymbolTable symbols) {
            return new ConstraintKind.Date(new com.clevercloud.biscuit.datalog.constraints.DateConstraint.Before(symbols.insert(this.value)));
        }

        @Override
        public String toString() {
            return "<= " + this.value;
        }
    }

    public static class After extends DateConstraint {
        String value;

        public After(String value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Date convert(SymbolTable symbols) {
            return new ConstraintKind.Date(new com.clevercloud.biscuit.datalog.constraints.DateConstraint.After(symbols.insert(this.value)));
        }

        @Override
        public String toString() {
            return ">= " + this.value;
        }
    }
}
