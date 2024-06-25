package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.control.Either;
import io.vavr.control.Option;
import net.i2p.crypto.eddsa.EdDSAEngine;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.builder.Block;
import org.biscuitsec.biscuit.token.format.SerializedBiscuit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class ThirdPartyBlockRequest {
    PublicKey previousKey;
    List<PublicKey> publicKeys;

    ThirdPartyBlockRequest(PublicKey previousKey, List<PublicKey> publicKeys) {
        this.previousKey = previousKey;
        this.publicKeys = publicKeys;
    }

    public Either<Error.FormatError, ThirdPartyBlockContents> createBlock(KeyPair keyPair, Block blockBuilder) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        SymbolTable symbols = new SymbolTable();
        for(PublicKey pk: this.publicKeys) {
            symbols.insert(pk);
        }

        org.biscuitsec.biscuit.token.Block block = blockBuilder.build(symbols, Option.some(keyPair.public_key()));

        Either<Error.FormatError, byte[]> res = block.to_bytes();
        if(res.isLeft()) {
            return Either.left(res.getLeft());
        }

        byte[] serializedBlock = res.get();

        Signature sgr = KeyPair.generateSignature(keyPair.public_key().algorithm);
        sgr.initSign(keyPair.private_key());
        sgr.update(serializedBlock);

        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algo_buf.putInt(Integer.valueOf(Schema.PublicKey.Algorithm.Ed25519.getNumber()));
        algo_buf.flip();
        sgr.update(algo_buf);
        sgr.update(previousKey.toBytes());
        byte[] signature = sgr.sign();

        PublicKey publicKey = keyPair.public_key();

        return Either.right(new ThirdPartyBlockContents(serializedBlock, signature, publicKey));
    }

    public Schema.ThirdPartyBlockRequest serialize() throws Error.FormatError.SerializationError {
        Schema.ThirdPartyBlockRequest.Builder b = Schema.ThirdPartyBlockRequest.newBuilder();
        b.setPreviousKey(this.previousKey.serialize());

        for(PublicKey pk: this.publicKeys) {
            b.addPublicKeys(pk.serialize());
        }

        return b.build();
    }

    static public ThirdPartyBlockRequest deserialize(Schema.ThirdPartyBlockRequest b) throws Error.FormatError.DeserializationError {
        PublicKey previousKey = PublicKey.deserialize(b.getPreviousKey());
        List<PublicKey> publicKeys = new ArrayList<>();
        for(Schema.PublicKey pk: b.getPublicKeysList()) {
            publicKeys.add(PublicKey.deserialize(pk));
        }
        return new ThirdPartyBlockRequest(previousKey, publicKeys);
    }

    static public ThirdPartyBlockRequest fromBytes(byte[] slice) throws InvalidProtocolBufferException, Error.FormatError.DeserializationError {
        return ThirdPartyBlockRequest.deserialize(Schema.ThirdPartyBlockRequest.parseFrom(slice));
    }

    public byte[] toBytes() throws IOException, Error.FormatError.SerializationError {
        Schema.ThirdPartyBlockRequest b = this.serialize();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.writeTo(stream);
        return stream.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThirdPartyBlockRequest that = (ThirdPartyBlockRequest) o;

        if (!Objects.equals(previousKey, that.previousKey)) return false;
        return Objects.equals(publicKeys, that.publicKeys);
    }

    @Override
    public int hashCode() {
        int result = previousKey != null ? previousKey.hashCode() : 0;
        result = 31 * result + (publicKeys != null ? publicKeys.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ThirdPartyBlockRequest{" +
                "previousKey=" + previousKey +
                ", publicKeys=" + publicKeys +
                '}';
    }
}

