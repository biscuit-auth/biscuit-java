package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class StrConstraint implements Serializable {
   public abstract boolean check(final String value);
   public abstract Schema.StringConstraintV1 serialize();

   static public Either<Error.FormatError, StrConstraint> deserialize_enumV0(Schema.StringConstraintV0 c) {
      if(c.getKind() == Schema.StringConstraintV0.Kind.PREFIX) {
         return Prefix.deserializeV0(c);
      } else if(c.getKind() == Schema.StringConstraintV0.Kind.SUFFIX) {
         return Suffix.deserializeV0(c);
      } else if(c.getKind() == Schema.StringConstraintV0.Kind.EQUAL) {
         return Equal.deserializeV0(c);
      } else if(c.getKind() == Schema.StringConstraintV0.Kind.REGEX) {
         return Regex.deserializeV0(c);
      } else if(c.getKind() == Schema.StringConstraintV0.Kind.IN) {
         return InSet.deserializeV0(c);
      } else if(c.getKind() == Schema.StringConstraintV0.Kind.NOT_IN) {
         return NotInSet.deserializeV0(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid String constraint kind"));
      }
   }

   static public Either<Error.FormatError, StrConstraint> deserialize_enumV1(Schema.StringConstraintV1 c) {
      if(c.hasPrefix()) {
         return Prefix.deserializeV1(c);
      } else if(c.hasSuffix()) {
         return Suffix.deserializeV1(c);
      } else if(c.hasEqual()) {
         return Equal.deserializeV1(c);
      } else if(c.hasRegex()) {
         return Regex.deserializeV1(c);
      } else if(c.hasInSet()) {
         return InSet.deserializeV1(c);
      } else if(c.hasNotInSet()) {
         return NotInSet.deserializeV1(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid String constraint kind"));
      }
   }

   public static final class Prefix extends StrConstraint implements Serializable {
      private final String value;

      public boolean check(final String value) {
         return value.startsWith(this.value);
      }

      public Prefix(final String value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "matches " + this.value + "*";
      }

      public Schema.StringConstraintV1 serialize() {
         return Schema.StringConstraintV1.newBuilder()
                 .setPrefix(this.value).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV0(Schema.StringConstraintV0 i) {
         if(!i.hasPrefix()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Prefix(i.getPrefix()));
         }
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV1(Schema.StringConstraintV1 i) {
         if(!i.hasPrefix()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Prefix(i.getPrefix()));
         }
      }
   }

   public static final class Suffix extends StrConstraint implements Serializable {
      private final String value;

      public boolean check(final String value) {
         return value.endsWith(this.value);
      }

      public Suffix(final String value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "matches *" + this.value;
      }

      public Schema.StringConstraintV1 serialize() {
         return Schema.StringConstraintV1.newBuilder()
                 .setSuffix(this.value).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV0(Schema.StringConstraintV0 i) {
         if(!i.hasSuffix()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Suffix(i.getSuffix()));
         }
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV1(Schema.StringConstraintV1 i) {
         if(!i.hasSuffix()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Suffix(i.getSuffix()));
         }
      }
   }

   public static final class Equal extends StrConstraint implements Serializable {
      private final String value;

      public boolean check(final String value) {
         return this.value.equals(value);
      }

      public Equal(final String value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "== " + this.value;
      }

      public Schema.StringConstraintV1 serialize() {
         return Schema.StringConstraintV1.newBuilder()
                 .setEqual(this.value).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV0(Schema.StringConstraintV0 i) {
         if(!i.hasEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Equal(i.getEqual()));
         }
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV1(Schema.StringConstraintV1 i) {
         if(!i.hasEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Equal(i.getEqual()));
         }
      }
   }

   public static final class Regex extends StrConstraint implements Serializable {
      private final String pattern;

      public boolean check(final String value) {
         Pattern p = Pattern.compile(this.pattern);
         Matcher m = p.matcher(value);
         return m.matches();
      }

      public Regex(final String value) {
         this.pattern = value;
      }

      @Override
      public String toString() {
         return "matches /" + this.pattern + "/";
      }

      public Schema.StringConstraintV1 serialize() {
         return Schema.StringConstraintV1.newBuilder()
                 .setRegex(this.pattern).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV0(Schema.StringConstraintV0 i) {
         if(!i.hasRegex()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Regex(i.getRegex()));
         }
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV1(Schema.StringConstraintV1 i) {
         if(!i.hasRegex()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new Regex(i.getRegex()));
         }
      }
   }

   public static final class InSet extends StrConstraint implements Serializable {
      private final Set<String> value;

      public boolean check(final String value) {
         return this.value.contains(value);
      }

      public InSet(final Set<String> value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "in " + this.value;
      }

      public Schema.StringConstraintV1 serialize() {
         Schema.StringConstraintV1.Builder b = Schema.StringConstraintV1.newBuilder();
         Schema.StringSet.Builder s = Schema.StringSet.newBuilder();

         for (String l: this.value) {
            s.addSet(l);
         }
         b.setInSet(s);

         return b.build();
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV0(Schema.StringConstraintV0 i) {
         Set<String> values = new HashSet<>();
         for (String l: i.getInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new InSet(values));
         }
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV1(Schema.StringConstraintV1 i) {
         Set<String> values = new HashSet<>();
         Schema.StringSet s = i.getInSet();
         for (String l: s.getSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new StrConstraint.InSet(values));
         }
      }
   }

   public static final class NotInSet extends StrConstraint implements Serializable {
      private final Set<String> value;

      public boolean check(final String value) {
         return !this.value.contains(value);
      }

      public NotInSet(final Set<String> value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "not in " + this.value;
      }

      public Schema.StringConstraintV1 serialize() {
         Schema.StringConstraintV1.Builder b = Schema.StringConstraintV1.newBuilder();
         Schema.StringSet.Builder s = Schema.StringSet.newBuilder();

         for (String l: this.value) {
            s.addSet(l);
         }
         b.setNotInSet(s);

         return b.build();
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV0(Schema.StringConstraintV0 i) {
         Set<String> values = new HashSet<>();
         for (String l: i.getNotInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new NotInSet(values));
         }
      }

      static public Either<Error.FormatError, StrConstraint> deserializeV1(Schema.StringConstraintV1 i) {
         Set<String> values = new HashSet<>();
         Schema.StringSet s = i.getNotInSet();
         for (String l: s.getSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         } else {
            return Right(new StrConstraint.NotInSet(values));
         }
      }
   }
}
