package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema.PublicKey.Algorithm;
import net.i2p.crypto.eddsa.Utils;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.crypto.Signer;
import org.biscuitsec.biscuit.error.Error;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.function.Function;

import static java.lang.ProcessBuilder.Redirect;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;


public class KmsSignerExampleTest {

    private static final String AWS_PROFILE = null;
    private static final String AWS_REGION = null;
    private static final String KMS_KEY_ARN = null;

    @BeforeAll
    public static void setup() {
        System.setProperty("aws.region", AWS_REGION);
        System.setProperty("aws.profile", AWS_PROFILE);
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty("aws.region");
        System.clearProperty("aws.profile");
    }

//    @Test
    public void createWithRemoteSigner() throws Error {
        Function<String, byte[]> getPublicKeyBytes = (keyId) -> {
            try (var kmsClient = KmsClient.create()) {
                var publicKeyResponse = kmsClient.getPublicKey(b -> b.keyId(keyId).build());
                return Convert.DEREncodedX509PkToSEC1CompressedEncodedPk(publicKeyResponse.publicKey().asByteArray());
            }
        };

        byte[] publicKeyBytes;
        try {
            publicKeyBytes = getPublicKeyBytes.apply(KMS_KEY_ARN);
        } catch (SdkClientException e) {
            sso();
            publicKeyBytes = getPublicKeyBytes.apply(KMS_KEY_ARN);
        }

        var publicKey = new PublicKey(Algorithm.SECP256R1, publicKeyBytes);
        var keyPair = KeyPair.generate(publicKey, new Signer() {

            @Override
            public byte[] sign(byte[] bytes) {
                try (var kmsClient = KmsClient.create()) {
                    var response = kmsClient.sign(builder -> builder
                            .keyId(KMS_KEY_ARN)
                            .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256)
                            .message(SdkBytes.fromByteArray(bytes))
                    );
                    var signature = response.signature().asByteArray();
                    var hex = Utils.bytesToHex(signature);
                    System.out.println("Signature: " + hex);

                    var sgr = KeyPair.generateSignature(Algorithm.SECP256R1);
                    sgr.initVerify(publicKey.key);
                    sgr.update(bytes);
                    var verified = sgr.verify(signature);
                    System.out.println("Verified: " + verified);

                    if (!verified) {
                        fail("Signature verification failed");
                    }

                    return signature;
                } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        var biscuit = Biscuit.builder(keyPair)
                .add_authority_fact("user(\"1234\")")
                .add_authority_check("check if operation(\"read\")")
                .build();
        assertDoesNotThrow(biscuit::serialize);
    }

    private static class Convert {

        // converts DER-encoded X.509 public key to SEC1 compressed encoded format
        static byte[] DEREncodedX509PkToSEC1CompressedEncodedPk(byte[] publicKeyBytes) {
            try (ASN1InputStream asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(publicKeyBytes))) {
                // Parse the ASN.1 encoded public key bytes
                var asn1Primitive = asn1InputStream.readObject();
                var subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(asn1Primitive);

                // Extract the public key data
                var publicKeyDataBitString = subjectPublicKeyInfo.getPublicKeyData();
                byte[] publicKeyData = publicKeyDataBitString.getBytes();

                // Parse the public key data to get the elliptic curve point
                var ecParameters = ECNamedCurveTable.getByName("secp256r1");
                var ecPoint = ecParameters.getCurve().decodePoint(publicKeyData);
                return ecPoint.getEncoded(true);

            } catch (IOException e) {
                throw new RuntimeException("Error converting DER-encoded X.509 to SEC1 compressed format", e);
            }
        }
    }

    private void sso() {
        try {
            var code = new ProcessBuilder()
                    .command("aws", "sso", "login", "--profile", AWS_PROFILE)
                    .redirectOutput(Redirect.INHERIT)
                    .redirectError(Redirect.INHERIT)
                    .start()
                    .waitFor();
            if (code != 0) {
                throw new RuntimeException("SSO login failed");
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
