package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.RunLimits;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Check;
import com.clevercloud.biscuit.token.builder.Rule;
import io.vavr.control.Either;
import org.junit.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

import static com.clevercloud.biscuit.crypto.TokenSignature.fromHex;
import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static com.clevercloud.biscuit.token.builder.Utils.*;
import static io.vavr.API.Right;


public class SamplesV2Test extends TestCase {

    public SamplesV2Test(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SamplesV2Test.class);
    }

    static byte[] rootData = fromHex("acdd6d5b53bfee478bf689f8e012fe7988bf755e3d7c5152947abc149bc20189");


    public void test1_Basic() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test1_basic.bc");

        System.out.println("a");
        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Either<Error, Biscuit> deser_res = Biscuit.from_bytes(data, root);
        Biscuit token = deser_res.get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_resource("file1");
        v1.add_operation("read");
        v1.allow();
        Either<Error, Long> res = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        if (res.isLeft()) {
            System.out.println("error: " + res.getLeft());
        }
        Assert.assertTrue(res.isRight());

        byte[] serialized = token.serialize().get();
        Assert.assertEquals(data.length, serialized.length);
        System.out.println(hex(data));
        System.out.println(hex(serialized));

        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], serialized[i]);
        }
    }

    public void test2_DifferentRootKey() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test2_different_root_key.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Either<Error, Biscuit> token = Biscuit.from_bytes(data, root);
        Error e = token.getLeft();
        System.out.println("got error: " + e);
        Assert.assertEquals(new Error.FormatError.Signature.InvalidSignature(), e);
    }

    public void test3_InvalidSignatureFormat() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test3_invalid_signature_format.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        try {
            Error e = Biscuit.from_bytes(data, root).getLeft();
            fail();
        } catch(SignatureException e) {
            System.out.println("got error: " + e);
        }
    }

    public void test4_random_block() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test4_random_block.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Error e = Biscuit.from_bytes(data, root).getLeft();
        System.out.println("got error: " + e);
        Assert.assertEquals(new Error.FormatError.Signature.InvalidSignature(), e);
    }

    public void test5_InvalidSignature() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test5_invalid_signature.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Error e = Biscuit.from_bytes(data, root).getLeft();
        System.out.println("got error: " + e);
        Assert.assertEquals(new Error.FormatError.Signature.InvalidSignature(), e);
    }

    public void test6_reordered_blocks() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test6_reordered_blocks.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Either<Error, Biscuit> deser =  Biscuit.from_bytes(data, root);
        Error e = deser.getLeft();
        assertEquals(e, new Error.FormatError.Signature.InvalidSignature());

    }

    public void test7_invalid_block_fact_authority() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test7_invalid_block_fact_authority.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Either<Error, Verifier> res = token.verifier();
        if (res.isLeft()) {
            System.out.println("error: " + res.getLeft());
        }
        Assert.assertEquals(new Error.FailedLogic(new LogicError.InvalidBlockFact(0, "right(#authority, \"file1\", #write)")), res.getLeft());
    }

    public void test8_invalid_block_fact_ambient() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test8_invalid_block_fact_ambient.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Either<Error, Verifier> res = token.verifier();
        if (res.isLeft()) {
            System.out.println("error: " + res.getLeft());
        }
        Assert.assertEquals(new Error.FailedLogic(new LogicError.InvalidBlockFact(0, "right(#ambient, \"file1\", #write)")), res.getLeft());
    }

    public void test9_ExpiredToken() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test9_expired_token.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_resource("file1");
        v1.add_operation("read");
        v1.set_time();
        System.out.println(v1.print_world());

        Error e = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500))).getLeft();
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(1, 1, "check if time(#ambient, $date), $date <= 2018-12-20T00:00:00Z")
                ))),
                e);
    }

    public void test10_AuthorityRules() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test10_authority_rules.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_resource("file1");
        v1.add_operation("read");
        v1.add_fact(fact("owner", Arrays.asList(s("ambient"), s("alice"), string("file1"))));
        v1.allow();
        Either<Error, Long> res = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println(res);
        Assert.assertTrue(res.isRight());
    }

    public void test11_VerifierAuthorityCaveats() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test11_verifier_authority_caveats.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_resource("file2");
        v1.add_operation("read");
        v1.add_check(check(rule(
                "caveat1",
                Arrays.asList(var("0")),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var("0"))),
                        pred("operation", Arrays.asList(s("ambient"), var("1"))),
                        pred("right", Arrays.asList(s("authority"), var("0"), var("1")))
                )
        )));
        v1.allow();
        Either<Error, Long> res = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedVerifier(0, "check if resource(#ambient, $0), operation(#ambient, $1), right(#authority, $0, $1)")
                ))),
                e);
    }

    public void test12_VerifierAuthorityCaveats() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test12_authority_caveats.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_resource("file1");
        v1.add_operation("read");
        v1.allow();
        Assert.assertTrue(v1.verify(new RunLimits(500, 100, Duration.ofMillis(500))).isRight());

        Verifier v2 = token.verifier().get();
        v2.add_resource("file2");
        v2.add_operation("read");
        v2.allow();

        Either<Error, Long> res = v2.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(0, 0, "check if resource(#ambient, \"file1\")")
                ))),
                e);
    }

    public void test13_BlockRules() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test13_block_rules.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_resource("file1");
        v1.set_time();
        v1.allow();
        Assert.assertTrue(v1.verify(new RunLimits(500, 100, Duration.ofMillis(500))).isRight());

        Verifier v2 = token.verifier().get();
        v2.add_resource("file2");
        v2.set_time();
        v2.allow();

        Either<Error, Long> res = v2.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(1, 0, "check if valid_date($0), resource(#ambient, $0)")
                ))),
                e);
    }

    public void test14_RegexConstraint() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test14_regex_constraint.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_resource("file1");
        v1.set_time();
        v1.allow();

        Either<Error, Long> res = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(0, 0, "check if resource(#ambient, $0), $0.matches(\"file[0-9]+.txt\")")
                ))),
                e);

        Verifier v2 = token.verifier().get();
        v2.add_resource("file123.txt");
        v2.set_time();
        v2.allow();
        Assert.assertTrue(v2.verify(new RunLimits(500, 100, Duration.ofMillis(500))).isRight());

    }

    public void test15_MultiQueriesCaveats() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test15_multi_queries_caveats.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        ArrayList<Rule> queries = new ArrayList<>();
        queries.add(rule(
                "test_must_be_present_authority",
                Arrays.asList(var("0")),
                Arrays.asList(
                        pred("must_be_present", Arrays.asList(s("authority"), var("0")))
                )
        ));
        queries.add(rule(
                "test_must_be_present",
                Arrays.asList(var("0")),
                Arrays.asList(
                        pred("mst_be_present", Arrays.asList(var("0")))
                )
        ));
        v1.add_check(new Check(queries));
        v1.allow();

        Assert.assertTrue(v1.verify(new RunLimits(500, 100, Duration.ofMillis(500))).isRight());
    }

    public void test16_CaveatHeadName() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test16_caveat_head_name.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.allow();

        Either<Error, Long> res = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println(res);
        Error e = res.getLeft();
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(0, 0, "check if resource(#ambient, #hello)")
                ))),
                e);
    }

    public void test17_Expressions() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test17_expressions.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.allow();

        Assert.assertEquals(Right(Long.valueOf(0)), v1.verify(new RunLimits(500, 100, Duration.ofMillis(500))));
    }

    public void test18_Unbound_Variables() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test18_unbound_variables_in_rule.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_operation("write");
        v1.allow();
        Either<Error, Long> result = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println("result: "+result);
        Assert.assertTrue(result.isLeft());
    }

    public void test19_generating_ambient_from_variables() throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        PublicKey root = new PublicKey(rootData);

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/test19_generating_ambient_from_variables.bc");

        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = token.verifier().get();
        v1.add_operation("write");
        v1.allow();
        Either<Error, Long> result = v1.verify(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println("result: "+result);
        Assert.assertTrue(result.isLeft());
    }
}
