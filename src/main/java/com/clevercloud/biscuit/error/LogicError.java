package com.clevercloud.biscuit.error;

import java.util.List;

public class LogicError {
    public class InvalidAuthorityFact extends LogicError {
        final public String e;

        public InvalidAuthorityFact(String e) {
            this.e = e;
        }
    }
    public class InvalidAmbientFact extends LogicError {
        final public String e;

        public InvalidAmbientFact(String e) {
            this.e = e;
        }
    }
    public class InvalidBlockFact extends LogicError {
        final public long id;
        final public String e;

        public InvalidBlockFact(long id, String e) {
            this.id = id;
            this.e = e;
        }
    }
    public class FailedCaveats extends LogicError {
        final public List<FailedCaveat> errors;

        public FailedCaveats(List<FailedCaveat> errors) {
            this.errors = errors;
        }
    }
}