package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;

public interface Signer {
    byte[] sign(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException;

    // format: block, algo, publicKey
    default byte[] signStandard(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        var algorithmBytes = getBufferForAlgorithm(algorithm);
        var payload = concatenateArrays(block, algorithmBytes, publicKey);
        return sign(payload);
    }

    // format: block, algo, publicKey, seal
    default byte[] signSealed(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey, byte[] seal) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        var algorithmBytes = getBufferForAlgorithm(algorithm);
        var payload = concatenateArrays(block, algorithmBytes, publicKey, seal);
        return sign(payload);
    }

    // format: block, external, algo, publicKey
    default byte[] signExternal(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey, byte[] external) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        var algorithmBytes = getBufferForAlgorithm(algorithm);
        var payload = concatenateArrays(block, external, algorithmBytes, publicKey);
        return sign(payload);
    }

    private static byte[] concatenateArrays(byte[]... arrays) {
        int totalLength = Arrays.stream(arrays).mapToInt(arr -> arr.length).sum();
        byte[] result = new byte[totalLength];
        int currentPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentPos, array.length);
            currentPos += array.length;
        }
        return result;
    }

    private static byte[] getBufferForAlgorithm(Schema.PublicKey.Algorithm algorithm) {
        var algorithmBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algorithmBuffer.putInt(algorithm.getNumber());
        algorithmBuffer.flip();
        return algorithmBuffer.array();
    }
}
