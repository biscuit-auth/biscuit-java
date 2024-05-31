package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema.PublicKey.Algorithm;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;

public class PrivateKeySigner implements Signer {

    private final Algorithm algorithm;
    private final PrivateKey privateKey;

    public PrivateKeySigner(Algorithm algorithm, PrivateKey privateKey) {
        this.algorithm = algorithm;
        this.privateKey = privateKey;
    }

    @Override
    public byte[] sign(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        var sgr = KeyPair.generateSignature(algorithm);
        sgr.initSign(privateKey);
        sgr.update(bytes);
        return sgr.sign();
    }
}
