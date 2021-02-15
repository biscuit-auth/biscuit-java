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
    public abstract Schema.BytesConstraintV1 serialize();

    static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserialize_enumV0(Schema.BytesConstraintV0 c) {
        if(c.getKind() == Schema.BytesConstraintV0.Kind.EQUAL) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.Equal.deserializeV0(c);
        } else if(c.getKind() == Schema.BytesConstraintV0.Kind.IN) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.InSet.deserializeV0(c);
        } else if(c.getKind() == Schema.BytesConstraintV0.Kind.NOT_IN) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.NotInSet.deserializeV0(c);
        } else {
            return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint kind"));
        }
    }

    static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserialize_enumV1(Schema.BytesConstraintV1 c) {
        if(c.hasEqual()) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.Equal.deserializeV1(c);
        } else if(c.hasInSet()) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.InSet.deserializeV1(c);
        } else if(c.hasNotInSet()) {
            return com.clevercloud.biscuit.datalog.constraints.BytesConstraint.NotInSet.deserializeV1(c);
        } else {
            return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint kind"));
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

        public Schema.BytesConstraintV1 serialize() {
            return Schema.BytesConstraintV1.newBuilder()
                    .setEqual(ByteString.EMPTY.copyFrom(this.value)).build();
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserializeV0(Schema.BytesConstraintV0 i) {
            if(!i.hasEqual()) {
                return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new Equal(i.getEqual().toByteArray()));
            }
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserializeV1(Schema.BytesConstraintV1 i) {
            if(!i.hasEqual()) {
                return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
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

        public Schema.BytesConstraintV1 serialize() {
            Schema.BytesConstraintV1.Builder b = Schema.BytesConstraintV1.newBuilder();
            Schema.BytesSet.Builder s = Schema.BytesSet.newBuilder();

            for (byte[] l: this.value) {
                s.addSet(ByteString.copyFrom(l));
            }
            b.setInSet(s);

            return b.build();
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserializeV0(Schema.BytesConstraintV0 i) {
            Set<byte[]> values = new HashSet<>();
            for (ByteString l: i.getInSetList()) {
                values.add(l.toByteArray());
            }
            if(values.isEmpty()) {
                return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new InSet(values));
            }
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserializeV1(Schema.BytesConstraintV1 i) {
            Set<byte[]> values = new HashSet<>();
            Schema.BytesSet s = i.getInSet();
            for (ByteString l: s.getSetList()) {
                values.add(l.toByteArray());
            }
            if(values.isEmpty()) {
                return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new BytesConstraint.InSet(values));
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

        public Schema.BytesConstraintV1 serialize() {
            Schema.BytesConstraintV1.Builder b = Schema.BytesConstraintV1.newBuilder();
            Schema.BytesSet.Builder s = Schema.BytesSet.newBuilder();

            for (byte[] l: this.value) {
                s.addSet(ByteString.copyFrom(l));
            }
            b.setNotInSet(s);

            return b.build();
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserializeV0(Schema.BytesConstraintV0 i) {
            Set<byte[]> values = new HashSet<>();
            for (ByteString l: i.getNotInSetList()) {
                values.add(l.toByteArray());
            }
            if(values.isEmpty()) {
                return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new NotInSet(values));
            }
        }

        static public Either<Error.FormatError, com.clevercloud.biscuit.datalog.constraints.BytesConstraint> deserializeV1(Schema.BytesConstraintV1 i) {
            Set<byte[]> values = new HashSet<>();
            Schema.BytesSet s = i.getNotInSet();
            for (ByteString l: s.getSetList()) {
                values.add(l.toByteArray());
            }
            if(values.isEmpty()) {
                return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
            } else {
                return Right(new BytesConstraint.NotInSet(values));
            }
        }
    }
}
