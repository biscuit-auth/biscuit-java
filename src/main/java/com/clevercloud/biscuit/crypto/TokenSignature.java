package com.clevercloud.biscuit.crypto;

import biscuit.format.schema.Schema;
import cafe.cryptography.curve25519.*;
import com.clevercloud.biscuit.error.Error;
import com.google.protobuf.ByteString;
import io.vavr.control.Either;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Signature aggregation
 */
public class TokenSignature {
    final ArrayList<RistrettoElement> parameters;
    final Scalar z;

    /**
     * Generates a new valid signature for a message and a private key
     * @param rng
     * @param keypair
     * @param message
     */
    public TokenSignature(final SecureRandom rng, KeyPair keypair, byte[] message) {
        byte[] b = new byte[64];
        rng.nextBytes(b);
        Scalar r = Scalar.fromBytesModOrderWide(b);

        RistrettoElement A = Constants.RISTRETTO_GENERATOR.multiply(r);
        ArrayList<RistrettoElement> l = new ArrayList<>();
        l.add(A);
        Scalar d = hash_points(l);
        Scalar e = hash_message(keypair.public_key, message);

        this.z = r.multiply(d).subtract(e.multiply(keypair.private_key));
        this.parameters = l;
    }

    TokenSignature(final ArrayList<RistrettoElement> parameters, final Scalar z) {
        this.parameters = parameters;
        this.z = z;
    }

    /**
     * Generates a new valid signature from an existing one, a private key and a message
     * @param rng
     * @param keypair
     * @param message
     * @return
     */
    public TokenSignature sign(final SecureRandom rng, KeyPair keypair, byte[] message) {
        byte[] b = new byte[64];
        rng.nextBytes(b);
        Scalar r = Scalar.fromBytesModOrderWide(b);

        RistrettoElement A = Constants.RISTRETTO_GENERATOR.multiply(r);
        ArrayList<RistrettoElement> l = new ArrayList<>();
        l.add(A);
        Scalar d = hash_points(l);
        Scalar e = hash_message(keypair.public_key, message);

        Scalar localZ = r.multiply(d).subtract(e.multiply(keypair.private_key));

        TokenSignature sig = new TokenSignature(this.parameters, this.z.add(localZ));
        sig.parameters.add(A);

        return sig;
    }

    /**
     * checks that a signature is valid for a set of public keys and messages
     * @param public_keys
     * @param messages
     * @return
     */
    public Either<Error, Void> verify(List<RistrettoElement> public_keys, List<byte[]> messages) {
        if (!(public_keys.size() == messages.size() && public_keys.size() == this.parameters.size())) {
            System.out.println(("lists are not the same size"));
            return Left(new Error.FormatError.Signature.InvalidFormat());
        }

        RistrettoElement zP = Constants.RISTRETTO_GENERATOR.multiply(this.z);
        
        RistrettoElement eiXi = RistrettoElement.IDENTITY;
        for(int i = 0; i < public_keys.size(); i++) {
            Scalar e = hash_message(public_keys.get(i), messages.get(i));
            
            eiXi = eiXi.add(public_keys.get(i).multiply(e));
        }

        RistrettoElement diAi = RistrettoElement.IDENTITY;
        for (RistrettoElement A: parameters) {
            ArrayList<RistrettoElement> l = new ArrayList<>();
            l.add(A);
            Scalar d = hash_points(l);

            diAi = diAi.add(A.multiply(d));
        }

        RistrettoElement res = zP.add(eiXi).subtract(diAi);

        if (res.ctEquals(RistrettoElement.IDENTITY) == 1) {
            return Right(null);
        } else {
            return Left(new Error.FormatError.Signature.InvalidSignature());
        }
    }

    /**
     * Serializes a signature to its Protobuf representation
     * @return
     */
    public Schema.Signature serialize() {
        Schema.Signature.Builder sig = Schema.Signature.newBuilder()
                .setZ(ByteString.copyFrom(this.z.toByteArray()));
        for (int i = 0; i < this.parameters.size(); i++) {
            sig.addParameters(ByteString.copyFrom(this.parameters.get(i).compress().toByteArray()));
        }

        return sig.build();
    }

    /**
     * Deserializes a signature from its Protobuf representation
     * @param sig
     * @return
     */
    public static  Either<Error, TokenSignature> deserialize(Schema.Signature sig) {
        try {
            ArrayList<RistrettoElement> parameters = new ArrayList<>();
            for (ByteString parameter : sig.getParametersList()) {
                parameters.add((new CompressedRistretto(parameter.toByteArray())).decompress());
            }

            Scalar z = Scalar.fromBytesModOrder(sig.getZ().toByteArray());

            return Right(new TokenSignature(parameters, z));
        } catch (InvalidEncodingException e) {
            return Left(new Error.FormatError.Signature.InvalidFormat());
        } catch(IllegalArgumentException e) {
            return Left(new Error.FormatError.DeserializationError(e.toString()));
        }
    }

    static Scalar hash_points(List<RistrettoElement> points) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();

            for (RistrettoElement point : points) {
                digest.update(point.compress().toByteArray());
            }

            return Scalar.fromBytesModOrderWide(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static Scalar hash_message(RistrettoElement point, byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();

            digest.update(point.compress().toByteArray());
            digest.update(data);

            return Scalar.fromBytesModOrderWide(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String hex(byte[] byteArray) {
        StringBuilder result = new StringBuilder();
        for (byte bb : byteArray) {
            result.append(String.format("%02X", bb));
        }
        return result.toString();
    }

    public static byte[] fromHex(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
