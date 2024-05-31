package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import net.i2p.crypto.eddsa.Utils;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.crypto.Signer;
import org.biscuitsec.biscuit.error.Error;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class KmsSignerExampleTest {

    static class KmsSigner implements Signer {

        private final KmsClient kmsClient = KmsClient.create();

        @Override
        public byte[] sign(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey)
                throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
//            var algorithmBuffer = getAlgorithmBuffer(algorithm);
//            var size = block.length + algorithmBuffer.capacity() + publicKey.length;
//            var payload = new byte[size];
//            System.arraycopy(block, 0, payload, 0, block.length);
//            System.arraycopy(algorithmBuffer.array(), 0, payload, block.length, algorithmBuffer.capacity());
//            System.arraycopy(publicKey, 0, payload, block.length + algorithmBuffer.capacity(), publicKey.length);
//            var response = kmsClient.sign(builder ->
//                    builder.keyId("yourKeyId")
//                        .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256)
//                        .message(SdkBytes.fromByteArray(payload)));
//            return response.signature().asByteArray();
            return new byte[0];
        }

        @Override
        public byte[] sign(byte[] block, Schema.PublicKey.Algorithm algorithm, byte[] publicKey, byte[] seal) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
            return new byte[0];
        }
    }

    @BeforeAll
    public static void setup() {
        System.setProperty("aws.region", "ap-southeast-2");
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty("aws.region");
    }

    @Test
    public void createAndSerializeToken() throws Error {
        var signer = new KmsSigner();
        var publicKey = new PublicKey(Schema.PublicKey.Algorithm.SECP256R1, publicKeyBytes);
        var keyPair = KeyPair.generate(publicKey, signer);
        var biscuit = Biscuit.builder(keyPair)
                .add_authority_fact("user(\"1234\")")
                .add_authority_check("check if operation(\"read\")")
                .build();
    }

    @Test
    public void test() throws Error {
        var root = KeyPair.generate(Schema.PublicKey.Algorithm.SECP256R1);
        var biscuit =  Biscuit.builder(root)
                .add_authority_fact("user(\"1234\")")
                .add_authority_check("check if operation(\"read\")")
                .build();

        System.out.println("Root private key: " + root.toHex());
        System.out.println("Root public key: " + root.public_key().toHex());
        var serializedBytes = biscuit.serialize();
        try (var file = new FileOutputStream("token.bc")) {
            file.write(serializedBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
