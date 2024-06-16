package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;
import biscuit.format.schema.Schema.PublicKey.Algorithm;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.builder.Utils;
import com.google.protobuf.ByteString;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import static org.biscuitsec.biscuit.crypto.KeyPair.ed25519;

public class PublicKey {

    public final EdDSAPublicKey key;
    public final Algorithm algorithm;

    public PublicKey(Algorithm algorithm, EdDSAPublicKey public_key) {
        this.key = public_key;
        this.algorithm = algorithm;
    }

    public PublicKey(Algorithm algorithm, byte[] data) {
        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(data, ed25519);
        this.key = new EdDSAPublicKey(pubKeySpec);
        this.algorithm = algorithm;
    }

    public byte[] toBytes() {
        return this.key.getAbyte();
    }

    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    public PublicKey(Algorithm algorithm, String hex) {
        byte[] data = Utils.hexStringToByteArray(hex);
        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(data, ed25519);
        this.key = new EdDSAPublicKey(pubKeySpec);
        this.algorithm = algorithm;
    }

    public Schema.PublicKey serialize() {
        Schema.PublicKey.Builder publicKey = Schema.PublicKey.newBuilder();
        publicKey.setKey(ByteString.copyFrom(this.toBytes()));
        publicKey.setAlgorithm(this.algorithm);
        return publicKey.build();
    }

    static public PublicKey deserialize(Schema.PublicKey pk) throws Error.FormatError.DeserializationError {
        if(!pk.hasAlgorithm() || !pk.hasKey() || pk.getAlgorithm() != Algorithm.Ed25519) {
            throw new Error.FormatError.DeserializationError("Invalid public key");
        }

        return new PublicKey(pk.getAlgorithm(), pk.getKey().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PublicKey publicKey = (PublicKey) o;

        return key.equals(publicKey.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "ed25519/" + toHex().toLowerCase();
    }
}
