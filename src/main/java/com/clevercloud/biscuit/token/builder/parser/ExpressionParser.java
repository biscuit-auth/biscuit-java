package com.clevercloud.biscuit.token.builder.parser;

import com.clevercloud.biscuit.token.builder.Term;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import com.clevercloud.biscuit.token.builder.Expression;

import static com.clevercloud.biscuit.token.builder.parser.Parser.space;
import static com.clevercloud.biscuit.token.builder.parser.Parser.term;

public class ExpressionParser {
    public static Either<Error, Tuple2<String, Expression>> parse(String s) {
        return expr(space(s));
    }

    public static Either<Error, Tuple2<String, Expression>> expr(String s) {
        Either<Error, Tuple2<String, Expression>> res1 = expr1(s);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<String, Expression> t1 = res1.get();

        s = t1._1;
        Expression e = t1._2;

        while(true) {
            s = space(s);
            if(s.length() == 0) {
                break;
            }

            Either<Error, Tuple2<String, Expression.Op>> res2 = binary_op0(s);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<String, Expression.Op> t2 = res2.get();
            s = t2._1;
            Expression.Op op = t2._2;

            s = space(s);

            Either<Error, Tuple2<String, Expression>> res3 = expr1(s);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<String, Expression> t3 = res3.get();

            s = t3._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(s, e));
    }

    public static Either<Error, Tuple2<String, Expression>> expr1(String s) {
        Either<Error, Tuple2<String, Expression>> res1 = expr2(s);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<String, Expression> t1 = res1.get();

        s = t1._1;
        Expression e = t1._2;

        while(true) {
            s = space(s);
            if(s.length() == 0) {
                break;
            }

            Either<Error, Tuple2<String, Expression.Op>> res2 = binary_op1(s);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<String, Expression.Op> t2 = res2.get();
            s = t2._1;
            Expression.Op op = t2._2;

            s = space(s);

            Either<Error, Tuple2<String, Expression>> res3 = expr2(s);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<String, Expression> t3 = res3.get();

            s = t3._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(s, e));
    }

    public static Either<Error, Tuple2<String, Expression>> expr2(String s) {
        Either<Error, Tuple2<String, Expression>> res1 = expr3(s);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<String, Expression> t1 = res1.get();

        s = t1._1;
        Expression e = t1._2;

        while(true) {
            s = space(s);
            if(s.length() == 0) {
                break;
            }

            Either<Error, Tuple2<String, Expression.Op>> res2 = binary_op2(s);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<String, Expression.Op> t2 = res2.get();
            s = t2._1;
            Expression.Op op = t2._2;

            s = space(s);

            Either<Error, Tuple2<String, Expression>> res3 = expr3(s);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<String, Expression> t3 = res3.get();

            s = t3._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(s, e));
    }

    public static Either<Error, Tuple2<String, Expression>> expr3(String s) {
        Either<Error, Tuple2<String, Expression>> res1 = expr4(s);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<String, Expression> t1 = res1.get();

        s = t1._1;
        Expression e = t1._2;

        while(true) {
            s = space(s);
            if(s.length() == 0) {
                break;
            }

            Either<Error, Tuple2<String, Expression.Op>> res2 = binary_op3(s);
            if (res2.isLeft()) {
                break;
            }
            Tuple2<String, Expression.Op> t2 = res2.get();
            s = t2._1;
            Expression.Op op = t2._2;

            s = space(s);

            Either<Error, Tuple2<String, Expression>> res3 = expr4(s);
            if (res3.isLeft()) {
                return Either.left(res3.getLeft());
            }
            Tuple2<String, Expression> t3 = res3.get();

            s = t3._1;
            Expression e2 = t3._2;

            e = new Expression.Binary(op, e, e2);
        }

        return Either.right(new Tuple2<>(s, e));
    }

    public static Either<Error, Tuple2<String, Expression>> expr4(String s) {
        Either<Error, Tuple2<String, Expression>> res1 = expr_term(s);
        if (res1.isLeft()) {
            return Either.left(res1.getLeft());
        }
        Tuple2<String, Expression> t1 = res1.get();
        s = space(t1._1);
        Expression e1 = t1._2;

        if(!s.startsWith(".")) {
            return Either.right(new Tuple2<>(s, e1));
        }
        s = s.substring(1);

        Either<Error, Tuple2<String, Expression.Op>> res2 = binary_op4(s);
        if (res2.isLeft()) {
            return Either.left(res2.getLeft());
        }
        Tuple2<String, Expression.Op> t2 = res2.get();
        s = space(t2._1);
        Expression.Op op = t2._2;

        if(!s.startsWith("(")) {
            return Either.left(new Error(s, "missing ("));
        }

        s = space(s.substring(1));

        Either<Error, Tuple2<String, Expression>> res3 = expr(s);
        if (res3.isLeft()) {
            return Either.left(res3.getLeft());
        }

        Tuple2<String, Expression> t3 = res3.get();

        s = space(t3._1);
        if(!s.startsWith(")")) {
            return Either.left(new Error(s, "missing )"));
        }
        s = space(s.substring(1));
        Expression e2 = t3._2;

        return Either.right(new Tuple2<>(s, new Expression.Binary(op, e1, e2)));
    }

    public static Either<Error, Tuple2<String, Expression>> expr_term(String s) {
        Either<Error, Tuple2<String, Expression>> res1 = unary(s);
        if (res1.isRight()) {
            return res1;
        }

        Either<Error, Tuple2<String, Term>> res2 = term(s);
        if (res2.isLeft()) {
            return Either.left(res2.getLeft());
        }
        Tuple2<String, Term> t2 = res2.get();
        Expression e = new Expression.Value(t2._2);

        return Either.right(new Tuple2<>(t2._1, e));
    }

    public static Either<Error, Tuple2<String, Expression>> unary(String s) {
        s = space(s);

        if(s.startsWith("!")) {
            s = space(s.substring(1));

            Either<Error, Tuple2<String, Expression>> res = expr(s);
            if (res.isLeft()) {
                return Either.left(res.getLeft());
            }

            Tuple2<String, Expression> t = res.get();
            return Either.right(new Tuple2<>(t._1, new Expression.Unary(Expression.Op.Negate, t._2)));
        }


        if(s.startsWith("(")) {
            Either<Error, Tuple2<String, Expression>> res = unary_parens(s);
            if (res.isLeft()) {
                return Either.left(res.getLeft());
            }

            Tuple2<String, Expression> t = res.get();
            return Either.right(new Tuple2<>(t._1,  t._2));
        }

        Expression e;
        Either<Error, Tuple2<String, Term>> res = term(s);
        if (res.isRight()) {
            Tuple2<String, Term> t = res.get();
            s = space(t._1);
            e = new Expression.Value(t._2);
        } else {
            Either<Error, Tuple2<String, Expression>> res2 = unary_parens(s);
            if (res2.isLeft()) {
                return Either.left(res2.getLeft());
            }

            Tuple2<String, Expression> t = res2.get();
            s = space(t._1);
            e = t._2;
        }

        if(s.startsWith(".length()")) {
            s = space(s.substring(9));
            return Either.right(new Tuple2<>(s, new Expression.Unary(Expression.Op.Length, e)));
        } else {
            return Either.left(new Error(s, "unexpected token"));
        }
    }

    public static Either<Error, Tuple2<String, Expression>> unary_parens(String s) {
        if(s.startsWith("(")) {
            s = space(s.substring(1));

            Either<Error, Tuple2<String, Expression>> res = expr(s);
            if (res.isLeft()) {
                return Either.left(res.getLeft());
            }

            Tuple2<String, Expression> t = res.get();

            s = space(t._1);
            if(!s.startsWith(")")) {
                return Either.left(new Error(s, "missing )"));
            }

            s = space(s.substring(1));
            return Either.right(new Tuple2<>(s, new Expression.Unary(Expression.Op.Parens, t._2)));
        } else {
            return Either.left(new Error(s, "missing ("));
        }
    }

    public static Either<Error, Tuple2<String, Expression.Op>> binary_op0(String s) {
        if(s.startsWith("&&")) {
            return Either.right(new Tuple2<>(s.substring(2), Expression.Op.And));
        }
        if(s.startsWith("||")) {
            return Either.right(new Tuple2<>(s.substring(2), Expression.Op.Or));
        }

        return Either.left(new Error(s, "unrecognized op"));
    }

    public static Either<Error, Tuple2<String, Expression.Op>> binary_op1(String s) {
        if(s.startsWith("<=")) {
            return Either.right(new Tuple2<>(s.substring(2), Expression.Op.LessOrEqual));
        }
        if(s.startsWith(">=")) {
            return Either.right(new Tuple2<>(s.substring(2), Expression.Op.GreaterOrEqual));
        }
        if(s.startsWith("<")) {
            return Either.right(new Tuple2<>(s.substring(1), Expression.Op.LessThan));
        }
        if(s.startsWith(">")) {
            return Either.right(new Tuple2<>(s.substring(1), Expression.Op.GreaterThan));
        }
        if(s.startsWith("==")) {
            return Either.right(new Tuple2<>(s.substring(2), Expression.Op.Equal));
        }

        return Either.left(new Error(s, "unrecognized op"));
    }

    public static Either<Error, Tuple2<String, Expression.Op>> binary_op2(String s) {

        if(s.startsWith("+")) {
            return Either.right(new Tuple2<>(s.substring(1), Expression.Op.Add));
        }
        if(s.startsWith("-")) {
            return Either.right(new Tuple2<>(s.substring(1), Expression.Op.Sub));
        }

        return Either.left(new Error(s, "unrecognized op"));
    }

    public static Either<Error, Tuple2<String, Expression.Op>> binary_op3(String s) {
        if(s.startsWith("*")) {
            return Either.right(new Tuple2<>(s.substring(1), Expression.Op.Mul));
        }
        if(s.startsWith("/")) {
            return Either.right(new Tuple2<>(s.substring(1), Expression.Op.Div));
        }

        return Either.left(new Error(s, "unrecognized op"));
    }

    public static Either<Error, Tuple2<String, Expression.Op>> binary_op4(String s) {
        if(s.startsWith("contains")) {
            return Either.right(new Tuple2<>(s.substring(8), Expression.Op.Contains));
        }
        if(s.startsWith("starts_with")) {
            return Either.right(new Tuple2<>(s.substring(11), Expression.Op.Prefix));
        }
        if(s.startsWith("ends_with")) {
            return Either.right(new Tuple2<>(s.substring(9), Expression.Op.Suffix));
        }
        if(s.startsWith("matches")) {
            return Either.right(new Tuple2<>(s.substring(7), Expression.Op.Regex));
        }

        return Either.left(new Error(s, "unrecognized op"));
    }
}
