package com.clevercloud.biscuit.error;

import java.util.List;
import java.util.Objects;

import io.vavr.control.Option;

public class LogicError {
    public Option<List<FailedCheck>> failed_checks() {
        return Option.none();
    }

    public static class InvalidAuthorityFact extends LogicError {
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

    public static class InvalidAmbientFact extends LogicError {
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

    public static class InvalidBlockFact extends LogicError {
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
    public static class FailedChecks extends LogicError {
        final public List<FailedCheck> errors;

        public FailedChecks(List<FailedCheck> errors) {
            this.errors = errors;
        }

        public Option<List<FailedCheck>> failed_checks() {
            return Option.some(errors);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedChecks other = (FailedChecks) o;
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
            return "LogicError.FailedChecks{ errors: " + errors + " }";
        }
    }

    public static class NoMatchingPolicy extends LogicError {
        public NoMatchingPolicy() {
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return "NoMatchingPolicy{}";
        }
    }

    public static class Denied extends LogicError {
        private long id;

        public Denied(long id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return "Denied("+id+")";
        }
    }

    public static class AuthorizerNotEmpty extends LogicError {

        public AuthorizerNotEmpty() {

        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return "AuthorizerNotEmpty";
        }
    }
}