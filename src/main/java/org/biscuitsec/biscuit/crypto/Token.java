package org.biscuitsec.biscuit.crypto;

import io.vavr.control.Either;
import net.i2p.crypto.eddsa.EdDSAEngine;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.Error.FormatError.Signature.InvalidSignature;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.ArrayList;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

final class Token {
    public final ArrayList<byte[]> blocks;
    public final ArrayList<PublicKey> keys;
    public final ArrayList<byte[]> signatures;
    public final KeyPair next;

    public Token(KeyPair rootKeyPair, byte[] message, KeyPair next)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(KeyPair.ed25519.getHashAlgorithm()));
        ByteBuffer algoBuf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
        algoBuf.putInt(next.publicKey().algorithm.getNumber());
        algoBuf.flip();
        sgr.initSign(rootKeyPair.privateKey);
        sgr.update(message);
        sgr.update(algoBuf);
        sgr.update(next.publicKey().toBytes());

        byte[] signature = sgr.sign();

        this.blocks = new ArrayList<>();
        this.blocks.add(message);
        this.keys = new ArrayList<>();
        this.keys.add(next.publicKey());
        this.signatures = new ArrayList<>();
        this.signatures.add(signature);
        this.next = next;
    }

    public Token(final ArrayList<byte[]> blocks,
                 final ArrayList<PublicKey> keys,
                 final ArrayList<byte[]> signatures,
                 final KeyPair next) {
        this.signatures = signatures;
        this.blocks = blocks;
        this.keys = keys;
        this.next = next;
    }

    public Token append(KeyPair keyPair, byte[] message)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(KeyPair.ed25519.getHashAlgorithm()));
        sgr.initSign(this.next.privateKey);
        ByteBuffer algoBuf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
        algoBuf.putInt(next.publicKey().algorithm.getNumber());
        algoBuf.flip();
        sgr.update(message);
        sgr.update(algoBuf);
        sgr.update(keyPair.publicKey().toBytes());

        byte[] signature = sgr.sign();

        Token token = new Token(this.blocks, this.keys, this.signatures, keyPair);
        token.blocks.add(message);
        token.signatures.add(signature);
        token.keys.add(keyPair.publicKey());

        return token;
    }

    // FIXME: rust version returns a Result<(), error::Signature>
    public Either<Error, Void> verify(PublicKey root)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey currentKey = root;
        for (int i = 0; i < this.blocks.size(); i++) {
            byte[] block = this.blocks.get(i);
            PublicKey nextKey = this.keys.get(i);
            byte[] signature = this.signatures.get(i);

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(KeyPair.ed25519.getHashAlgorithm()));
            ByteBuffer algoBuf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            algoBuf.putInt(next.publicKey().algorithm.getNumber());
            algoBuf.flip();
            sgr.initVerify(currentKey.key);
            sgr.update(block);
            sgr.update(algoBuf);
            sgr.update(nextKey.toBytes());

            if (sgr.verify(signature)) {
                currentKey = nextKey;
            } else {
                return Left(new InvalidSignature("signature error: Verification equation was not satisfied"));
            }
        }

        if (this.next.publicKey == currentKey.key) {
            return Right(null);
        } else {
            return Left(new InvalidSignature("signature error: Verification equation was not satisfied"));
        }
    }
}
