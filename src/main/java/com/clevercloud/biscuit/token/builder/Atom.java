package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;

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
            return ""+value+"?";
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
    }
}
