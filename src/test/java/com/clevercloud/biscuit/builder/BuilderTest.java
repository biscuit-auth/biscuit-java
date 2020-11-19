package com.clevercloud.biscuit.builder;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.token.Biscuit;
import com.clevercloud.biscuit.token.builder.Block;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import static com.clevercloud.biscuit.token.builder.Utils.*;

public class BuilderTest extends TestCase {
    public BuilderTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(com.clevercloud.biscuit.builder.BuilderTest.class);
    }

    public void testBuild() {
        SecureRandom rng = new SecureRandom();
        KeyPair root = new KeyPair(rng);
        SymbolTable symbols = Biscuit.default_symbol_table();

        Block authority_builder = new Block(0, symbols);
        authority_builder.add_fact(fact("revocation_id", Arrays.asList(date(Date.from(Instant.now())))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("admin"))));
        authority_builder.add_rule(constrained_rule("right",
                Arrays.asList(s("authority"), s("namespace"), var("tenant"), var("namespace"), var("operation")),
                Arrays.asList(pred("ns_operation", Arrays.asList(s("authority"), s("namespace"), var("tenant"), var("namespace"), var("operation")))),
                Arrays.asList(new com.clevercloud.biscuit.token.builder.constraints.SymbolConstraint.InSet("operation", new HashSet<>(Arrays.asList(
                        "create_topic",
                        "get_topic",
                        "get_topics"
                ))))
        ));
        authority_builder.add_rule(constrained_rule("right",
                Arrays.asList(s("authority"), s("topic"), var("tenant"), var("namespace"), var("topic"), var("operation")),
                Arrays.asList(pred("topic_operation", Arrays.asList(s("authority"), s("topic"), var("tenant"), var("namespace"), var("topic"), var("operation")))),
                Arrays.asList(new com.clevercloud.biscuit.token.builder.constraints.SymbolConstraint.InSet("operation", new HashSet<>(Arrays.asList(
                        "lookup"
                ))))
        ));
        Biscuit rootBiscuit = Biscuit.make(rng, root, symbols, authority_builder.build()).get();

        System.out.println(rootBiscuit.print());

        assertNotNull(rootBiscuit);
    }
}
