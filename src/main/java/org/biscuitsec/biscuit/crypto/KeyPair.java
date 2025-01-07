package org.biscuitsec.biscuit.crypto;


import biscuit.format.schema.Schema;
import biscuit.format.schema.Schema.PublicKey.Algorithm;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.biscuitsec.biscuit.token.builder.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;

/**
 * Private and public key
 */
public final class KeyPair {
    public final EdDSAPrivateKey privateKey;
    public final EdDSAPublicKey publicKey;

    public static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);

    public KeyPair() {
        this(new SecureRandom());
    }

    public KeyPair(final SecureRandom rng) {
        byte[] b = new byte[32];
        rng.nextBytes(b);

        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(b, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.privateKey = privKey;
        this.publicKey = pubKey;
    }

    public static KeyPair generate(Algorithm algorithm) {
        return generate(algorithm, new SecureRandom());
    }

    public static KeyPair generate(Algorithm algorithm, SecureRandom rng) {
        if (algorithm == Algorithm.Ed25519) {
            return new KeyPair(rng);
        } else {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
    }

    public static Signature generateSignature(Algorithm algorithm) throws NoSuchAlgorithmException {
        if (algorithm == Algorithm.Ed25519) {
            return KeyPair.getSignature();
        } else {
            throw new NoSuchAlgorithmException("Unsupported algorithm");
        }
    }

    public byte[] toBytes() {
        return this.privateKey.getSeed();
    }

    public KeyPair(byte[] b) {
        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(b, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.privateKey = privKey;
        this.publicKey = pubKey;
    }

    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    public KeyPair(String hex) {
        byte[] b = Utils.hexStringToByteArray(hex);

        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(b, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.privateKey = privKey;
        this.publicKey = pubKey;
    }

    public static Signature getSignature() throws NoSuchAlgorithmException {
        return new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
    }

    public PublicKey publicKey() {
        return new PublicKey(Schema.PublicKey.Algorithm.Ed25519, this.publicKey);
    }
}
