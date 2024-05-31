package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema.PublicKey.Algorithm;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;

public class PrivateKeySigner implements Signer {

    private final PrivateKey privateKey;

    public PrivateKeySigner(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public byte[] sign(byte[] block, Algorithm algorithm, byte[] publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        var algorithmBuffer = Signer.getAlgorithmBuffer(algorithm);
        var signature = KeyPair.generateSignature(algorithm);
        signature.initSign(privateKey);
        signature.update(block);
        signature.update(algorithmBuffer);
        signature.update(publicKey);
        return signature.sign();
    }

    @Override
    public byte[] sign(byte[] block, Algorithm algorithm, byte[] publicKey, byte[] seal)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        var algorithmBuffer = Signer.getAlgorithmBuffer(algorithm);
        var signature = KeyPair.generateSignature(algorithm);
        signature.initSign(privateKey);
        signature.update(block);
        signature.update(algorithmBuffer);
        signature.update(publicKey);
        signature.update(seal);
        return signature.sign();
    }
}
