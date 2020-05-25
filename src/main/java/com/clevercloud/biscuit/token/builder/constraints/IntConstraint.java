package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;

public abstract class IntConstraint implements ConstraintBuilder {
    abstract public Constraint convert(SymbolTable symbols);

    public static class Equal extends IntConstraint {
        long id;
        long value;

        public Equal(long id, long value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Int(new com.clevercloud.biscuit.datalog.constraints.IntConstraint.Equal(this.value)));
        }

        @Override
        public String toString() {
            return "== " + this.value;
        }
    }

    public static class Greater extends IntConstraint {
        long id;
        long value;

        public Greater(long id, long value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Int(new com.clevercloud.biscuit.datalog.constraints.IntConstraint.Greater(this.value)));
        }

        @Override
        public String toString() {
            return "> " + this.value;
        }
    }

    public static class GreaterOrEqual extends IntConstraint {
        long id;
        long value;

        public GreaterOrEqual(long id, long value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Int(new com.clevercloud.biscuit.datalog.constraints.IntConstraint.GreaterOrEqual(this.value)));
        }

        @Override
        public String toString() {
            return ">= " + this.value;
        }
    }

    public static class Lower extends IntConstraint {
        long id;
        long value;

        public Lower(long id, long value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Int(new com.clevercloud.biscuit.datalog.constraints.IntConstraint.Lower(this.value)));
        }

        @Override
        public String toString() {
            return "< " + this.value;
        }
    }

    public static class LowerOrEqual extends IntConstraint {
        long id;
        long value;

        public LowerOrEqual(long id, long value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Int(new com.clevercloud.biscuit.datalog.constraints.IntConstraint.LowerOrEqual(this.value)));
        }

        @Override
        public String toString() {
            return "<= " + this.value;
        }
    }

    public static class InSet extends IntConstraint {
        long id;
        Set<Long> value;

        public InSet(long id, Set<Long> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Int(new com.clevercloud.biscuit.datalog.constraints.IntConstraint.InSet(this.value)));
        }

        @Override
        public String toString() {
            return "in " + this.value;
        }
    }

    public static class NotInSet extends IntConstraint {
        long id;
        Set<Long> value;

        public NotInSet(long id, Set<Long> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Int(new com.clevercloud.biscuit.datalog.constraints.IntConstraint.NotInSet(this.value)));
        }

        @Override
        public String toString() {
            return "not in " + this.value;
        }
    }
}
