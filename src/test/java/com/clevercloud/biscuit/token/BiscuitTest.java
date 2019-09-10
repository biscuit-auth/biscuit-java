package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static com.clevercloud.biscuit.token.builder.Block.*;

import com.clevercloud.biscuit.token.builder.Block;

public class BiscuitTest extends TestCase {
    public BiscuitTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BiscuitTest.class);
    }

    public void testBasic() {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);

        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file2"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, authority_builder.build()).get();

        System.out.println(b.print());

        System.out.println("serializing the first token");

        byte[] data = b.serialize().get();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(hex(data));

        System.out.println("deserializing the first token");
        Biscuit deser = Biscuit.from_bytes(data, root.public_key).get();

        System.out.println(deser.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.create_block();
        builder.add_caveat(rule(
                "caveat1",
                Arrays.asList(var(0)),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var(0))),
                        pred("operation", Arrays.asList(s("ambient"), s("read"))),
                        pred("right", Arrays.asList(s("authority"), var(0), s("read")))
                )
        ));

        Biscuit b2 = deser.append(rng, keypair2, builder.build()).get();

        System.out.println(b2.print());

        System.out.println("serializing the second token");

        byte[] data2 = b2.serialize().get();

        System.out.print("data len: ");
        System.out.println(data2.length);
        System.out.println(hex(data2));

        System.out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.from_bytes(data2, root.public_key).get();

        System.out.println(deser2.print());

        /*
        String message1 = "hello";
        KeyPair keypair1 = new KeyPair(rng);
        Token token1 = new Token(rng, keypair1, message1.getBytes());
        Assert.assertEquals(Right(null), token1.verify());

        Schema.Biscuit biscuitSer = token1.serialize();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        biscuitSer.writeTo(stream);
        byte[] data = stream.toByteArray();

        Schema.Biscuit biscuitDeser = Schema.Biscuit.parseFrom(data);

        Token tokenDeser = Token.deserialize(biscuitDeser);
        Assert.assertEquals(Right(null), tokenDeser.verify());
        System.out.println("token1");
        for(byte[] block: token1.blocks) {
            System.out.println(hex(block));
        }
        System.out.println("tokenDeser");
        for(byte[] block: tokenDeser.blocks) {
            System.out.println(hex(block));
        }
        Assert.assertEquals(token1.blocks, tokenDeser.blocks);
        */
    }
}
