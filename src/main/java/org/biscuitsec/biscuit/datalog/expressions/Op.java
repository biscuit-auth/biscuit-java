package org.biscuitsec.biscuit.datalog.expressions;

import biscuit.format.schema.Schema;
import org.biscuitsec.biscuit.datalog.TemporarySymbolTable;
import org.biscuitsec.biscuit.datalog.Term;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.*;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class Op {
    public abstract boolean evaluate(Deque<Term> stack, Map<Long, Term> variables, TemporarySymbolTable symbols);

    public abstract String print(Deque<String> stack, SymbolTable symbols);

    public abstract Schema.Op serialize();

    static public Either<Error.FormatError, Op> deserializeV2(Schema.Op op) {
        if (op.hasValue()) {
            return Term.deserialize_enumV2(op.getValue()).map(v -> new Op.Value(v));
        } else if (op.hasUnary()) {
            return Op.Unary.deserializeV2(op.getUnary());
        } else if (op.hasBinary()) {
            return Op.Binary.deserializeV1(op.getBinary());
        } else {
            return Left(new Error.FormatError.DeserializationError("invalid unary operation"));
        }
    }

    public final static class Value extends Op {
        private final Term value;

        public Value(Term value) {
            this.value = value;
        }

        public Term getValue() {
            return value;
        }

        @Override
        public boolean evaluate(Deque<Term> stack, Map<Long, Term> variables, TemporarySymbolTable symbols) {
            if (value instanceof Term.Variable) {
                Term.Variable var = (Term.Variable) value;
                Term valueVar = variables.get(var.value());
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
            String s = symbols.print_term(value);
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

    public final static class Unary extends Op {
        private final UnaryOp op;

        public Unary(UnaryOp op) {
            this.op = op;
        }

        public UnaryOp getOp() {
            return op;
        }

        @Override
        public boolean evaluate(Deque<Term> stack, Map<Long, Term> variables, TemporarySymbolTable symbols) {
            Term value = stack.pop();
            switch (this.op) {
                case Negate:
                    if (value instanceof Term.Bool) {
                        Term.Bool b = (Term.Bool) value;
                        stack.push(new Term.Bool(!b.value()));
                    } else {
                        return false;
                    }
                    break;
                case Parens:
                    stack.push(value);
                    break;
                case Length:
                    if (value instanceof Term.Str) {
                        Option<String> s = symbols.get_s((int)((Term.Str) value).value());
                        if(s.isEmpty()) {
                            return false;
                        } else {
                            stack.push(new Term.Integer(s.get().length()));
                        }
                    } else if (value instanceof Term.Bytes) {
                        stack.push(new Term.Integer(((Term.Bytes) value).value().length));
                    } else if (value instanceof Term.Set) {
                        stack.push(new Term.Integer(((Term.Set) value).value().size()));
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
                    _s = "!" + prec;
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

        static public Either<Error.FormatError, Op> deserializeV2(Schema.OpUnary op) {
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
        NotEqual,
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
        BitwiseAnd,
        BitwiseOr,
        BitwiseXor,
    }

    public final static class Binary extends Op {
        private final BinaryOp op;

        public Binary(BinaryOp value) {
            this.op = value;
        }

        public BinaryOp getOp() {
            return op;
        }

        @Override
        public boolean evaluate(Deque<Term> stack, Map<Long, Term> variables, TemporarySymbolTable symbols) {
            Term right = stack.pop();
            Term left = stack.pop();

            switch (this.op) {
                case LessThan:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Bool(((Term.Integer) left).value() < ((Term.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Date && left instanceof Term.Date) {
                        stack.push(new Term.Bool(((Term.Date) left).value() < ((Term.Date) right).value()));
                        return true;
                    }
                    break;
                case GreaterThan:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Bool(((Term.Integer) left).value() > ((Term.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Date && left instanceof Term.Date) {
                        stack.push(new Term.Bool(((Term.Date) left).value() > ((Term.Date) right).value()));
                        return true;
                    }
                    break;
                case LessOrEqual:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Bool(((Term.Integer) left).value() <= ((Term.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Date && left instanceof Term.Date) {
                        stack.push(new Term.Bool(((Term.Date) left).value() <= ((Term.Date) right).value()));
                        return true;
                    }
                    break;
                case GreaterOrEqual:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Bool(((Term.Integer) left).value() >= ((Term.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Date && left instanceof Term.Date) {
                        stack.push(new Term.Bool(((Term.Date) left).value() >= ((Term.Date) right).value()));
                        return true;
                    }
                    break;
                case Equal:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Bool(((Term.Integer) left).value() == ((Term.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Str && left instanceof Term.Str) {
                        stack.push(new Term.Bool(((Term.Str) left).value() == ((Term.Str) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Bytes && left instanceof Term.Bytes) {
                        stack.push(new Term.Bool(Arrays.equals(((Term.Bytes) left).value(), (((Term.Bytes) right).value()))));
                        return true;
                    }
                    if (right instanceof Term.Date && left instanceof Term.Date) {
                        stack.push(new Term.Bool(((Term.Date) left).value() == ((Term.Date) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Set && left instanceof Term.Set) {
                        Set<Term> leftSet = ((Term.Set) left).value();
                        Set<Term> rightSet = ((Term.Set) right).value();
                        stack.push(new Term.Bool( leftSet.size() == rightSet.size() && leftSet.containsAll(rightSet)));
                        return true;
                    }
                    break;
                case NotEqual:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Bool(((Term.Integer) left).value() != ((Term.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Str && left instanceof Term.Str) {
                        stack.push(new Term.Bool(((Term.Str) left).value() != ((Term.Str) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Bytes && left instanceof Term.Bytes) {
                        stack.push(new Term.Bool(!Arrays.equals(((Term.Bytes) left).value(), (((Term.Bytes) right).value()))));
                        return true;
                    }
                    if (right instanceof Term.Date && left instanceof Term.Date) {
                        stack.push(new Term.Bool(((Term.Date) left).value() != ((Term.Date) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Set && left instanceof Term.Set) {
                        Set<Term> leftSet = ((Term.Set) left).value();
                        Set<Term> rightSet = ((Term.Set) right).value();
                        stack.push(new Term.Bool( leftSet.size() != rightSet.size() || !leftSet.containsAll(rightSet)));
                        return true;
                    }
                    break;
                case Contains:
                    if (left instanceof Term.Set &&
                            (right instanceof Term.Integer ||
                                    right instanceof Term.Str ||
                                    right instanceof Term.Bytes ||
                                    right instanceof Term.Date ||
                                    right instanceof Term.Bool)) {

                        stack.push(new Term.Bool(((Term.Set) left).value().contains(right)));
                        return true;
                    }
                    if (right instanceof Term.Set && left instanceof Term.Set) {
                        Set<Term> leftSet = ((Term.Set) left).value();
                        Set<Term> rightSet = ((Term.Set) right).value();
                        stack.push(new Term.Bool(leftSet.containsAll(rightSet)));
                        return true;
                    }
                    if (left instanceof Term.Str && right instanceof Term.Str) {
                        Option<String> left_s = symbols.get_s((int)((Term.Str) left).value());
                        Option<String> right_s = symbols.get_s((int)((Term.Str) right).value());

                        if (left_s.isEmpty() || right_s.isEmpty()) {
                            return false;
                        } else {
                            stack.push(new Term.Bool(left_s.get().contains(right_s.get())));
                            return true;
                        }
                    }
                    break;
                case Prefix:
                    if (right instanceof Term.Str && left instanceof Term.Str) {
                        Option<String> left_s = symbols.get_s((int)((Term.Str) left).value());
                        Option<String> right_s = symbols.get_s((int)((Term.Str) right).value());
                        if(left_s.isEmpty() || right_s.isEmpty()) {
                            return false;
                        }

                        stack.push(new Term.Bool(left_s.get().startsWith(right_s.get())));
                        return true;
                    }
                    break;
                case Suffix:
                    if (right instanceof Term.Str && left instanceof Term.Str) {
                        Option<String> left_s = symbols.get_s((int)((Term.Str) left).value());
                        Option<String> right_s = symbols.get_s((int)((Term.Str) right).value());
                        if(left_s.isEmpty() || right_s.isEmpty()) {
                            return false;
                        }
                        stack.push(new Term.Bool(left_s.get().endsWith(right_s.get())));
                        return true;
                    }
                    break;
                case Regex:
                    if (right instanceof Term.Str && left instanceof Term.Str) {
                        Option<String> left_s = symbols.get_s((int)((Term.Str) left).value());
                        Option<String> right_s = symbols.get_s((int)((Term.Str) right).value());
                        if(left_s.isEmpty() || right_s.isEmpty()) {
                            return false;
                        }

                        Pattern p = Pattern.compile(right_s.get());
                        Matcher m = p.matcher(left_s.get());
                        stack.push(new Term.Bool(m.find()));
                        return true;
                    }
                    break;
                case Add:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Integer(((Term.Integer) left).value() + ((Term.Integer) right).value()));
                        return true;
                    }
                    if (right instanceof Term.Str && left instanceof Term.Str) {
                        Option<String> left_s = symbols.get_s((int)((Term.Str) left).value());
                        Option<String> right_s = symbols.get_s((int)((Term.Str) right).value());

                        if(left_s.isEmpty() || right_s.isEmpty()) {
                            return false;
                        }
                        String concatenation = left_s.get() + right_s.get();
                        long index = symbols.insert(concatenation);
                        stack.push(new Term.Str(index));
                        return true;
                    }
                    break;
                case Sub:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Integer(((Term.Integer) left).value() - ((Term.Integer) right).value()));
                        return true;
                    }
                    break;
                case Mul:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        stack.push(new Term.Integer(((Term.Integer) left).value() * ((Term.Integer) right).value()));
                        return true;
                    }
                    break;
                case Div:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        long rl = ((Term.Integer) right).value();
                        if (rl != 0) {
                            stack.push(new Term.Integer(((Term.Integer) left).value() / rl));
                            return true;
                        }
                    }
                    break;
                case And:
                    if (right instanceof Term.Bool && left instanceof Term.Bool) {
                        stack.push(new Term.Bool(((Term.Bool) left).value() && ((Term.Bool) right).value()));
                        return true;
                    }
                    break;
                case Or:
                    if (right instanceof Term.Bool && left instanceof Term.Bool) {
                        stack.push(new Term.Bool(((Term.Bool) left).value() || ((Term.Bool) right).value()));
                        return true;
                    }
                    break;
                case Intersection:
                    if (right instanceof Term.Set && left instanceof Term.Set) {
                        HashSet<Term> intersec = new HashSet<Term>();
                        HashSet<Term> _right = ((Term.Set) right).value();
                        HashSet<Term> _left = ((Term.Set) left).value();
                        for (Term _id : _right) {
                            if (_left.contains(_id)) {
                                intersec.add(_id);
                            }
                        }
                        stack.push(new Term.Set(intersec));
                        return true;
                    }
                    break;
                case Union:
                    if (right instanceof Term.Set && left instanceof Term.Set) {
                        HashSet<Term> union = new HashSet<Term>();
                        HashSet<Term> _right = ((Term.Set) right).value();
                        HashSet<Term> _left = ((Term.Set) left).value();
                        union.addAll(_right);
                        union.addAll(_left);
                        stack.push(new Term.Set(union));
                        return true;
                    }
                    break;
                case BitwiseAnd:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        long r = ((Term.Integer) right).value();
                        long l = ((Term.Integer) left).value();
                        stack.push(new Term.Integer(r & l));
                        return true;
                    }
                    break;
                case BitwiseOr:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        long r = ((Term.Integer) right).value();
                        long l = ((Term.Integer) left).value();
                        stack.push(new Term.Integer(r | l));
                        return true;
                    }
                    break;
                case BitwiseXor:
                    if (right instanceof Term.Integer && left instanceof Term.Integer) {
                        long r = ((Term.Integer) right).value();
                        long l = ((Term.Integer) left).value();
                        stack.push(new Term.Integer(r ^ l));
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
                case NotEqual:
                    _s = left + " != " + right;
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
                case BitwiseAnd:
                    _s = left + " & " + right;
                    stack.push(_s);
                    break;
                case BitwiseOr:
                    _s = left + " | " + right;
                    stack.push(_s);
                    break;
                case BitwiseXor:
                    _s = left + " ^ " + right;
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
                case NotEqual:
                    b1.setKind(Schema.OpBinary.Kind.NotEqual);
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
                case BitwiseAnd:
                    b1.setKind(Schema.OpBinary.Kind.BitwiseAnd);
                    break;
                case BitwiseOr:
                    b1.setKind(Schema.OpBinary.Kind.BitwiseOr);
                    break;
                case BitwiseXor:
                    b1.setKind(Schema.OpBinary.Kind.BitwiseXor);
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
                case NotEqual:
                    return Right(new Op.Binary(BinaryOp.NotEqual));
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
                case BitwiseAnd:
                    return Right(new Op.Binary(BinaryOp.BitwiseAnd));
                case BitwiseOr:
                    return Right(new Op.Binary(BinaryOp.BitwiseOr));
                case BitwiseXor:
                    return Right(new Op.Binary(BinaryOp.BitwiseXor));
            }

            return Left(new Error.FormatError.DeserializationError("invalid binary operation: "+op.getKind()));
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
