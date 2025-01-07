package org.biscuitsec.biscuit.token.builder.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;

public class Error extends Exception {
    final String input;
    final String message;

    public Error(String input, String message) {
        super(message);
        this.input = input;
        this.message = message;
    }

    @Override
    public int hashCode() {
        int result = input != null ? input.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Error error = (Error) o;
        if (!Objects.equals(input, error.input)) {
            return false;
        }

        return Objects.equals(message, error.message);
    }

    public JsonElement toJson() {
        JsonObject jo = new JsonObject();
        jo.addProperty("input", this.input);
        jo.addProperty("message", this.message);
        return jo;
    }

    @Override
    public String toString() {
        return "Error{" +
                "input='" + input + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
