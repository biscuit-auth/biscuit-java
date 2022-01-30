package com.clevercloud.biscuit.token.builder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class Utils {
    public static com.clevercloud.biscuit.token.builder.Fact fact(String name, List<Term> ids) {
        return new com.clevercloud.biscuit.token.builder.Fact(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Predicate pred(String name, List<Term> ids) {
        return new com.clevercloud.biscuit.token.builder.Predicate(name, ids);
    }

    public static com.clevercloud.biscuit.token.builder.Rule rule(String head_name, List<Term> head_ids,
                                                                  List<com.clevercloud.biscuit.token.builder.Predicate> predicates) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, new ArrayList<>());
    }

    public static com.clevercloud.biscuit.token.builder.Rule constrained_rule(String head_name, List<Term> head_ids,
                                                                              List<com.clevercloud.biscuit.token.builder.Predicate> predicates,
                                                                              List<Expression> expressions) {
        return new com.clevercloud.biscuit.token.builder.Rule(pred(head_name, head_ids), predicates, expressions);
    }

    public static Check check(com.clevercloud.biscuit.token.builder.Rule rule) {
        return new Check(rule);
    }

    public static Term integer(long i) {
        return new Term.Integer(i);
    }

    public static Term string(String s) {
        return new Term.Str(s);
    }

    public static Term s(String str) {
        return new Term.Str(str);
    }

    public static Term date(Date d) {
        return new Term.Date(d.getTime() / 1000);
    }

    public static Term var(String name) {
        return new Term.Variable(name);
    }

    public static Term set(HashSet<Term> s) {
        return new Term.Set(s);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String hex) {
        int l = hex.length();
        byte[] data = new byte[l/2];
        for (int i = 0; i < l; i += 2) {
            data[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
