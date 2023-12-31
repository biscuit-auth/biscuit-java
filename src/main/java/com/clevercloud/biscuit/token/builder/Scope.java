package com.clevercloud.biscuit.token.builder;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;


import java.util.ArrayList;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Scope {
    enum Kind {
        Authority,
        Previous,
        PublicKey,
        Parameter,
    }

    Kind kind;
    PublicKey publicKey;
    String parameter;

    private Scope(Kind kind) {
        this.kind = kind;
        this.publicKey = null;
        this.parameter = new String();
    }

    private Scope(Kind kind, PublicKey publicKey) {
        this.kind = kind;
        this.publicKey = publicKey;
        this.parameter = new String();
    }
    private Scope(Kind kind, String parameter) {
        this.kind = kind;
        this.publicKey = null;
        this.parameter = parameter;
    }
    public static Scope authority() {
        return new Scope(Kind.Authority);
    }

    public static Scope previous() {
        return new Scope(Kind.Previous);
    }

    public static Scope publicKey(PublicKey publicKey) {
        return new Scope(Kind.PublicKey, publicKey);
    }

    public com.clevercloud.biscuit.datalog.Scope convert(SymbolTable symbols) {
        switch (this.kind) {
            case Authority:
                return com.clevercloud.biscuit.datalog.Scope.authority();
            case Previous:
                return com.clevercloud.biscuit.datalog.Scope.previous();
            case Parameter:
                throw new Exception("Remaining parameter: "+this.parameter);
            case PublicKey:
                return com.clevercloud.biscuit.datalog.Scope.publicKey(symbols.insert(this.publicKey));
        }
    }

    public static Scope convert_from(com.clevercloud.biscuit.datalog.Scope scope, SymbolTable symbols) {
        switch(scope.kind()) {
            case Authority:
                return new Scope(Kind.Authority);
            case Previous:
                return new Scope(Kind.Previous);
            case PublicKey:
                //FIXME error management should bubble up here
                return new Scope(Kind.PublicKey, symbols.get_pk((int) scope.publicKey()).get());
        }

        //FIXME error management should bubble up here
        throw new Exception("panic");

    }
}
