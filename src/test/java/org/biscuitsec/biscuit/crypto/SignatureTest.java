package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;
import org.biscuitsec.biscuit.error.Error;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @serial exclude
 */
public class SignatureTest {

    @Test
    public void testSerialize() {
        byte[] seed = {1, 2, 3, 4};
        SecureRandom rng = new SecureRandom(seed);

        KeyPair keypair = new KeyPair(rng);
        PublicKey pubkey = keypair.publicKey();

        byte[] serializedSecretKey = keypair.toBytes();
        byte[] serializedPublicKey = pubkey.toBytes();

        KeyPair deserializedSecretKey = new KeyPair(serializedSecretKey);
        PublicKey deserializedPublicKey = new PublicKey(Schema.PublicKey.Algorithm.Ed25519, serializedPublicKey);

        assertEquals(32, serializedSecretKey.length);
        assertEquals(32, serializedPublicKey.length);

        out.println(keypair.toHex());
        out.println(deserializedSecretKey.toHex());
        assertEquals(keypair.toBytes(), deserializedSecretKey.toBytes());
        out.println(pubkey.toHex());
        out.println(deserializedPublicKey.toHex());
        assertEquals(pubkey.toHex(), deserializedPublicKey.toHex());
    }

    @Test
    public void testThreeMessages() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        String message1 = "hello";
        KeyPair root = new KeyPair(rng);
        KeyPair keypair2 = new KeyPair(rng);
        out.println("root key: " + root.toHex());
        out.println("keypair2: " + keypair2.toHex());
        out.println("root key public: " + root.publicKey().toHex());
        out.println("keypair2 public: " + keypair2.publicKey().toHex());

        Token token1 = new Token(root, message1.getBytes(), keypair2);
        assertEquals(Right(null), token1.verify(root.publicKey()));

        String message2 = "world";
        KeyPair keypair3 = new KeyPair(rng);
        Token token2 = token1.append(keypair3, message2.getBytes());
        assertEquals(Right(null), token2.verify(root.publicKey()));

        String message3 = "!!";
        KeyPair keypair4 = new KeyPair(rng);
        Token token3 = token2.append(keypair4, message3.getBytes());
        assertEquals(Right(null), token3.verify(root.publicKey()));
    }

    @Test
    public void testChangeMessages() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        String message1 = "hello";
        KeyPair root = new KeyPair(rng);
        KeyPair keypair2 = new KeyPair(rng);
        Token token1 = new Token(root, message1.getBytes(), keypair2);
        assertEquals(Right(null), token1.verify(new PublicKey(Schema.PublicKey.Algorithm.Ed25519, root.publicKey)));

        String message2 = "world";
        KeyPair keypair3 = new KeyPair(rng);
        Token token2 = token1.append(keypair3, message2.getBytes());
        token2.blocks.set(1, "you".getBytes());
        assertEquals(Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied")),
                token2.verify(new PublicKey(Schema.PublicKey.Algorithm.Ed25519, root.publicKey)));

        String message3 = "!!";
        KeyPair keypair4 = new KeyPair(rng);
        Token token3 = token2.append(keypair4, message3.getBytes());
        assertEquals(Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied")),
                token3.verify(new PublicKey(Schema.PublicKey.Algorithm.Ed25519, root.publicKey)));
    }
}
