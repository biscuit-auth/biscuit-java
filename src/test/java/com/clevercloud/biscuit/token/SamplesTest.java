package com.clevercloud.biscuit.token;

import cafe.cryptography.curve25519.CompressedRistretto;
import cafe.cryptography.curve25519.InvalidEncodingException;
import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Fact;
import io.vavr.control.Either;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static com.clevercloud.biscuit.crypto.TokenSignature.fromHex;
import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static com.clevercloud.biscuit.token.builder.Utils.*;

public class SamplesTest extends TestCase {

    public SamplesTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SamplesTest.class);
    }

    static byte[] rootData = fromHex("529e780f28d9181c968b0eab9977ed8494a27a4544c3adc1910f41bb3dc36958");

    public void test1_Basic() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test1_basic.bc");

        System.out.println("a");
        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Verifier v1 = token.verify(root).get();
        v1.add_resource("file1");
        v1.add_operation("read");
        Either<Error, Void> res = v1.verify();
        if(res.isLeft()) {
            System.out.println("error: "+res.getLeft());
        }
        Assert.assertTrue(res.isRight());

        byte[] serialized = token.serialize().get();
        Assert.assertEquals(data.length, serialized.length);
        System.out.println(hex(data));
        System.out.println(hex(serialized));

        for(int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], serialized[i]);
        }
    }

    public void test2_DifferentRootKey() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test2_different_root_key.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        Error e = token.check_root_key(root).getLeft();
        System.out.println("got error: "+ e);
        Assert.assertEquals(new Error().new FormatError().new UnknownPublicKey(), e);
    }

    public void test3_InvalidSignatureFormat() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test3_invalid_signature_format.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Error e = Biscuit.from_bytes(data).getLeft();
        System.out.println("got error: "+ e);
        Assert.assertEquals(new Error().new FormatError().new DeserializationError("java.lang.IllegalArgumentException: Input must by 32 bytes"), e);
    }

    public void test4_random_block() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test4_random_block.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Error e = Biscuit.from_bytes(data).getLeft();
        System.out.println("got error: "+ e);
        Assert.assertEquals(new Error().new FormatError().new Signature().new InvalidSignature(), e);
    }

    public void test5_InvalidSignature() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test5_invalid_signature.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Error e = Biscuit.from_bytes(data).getLeft();
        System.out.println("got error: "+ e);
        Assert.assertEquals(new Error().new FormatError().new Signature().new InvalidSignature(), e);
    }

    public void test6_reordered_blocks() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test6_reordered_blocks.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();

        Either<Error, Verifier> res = token.verify(root);
        System.out.println(token.print());
        System.out.println(res);
        if(res.isLeft()) {
            System.out.println("error: "+res.getLeft());
        }
        Assert.assertEquals(new Error().new InvalidBlockIndex(3, 2), res.getLeft());

    }

    public void test7_invalid_block_fact_authority() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test7_invalid_block_fact_authority.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Either<Error, Verifier> res = token.verify(root);
        if(res.isLeft()) {
            System.out.println("error: "+res.getLeft());
        }
        Assert.assertEquals(new Error().new FailedLogic(new LogicError().new InvalidBlockFact(0, "right(#authority, \"file1\", #write)")), res.getLeft());
    }

    public void test8_invalid_block_fact_ambient() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test8_invalid_block_fact_ambient.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Either<Error, Verifier> res = token.verify(root);
        if(res.isLeft()) {
            System.out.println("error: "+res.getLeft());
        }
        Assert.assertEquals(new Error().new FailedLogic(new LogicError().new InvalidBlockFact(0, "right(#ambient, \"file1\", #write)")), res.getLeft());
    }

    public void test9_ExpiredToken() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test9_expired_token.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Verifier v1 = token.verify(root).get();
        v1.add_resource("file1");
        v1.add_operation("read");
        v1.set_time();
        Error e = v1.verify().getLeft();
        Assert.assertEquals(
                new Error().new FailedLogic(new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(1, 1, "expiration(0?) <- time(#ambient, 0?) | 0? <= 1545264000")
                ))),
                e);
    }

    public void test10_AuthorityRules() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test10_authority_rules.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Verifier v1 = token.verify(root).get();
        v1.add_resource("file1");
        v1.add_operation("read");
        v1.add_fact(fact("owner", Arrays.asList(s("ambient"), s("alice"), string("file1"))));
        Either<Error, Void> res = v1.verify();
        System.out.println(res);
        Assert.assertTrue(res.isRight());
    }

    public void test11_VerifierAuthorityCaveats() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test11_verifier_authority_caveats.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Verifier v1 = token.verify(root).get();
        v1.add_resource("file2");
        v1.add_operation("read");
        v1.add_caveat(rule(
                "caveat1",
                Arrays.asList(var(0)),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var(0))),
                        pred("operation", Arrays.asList(s("ambient"), var(1))),
                        pred("right", Arrays.asList(s("authority"), var(0), var(1)))
                )
        ));
        Either<Error, Void> res = v1.verify();
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error().new FailedLogic(new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedVerifier(0, "caveat1(0?) <- resource(#ambient, 0?) && operation(#ambient, 1?) && right(#authority, 0?, 1?) | ")
                ))),
                e);
    }

    public void test12_VerifierAuthorityCaveats() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test12_authority_caveats.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Verifier v1 = token.verify(root).get();
        v1.add_resource("file1");
        v1.add_operation("read");
        Assert.assertTrue(v1.verify().isRight());

        Verifier v2 = token.verify(root).get();
        v2.add_resource("file2");
        v2.add_operation("read");

        Either<Error, Void> res = v2.verify();
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error().new FailedLogic(new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(0, 0, "caveat1(\"file1\") <- resource(#ambient, \"file1\") | ")
                ))),
                e);
    }

    public void test13_BlockRules() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test13_block_rules.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Verifier v1 = token.verify(root).get();
        v1.add_resource("file1");
        v1.set_time();
        Assert.assertTrue(v1.verify().isRight());

        Verifier v2 = token.verify(root).get();
        v2.add_resource("file2");
        v2.set_time();

        Either<Error, Void> res = v2.verify();
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error().new FailedLogic(new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(1, 0, "caveat1(0?) <- valid_date(0?) && resource(#ambient, 0?) | ")
                ))),
                e);
    }

    public void test14_RegexConstraint() throws IOException, InvalidEncodingException {
        PublicKey root = new PublicKey((new CompressedRistretto(rootData)).decompress());

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test14_regex_constraint.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data).get();
        System.out.println(token.print());

        Verifier v1 = token.verify(root).get();
        v1.add_resource("file1");
        v1.set_time();

        Either<Error, Void> res = v1.verify();
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error().new FailedLogic(new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(0, 0, "resource_match(0?) <- resource(#ambient, 0?) | 0? matches /file[0-9]+.txt/")
                ))),
                e);

        Verifier v2 = token.verify(root).get();
        v2.add_resource("file123.txt");
        v2.set_time();
        Assert.assertTrue(v2.verify().isRight());

    }
}
