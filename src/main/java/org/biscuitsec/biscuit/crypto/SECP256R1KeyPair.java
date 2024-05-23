package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;
import org.biscuitsec.biscuit.token.builder.Utils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.BigIntegers;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

class SECP256R1KeyPair extends KeyPair {

    private final BCECPrivateKey privateKey;
    private final BCECPublicKey publicKey;
    private final byte[] seed;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public SECP256R1KeyPair(byte[] bytes) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var kpg = getBcKeyPairGenerator();
        var spec = new ECGenParameterSpec("secp256r1");
        kpg.initialize(spec, new SecureRandom(bytes));
        var keyPair = kpg.generateKeyPair();
        privateKey = (BCECPrivateKey) keyPair.getPrivate();
        publicKey = (BCECPublicKey) keyPair.getPublic();
        seed = bytes;
    }

    public SECP256R1KeyPair(SecureRandom rng) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);
        var kpg = getBcKeyPairGenerator();
        var spec = new ECGenParameterSpec("secp256r1");
        kpg.initialize(spec, new SecureRandom(bytes));
        var keyPair = kpg.generateKeyPair();
        privateKey = (BCECPrivateKey) keyPair.getPrivate();
        publicKey = (BCECPublicKey) keyPair.getPublic();
        seed = bytes;
    }

    public SECP256R1KeyPair(String hex) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        this(Utils.hexStringToByteArray(hex));
    }

    public static java.security.PublicKey decode(byte[] data) {
        var params = ECNamedCurveTable.getParameterSpec("secp256r1");
        var spec = new ECPublicKeySpec(params.getCurve().decodePoint(data), params);
        return new BCECPublicKey("ECDSA", spec, BouncyCastleProvider.CONFIGURATION);
    }

    public static Signature getSignature() throws NoSuchAlgorithmException {
        try {
            return Signature.getInstance("SHA256withECDSA", "BC");
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] toBytes() {
        return BigIntegers.asUnsignedByteArray(privateKey.getD());
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
    public PrivateKey private_key() {
        return privateKey;
    }

    @Override
    public PublicKey public_key() {
        return new PublicKey(Schema.PublicKey.Algorithm.SECP256R1, publicKey);
    }

    private static KeyPairGenerator getBcKeyPairGenerator() throws NoSuchAlgorithmException {
        try {
            return KeyPairGenerator.getInstance("EC", "BC");
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
