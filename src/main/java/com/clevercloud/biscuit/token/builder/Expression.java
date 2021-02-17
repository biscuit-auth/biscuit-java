package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;

public abstract class Expression {
    public abstract com.clevercloud.biscuit.datalog.expressions.Expression convert(SymbolTable symbols);

    public enum Op {
        Negate,
        LessThan,
        GreaterThan,
        LessOrEqual,
        GreaterOrEqual,
        Equal,
        In,
        NotIn,
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
    }
}
