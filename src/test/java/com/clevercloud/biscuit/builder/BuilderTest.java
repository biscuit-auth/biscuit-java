package com.clevercloud.biscuit.builder;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Biscuit;
import com.clevercloud.biscuit.token.builder.Block;
import com.clevercloud.biscuit.token.builder.Expression;
import com.clevercloud.biscuit.token.builder.Term;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.clevercloud.biscuit.token.builder.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

public class BuilderTest {

    @Test
    public void testBuild() throws Error.Language, Error.SymbolTableOverlap, Error.FormatError {
        SecureRandom rng = new SecureRandom();
        KeyPair root = new KeyPair(rng);
        SymbolTable symbols = Biscuit.default_symbol_table();

        Block authority_builder = new Block(0, symbols);
        authority_builder.add_fact(fact("revocation_id", Arrays.asList(date(Date.from(Instant.now())))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("admin"))));
        authority_builder.add_rule(constrained_rule("right",
                Arrays.asList(s("namespace"), var("tenant"), var("namespace"), var("operation")),
                Arrays.asList(pred("ns_operation", Arrays.asList(s("namespace"), var("tenant"), var("namespace"), var("operation")))),
                Arrays.asList(
                        new Expression.Binary(
                                Expression.Op.Contains,
                                new Expression.Value(var("operation")),
                                new Expression.Value(new Term.Set(new HashSet<>(Arrays.asList(
                                        s("create_topic"),
                                        s("get_topic"),
                                        s("get_topics")
                                )))))
                )
        ));
        authority_builder.add_rule(constrained_rule("right",
                Arrays.asList(s("topic"), var("tenant"), var("namespace"), var("topic"), var("operation")),
                Arrays.asList(pred("topic_operation", Arrays.asList(s("topic"), var("tenant"), var("namespace"), var("topic"), var("operation")))),
                Arrays.asList(
                        new Expression.Binary(
                                Expression.Op.Contains,
                                new Expression.Value(var("operation")),
                                new Expression.Value(new Term.Set(new HashSet<>(Arrays.asList(
                                        s("lookup")
                                )))))
                )
        ));
        Biscuit rootBiscuit = Biscuit.make(rng, root, symbols, authority_builder.build());

        System.out.println(rootBiscuit.print());

        assertNotNull(rootBiscuit);
    }

    @Test
    public void testStringValueOfAStringTerm() {
        assertEquals( "\"hello\"", new Term.Str("hello").toString() );
    }

    @Test
    public void testStringValueOfAnIntegerTerm() {
        assertEquals( "123", new Term.Integer(123).toString() );
    }

    @Test
    public void testStringValueOfAVariableTerm() {
        assertEquals( "$hello", new Term.Variable("hello").toString() );
    }

    @Test
    public void testStringValueOfASetTerm() {
        String actual = new Term.Set(Set.of(new Term.Str("a"), new Term.Str("b"), new Term.Integer((3)))).toString();
        assertTrue(actual.startsWith("[["), "starts with [[");
        assertTrue(actual.endsWith("]]"), "ends with ]]");
        assertTrue(actual.contains("\"a\""), "contains a");
        assertTrue(actual.contains("\"b\""), "contains b");
        assertTrue(actual.contains("3"), "contains 3");
    }

    @Test
    public void testStringValueOfAByteArrayTermIsJustTheArrayReferenceNotTheContents() {
        String string = new Term.Bytes("Hello".getBytes(StandardCharsets.UTF_8)).toString();
        assertTrue(string.startsWith("\"[B@"), "starts with quote, and array reference");
        assertTrue(string.endsWith("\""), "ends with quote");
    }

    @Test
    public void testArrayValueIsCopy() {
        byte[] someBytes = "Hello".getBytes(StandardCharsets.UTF_8);
        Term.Bytes term = new Term.Bytes(someBytes);
        assertTrue(Arrays.equals(someBytes, term.getValue()), "same content");
        assertNotEquals(System.identityHashCode(someBytes), System.identityHashCode(term.getValue()), "different objects");
    }
}
