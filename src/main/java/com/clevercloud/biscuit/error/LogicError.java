package com.clevercloud.biscuit.error;

import java.util.ArrayList;
import java.util.List;
import io.vavr.control.Option;

public class LogicError {
    public Option<List<FailedCaveat>> failed_caveats() {
        return Option.none();
    }

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
        final public ArrayList<FailedCaveat> errors;

        public FailedCaveats(ArrayList<FailedCaveat> errors) {
            this.errors = errors;
        }

        public Option<List<FailedCaveat>> failed_caveats() {
            return Option.some(errors);
        }
    }
}