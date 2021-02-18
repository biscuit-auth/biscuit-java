package com.clevercloud.biscuit.datalog.expressions;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.util.List;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class Op {
    public abstract boolean evaluate(List<ID> stack);
    public abstract String print(List<String> stack, SymbolTable symbols);
    public abstract Schema.Op serialize();
    static public Either<Error.FormatError, Op> deserializeV1(Schema.Op op) {
        if(op.hasValue()) {
            return ID.deserialize_enumV1(op.getValue()).map(v -> new Op.Value(v));
        } else if(op.hasUnary()) {
            return Op.Unary.deserializeV1(op.getUnary());
        } else if(op.hasBinary()) {
            return Op.Binary.deserializeV1(op.getBinary());
        } else {
            return Left(new Error.FormatError.DeserializationError("invalid unary operation"));
        }
    }

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

        @Override
        public Schema.Op serialize() {
            Schema.Op.Builder b = Schema.Op.newBuilder();

            b.setValue(this.value.serialize());

            return b.build();
        }
    }

    public enum UnaryOp {
        Negate,
        Parens,
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

        @Override
        public Schema.Op serialize() {
            Schema.Op.Builder b = Schema.Op.newBuilder();

            Schema.OpUnary.Builder b1 = Schema.OpUnary.newBuilder();

            switch(this.op) {
                case Negate:
                    b1.setKind(Schema.OpUnary.Kind.Negate);
                    break;
                case Parens:
                    b1.setKind(Schema.OpUnary.Kind.Parens);
                    break;
            }

            b.setUnary(b1.build());

            return b.build();
        }

        static public Either<Error.FormatError, Op> deserializeV1(Schema.OpUnary op) {
            switch(op.getKind()) {
                case Negate:
                    return Right(new Op.Unary(UnaryOp.Negate));
                case Parens:
                    return Right(new Op.Unary(UnaryOp.Parens));
            }

            return Left(new Error.FormatError.DeserializationError("invalid unary operation"));
        }
    }

    public enum BinaryOp {
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

    public final static class Binary extends Op {
        private final BinaryOp op;

        public Binary(BinaryOp value) {
            this.op = value;
        }

        @Override
        public boolean evaluate(List<ID> stack) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public String print(List<String> stack, SymbolTable symbols) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Schema.Op serialize() {
            Schema.Op.Builder b = Schema.Op.newBuilder();

            Schema.OpBinary.Builder b1 = Schema.OpBinary.newBuilder();

            switch(this.op) {
                case LessThan:
                    b1.setKind(Schema.OpBinary.Kind.LessThan);
                    break;
                case GreaterThan:
                    b1.setKind(Schema.OpBinary.Kind.GreaterThan);
                    break;
                case LessOrEqual:
                    b1.setKind(Schema.OpBinary.Kind.LessOrEqual);
                    break;
                case GreaterOrEqual:
                    b1.setKind(Schema.OpBinary.Kind.GreaterOrEqual);
                    break;
                case Equal:
                    b1.setKind(Schema.OpBinary.Kind.Equal);
                    break;
                case Contains:
                    b1.setKind(Schema.OpBinary.Kind.Contains);
                    break;
                case Prefix:
                    b1.setKind(Schema.OpBinary.Kind.Prefix);
                    break;
                case Suffix:
                    b1.setKind(Schema.OpBinary.Kind.Suffix);
                    break;
                case Regex:
                    b1.setKind(Schema.OpBinary.Kind.Regex);
                    break;
                case Add:
                    b1.setKind(Schema.OpBinary.Kind.Add);
                    break;
                case Sub:
                    b1.setKind(Schema.OpBinary.Kind.Sub);
                    break;
                case Mul:
                    b1.setKind(Schema.OpBinary.Kind.Mul);
                    break;
                case Div:
                    b1.setKind(Schema.OpBinary.Kind.Div);
                    break;
                case And:
                    b1.setKind(Schema.OpBinary.Kind.And);
                    break;
                case Or:
                    b1.setKind(Schema.OpBinary.Kind.Or);
                    break;
            }

            b.setBinary(b1.build());

            return b.build();
        }

        static public Either<Error.FormatError, Op> deserializeV1(Schema.OpBinary op) {
            switch (op.getKind()) {
                case LessThan:
                    return Right(new Op.Binary(BinaryOp.LessThan));
                case GreaterThan:
                    return Right(new Op.Binary(BinaryOp.GreaterThan));
                case LessOrEqual:
                    return Right(new Op.Binary(BinaryOp.LessOrEqual));
                case GreaterOrEqual:
                    return Right(new Op.Binary(BinaryOp.GreaterOrEqual));
                case Equal:
                    return Right(new Op.Binary(BinaryOp.Equal));
                case Contains:
                    return Right(new Op.Binary(BinaryOp.Contains));
                case Prefix:
                    return Right(new Op.Binary(BinaryOp.Prefix));
                case Suffix:
                    return Right(new Op.Binary(BinaryOp.Suffix));
                case Regex:
                    return Right(new Op.Binary(BinaryOp.Regex));
                case Add:
                    return Right(new Op.Binary(BinaryOp.Add));
                case Sub:
                    return Right(new Op.Binary(BinaryOp.Sub));
                case Mul:
                    return Right(new Op.Binary(BinaryOp.Mul));
                case Div:
                    return Right(new Op.Binary(BinaryOp.Div));
                case And:
                    return Right(new Op.Binary(BinaryOp.And));
                case Or:
                    return Right(new Op.Binary(BinaryOp.Or));
            }

            return Left(new Error.FormatError.DeserializationError("invalid binary operation"));
        }
    }
}
