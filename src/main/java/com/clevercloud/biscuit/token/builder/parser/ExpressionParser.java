package com.clevercloud.biscuit.token.builder.parser;

import com.clevercloud.biscuit.token.builder.Term;
import io.vavr.Tuple2;
import io.vavr.control.Either;

public class Expression {
    public static Either<Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Expression>> parse(String s) {
        return Either.left(new Error(s, "unimplemented"));
    }
}
