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


public class TokenSignature {
    final public ArrayList<RistrettoElement> parameters;
    final public Scalar z;

    public TokenSignature(final SecureRandom rng, KeyPair keypair, byte[] message) {
        byte[] b = new byte[64];
        rng.nextBytes(b);
        Scalar r = Scalar.fromBytesModOrderWide(b);

        RistrettoElement A = Constants.RISTRETTO_GENERATOR.multiply(r);
        ArrayList<RistrettoElement> l = new ArrayList<>();
        l.add(A);
        Scalar d = hash_points(l);
        Scalar e = hash_message(keypair.public_key, message);

        Scalar z = r.multiply(d).subtract(e.multiply(keypair.private_key));

        this.parameters = l;
        this.z = z;
    }

    public TokenSignature(final ArrayList<RistrettoElement> parameters, final Scalar z) {
        this.parameters = parameters;
        this.z = z;
    }


    public TokenSignature sign(final SecureRandom rng, List<RistrettoElement> public_keys, List<byte[]> messages,
                               KeyPair keypair, byte[] message) {
        byte[] b = new byte[64];
        rng.nextBytes(b);
        Scalar r = Scalar.fromBytesModOrderWide(b);

        RistrettoElement A = Constants.RISTRETTO_GENERATOR.multiply(r);
        ArrayList<RistrettoElement> l = new ArrayList<>();
        l.add(A);
        Scalar d = hash_points(l);
        Scalar e = hash_message(keypair.public_key, message);

        Scalar z = r.multiply(d).subtract(e.multiply(keypair.private_key));

        TokenSignature sig = new TokenSignature(this.parameters, this.z.add(z));
        sig.parameters.add(A);

        return sig;
    }

    public Either<Error, Void> verify(List<RistrettoElement> public_keys, List<byte[]> messages) {
        if (!(public_keys.size() == messages.size() && public_keys.size() == this.parameters.size())) {
            System.out.println(("lists are not the same size"));
            return Left(new Error().new FormatError().new Signature().new InvalidFormat());
        }


        System.out.println("z, zp");
        RistrettoElement zP = Constants.RISTRETTO_GENERATOR.multiply(this.z);
        System.out.println(hex(z.toByteArray()));
        System.out.println(hex(zP.compress().toByteArray()));


        System.out.println("eiXi");
        RistrettoElement eiXi = RistrettoElement.IDENTITY;
        for(int i = 0; i < public_keys.size(); i++) {
            Scalar e = hash_message(public_keys.get(i), messages.get(i));
            System.out.println(hex(e.toByteArray()));
            System.out.println(hex((public_keys.get(i).multiply(e)).compress().toByteArray()));


            eiXi = eiXi.add(public_keys.get(i).multiply(e));
            System.out.println(hex(eiXi.compress().toByteArray()));

        }

        System.out.println("diAi");
        RistrettoElement diAi = RistrettoElement.IDENTITY;
        for (RistrettoElement A: parameters) {
            ArrayList<RistrettoElement> l = new ArrayList<>();
            l.add(A);
            Scalar d = hash_points(l);

            diAi = diAi.add(A.multiply(d));
        }

        System.out.println(hex(eiXi.compress().toByteArray()));
        System.out.println(hex(diAi.compress().toByteArray()));



        RistrettoElement res = zP.add(eiXi).subtract(diAi);

        System.out.println(hex(RistrettoElement.IDENTITY.compress().toByteArray()));
        System.out.println(hex(res.compress().toByteArray()));

        if (res.ctEquals(RistrettoElement.IDENTITY) == 1) {
            return Right(null);
        } else {
            return Left(new Error().new FormatError().new Signature().new InvalidSignature());
        }
    }

    public Schema.Signature serialize() {
        Schema.Signature.Builder sig = Schema.Signature.newBuilder()
                .setZ(ByteString.copyFrom(this.z.toByteArray()));

        System.out.println(this.parameters.size());
        for (int i = 0; i < this.parameters.size(); i++) {
            System.out.println(i);
            sig.addParameters(ByteString.copyFrom(this.parameters.get(i).compress().toByteArray()));
        }

        return sig.build();
    }

    static public TokenSignature deserialize(Schema.Signature sig) throws InvalidEncodingException {
        ArrayList<RistrettoElement> parameters = new ArrayList<>();
        for (ByteString parameter: sig.getParametersList()) {
            parameters.add((new CompressedRistretto(parameter.toByteArray())).decompress());
        }

        System.out.println(hex(sig.getZ().toByteArray()));
        System.out.println(sig.getZ().toByteArray().length);

        Scalar z = Scalar.fromBytesModOrder(sig.getZ().toByteArray());

        return new TokenSignature(parameters, z);
    }

    static public Scalar hash_points(List<RistrettoElement> points) {
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

    static public Scalar hash_message(RistrettoElement point, byte[] data) {
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

    static public String hex(byte[] byteArray) {
        StringBuilder result = new StringBuilder();
        for (byte bb : byteArray) {
            result.append(String.format("%02X", bb));
        }
        return result.toString();
    }
}
