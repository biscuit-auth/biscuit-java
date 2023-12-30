package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.expressions.Expression;
import com.clevercloud.biscuit.datalog.expressions.Op;
import com.clevercloud.biscuit.datalog.expressions.Op.BinaryOp;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Block;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import io.vavr.control.Either;

import java.util.List;

import static com.clevercloud.biscuit.datalog.Check.Kind.All;
import static com.clevercloud.biscuit.token.format.SerializedBiscuit.MIN_SCHEMA_VERSION;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class SchemaVersion {
    private boolean containsScopes;
    private boolean containsCheckAll;
    private boolean containsV4;

    public SchemaVersion(List<Fact> facts, List<Rule> rules, List<Check> checks) {
        // TODO
        containsScopes = false;
        /*
        let contains_scopes = !scopes.is_empty()
        || rules.iter().any(|r: &Rule| !r.scopes.is_empty())
        || checks
            .iter()
            .any(|c: &Check| c.queries.iter().any(|q| !q.scopes.is_empty()));
         */

        containsCheckAll = false;
        for(Check check: checks) {
            if (check.kind() == All) {
                containsCheckAll = true;
                break;
            }
        }

        containsV4 = false;
        for(Check check: checks) {
            for(Rule query: check.queries()) {
                if (containsV4Ops(query.expressions())) {
                    containsV4 = true;
                    break;
                }
            }
        }
    }

    public int version() {
        if (containsScopes || containsV4 || containsCheckAll) {
          return  4;
        } else {
            return MIN_SCHEMA_VERSION;
        }
    }

    public Either<Error.FormatError, Void> checkCompatibility(int version) {
        if (version < 4) {
            if (containsScopes) {
                return Left(new Error.FormatError.DeserializationError("v3 blocks must not have scopes"));
            }
            if(containsV4) {
                return Left(new Error.FormatError.DeserializationError("v3 blocks must not have v4 operators (bitwise operators or !="));
            }
            if(containsCheckAll) {
                return Left(new Error.FormatError.DeserializationError("v3 blocks must not use check all"));
            }
        }

        return Right(null);
    }

    public static boolean containsV4Ops(List<Expression> expressions) {
        for(Expression e: expressions) {
            for (Op op: e.getOps()) {
                if (op instanceof Op.Binary) {
                    Op.Binary b = (Op.Binary) op;
                    switch (b.getOp()) {
                        case BitwiseAnd:
                        case BitwiseOr:
                        case BitwiseXor:
                        case NotEqual:
                            return true;
                    }
                }
            }
        }
        return false;
    }

}
