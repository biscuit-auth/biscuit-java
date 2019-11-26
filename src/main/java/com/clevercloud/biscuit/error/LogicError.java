package com.clevercloud.biscuit.error;

import java.util.List;
import java.util.Objects;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidAuthorityFact other = (InvalidAuthorityFact) o;
            return e.equals(other.e);
        }

        @Override
        public int hashCode() {
            return Objects.hash(e);
        }

        @Override
        public String toString() {
            return "LogicError.InvalidAuthorityFact{ error: "+ e + " }";
        }
    }

    public class InvalidAmbientFact extends LogicError {
        final public String e;

        public InvalidAmbientFact(String e) {
            this.e = e;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidAmbientFact other = (InvalidAmbientFact) o;
            return e.equals(other.e);
        }

        @Override
        public int hashCode() {
            return Objects.hash(e);
        }

        @Override
        public String toString() {
            return "LogicError.InvalidAmbientFact{ error: "+ e + " }";
        }
    }

    public class InvalidBlockFact extends LogicError {
        final public long id;
        final public String e;

        public InvalidBlockFact(long id, String e) {
            this.id = id;
            this.e = e;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidBlockFact other = (InvalidBlockFact) o;
            return id == other.id && e.equals(other.e);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, e);
        }

        @Override
        public String toString() {
            return "LogicError.InvalidBlockFact{ id: "+id+", error: "+  e + " }";
        }
    }
    public class FailedCaveats extends LogicError {
        final public List<FailedCaveat> errors;

        public FailedCaveats(List<FailedCaveat> errors) {
            this.errors = errors;
        }

        public Option<List<FailedCaveat>> failed_caveats() {
            return Option.some(errors);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedCaveats other = (FailedCaveats) o;
            if(errors.size() != other.errors.size()) {
                return false;
            }
            for(int i = 0; i < errors.size(); i++) {
                if(!errors.get(i).equals(other.errors.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(errors);
        }

        @Override
        public String toString() {
            return "LogicError.FailedCaveats{ errors: " + errors + " }";
        }
    }
}