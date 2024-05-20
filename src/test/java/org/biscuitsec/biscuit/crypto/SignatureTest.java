package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

import org.biscuitsec.biscuit.error.Error;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @serial exclude
 */
public class SignatureTest {

    @Test
    public void testSerialize() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        byte[] seed = {1, 2, 3, 4};
        SecureRandom rng = new SecureRandom(seed);

        KeyPair keypair = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        PublicKey pubkey = keypair.public_key();

        byte[] serializedSecretKey = keypair.toBytes();
        byte[] serializedPublicKey = pubkey.toBytes();

        KeyPair deserializedSecretKey = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, serializedSecretKey);
        PublicKey deserializedPublicKey = new PublicKey(Schema.PublicKey.Algorithm.Ed25519, serializedPublicKey);

        assertEquals(32, serializedSecretKey.length);
        assertEquals(32, serializedPublicKey.length);

        System.out.println(keypair.toHex());
        System.out.println(deserializedSecretKey.toHex());
        assertEquals(keypair.toBytes(), deserializedSecretKey.toBytes());
        System.out.println(pubkey.toHex());
        System.out.println(deserializedPublicKey.toHex());
        assertEquals(pubkey.toHex(), deserializedPublicKey.toHex());
    }

    @Test
    public void testThreeMessages() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        String message1 = "hello";
        KeyPair root = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        KeyPair keypair2 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        System.out.println("root key: " + root.toHex());
        System.out.println("keypair2: " + keypair2.toHex());
        System.out.println("root key public: " + root.public_key().toHex());
        System.out.println("keypair2 public: " + keypair2.public_key().toHex());

        Token token1 = new Token(root, message1.getBytes(), keypair2);
        assertEquals(Right(null), token1.verify(root.public_key()));

        String message2 = "world";
        KeyPair keypair3 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        Token token2 = token1.append(keypair3, message2.getBytes());
        assertEquals(Right(null), token2.verify(root.public_key()));

        String message3 = "!!";
        KeyPair keypair4 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        Token token3 = token2.append(keypair4, message3.getBytes());
        assertEquals(Right(null), token3.verify(root.public_key()));
    }

    @Test
    public void testChangeMessages() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        String message1 = "hello";
        KeyPair root = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        KeyPair keypair2 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        Token token1 = new Token(root, message1.getBytes(), keypair2);
        assertEquals(Right(null), token1.verify(new PublicKey(Schema.PublicKey.Algorithm.Ed25519, root.publicKey())));

        String message2 = "world";
        KeyPair keypair3 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        Token token2 = token1.append(keypair3, message2.getBytes());
        token2.blocks.set(1, "you".getBytes());
        assertEquals(Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied")),
                token2.verify(new PublicKey(Schema.PublicKey.Algorithm.Ed25519, root.publicKey())));

        String message3 = "!!";
        KeyPair keypair4 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        Token token3 = token2.append(keypair4, message3.getBytes());
        assertEquals(Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied")),
                token3.verify(new PublicKey(Schema.PublicKey.Algorithm.Ed25519, root.publicKey())));
    }
}
