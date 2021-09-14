package com.clevercloud.biscuit.crypto;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import com.clevercloud.biscuit.error.Error;

/**
 * @serial exclude
 */
public class SignatureTest extends TestCase {
        public SignatureTest(String testName) {
            super(testName);
        }

        public static Test suite() {
            return new TestSuite(SignatureTest.class);
        }

    public void testSerialize() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] seed = {1, 2, 3, 4};
        SecureRandom rng = new SecureRandom(seed);

        KeyPair keypair = new KeyPair(rng);
        PublicKey pubkey = keypair.public_key();

        byte[] serializedSecretKey = keypair.toBytes();
        byte[] serializedPublicKey = pubkey.toBytes();

        KeyPair deserializedSecretKey = new KeyPair(serializedSecretKey);
        PublicKey deserializedPublicKey = new PublicKey(serializedPublicKey);

        assertEquals(32, serializedSecretKey.length);
        assertEquals(32, serializedPublicKey.length);

        System.out.println(keypair.toHex());
        System.out.println(deserializedSecretKey.toHex());
        assertEquals(keypair.toBytes(), deserializedSecretKey.toBytes());
        System.out.println(pubkey.toHex());
        System.out.println(deserializedPublicKey.toHex());
        assertEquals(pubkey.toHex(), deserializedPublicKey.toHex());
    }

    public void testThreeMessages() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
            byte[] seed = {0, 0, 0, 0};
            SecureRandom rng = new SecureRandom(seed);

            String message1 = "hello";
            KeyPair root = new KeyPair(rng);
            KeyPair keypair2 = new KeyPair(rng);
            System.out.println("root key: "+root.toHex());
            System.out.println("keypair2: "+keypair2.toHex());
        System.out.println("root key public: "+root.public_key().toHex());
        System.out.println("keypair2 public: "+keypair2.public_key().toHex());

            Token token1 = new Token(root, message1.getBytes(), keypair2);
            Assert.assertEquals(Right(null), token1.verify(root.public_key()));

            String message2 = "world";
            KeyPair keypair3 = new KeyPair(rng);
            Token token2 = token1.append(keypair3, message2.getBytes());
            Assert.assertEquals(Right(null), token2.verify(root.public_key()));

            String message3 = "!!";
            KeyPair keypair4 = new KeyPair(rng);
            Token token3 = token2.append(keypair4, message3.getBytes());
            Assert.assertEquals(Right(null), token3.verify(root.public_key()));
        }

    public void testChangeMessages() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        String message1 = "hello";
        KeyPair root = new KeyPair(rng);
        KeyPair keypair2 = new KeyPair(rng);
        Token token1 = new Token(root, message1.getBytes(), keypair2);
        Assert.assertEquals(Right(null), token1.verify(new PublicKey(root.public_key)));

        String message2 = "world";
        KeyPair keypair3 = new KeyPair(rng);
        Token token2 = token1.append(keypair3, message2.getBytes());
        token2.blocks.set(1, "you".getBytes());
        Assert.assertEquals(Left(new Error.FormatError.Signature.InvalidSignature()),
                token2.verify(new PublicKey(root.public_key)));

        String message3 = "!!";
        KeyPair keypair4 = new KeyPair(rng);
        Token token3 = token2.append(keypair4, message3.getBytes());
        Assert.assertEquals(Left(new Error.FormatError.Signature.InvalidSignature()),
                token3.verify(new PublicKey(root.public_key)));
    }
}
