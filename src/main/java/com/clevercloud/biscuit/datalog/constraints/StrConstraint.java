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
   public abstract Schema.StringConstraint serialize();

   static public Either<Error.FormatError, StrConstraint> deserialize_enum(Schema.StringConstraint c) {
      if(c.getKind() == Schema.StringConstraint.Kind.PREFIX) {
         return Prefix.deserialize(c);
      } else if(c.getKind() == Schema.StringConstraint.Kind.SUFFIX) {
         return Suffix.deserialize(c);
      } else if(c.getKind() == Schema.StringConstraint.Kind.EQUAL) {
         return Equal.deserialize(c);
      } else if(c.getKind() == Schema.StringConstraint.Kind.REGEX) {
         return Regex.deserialize(c);
      } else if(c.getKind() == Schema.StringConstraint.Kind.IN) {
         return InSet.deserialize(c);
      } else if(c.getKind() == Schema.StringConstraint.Kind.NOT_IN) {
         return NotInSet.deserialize(c);
      } else {
         return Left(new Error().new FormatError().new DeserializationError("invalid String constraint kind"));
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

      public Schema.StringConstraint serialize() {
         return Schema.StringConstraint.newBuilder()
                 .setKind(Schema.StringConstraint.Kind.PREFIX)
                 .setPrefix(this.value).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserialize(Schema.StringConstraint i) {
         if(!i.hasPrefix()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid String constraint"));
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

      public Schema.StringConstraint serialize() {
         return Schema.StringConstraint.newBuilder()
                 .setKind(Schema.StringConstraint.Kind.SUFFIX)
                 .setSuffix(this.value).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserialize(Schema.StringConstraint i) {
         if(!i.hasSuffix()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid String constraint"));
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

      public Schema.StringConstraint serialize() {
         return Schema.StringConstraint.newBuilder()
                 .setKind(Schema.StringConstraint.Kind.EQUAL)
                 .setEqual(this.value).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserialize(Schema.StringConstraint i) {
         if(!i.hasEqual()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid String constraint"));
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

      public Schema.StringConstraint serialize() {
         return Schema.StringConstraint.newBuilder()
                 .setKind(Schema.StringConstraint.Kind.REGEX)
                 .setRegex(this.pattern).build();
      }

      static public Either<Error.FormatError, StrConstraint> deserialize(Schema.StringConstraint i) {
         if(!i.hasRegex()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid String constraint"));
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

      public Schema.StringConstraint serialize() {
         Schema.StringConstraint.Builder b = Schema.StringConstraint.newBuilder()
                 .setKind(Schema.StringConstraint.Kind.IN);
         for (String s: this.value) {
            b.addInSet(s);
         }
         return b.build();
      }

      static public Either<Error.FormatError, StrConstraint> deserialize(Schema.StringConstraint i) {
         Set<String> values = new HashSet<>();
         for (String l: i.getInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid String constraint"));
         } else {
            return Right(new InSet(values));
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

      public Schema.StringConstraint serialize() {
         Schema.StringConstraint.Builder b = Schema.StringConstraint.newBuilder()
                 .setKind(Schema.StringConstraint.Kind.NOT_IN);
         for (String s: this.value) {
            b.addNotInSet(s);
         }
         return b.build();
      }

      static public Either<Error.FormatError, StrConstraint> deserialize(Schema.StringConstraint i) {
         Set<String> values = new HashSet<>();
         for (String l: i.getNotInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid String constraint"));
         } else {
            return Right(new NotInSet(values));
         }
      }
   }
}
