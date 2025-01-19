package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema.PublicKey.Algorithm;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.biscuitsec.biscuit.token.builder.Utils;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

final class Ed25519KeyPair extends KeyPair {

    static final int SIGNATURE_LENGTH = 64;

    private final EdDSAPrivateKey privateKey;
    private final EdDSAPublicKey publicKey;
    private final Signer signer;

    private static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);

    public Ed25519KeyPair(byte[] bytes) {
        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(bytes, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.privateKey = privKey;
        this.publicKey = pubKey;
        this.signer = new PrivateKeySigner(privKey);
    }

    public Ed25519KeyPair(SecureRandom rng) {
        byte[] b = new byte[32];
        rng.nextBytes(b);

        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(b, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.privateKey = privKey;
        this.publicKey = pubKey;
        this.signer = new PrivateKeySigner(privKey);
    }

    public Ed25519KeyPair(String hex) {
        this(Utils.hexStringToByteArray(hex));
    }

    public static java.security.PublicKey decode(byte[] data) {
        return new EdDSAPublicKey(new EdDSAPublicKeySpec(data, ed25519));
    }

    public static Signature getSignature() throws NoSuchAlgorithmException {
        return new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
    }

    @Override
    public byte[] toBytes() {
        return privateKey.getSeed();
    }

    @Override
    public String toHex() {
        return Utils.byteArrayToHexString(toBytes());
    }

    @Override
    public java.security.PublicKey publicKey() {
        return publicKey;
    }

    @Override
    public byte[] sign(byte[] block, byte[] publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.sign(block, Algorithm.Ed25519, publicKey);
    }

    @Override
    public byte[] sign(byte[] block, byte[] publicKey, byte[] signature) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.sign(block, Algorithm.Ed25519, publicKey, signature);
    }

    @Override
    public PublicKey public_key() {
        return new PublicKey(Algorithm.Ed25519, this.publicKey);
    }
}
