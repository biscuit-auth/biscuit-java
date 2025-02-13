package org.biscuitsec.biscuit.crypto;


import biscuit.format.schema.Schema.PublicKey.Algorithm;
import net.i2p.crypto.eddsa.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Optional;

/**
 * Private and public key.
 */
public abstract class KeyPair {

    public static KeyPair generate(Algorithm algorithm) {
        return generate(algorithm, new SecureRandom());
    }

    public static KeyPair generate(Algorithm algorithm, String hex) {
        return generate(algorithm, Utils.hexToBytes(hex));
    }

    public static KeyPair generate(Algorithm algorithm, byte[] bytes) {
        if (algorithm == Algorithm.Ed25519) {
            return new Ed25519KeyPair(bytes);
        } else if (algorithm == Algorithm.SECP256R1) {
            return new SECP256R1KeyPair(bytes);
        } else {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
    }

    public static KeyPair generate(Algorithm algorithm, SecureRandom rng) {
        if (algorithm == Algorithm.Ed25519) {
            return new Ed25519KeyPair(rng);
        } else if (algorithm == Algorithm.SECP256R1) {
            return new SECP256R1KeyPair(rng);
        } else {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
    }

    public static KeyPair generate(PublicKey publicKey, Signer signer) {
        return new RemoteKeyPair(publicKey, signer);
    }

    public static Signature generateSignature(Algorithm algorithm) throws NoSuchAlgorithmException {
        if (algorithm == Algorithm.Ed25519) {
            return Ed25519KeyPair.getSignature();
        } else if (algorithm == Algorithm.SECP256R1) {
            return SECP256R1KeyPair.getSignature();
        } else {
            throw new NoSuchAlgorithmException("Unsupported algorithm");
        }
    }

    public abstract byte[] toBytes();

    public abstract String toHex();

    public abstract java.security.PublicKey publicKey();

    public abstract PublicKey public_key();

    public abstract byte[] sign(byte[] block, byte[] publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException;

    public abstract byte[] signExternal(byte[] block, byte[] publicKey, byte[] external) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException;

    public abstract byte[] signSealed(byte[] block, byte[] publicKey, byte[] seal) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException;
}
