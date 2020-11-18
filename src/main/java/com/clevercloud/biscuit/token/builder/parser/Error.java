package com.clevercloud.biscuit.token.builder.parser;

public class Error {
    String input;
    String message;

    public Error(String input, String message) {
        this.input = input;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Error{" +
                "input='" + input + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Error error = (Error) o;

        if (input != null ? !input.equals(error.input) : error.input != null) return false;
        return message != null ? message.equals(error.message) : error.message == null;
    }

    @Override
    public int hashCode() {
        int result = input != null ? input.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
