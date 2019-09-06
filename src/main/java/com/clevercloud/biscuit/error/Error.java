package com.clevercloud.biscuit.error;

import java.util.List;

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
        }
        public class SerializationError extends FormatError {
            final public String e;

            public SerializationError(String e) {
                this.e = e;
            }
        }
        public class BlockDeserializationError extends FormatError {
            final public String e;

            public BlockDeserializationError(String e) {
                this.e = e;
            }
        }
        public class BlockSerializationError extends FormatError {
            final public String e;

            public BlockSerializationError(String e) {
                this.e = e;
            }
        }
    }
    public class InvalidAuthorityIndex extends Error {
        final public long index;

        public InvalidAuthorityIndex(long index) {
            this.index = index;
        }
    }
    public class InvalidBlockIndex extends Error {
        final public long expected;
        final public long found;

        public InvalidBlockIndex(long expected, long found) {
            this.expected = expected;
            this.found = found;
        }
    }
    public class SymbolTableOverlap extends Error {}
    public class Sealed extends Error {}
    public class FailedLogic extends Error {
        final public List<LogicError> errors;

        public FailedLogic(List<LogicError> errors) {
            this.errors = errors;
        }


    }
}
