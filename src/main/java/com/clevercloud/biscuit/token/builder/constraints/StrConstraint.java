package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;

public abstract class StrConstraint implements ConstraintBuilder {
    abstract public Constraint convert(SymbolTable symbols);

    public static class Prefix extends StrConstraint {
        String id;
        String value;

        public Prefix(String id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Prefix(this.value)));
        }

        @Override
        public String toString() {
            return "matches " + this.value + "*";
        }
    }

    public static class Suffix extends StrConstraint {
        String id;
        String value;

        public Suffix(String id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Suffix(this.value)));
        }

        @Override
        public String toString() {
            return "matches *" + this.value;
        }
    }

    public static class Equal extends StrConstraint {
        String id;
        String value;

        public Equal(String id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Equal(this.value)));
        }

        @Override
        public String toString() {
            return "== " + this.value;
        }
    }

    public static class Regex extends StrConstraint {
        String id;
        String pattern;

        public Regex(String id, String pattern) {
            this.id = id;
            this.pattern = pattern;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Regex(this.pattern)));
        }

        @Override
        public String toString() {
            return "matches /" + this.pattern + "/";
        }
    }

    public static class InSet extends StrConstraint {
        String id;
        Set<String> value;

        public InSet(String id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.InSet(this.value)));
        }

        @Override
        public String toString() {
            return "in " + this.value;
        }
    }

    public static class NotInSet extends StrConstraint {
        String id;
        Set<String> value;

        public NotInSet(String id, Set<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(symbols.insert(this.id), new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.NotInSet(this.value)));
        }

        @Override
        public String toString() {
            return "not in " + this.value;
        }
    }
}
