package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.google.protobuf.ByteString;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class BytesConstraint implements Serializable {

     public abstract boolean check(final byte[] value);
    public abstract Schema.BytesConstraint serialize();

    static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserialize_enum(Schema.BytesConstraint c) {
        if(c.getKind() == Schema.BytesConstraint.Kind.EQUAL) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.Equal.deserialize(c);
        } else if(c.getKind() == Schema.BytesConstraint.Kind.IN) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.InSet.deserialize(c);
        } else if(c.getKind() == Schema.BytesConstraint.Kind.NOT_IN) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.NotInSet.deserialize(c);
        } else {
            return Left(new Error().new FormatError().new DeserializationError("invalid Bytes constraint kind"));
        }
    }

    public static final class Equal extends com.clevercloud.biscuit.datalog.constraints.BytesConstraint implements Serializable {
        private final byte[] value;

        public boolean check(final byte[] value) {
            return this.value.equals(value);
        }

        public Equal(final byte[] value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "== " + this.value;
        }

        public Schema.BytesConstraint serialize() {
            return Schema.BytesConstraint.newBuilder()
                    .setKind(Schema.BytesConstraint.Kind.EQUAL)
                    .setEqual(ByteString.EMPTY.copyFrom(this.value)).build();
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserialize(Schema.BytesConstraint i) {
            if(!i.hasEqual()) {
                return Left(new Error().new FormatError().new DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new Equal(i.getEqual().toByteArray()));
            }
        }
    }

    public static final class InSet extends com.clevercloud.biscuit.datalog.constraints.BytesConstraint implements Serializable {
        private final Set<byte[]> value;

        public boolean check(final byte[] value) {
            return this.value.contains(value);
        }

        public InSet(final Set<byte[]> value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "in " + this.value;
        }

        public Schema.BytesConstraint serialize() {
            Schema.BytesConstraint.Builder b = Schema.BytesConstraint.newBuilder()
                    .setKind(Schema.BytesConstraint.Kind.IN);
            for (byte[] s: this.value) {
                b.addInSet(ByteString.EMPTY.copyFrom(s));
            }
            return b.build();
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserialize(Schema.BytesConstraint i) {
            Set<byte[]> values = new HashSet<>();
            for (ByteString l: i.getInSetList()) {
                values.add(l.toByteArray());
            }
            if(values.isEmpty()) {
                return Left(new Error().new FormatError().new DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new InSet(values));
            }
        }
    }

    public static final class NotInSet extends com.clevercloud.biscuit.datalog.constraints.BytesConstraint implements Serializable {
        private final Set<byte[]> value;

        public boolean check(final byte[] value) {
            return !this.value.contains(value);
        }

        public NotInSet(final Set<byte[]> value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "not in " + this.value;
        }

        public Schema.BytesConstraint serialize() {
            Schema.BytesConstraint.Builder b = Schema.BytesConstraint.newBuilder()
                    .setKind(Schema.BytesConstraint.Kind.NOT_IN);
            for (byte[] s: this.value) {
                b.addNotInSet(ByteString.EMPTY.copyFrom(s));
            }
            return b.build();
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserialize(Schema.BytesConstraint i) {
            Set<byte[]> values = new HashSet<>();
            for (ByteString l: i.getNotInSetList()) {
                values.add(l.toByteArray());
            }
            if(values.isEmpty()) {
                return Left(new Error().new FormatError().new DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new NotInSet(values));
            }
        }
    }
}
