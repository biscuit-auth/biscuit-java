package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public interface Signer {
    byte[] sign(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException;

    default byte[] signStandard(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        var algorithmBytes = getBufferForAlgorithm(algorithm);
        var payload = new byte[block.length + algorithmBytes.length + publicKey.length];
        System.arraycopy(block, 0, payload, 0, block.length);
        System.arraycopy(algorithmBytes, 0, payload, block.length, algorithmBytes.length);
        System.arraycopy(publicKey, 0, payload, block.length + algorithmBytes.length, publicKey.length);
        return sign(payload);
    }

    default byte[] signSealed(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey, byte[] seal) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        var algorithmBytes = getBufferForAlgorithm(algorithm);
        var payload = new byte[block.length + algorithmBytes.length + publicKey.length + seal.length];
        System.arraycopy(block, 0, payload, 0, block.length);
        System.arraycopy(algorithmBytes, 0, payload, block.length, algorithmBytes.length);
        System.arraycopy(publicKey, 0, payload, block.length + algorithmBytes.length, publicKey.length);
        System.arraycopy(seal, 0, payload, block.length + algorithmBytes.length + publicKey.length, seal.length);
        return sign(payload);
    }

    default byte[] signExternal(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey, byte[] external) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        var algorithmBytes = getBufferForAlgorithm(algorithm);
        var payload = new byte[block.length + external.length + algorithmBytes.length + publicKey.length];
        System.arraycopy(block, 0, payload, 0, block.length);
        System.arraycopy(external, 0, payload, block.length, external.length);
        System.arraycopy(algorithmBytes, 0, payload, block.length + external.length, algorithmBytes.length);
        System.arraycopy(publicKey, 0, payload, block.length + external.length + algorithmBytes.length, publicKey.length);
        return sign(payload);
    }

    private static byte[] getBufferForAlgorithm(Schema.PublicKey.Algorithm algorithm) {
        var algorithmBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algorithmBuffer.putInt(algorithm.getNumber());
        algorithmBuffer.flip();
        return algorithmBuffer.array();
    }
}
