package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

import java.util.Set;

public abstract class BytesConstraint implements ConstraintBuilder {
    abstract public Constraint convert(SymbolTable symbols);

    public static class Equal extends BytesConstraint {
        long id;
        byte[] value;

        public Equal(long id, byte[] value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Bytes(new com.clevercloud.biscuit.datalog.constraints.BytesConstraint.Equal(this.value)));
        }

        @Override
        public String toString() {
            return "== " + this.value;
        }
    }

    public static class InSet extends BytesConstraint {
        long id;
        Set<byte[]> value;

        public InSet(long id, Set<byte[]> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Bytes(new com.clevercloud.biscuit.datalog.constraints.BytesConstraint.InSet(this.value)));
        }

        @Override
        public String toString() {
            return "in " + this.value;
        }
    }

    public static class NotInSet extends BytesConstraint {
        long id;
        Set<byte[]> value;

        public NotInSet(long id, Set<byte[]> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Constraint convert(SymbolTable symbols) {
            return new Constraint(this.id, new ConstraintKind.Bytes(new com.clevercloud.biscuit.datalog.constraints.BytesConstraint.NotInSet(this.value)));
        }

        @Override
        public String toString() {
            return "not in " + this.value;
        }
    }
}
