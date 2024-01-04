package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;


import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Scope {
    public enum Kind {
        Authority,
        Previous,
        PublicKey
    }

    Kind kind;
    long publicKey;

    private Scope(Kind kind, long publicKey) {
        this.kind = kind;
        this.publicKey = publicKey;
    }

    public static Scope authority() {
        return new Scope(Kind.Authority, 0);
    }

    public static Scope previous() {
        return new Scope(Kind.Previous, 0);
    }

    public static Scope publicKey(long publicKey) {
        return new Scope(Kind.PublicKey, publicKey);
    }

    public Kind kind() {
        return kind;
    }

    public long publicKey() {
        return publicKey;
    }

    public Schema.Scope serialize() {
        Schema.Scope.Builder b = Schema.Scope.newBuilder();

        switch (this.kind) {
            case Authority:
                b.setScopeType(Schema.Scope.ScopeType.Authority);
                break;
            case Previous:
                b.setScopeType(Schema.Scope.ScopeType.Previous);
                break;
            case PublicKey:
                b.setPublicKey(this.publicKey);
        }

        return b.build();
    }

    static public Either<Error.FormatError, Scope> deserialize(Schema.Scope scope) {
        if (scope.hasPublicKey()) {
            long publicKey = scope.getPublicKey();
            return Right(Scope.publicKey(publicKey));
        }
        if (scope.hasScopeType()) {
            switch (scope.getScopeType()) {
                case Authority:
                    return Right(Scope.authority());
                case Previous:
                    return Right(Scope.previous());
            }
        }
        return Left(new Error.FormatError.DeserializationError("invalid Scope"));
    }

    @Override
    public String toString() {
        return "Scope{" +
                "kind=" + kind +
                ", publicKey=" + publicKey +
                '}';
    }
}
