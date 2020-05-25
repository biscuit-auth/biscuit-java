package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;

public abstract class StrConstraint {
    abstract public ConstraintKind.Str convert(SymbolTable symbols);

    public static class Prefix extends StrConstraint {
        String value;

        public Prefix(String value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Str convert(SymbolTable symbols) {
            return new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Prefix(this.value));
        }

        @Override
        public String toString() {
            return "matches " + this.value + "*";
        }
    }

    public static class Suffix extends StrConstraint {
        String value;

        public Suffix(String value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Str convert(SymbolTable symbols) {
            return new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Suffix(this.value));
        }

        @Override
        public String toString() {
            return "matches *" + this.value;
        }
    }

    public static class Equal extends StrConstraint {
        String value;

        public Equal(String value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Str convert(SymbolTable symbols) {
            return new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Equal(this.value));
        }

        @Override
        public String toString() {
            return "== " + this.value;
        }
    }

    public static class Regex extends StrConstraint {
        String pattern;

        public Regex(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public ConstraintKind.Str convert(SymbolTable symbols) {
            return new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.Regex(this.pattern));
        }

        @Override
        public String toString() {
            return "matches /" + this.pattern + "/";
        }
    }

    public static class InSet extends StrConstraint {
        Set<String> value;

        public InSet(Set<String> value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Str convert(SymbolTable symbols) {
            return new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.InSet(this.value));
        }

        @Override
        public String toString() {
            return "in " + this.value;
        }
    }

    public static class NotInSet extends StrConstraint {
        Set<String> value;

        public NotInSet(Set<String> value) {
            this.value = value;
        }

        @Override
        public ConstraintKind.Str convert(SymbolTable symbols) {
            return new ConstraintKind.Str(new com.clevercloud.biscuit.datalog.constraints.StrConstraint.NotInSet(this.value));
        }

        @Override
        public String toString() {
            return "not in " + this.value;
        }
    }
}
