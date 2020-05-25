package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;

public abstract class StrConstraint {
    abstract public Constraint convert(SymbolTable symbols);

    public static class Prefix extends StrConstraint {
        long id;
        String value;

        public Prefix(long id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Prefix(this.value)));
        }

        @Override
        public String toString() {
            return "matches " + this.value + "*";
        }
    }

    public static class Suffix extends StrConstraint {
        long id;
        String value;

        public Suffix(long id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Suffix(this.value)));
        }

        @Override
        public String toString() {
            return "matches *" + this.value;
        }
    }

    public static class Equal extends StrConstraint {
        long id;
        String value;

        public Equal(long id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Equal(this.value)));
        }

        @Override
        public String toString() {
            return "== " + this.value;
        }
    }

    public static class Regex extends StrConstraint {
        long id;
        String pattern;

        public Regex(long id, String pattern) {
            this.id = id;
            this.pattern = pattern;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Regex(this.pattern)));
        }

        @Override
        public String toString() {
            return "matches /" + this.pattern + "/";
        }
    }

    public static class InSet extends StrConstraint {
        long id;
        Set<String> value;

        public InSet(long id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.InSet(this.value)));
        }

        @Override
        public String toString() {
            return "in " + this.value;
        }
    }

    public static class NotInSet extends StrConstraint {
        long id;
        Set<String> value;

        public NotInSet(long id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.NotInSet(this.value)));
        }

        @Override
        public String toString() {
            return "not in " + this.value;
        }
    }
}
