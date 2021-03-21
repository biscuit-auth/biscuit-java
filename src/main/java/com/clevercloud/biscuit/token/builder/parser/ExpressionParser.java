package com.clevercloud.biscuit.token.builder.parser;

import com.clevercloud.biscuit.token.builder.Term;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import com.clevercloud.biscuit.token.builder.Expression;

import static com.clevercloud.biscuit.token.builder.parser.Parser.space;
import static com.clevercloud.biscuit.token.builder.parser.Parser.term;

public class ExpressionParser {
    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> parse(String s, int offset, int end_offset) {
        return expr(s, space(s, offset, end_offset), end_offset);
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expr(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res1 = expr1(s, offset, end_offset);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<Tuple2<Integer, Integer>, Expression> t1 = res1.get();

        //s = t1._1;
        Tuple2<Integer, Integer> rTuple = t1._1;
        int rOffset = rTuple._1;
        Expression e = t1._2;

        while (true) {
            //s = space(s);
            rOffset = space(s, rOffset, rTuple._2);
            if ((end_offset - rOffset) == 0) {
                break;
            }

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> res2 = binary_op0(s, rOffset, rTuple._2);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<Tuple2<Integer, Integer>, Expression.Op> t2 = res2.get();
            //s = t2._1;
            rTuple = t2._1;
            Expression.Op op = t2._2;

            //s = space(s);
            rOffset = space(s, rTuple._1, rTuple._2);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res3 = expr1(s, rOffset, rTuple._2);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<Tuple2<Integer, Integer>, Expression> t3 = res3.get();

            //s = t3._1;
            rTuple = t3._1;
            rOffset = rTuple._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(new Tuple2<>(rOffset, rTuple._2), e));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expr1(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res1 = expr2(s, offset, end_offset);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<Tuple2<Integer, Integer>, Expression> t1 = res1.get();


        //s = t1._1;
        Tuple2<Integer, Integer> rTuple = t1._1;
        int rOffset = rTuple._1;

        Expression e = t1._2;

        while (true) {
            //s = space(s);
            rOffset = space(s, rOffset, rTuple._2);
            if ((rTuple._2 - rOffset) == 0) {
                break;
            }

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> res2 = binary_op1(s, rOffset, rTuple._2);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<Tuple2<Integer, Integer>, Expression.Op> t2 = res2.get();
            //s = t2._1;
            rTuple = t2._1;
            rOffset = rTuple._1;
            Expression.Op op = t2._2;

            //s = space(s);
            rOffset = space(s, rOffset, rTuple._2);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res3 = expr2(s, rOffset, rTuple._2);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<Tuple2<Integer, Integer>, Expression> t3 = res3.get();

            //s = t3._1;
            rTuple = t3._1;
            rOffset = rTuple._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(new Tuple2<>(rOffset, rTuple._2), e));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expr2(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res1 = expr3(s, offset, end_offset);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<Tuple2<Integer, Integer>, Expression> t1 = res1.get();

        //s = t1._1;
        Tuple2<Integer, Integer> rTuple = t1._1;
        int rOffset = rTuple._1;
        Expression e = t1._2;

        while (true) {
            //s = space(s);
            rOffset = space(s, rOffset, rTuple._2);
            if ((rTuple._2 - rOffset) == 0) {
                break;
            }

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> res2 = binary_op2(s, rOffset, rTuple._2);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<Tuple2<Integer, Integer>, Expression.Op> t2 = res2.get();
            //s = t2._1;
            rTuple = t2._1;
            rOffset = rTuple._1;
            Expression.Op op = t2._2;

            //s = space(s);
            rOffset = space(s, rOffset, rTuple._2);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res3 = expr3(s, rOffset, rTuple._2);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<Tuple2<Integer, Integer>, Expression> t3 = res3.get();

            //s = t3._1;
            rTuple = t3._1;
            rOffset = rTuple._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(new Tuple2<>(rOffset, rTuple._2), e));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expr3(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res1 = expr4(s, offset, end_offset);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<Tuple2<Integer, Integer>, Expression> t1 = res1.get();

        //s = t1._1;
        Tuple2<Integer, Integer> rTuple = t1._1;
        int rOffset = rTuple._1;
        Expression e = t1._2;

        while (true) {
            //s = space(s);
            rOffset = space(s, rOffset, rTuple._2);
            //if(s.length() == 0) {
            if ((rTuple._2 - rOffset) == 0) {
                break;
            }

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> res2 = binary_op3(s, rOffset, rTuple._2);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<Tuple2<Integer, Integer>, Expression.Op> t2 = res2.get();
            //s = t2._1;
            rTuple = t2._1;
            rOffset = rTuple._1;
            Expression.Op op = t2._2;

            //s = space(s);
            rOffset = space(s, rOffset, rTuple._2);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res3 = expr4(s, rOffset, rTuple._2);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<Tuple2<Integer, Integer>, Expression> t3 = res3.get();

            //s = t3._1;
            rTuple = t3._1;
            rOffset = rTuple._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(new Tuple2<>(rOffset, rTuple._2), e));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expr4(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res1 = expr_term(s, offset, end_offset);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<Tuple2<Integer, Integer>, Expression> t1 = res1.get();
        Tuple2<Integer, Integer> rTuple = t1._1;
        int rOffset = space(s, rTuple._1, rTuple._2);
        Expression e1 = t1._2;

        if (!s.startsWith(".", rOffset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(rOffset, rTuple._2), e1));
        }
        //s = s.substring(1);
        rOffset += 1;

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> res2 = binary_op4(s, rOffset, rTuple._2);
        if (res2.isLeft()) {
            return Either.left(res2.getLeft());
        }
        Tuple2<Tuple2<Integer, Integer>, Expression.Op> t2 = res2.get();
        rTuple = t2._1;
        //s = space(t2._1);
        rOffset = space(s, rTuple._1, rTuple._2);
        Expression.Op op = t2._2;

        if (!s.startsWith("(", rOffset)) {
            return Either.left(new Error(s.substring(rOffset, rTuple._2), "missing ("));
        }

        //s = space(s.substring(1));
        rOffset = space(s, rOffset + 1, rTuple._2);

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res3 = expr(s, rOffset, rTuple._2);
        if (res3.isLeft()) {
            return Either.left(res3.getLeft());
        }

        Tuple2<Tuple2<Integer, Integer>, Expression> t3 = res3.get();
        rTuple = t3._1;

        //s = space(t3._1);
        rOffset = space(s, rTuple._1, rTuple._2);
        if (!s.startsWith(")", rOffset)) {
            return Either.left(new Error(s, "missing )"));
        }
        //s = space(s.substring(1));
        rOffset = space(s, rOffset + 1, rTuple._2);
        Expression e2 = t3._2;

        return Either.right(new Tuple2<>(new Tuple2<>(rOffset, rTuple._2), new Expression.Binary(op, e1, e2)));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> expr_term(String s, int offset, int end_offset) {
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res1 = unary(s, offset, end_offset);
        if (res1.isRight()) {
            return res1;
        }

        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term>> res2 = term(s, offset, end_offset);
        if (res2.isLeft()) {
            return Either.left(res2.getLeft());
        }
        Tuple2<Tuple2<Integer, Integer>, Term> t2 = res2.get();
        Expression e = new Expression.Value(t2._2);

        return Either.right(new Tuple2<>(t2._1, e));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> unary(String s, int offset, int end_offset) {
        //s = space(s);
        int rOffset = space(s, offset, end_offset);

        if (s.startsWith("!", rOffset)) {
            //s = space(s.substring(1));
            rOffset = space(s, rOffset + 1, end_offset);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res = expr(s, rOffset, end_offset);
            if (res.isLeft()) {
                return Either.left(res.getLeft());
            }

            Tuple2<Tuple2<Integer, Integer>, Expression> t = res.get();
            return Either.right(new Tuple2<>(t._1, new Expression.Unary(Expression.Op.Negate, t._2)));
        }


        if (s.startsWith("(", rOffset)) {
            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res = unary_parens(s, rOffset, end_offset);
            if (res.isLeft()) {
                return Either.left(res.getLeft());
            }

            Tuple2<Tuple2<Integer, Integer>, Expression> t = res.get();

            //s = space(s.substring(1));
            rOffset += 1;
            return Either.right(new Tuple2<>(new Tuple2<>(t._1._1 + 1, t._1._2), t._2));
        }

        Expression e;
        Either<Error, Tuple2<Tuple2<Integer, Integer>, Term>> res = term(s, rOffset, end_offset);
        if (res.isRight()) {
            Tuple2<Tuple2<Integer, Integer>, Term> t = res.get();
            //s = space(t._1);
            rOffset = space(s, t._1._1, t._1._2);
            e = new Expression.Value(t._2);
        } else {
            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res2 = unary_parens(s, rOffset, end_offset);
            if (res2.isLeft()) {
                return Either.left(res2.getLeft());
            }

            Tuple2<Tuple2<Integer, Integer>, Expression> t = res2.get();
            //s = space(t._1);
            rOffset = space(s, t._1._1, t._1._2);
            e = t._2;
        }

        if (s.startsWith(".length()", rOffset)) {
            //s = space(s.substring(9));
            rOffset = space(s, rOffset + 9, end_offset);
            return Either.right(new Tuple2<>(new Tuple2<>(rOffset, end_offset), new Expression.Unary(Expression.Op.Length, e)));
        } else {
            return Either.left(new Error(s.substring(rOffset, end_offset), "unexpected token"));
        }
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> unary_parens(String s, int offset, int end_offset) {
        int rOffset = offset;
        Tuple2<Integer, Integer> rTuple = new Tuple2<>(offset, end_offset);
        if (s.startsWith("(", offset)) {
            //s = space(s.substring(1));
            rOffset = space(s, offset + 1, end_offset);

            Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression>> res = expr(s, rOffset, end_offset);
            if (res.isLeft()) {
                return Either.left(res.getLeft());
            }

            Tuple2<Tuple2<Integer, Integer>, Expression> t = res.get();

            //s = space(t._1);
            rTuple = t._1;
            rOffset = space(s, rTuple._1, rTuple._2);
            if (!s.startsWith(")", rOffset)) {
                return Either.left(new Error(s.substring(rOffset, rTuple._2), "missing )"));
            }

            //s = space(s.substring(1));

            return Either.right(new Tuple2<>(new Tuple2<>(rOffset + 1, rTuple._2), new Expression.Unary(Expression.Op.Parens, t._2)));
        } else {
            return Either.left(new Error(s, "missing ("));
        }
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> binary_op0(String s, int offset, int end_offset) {
        if (s.startsWith("&&", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 2, end_offset), Expression.Op.And));
        }
        if (s.startsWith("||", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 2, end_offset), Expression.Op.Or));
        }

        return Either.left(new Error(s.substring(offset, end_offset), "unrecognized op"));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> binary_op1(String s, int offset, int end_offset) {
        if (s.startsWith("<=", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 2, end_offset), Expression.Op.LessOrEqual));
        }
        if (s.startsWith(">=", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 2, end_offset), Expression.Op.GreaterOrEqual));
        }
        if (s.startsWith("<", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 1, end_offset), Expression.Op.LessThan));
        }
        if (s.startsWith(">", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 1, end_offset), Expression.Op.GreaterThan));
        }
        if (s.startsWith("==", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 2, end_offset), Expression.Op.Equal));
        }

        return Either.left(new Error(s.substring(offset, end_offset), "unrecognized op"));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> binary_op2(String s, int offset, int end_offset) {

        if (s.startsWith("+", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 1, end_offset), Expression.Op.Add));
        }
        if (s.startsWith("-", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 1, end_offset), Expression.Op.Sub));
        }

        return Either.left(new Error(s.substring(offset, end_offset), "unrecognized op"));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> binary_op3(String s, int offset, int end_offset) {
        if (s.startsWith("*", offset)) {
            //return Either.right(new Tuple2<>(s.substring(1), Expression.Op.Mul));
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 1, end_offset), Expression.Op.Mul));
        }
        if (s.startsWith("/", offset)) {
            //return Either.right(new Tuple2<>(s.substring(1), Expression.Op.Div));
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 1, end_offset), Expression.Op.Div));
        }

        return Either.left(new Error(s.substring(offset, end_offset), "unrecognized op"));
    }

    public static Either<Error, Tuple2<Tuple2<Integer, Integer>, Expression.Op>> binary_op4(String s, int offset, int end_offset) {
        if (s.startsWith("contains", offset)) {
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 8, end_offset), Expression.Op.Contains));
        }
        if (s.startsWith("starts_with", offset)) {
            //return Either.right(new Tuple2<>(s.substring(11), Expression.Op.Prefix));
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 11, end_offset), Expression.Op.Prefix));
        }
        if (s.startsWith("ends_with", offset)) {
            //return Either.right(new Tuple2<>(s.substring(9), Expression.Op.Suffix));
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 9, end_offset), Expression.Op.Suffix));
        }
        if (s.startsWith("matches", offset)) {
            //return Either.right(new Tuple2<>(s.substring(7), Expression.Op.Regex));
            return Either.right(new Tuple2<>(new Tuple2<>(offset + 7, end_offset), Expression.Op.Suffix));
        }

        return Either.left(new Error(s.substring(offset, end_offset), "unrecognized op"));
    }
}
