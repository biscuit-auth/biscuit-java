package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;

public abstract class Expression {
    public abstract com.clevercloud.biscuit.datalog.expressions.Expression convert(SymbolTable symbols);

    public enum Op {
        Negate,
        Parens,
        LessThan,
        GreaterThan,
        LessOrEqual,
        GreaterOrEqual,
        Equal,
        Contains,
        Prefix,
        Suffix,
        Regex,
        Add,
        Sub,
        Mul,
        Div,
        And,
        Or,
    }

    public final static class Value extends Expression {
        private final Term value;

        public Value(Term value) {
            this.value = value;
        }

        public  com.clevercloud.biscuit.datalog.expressions.Expression convert(SymbolTable symbols) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Value value1 = (Value) o;

            return value != null ? value.equals(value1.value) : value1.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    public final static class Unary extends Expression {
        private final Op op;
        private final Expression arg1;

        public Unary(Op op, Expression arg1) {
            this.op = op;
            this.arg1 = arg1;
        }

        public  com.clevercloud.biscuit.datalog.expressions.Expression convert(SymbolTable symbols) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Unary unary = (Unary) o;

            if (op != unary.op) return false;
            return arg1.equals(unary.arg1);
        }

        @Override
        public int hashCode() {
            int result = op.hashCode();
            result = 31 * result + arg1.hashCode();
            return result;
        }
    }

    public final static class Binary extends Expression {
        private final Op op;
        private final Expression arg1;
        private final Expression arg2;

        public Binary(Op op, Expression arg1, Expression arg2) {
            this.op = op;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public  com.clevercloud.biscuit.datalog.expressions.Expression convert(SymbolTable symbols) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Binary binary = (Binary) o;

            if (op != binary.op) return false;
            if (!arg1.equals(binary.arg1)) return false;
            return arg2.equals(binary.arg2);
        }

        @Override
        public int hashCode() {
            int result = op.hashCode();
            result = 31 * result + arg1.hashCode();
            result = 31 * result + arg2.hashCode();
            return result;
        }
    }
}
