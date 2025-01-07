package org.biscuitsec.biscuit.token.builder;

import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SymbolTable;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Scope {

    final Kind kind;
    final PublicKey publicKey;
    final String parameter;

    // TODO Use all-caps naming convention for enums.
    //  This convention also applies to protobuf enums.
    enum Kind {
        Authority,
        Previous,
        PublicKey,
        Parameter,
    }

    private Scope(Kind kind) {
        this.kind = kind;
        this.publicKey = null;
        this.parameter = "";
    }

    private Scope(PublicKey publicKey) {
        this.kind = Kind.PublicKey;
        this.publicKey = publicKey;
        this.parameter = "";
    }

    private Scope(String parameter) {
        this.kind = Kind.Parameter;
        this.publicKey = null;
        this.parameter = parameter;
    }

    public static Scope authority() {
        return new Scope(Kind.Authority);
    }

    public org.biscuitsec.biscuit.datalog.Scope convert(SymbolTable symbolTable) {
        switch (this.kind) {
            case Authority:
                return org.biscuitsec.biscuit.datalog.Scope.authority();
            case Previous:
                return org.biscuitsec.biscuit.datalog.Scope.previous();
            case Parameter:
                //FIXME
                return null;
            //throw new Exception("Remaining parameter: "+this.parameter);
            case PublicKey:
                return org.biscuitsec.biscuit.datalog.Scope.publicKey(symbolTable.insert(this.publicKey));
        }
        //FIXME
        return null;
    }

    public static Scope convertFrom(org.biscuitsec.biscuit.datalog.Scope scope, SymbolTable symbolTable) {
        switch (scope.kind()) {
            case Authority:
                return new Scope(Kind.Authority);
            case Previous:
                return new Scope(Kind.Previous);
            case PublicKey:
                //FIXME error management should bubble up here
                return new Scope(symbolTable.getPk((int) scope.publicKey()).get());
        }

        //FIXME error management should bubble up here
        //throw new Exception("panic");
        return null;

    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + (publicKey != null ? publicKey.hashCode() : 0);
        result = 31 * result + (parameter != null ? parameter.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scope scope = (Scope) o;

        if (kind != scope.kind) return false;
        if (!Objects.equals(publicKey, scope.publicKey)) return false;
        return Objects.equals(parameter, scope.parameter);
    }

    @Override
    public String toString() {
        switch (this.kind) {
            case Authority:
                return "authority";
            case Previous:
                return "previous";
            case Parameter:
                return "{" + this.parameter + "}";
            case PublicKey:
                return requireNonNull(this.publicKey, "publicKey cannot be null").toString();
        }
        return null;
    }

    public static Scope parameter(String parameter) {
        return new Scope(parameter);
    }

    public static Scope previous() {
        return new Scope(Kind.Previous);
    }

    public static Scope publicKey(PublicKey publicKey) {
        return new Scope(publicKey);
    }
}
