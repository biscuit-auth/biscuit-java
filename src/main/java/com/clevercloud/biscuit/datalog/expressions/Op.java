package com.clevercloud.biscuit.datalog.expressions;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import io.vavr.control.Either;
import java.util.*;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class Op {
    public abstract boolean evaluate(Deque<ID> stack, Map<Long, ID> variables);

    public abstract String print(Deque<String> stack, SymbolTable symbols);

    public abstract Schema.Op serialize();

    public static Either<Error.FormatError, Op> deserializeV1(Schema.Op op) {
        if (op.hasValue()) {
            return ID.deserialize_enumV1(op.getValue()).map(v -> new Op.Value(v));
        } else if (op.hasUnary()) {
            return Op.Unary.deserializeV1(op.getUnary());
        } else if (op.hasBinary()) {
            return Op.Binary.deserializeV1(op.getBinary());
        } else {
            return Left(new Error.FormatError.DeserializationError("invalid unary operation"));
        }
    }

    public static final class Value extends Op {
        private final ID value;

        public Value(ID value) {
            this.value = value;
        }

        public ID getValue() {
            return value;
        }

        @Override
        public boolean evaluate(Deque<ID> stack, Map<Long, ID> variables) {
            if (value instanceof ID.Variable) {
                ID.Variable var = (ID.Variable) value;
                ID valueVar = variables.get(var.value());
                if (valueVar != null) {
                    stack.push(valueVar);
                    return true;
                } else {
                    return false;
                }
            } else {
                stack.push(value);
                return true;
            }

        }

        @Override
        public String print(Deque<String> stack, SymbolTable symbols) {
            String s = symbols.print_id(value);
            stack.push(s);
            return s;
        }

        @Override
        public Schema.Op serialize() {
            Schema.Op.Builder b = Schema.Op.newBuilder();

            b.setValue(this.value.serialize());

            return b.build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Value value1 = (Value) o;

            return value.equals(value1.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "Value(" + value + ')';
        }
    }

    public enum UnaryOp {
        Negate,
        Parens,
        Length,
    }

    public static final class Unary extends Op {
        private final UnaryOp op;

        public Unary(UnaryOp op) {
            this.op = op;
        }

        public UnaryOp getOp() {
            return op;
        }

        @Override
        public boolean evaluate(Deque<ID> stack, Map<Long, ID> variables) {
            ID value = stack.pop();
            switch (this.op) {
                case Negate:
                    if (value instanceof ID.Bool) {
                        ID.Bool b = (ID.Bool) value;
                        stack.push(new ID.Bool(!b.value()));
                    } else {
                        return false;
                    }
                    break;
                case Parens:
                    stack.push(value);
                    break;
                case Length:
                    if (value instanceof ID.Str) {
                        stack.push(new ID.Integer(((ID.Str) value).value().length()));
                    } else if (value instanceof ID.Bytes) {
                        stack.push(new ID.Integer(((ID.Bytes) value).value().length));
                    } else if (value instanceof ID.Set) {
                        stack.push(new ID.Integer(((ID.Set) value).value().size()));
                    } else {
                        return false;
                    }
            }
            return true;
        }

        @Override
        public String print(Deque<String> stack, SymbolTable symbols) {
            String prec = stack.pop();
            String _s = "";
            switch (this.op) {
                case Negate:
                    _s = "! " + prec;
                    stack.push(_s);
                    break;
                case Parens:
                    _s = "(" + prec + ")";
                    stack.push(_s);
                    break;
            }
            return _s;
        }

        @Override
        public Schema.Op serialize() {
            Schema.Op.Builder b = Schema.Op.newBuilder();

            Schema.OpUnary.Builder b1 = Schema.OpUnary.newBuilder();

            switch (this.op) {
                case Negate:
                    b1.setKind(Schema.OpUnary.Kind.Negate);
                    break;
                case Parens:
                    b1.setKind(Schema.OpUnary.Kind.Parens);
                    break;
                case Length:
                    b1.setKind(Schema.OpUnary.Kind.Length);
                    break;
            }

            b.setUnary(b1.build());

            return b.build();
        }

        public static Either<Error.FormatError, Op> deserializeV1(Schema.OpUnary op) {
            switch (op.getKind()) {
                case Negate:
                    return Right(new Op.Unary(UnaryOp.Negate));
                case Parens:
                    return Right(new Op.Unary(UnaryOp.Parens));
                case Length:
                    return Right(new Op.Unary(UnaryOp.Length));
            }

            return Left(new Error.FormatError.DeserializationError("invalid unary operation"));
        }

        @Override
        public String toString() {
            return "Unary."+op;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Unary unary = (Unary) o;

            return op == unary.op;
        }

        @Override
        public int hashCode() {
            return op.hashCode();
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
        Intersection,
        Union,
    }

    public static final class Binary extends Op {
        private final BinaryOp op;

        public Binary(BinaryOp value) {
            this.op = value;
        }

        public BinaryOp getOp() {
            return op;
        }

        @Override
        public boolean evaluate(Deque<ID> stack, Map<Long, ID> variables) {
            ID right = stack.pop();
            ID left = stack.pop();

            switch (this.op) {
                case LessThan:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Bool(((ID.Integer) left).value() < ((ID.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof ID.Date && left instanceof ID.Date) {
                        stack.push(new ID.Bool(((ID.Date) left).value() < ((ID.Date) right).value()));
                        return true;
                    }
                    break;
                case GreaterThan:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Bool(((ID.Integer) left).value() > ((ID.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof ID.Date && left instanceof ID.Date) {
                        stack.push(new ID.Bool(((ID.Date) left).value() > ((ID.Date) right).value()));
                        return true;
                    }
                    break;
                case LessOrEqual:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Bool(((ID.Integer) left).value() <= ((ID.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof ID.Date && left instanceof ID.Date) {
                        stack.push(new ID.Bool(((ID.Date) left).value() <= ((ID.Date) right).value()));
                        return true;
                    }
                    break;
                case GreaterOrEqual:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Bool(((ID.Integer) left).value() >= ((ID.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof ID.Date && left instanceof ID.Date) {
                        stack.push(new ID.Bool(((ID.Date) left).value() >= ((ID.Date) right).value()));
                        return true;
                    }
                    break;
                case Equal:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Bool(((ID.Integer) left).value() == ((ID.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof ID.Str && left instanceof ID.Str) {
                        stack.push(new ID.Bool(((ID.Str) left).value().equals(((ID.Str) right).value())));
                        return true;
                    }
                    if (right instanceof ID.Bytes && left instanceof ID.Bytes) {
                        stack.push(new ID.Bool(Arrays.equals(((ID.Bytes) left).value(), (((ID.Bytes) right).value()))));
                        return true;
                    }
                    if (right instanceof ID.Date && left instanceof ID.Date) {
                        stack.push(new ID.Bool(((ID.Date) left).value() == ((ID.Date) right).value()));
                        return true;
                    }
                    if (right instanceof ID.Symbol && left instanceof ID.Symbol) {
                        stack.push(new ID.Bool(((ID.Symbol) left).value() == ((ID.Symbol) right).value()));
                        return true;
                    }
                    if (right instanceof ID.Set && left instanceof ID.Set) {
                        Set<ID> leftSet = ((ID.Set) left).value();
                        Set<ID> rightSet = ((ID.Set) right).value();
                        stack.push(new ID.Bool( leftSet.size() == rightSet.size() && leftSet.containsAll(rightSet)));
                        return true;
                    }
                    break;
                case Contains:
                    if (left instanceof ID.Set &&
                            (right instanceof ID.Integer ||
                                    right instanceof ID.Str ||
                                    right instanceof ID.Bytes ||
                                    right instanceof ID.Date ||
                                    right instanceof ID.Bool ||
                                    right instanceof ID.Symbol)) {

                        stack.push(new ID.Bool(((ID.Set) left).value().contains(right)));
                        return true;
                    }
                    if (right instanceof ID.Set && left instanceof ID.Set) {
                        Set<ID> leftSet = ((ID.Set) left).value();
                        Set<ID> rightSet = ((ID.Set) right).value();
                        stack.push(new ID.Bool(leftSet.containsAll(rightSet)));
                        return true;
                    }
                    break;
                case Prefix:
                    if (right instanceof ID.Str && left instanceof ID.Str) {
                        stack.push(new ID.Bool(((ID.Str) left).value().startsWith(((ID.Str) right).value())));
                        return true;
                    }
                    break;
                case Suffix:
                    if (right instanceof ID.Str && left instanceof ID.Str) {
                        stack.push(new ID.Bool(((ID.Str) left).value().endsWith(((ID.Str) right).value())));
                        return true;
                    }
                    break;
                case Regex:
                    if (right instanceof ID.Str && left instanceof ID.Str) {
                        Pattern p = Pattern.compile(((ID.Str) right).value());
                        Matcher m = p.matcher(((ID.Str) left).value());
                        stack.push(new ID.Bool(m.find()));
                        return true;
                    }
                    break;
                case Add:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Integer(((ID.Integer) left).value() + ((ID.Integer) right).value()));
                        return true;
                    }
                    break;
                case Sub:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Integer(((ID.Integer) left).value() - ((ID.Integer) right).value()));
                        return true;
                    }
                    break;
                case Mul:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        stack.push(new ID.Integer(((ID.Integer) left).value() * ((ID.Integer) right).value()));
                        return true;
                    }
                    break;
                case Div:
                    if (right instanceof ID.Integer && left instanceof ID.Integer) {
                        long rl = ((ID.Integer) right).value();
                        if (rl != 0) {
                            stack.push(new ID.Integer(((ID.Integer) left).value() / rl));
                            return true;
                        }
                    }
                    break;
                case And:
                    if (right instanceof ID.Bool && left instanceof ID.Bool) {
                        stack.push(new ID.Bool(((ID.Bool) left).value() && ((ID.Bool) right).value()));
                        return true;
                    }
                    break;
                case Or:
                    if (right instanceof ID.Bool && left instanceof ID.Bool) {
                        stack.push(new ID.Bool(((ID.Bool) left).value() || ((ID.Bool) right).value()));
                        return true;
                    }
                    break;
                case Intersection:
                    if (right instanceof ID.Set && left instanceof ID.Set) {
                        HashSet<ID> intersec = new HashSet<>();
                        HashSet<ID> _right = ((ID.Set) right).value();
                        HashSet<ID> _left = ((ID.Set) left).value();
                        for (ID _id : _right) {
                            if (_left.contains(_id)) {
                                intersec.add(_id);
                            }
                        }
                        stack.push(new ID.Set(intersec));
                        return true;
                    }
                    break;
                case Union:
                    if (right instanceof ID.Set && left instanceof ID.Set) {
                        HashSet<ID> union = new HashSet<ID>();
                        HashSet<ID> _right = ((ID.Set) right).value();
                        HashSet<ID> _left = ((ID.Set) left).value();
                        union.addAll(_right);
                        union.addAll(_left);
                        stack.push(new ID.Set(union));
                        return true;
                    }
                    break;
                default:
                    return false;
            }
            return false;
        }

        @Override
        public String print(Deque<String> stack, SymbolTable symbols) {
            String right = stack.pop();
            String left = stack.pop();
            String _s = "";
            switch (this.op) {
                case LessThan:
                    _s = left + " < " + right;
                    stack.push(_s);
                    break;
                case GreaterThan:
                    _s = left + " > " + right;
                    stack.push(_s);
                    break;
                case LessOrEqual:
                    _s = left + " <= " + right;
                    stack.push(_s);
                    break;
                case GreaterOrEqual:
                    _s = left + " >= " + right;
                    stack.push(_s);
                    break;
                case Equal:
                    _s = left + " == " + right;
                    stack.push(_s);
                    break;
                case Contains:
                    _s = left + ".contains(" + right + ")";
                    stack.push(_s);
                    break;
                case Prefix:
                    _s = left + ".starts_with(" + right + ")";
                    stack.push(_s);
                    break;
                case Suffix:
                    _s = left + ".ends_with(" + right + ")";
                    stack.push(_s);
                    break;
                case Regex:
                    _s = left + ".matches(" + right + ")";
                    stack.push(_s);
                    break;
                case Add:
                    _s = left + " + " + right;
                    stack.push(_s);
                    break;
                case Sub:
                    _s = left + " - " + right;
                    stack.push(_s);
                    break;
                case Mul:
                    _s = left + " * " + right;
                    stack.push(_s);
                    break;
                case Div:
                    _s = left + " / " + right;
                    stack.push(_s);
                    break;
                case And:
                    _s = left + " && " + right;
                    stack.push(_s);
                    break;
                case Or:
                    _s = left + " || " + right;
                    stack.push(_s);
                    break;
                case Intersection:
                    _s = left + ".intersection("+right+")";
                    stack.push(_s);
                    break;
                case Union:
                    _s = left + ".union("+right+")";
                    stack.push(_s);
                    break;
            }

            return _s;
        }

        @Override
        public Schema.Op serialize() {
            Schema.Op.Builder b = Schema.Op.newBuilder();

            Schema.OpBinary.Builder b1 = Schema.OpBinary.newBuilder();

            switch (this.op) {
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
                case Intersection:
                    b1.setKind(Schema.OpBinary.Kind.Intersection);
                    break;
                case Union:
                    b1.setKind(Schema.OpBinary.Kind.Union);
                    break;
            }

            b.setBinary(b1.build());

            return b.build();
        }

        public static Either<Error.FormatError, Op> deserializeV1(Schema.OpBinary op) {
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
                case Intersection:
                    return Right(new Op.Binary(BinaryOp.Intersection));
                case Union:
                    return Right(new Op.Binary(BinaryOp.Union));
            }

            return Left(new Error.FormatError.DeserializationError("invalid binary operation"));
        }

        @Override
        public String toString() {
            return "Binary."+ op;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Binary binary = (Binary) o;

            return op == binary.op;
        }

        @Override
        public int hashCode() {
            return op.hashCode();
        }
    }
}
