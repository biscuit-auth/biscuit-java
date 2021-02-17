package com.clevercloud.biscuit.datalog.expressions;

import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.List;

public abstract class Op {
    public abstract boolean evaluate(List<ID> stack);
    public abstract String print(List<String> stack, SymbolTable symbols);

    public final static class Value extends Op {
        private final ID value;

        public Value(ID value) {
            this.value = value;
        }

        @Override
        public boolean evaluate(List<ID> stack) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String print(List<String> stack, SymbolTable symbols) {
            throw new UnsupportedOperationException("not implemented");

        }
    }

    public enum UnaryOp {
        Negate,
    }

    public final static class Unary extends Op {
        private final UnaryOp op;

        public Unary(UnaryOp op) {
            this.op = op;
        }

        @Override
        public boolean evaluate(List<ID> stack) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String print(List<String> stack, SymbolTable symbols) {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    public enum BinaryOp {
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

    public final static class Binary extends Op {
        private final BinaryOp value;

        public Binary(BinaryOp value) {
            this.value = value;
        }

        @Override
        public boolean evaluate(List<ID> stack) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String print(List<String> stack, SymbolTable symbols) {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
