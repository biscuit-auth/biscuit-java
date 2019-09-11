package com.clevercloud.biscuit.error;

import java.util.List;
import java.util.Objects;

public class Error {

    public class InternalError extends Error {}

    public class FormatError extends Error {
        public class Signature extends FormatError {
            public class InvalidFormat extends Signature {
                public InvalidFormat() {
                }
            }
            public class InvalidSignature extends Signature {}
        }

        public class SealedSignature extends FormatError {}
        public class EmptyKeys extends FormatError {}
        public class UnknownPublicKey extends FormatError {}
        public class DeserializationError extends FormatError {
            final public String e;

            public DeserializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                DeserializationError other = (DeserializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.DeserializationError{ error: "+  e + " }";
            }
        }
        public class SerializationError extends FormatError {
            final public String e;

            public SerializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                SerializationError other = (SerializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.SerializationError{ error: "+  e + " }";
            }
        }
        public class BlockDeserializationError extends FormatError {
            final public String e;

            public BlockDeserializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                BlockDeserializationError other = (BlockDeserializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.BlockDeserializationError{ error: "+  e + " }";
            }
        }
        public class BlockSerializationError extends FormatError {
            final public String e;

            public BlockSerializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                BlockSerializationError other = (BlockSerializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.BlockSerializationError{ error: "+  e + " }";
            }
        }
    }
    public class InvalidAuthorityIndex extends Error {
        final public long index;

        public InvalidAuthorityIndex(long index) {
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidAuthorityIndex other = (InvalidAuthorityIndex) o;
            return index == other.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index);
        }

        @Override
        public String toString() {
            return "Error.InvalidAuthorityIndex{ index: "+ index + " }";
        }
    }
    public class InvalidBlockIndex extends Error {
        final public long expected;
        final public long found;

        public InvalidBlockIndex(long expected, long found) {
            this.expected = expected;
            this.found = found;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidBlockIndex other = (InvalidBlockIndex) o;
            return expected == other.expected && found == other.found;
        }

        @Override
        public int hashCode() {
            return Objects.hash(expected, found);
        }

        @Override
        public String toString() {
            return "Error.InvalidBlockIndex{ expected: " + expected + ", found: " + found + " }";
        }
    }
    public class SymbolTableOverlap extends Error {}
    public class Sealed extends Error {}
    public class FailedLogic extends Error {
        final public List<LogicError> errors;

        public FailedLogic(List<LogicError> errors) {
            this.errors = errors;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedLogic other = (FailedLogic) o;
            if((errors.size() != other.errors.size())) {
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
            return "Error.FailedLogic{ errors: "+ errors + " }";
        }
    }
}
