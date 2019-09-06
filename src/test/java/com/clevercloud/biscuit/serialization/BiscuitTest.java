package com.clevercloud.biscuit.serialization;

import biscuit.format.schema.Schema;
import cafe.cryptography.curve25519.InvalidEncodingException;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.SignatureTest;
import com.clevercloud.biscuit.crypto.Token;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static io.vavr.API.Right;

public class BiscuitTest extends TestCase {
    public BiscuitTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BiscuitTest.class);
    }

    public void testSerialize() throws IOException, InvalidEncodingException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

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

    }
}
