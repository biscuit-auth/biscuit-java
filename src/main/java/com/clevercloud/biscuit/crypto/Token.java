package com.clevercloud.biscuit.crypto;

import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;
import net.i2p.crypto.eddsa.EdDSAEngine;

import java.security.*;
import java.util.ArrayList;

import static com.clevercloud.biscuit.crypto.KeyPair.ed25519;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

class Token {
    public final ArrayList<byte[]> blocks;
    public final ArrayList<PublicKey> keys;
    public final ArrayList<byte[]> signatures;
    public final KeyPair next;

    public Token(KeyPair rootKeyPair, byte[] message, KeyPair next) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
        sgr.initSign(rootKeyPair.private_key);
        sgr.update(message);
        sgr.update(next.public_key().toBytes());

        byte[] signature = sgr.sign();

        this.blocks = new ArrayList<>();
        this.blocks.add(message);
        this.keys = new ArrayList<>();
        this.keys.add(next.public_key());
        this.signatures = new ArrayList<>();
        this.signatures.add(signature);
        this.next = next;
    }

    public Token(final ArrayList<byte[]> blocks, final ArrayList<PublicKey> keys, final ArrayList<byte[]> signatures,
                 final KeyPair next) {
        this.signatures = signatures;
        this.blocks = blocks;
        this.keys = keys;
        this.next = next;
    }

    public Token append(KeyPair keyPair, byte[] message) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
        sgr.initSign(this.next.private_key);
        sgr.update(message);
        sgr.update(keyPair.public_key().toBytes());

        byte[] signature = sgr.sign();

        Token token = new Token(this.blocks, this.keys, this.signatures, keyPair);
        token.blocks.add(message);
        token.signatures.add(signature);
        token.keys.add(keyPair.public_key());

        return token;
    }

    // FIXME: rust version returns a Result<(), error::Signature>
    public Either<Error, Void> verify(PublicKey root) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey current_key = root;
        for(int i = 0; i < this.blocks.size(); i++) {
            byte[] block = this.blocks.get(i);
            PublicKey next_key  = this.keys.get(i);
            byte[] signature = this.signatures.get(i);

            System.out.println("verifying block "+i+" with current key "+current_key.toHex()+" block "+block+" next key "+next_key.toHex()+" signature "+signature);
            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));

            sgr.initVerify(current_key.key);
            sgr.update(block);
            sgr.update(next_key.toBytes());

            if (sgr.verify(signature)) {
                current_key = next_key;
            } else {
                System.out.println("signature not verified");
                return Left(new Error.FormatError.Signature.InvalidSignature());
            }
        }

        if(this.next.public_key == current_key.key) {
            return Right(null);
        } else {
            System.out.println("current key and next public key not equal:");
            System.out.println("current: "+current_key.toHex());
            System.out.println("next: "+this.next.public_key().toHex());
            return Left(new Error.FormatError.Signature.InvalidSignature());
        }
    }
}
