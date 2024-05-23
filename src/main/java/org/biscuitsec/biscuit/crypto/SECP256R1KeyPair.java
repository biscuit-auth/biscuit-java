package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;
import org.biscuitsec.biscuit.token.builder.Utils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.BigIntegers;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;

class SECP256R1KeyPair extends KeyPair {

    private final BCECPrivateKey privateKey;
    private final BCECPublicKey publicKey;

    private static final String ALGORITHM = "ECDSA";
    private static final String CURVE = "secp256r1";
    private static final ECNamedCurveParameterSpec SECP256R1 = ECNamedCurveTable.getParameterSpec(CURVE);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public SECP256R1KeyPair(byte[] bytes) {
        var privateKeySpec = new ECPrivateKeySpec(BigIntegers.fromUnsignedByteArray(bytes), SECP256R1);
        var privateKey = new BCECPrivateKey(ALGORITHM, privateKeySpec, BouncyCastleProvider.CONFIGURATION);

        var publicKeySpec = new ECPublicKeySpec(SECP256R1.getG().multiply(privateKeySpec.getD()), SECP256R1);
        var publicKey = new BCECPublicKey(ALGORITHM, publicKeySpec, BouncyCastleProvider.CONFIGURATION);

        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public SECP256R1KeyPair(SecureRandom rng) {
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);

        var privateKeySpec = new ECPrivateKeySpec(BigIntegers.fromUnsignedByteArray(bytes), SECP256R1);
        var privateKey = new BCECPrivateKey(ALGORITHM, privateKeySpec, BouncyCastleProvider.CONFIGURATION);

        var publicKeySpec = new ECPublicKeySpec(SECP256R1.getG().multiply(privateKeySpec.getD()), SECP256R1);
        var publicKey = new BCECPublicKey(ALGORITHM, publicKeySpec, BouncyCastleProvider.CONFIGURATION);

        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public SECP256R1KeyPair(String hex) {
        this(Utils.hexStringToByteArray(hex));
    }

    public static java.security.PublicKey decode(byte[] data) {
        var params = ECNamedCurveTable.getParameterSpec(CURVE);
        var spec = new ECPublicKeySpec(params.getCurve().decodePoint(data), params);
        return new BCECPublicKey(ALGORITHM, spec, BouncyCastleProvider.CONFIGURATION);
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
}
