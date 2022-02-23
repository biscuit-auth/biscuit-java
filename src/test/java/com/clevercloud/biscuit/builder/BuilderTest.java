package com.clevercloud.biscuit.builder;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Biscuit;
import com.clevercloud.biscuit.token.builder.Block;
import com.clevercloud.biscuit.token.builder.Expression;
import com.clevercloud.biscuit.token.builder.Term;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import static com.clevercloud.biscuit.token.builder.Utils.*;

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
}
