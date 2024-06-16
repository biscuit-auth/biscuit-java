package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import io.vavr.control.Either;
import net.i2p.crypto.eddsa.EdDSAEngine;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.builder.Block;
import org.biscuitsec.biscuit.token.format.SerializedBiscuit;

import java.security.*;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ThirdPartyRequest {
    PublicKey previousKey;
    List<PublicKey> publicKeys;

    ThirdPartyRequest(PublicKey previousKey, List<PublicKey> publicKeys) {
        this.previousKey = previousKey;
        this.publicKeys = publicKeys;
    }

    public Either<Error.FormatError, ThirdPartyBlock> createBlock(KeyPair keyPair, Block blockBuilder) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        SymbolTable symbols = new SymbolTable();
        for(PublicKey pk: this.publicKeys) {
            symbols.insert(pk);
        }

        org.biscuitsec.biscuit.token.Block block = blockBuilder.build(symbols);
        block.version = SerializedBiscuit.MAX_SCHEMA_VERSION;

        Either<Error.FormatError, byte[]> res = block.to_bytes();
        if(res.isLeft()) {
            return Either.left(res.getLeft());
        }

        byte[] serializedBlock = res.get();

        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(KeyPair.ed25519.getHashAlgorithm()));
        sgr.initSign(keyPair.private_key);
        sgr.update(serializedBlock);

        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algo_buf.putInt(Integer.valueOf(Schema.PublicKey.Algorithm.Ed25519.getNumber()));
        algo_buf.flip();
        sgr.update(algo_buf);
        sgr.update(previousKey.toBytes());
        byte[] signature = sgr.sign();

        PublicKey publicKey = keyPair.public_key();

        return Either.right(new ThirdPartyBlock(serializedBlock, signature, publicKey));
    }

    /*byte[] serialize() throws Error.FormatError.SerializationError {
        return new byte[0];
    }

    static public ThirdPartyRequest from_bytes(byte[] data) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
    }*/
}

