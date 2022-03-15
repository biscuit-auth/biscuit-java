package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;

import java.util.Objects;

public class ExecutionScope {
    private final Schema.ExecutionScope.Scope scope;

    public ExecutionScope() {
        this.scope = Schema.ExecutionScope.Scope.AUTHORITY;
    }

    public ExecutionScope(Schema.ExecutionScope.Scope scope) {
        this.scope = scope;
    }

    public Schema.ExecutionScope.Scope scope() {
        return scope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public Schema.ExecutionScope serialize() {
        Schema.ExecutionScope.Builder b = Schema.ExecutionScope.newBuilder();
        b.setScope(scope);
        return b.build();
    }

    static public ExecutionScope deserialize(Schema.ExecutionScope executionScope) {
        return new ExecutionScope(executionScope.getScope());
    }
}
