package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

public abstract class Term {
    abstract public ID convert(SymbolTable symbols);
    static public Term convert_from(ID id, SymbolTable symbols) {
        return id.toTerm(symbols);
    }

    public static class Str extends Term {
        String value;

        public String getValue() {
            return value;
        }

        public Str(String value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Str(symbols.insert(this.value));
        }

            @Override
        public String toString() {
            return "\""+value+"\"";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Str s = (Str) o;
            return Objects.equals(value, s.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class Variable extends Term {
        String value;

        public Variable(String value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Variable(symbols.insert(this.value));
        }

        @Override
        public String toString() {
            return "$"+value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Variable variable = (Variable) o;

            return value.equals(variable.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    public static class Integer extends Term {
        long value;

        public Integer(long value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Integer(this.value);
        }

        @Override
        public String toString() {
            return ""+value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Integer integer = (Integer) o;

            return value == integer.value;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }
    }


    public static class Bytes extends Term {
        byte[] value;

        public Bytes(byte[] value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Bytes(this.value);
        }

        @Override
        public String toString() {
            return "\""+value+"\"";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Bytes bytes = (Bytes) o;

            return Arrays.equals(value, bytes.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    public static class Date extends Term {
        long value;

        public Date(long value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Date(this.value);
        }

        @Override
        public String toString() {
            return ""+value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Date date = (Date) o;

            return value == date.value;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }
    }

    public static class Bool extends Term {
        boolean value;

        public Bool(boolean value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Bool(this.value);
        }

        @Override
        public String toString() {
            return ""+value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Bool bool = (Bool) o;

            return value == bool.value;
        }

        @Override
        public int hashCode() {
            return (value ? 1 : 0);
        }
    }

    public static class Set extends Term {
        HashSet<Term> value;

        public Set(HashSet<Term> value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            HashSet<ID> s = new HashSet<>();

            for(Term t: this.value) {
                s.add(t.convert(symbols));
            }

            return new ID.Set(s);
        }

        @Override
        public String toString() {
            return "[" +
                     value +
                    ']';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Set set = (Set) o;

            return value != null ? value.equals(set.value) : set.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }
}
