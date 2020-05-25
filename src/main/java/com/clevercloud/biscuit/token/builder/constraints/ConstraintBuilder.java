package com.clevercloud.biscuit.token.builder.constraints;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;

public interface ConstraintBuilder {
    Constraint convert(SymbolTable symbols);
}
