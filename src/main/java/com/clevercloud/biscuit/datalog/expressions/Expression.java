package com.clevercloud.biscuit.datalog.expressions;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.ArrayList;
import java.util.Map;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Expression {
    private final ArrayList<Op> ops;

    public Expression(ArrayList<Op> ops) {
        this.ops = ops;
    }

    public Option<ID> evaluate(Map<Long, ID> variables) {
        throw new UnsupportedOperationException("not implemented");
    }

    public Option<String> print(SymbolTable symbols) {
        throw new UnsupportedOperationException("not implemented");
    }

    public Schema.ExpressionV1 serialize() {
        Schema.ExpressionV1.Builder b = Schema.ExpressionV1.newBuilder();

        for(Op op: this.ops) {
            b.addOps(op.serialize());
        }

        return b.build();
    }

    static public Either<Error.FormatError, Expression> deserializeV1(Schema.ExpressionV1 e) {
        ArrayList<Op> ops = new ArrayList<>();

        for(Schema.Op op: e.getOpsList()) {
            Either<Error.FormatError, Op> res = Op.deserializeV1(op);

            if(res.isLeft()) {
                Error.FormatError err = res.getLeft();
                return Left(err);
            } else {
                ops.add(res.get());
            }
        }

        return Right(new Expression(ops));
    }
}
