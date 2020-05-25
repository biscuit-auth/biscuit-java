package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;

public class Constraint {
    private final long id;
    private final ConstraintKind kind;

    public Constraint(long id, ConstraintKind kind) {
        this.id = id;
        this.kind = kind;
    }
}
