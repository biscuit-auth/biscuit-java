package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.Arrays;
import java.util.Objects;

public abstract class Atom {
    abstract public ID convert(SymbolTable symbols);

    public static class Symbol extends Atom {
        String value;

        public Symbol(String value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Symbol(symbols.insert(this.value));
        }

            @Override
        public String toString() {
            return "#"+value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Symbol symbol = (Symbol) o;
            return Objects.equals(value, symbol.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class Variable extends Atom {
        int value;

        public Variable(int value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Variable(this.value);
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

            return value == variable.value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    public static class Integer extends Atom {
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

    public static class Str extends Atom {
        String value;

        public Str(String value) {
            this.value = value;
        }

        @Override
        public ID convert(SymbolTable symbols) {
            return new ID.Str(this.value);
        }

        @Override
        public String toString() {
            return "\""+value+"\"";
        }

        public String value() { return value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Str str = (Str) o;

            return value != null ? value.equals(str.value) : str.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    public static class Bytes extends Atom {
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

    public static class Date extends Atom {
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
}
