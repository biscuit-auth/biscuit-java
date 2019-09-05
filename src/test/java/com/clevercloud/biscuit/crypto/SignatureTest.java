package com.clevercloud.biscuit.crypto;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.SecureRandom;

public class SignatureTest extends TestCase {
        public SignatureTest(String testName) {
            super(testName);
        }

        public static Test suite() {
            return new TestSuite(SignatureTest.class);
        }

        public void testThreeMessages() {
            byte[] seed = {0, 0, 0, 0};
            SecureRandom rng = new SecureRandom(seed);

            String message1 = "hello";
            KeyPair keypair1 = new KeyPair(rng);
            Token token1 = new Token(rng, keypair1, message1.getBytes());
            Assert.assertTrue(token1.verify());

            String message2 = "world";
            KeyPair keypair2 = new KeyPair(rng);
            Token token2 = token1.append(rng, keypair2, message2.getBytes());
            Assert.assertTrue(token2.verify());

            String message3 = "!!";
            KeyPair keypair3 = new KeyPair(rng);
            Token token3 = token2.append(rng, keypair3, message3.getBytes());
            Assert.assertTrue(token3.verify());
        }

    public void testChangeMessages() {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        String message1 = "hello";
        KeyPair keypair1 = new KeyPair(rng);
        Token token1 = new Token(rng, keypair1, message1.getBytes());
        Assert.assertTrue(token1.verify());

        String message2 = "world";
        KeyPair keypair2 = new KeyPair(rng);
        Token token2 = token1.append(rng, keypair2, message2.getBytes());
        token2.blocks.set(1, "you".getBytes());
        Assert.assertFalse(token2.verify());

        String message3 = "!!";
        KeyPair keypair3 = new KeyPair(rng);
        Token token3 = token2.append(rng, keypair3, message3.getBytes());
        Assert.assertFalse(token3.verify());
    }
}
