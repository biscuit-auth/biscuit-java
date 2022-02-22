package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Block;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Instant;
import java.util.*;

import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static com.clevercloud.biscuit.token.builder.Utils.*;

public class BiscuitTest extends TestCase {
    public BiscuitTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BiscuitTest.class);
    }

    public void testBasic() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CloneNotSupportedException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);

        authority_builder.add_fact(fact("right", Arrays.asList(s("file1"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("file2"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build());

        System.out.println(b.print());

        System.out.println("serializing the first token");

        byte[] data = b.serialize();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(hex(data));

        System.out.println("deserializing the first token");
        Biscuit deser = Biscuit.from_bytes(data, root.public_key());

        System.out.println(deser.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.create_block();
        builder.add_check(check(rule(
                "caveat1",
                Arrays.asList(var("resource")),
                Arrays.asList(
                        pred("resource", Arrays.asList(var("resource"))),
                        pred("operation", Arrays.asList(s("read"))),
                        pred("right", Arrays.asList(var("resource"), s("read")))
                )
        )));

        Biscuit b2 = deser.attenuate(rng, keypair2, builder.build());

        System.out.println(b2.print());

        System.out.println("serializing the second token");

        byte[] data2 = b2.serialize();

        System.out.print("data len: ");
        System.out.println(data2.length);
        System.out.println(hex(data2));

        System.out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.from_bytes(data2, root.public_key());

        System.out.println(deser2.print());

        // THIRD BLOCK
        System.out.println("preparing the third block");

        KeyPair keypair3 = new KeyPair(rng);

        Block builder3 = deser2.create_block();
        builder3.add_check(check(rule(
                "caveat2",
                Arrays.asList(s("file1")),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("file1")))
                )
        )));

        Biscuit b3 = deser2.attenuate(rng, keypair3, builder3.build());

        System.out.println(b3.print());

        System.out.println("serializing the third token");

        byte[] data3 = b3.serialize();

        System.out.print("data len: ");
        System.out.println(data3.length);
        System.out.println(hex(data3));

        System.out.println("deserializing the third token");
        Biscuit final_token = Biscuit.from_bytes(data3, root.public_key());

        System.out.println(final_token.print());

        // check
        System.out.println("will check the token for resource=file1 and operation=read");

        SymbolTable check_symbols = new SymbolTable(final_token.symbols);
        List<Fact> ambient_facts = Arrays.asList(
                fact("resource", Arrays.asList(s("file1"))).convert(check_symbols),
                fact("operation", Arrays.asList(s("read"))).convert(check_symbols)
        );

        final_token.check(check_symbols, ambient_facts,
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());

        System.out.println("will check the token for resource=file2 and operation=write");

        SymbolTable check_symbols2 = new SymbolTable(final_token.symbols);
        List<Fact> ambient_facts2 = Arrays.asList(
                fact("resource", Arrays.asList(s("file2"))).convert(check_symbols2),
                fact("operation", Arrays.asList(s("write"))).convert(check_symbols2)
        );

        try {
            final_token.check(check_symbols2, ambient_facts2,
                    new ArrayList<>(), new ArrayList<>(), new HashMap<>());
            Assert.fail();
        } catch (Error e) {
            System.out.println(e);
            Assert.assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), operation(\"read\"), right($resource, \"read\")"),
                            new FailedCheck.FailedBlock(2, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    public void testFolders() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        com.clevercloud.biscuit.token.builder.Biscuit builder = Biscuit.builder(rng, root);

        builder.add_right("/folder1/file1", "read");
        builder.add_right("/folder1/file1", "write");
        builder.add_right("/folder1/file2", "read");
        builder.add_right("/folder1/file2", "write");
        builder.add_right("/folder2/file3", "read");

        System.out.println(builder.build());
        Biscuit b = builder.build();

        System.out.println(b.print());

        Block block2 = b.create_block();
        block2.resource_prefix("/folder1/");
        block2.check_right("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.attenuate(rng, keypair2, block2.build());

        Authorizer v1 = b2.authorizer();
        v1.add_fact("resource(\"/folder1/file1\")");
        v1.add_fact("operation(\"read\")");
        v1.allow();
        v1.authorize();

        Authorizer v2 = b2.authorizer();
        v2.add_fact("resource(\"/folder2/file3\")");
        v2.add_fact("operation(\"read\")");
        v2.allow();
        try {
            v2.authorize();
            Assert.fail();
        } catch (Error e2) {
            // Empty
        }

        Authorizer v3 = b2.authorizer();
        v3.add_fact("resource(\"/folder2/file1\")");
        v3.add_fact("operation(\"write\")");
        v3.allow();
        try {
            v3.authorize();
            Assert.fail();
        } catch (Error e) {
            System.out.println(v3.print_world());
            for (FailedCheck f : e.failed_checks().get()) {
                System.out.println(f.toString());
            }
            Assert.assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), $resource.starts_with(\"/folder1/\")"),
                            new FailedCheck.FailedBlock(1, 1, "check if resource($resource), operation(\"read\"), right($resource, \"read\")")
                    ))),
                    e);
        }
    }

    public void testSealedTokens() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);

        authority_builder.add_fact(fact("right", Arrays.asList(s("file1"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("file2"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build());

        System.out.println(b.print());

        System.out.println("serializing the first token");

        byte[] data = b.serialize();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(hex(data));

        System.out.println("deserializing the first token");
        Biscuit deser = Biscuit.from_bytes(data, root.public_key());

        System.out.println(deser.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.create_block();
        builder.add_check(check(rule(
                "caveat1",
                Arrays.asList(var("resource")),
                Arrays.asList(
                        pred("resource", Arrays.asList(var("resource"))),
                        pred("operation", Arrays.asList(s("read"))),
                        pred("right", Arrays.asList(var("resource"), s("read")))
                )
        )));

        Biscuit b2 = deser.attenuate(rng, keypair2, builder.build());

        System.out.println(b2.print());

        System.out.println("sealing the second token");

        byte[] sealed = b2.seal();
        System.out.print("sealed data len: ");
        System.out.println(sealed.length);
        System.out.println(hex(sealed));

        System.out.println("deserializing the sealed token with an invalid key");
        KeyPair invalid_key = new KeyPair(rng);
        try {
            Biscuit.from_bytes(sealed, invalid_key.public_key());
            Assert.fail();
        } catch (Error e) {
            System.out.println(e);
            Assert.assertEquals(
                    new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied"),
                    e);
        }

        System.out.println("deserializing the sealed token with a valid key");
        Biscuit deser2 = Biscuit.from_bytes(sealed, root.public_key());
        System.out.println(deser2.print());

        System.out.println("trying to attenuate to a sealed token");
        Block builder2 = deser2.create_block();
        Error e2 = (Error) Try.of(() -> deser2.attenuate(rng, keypair2, builder.build())).getCause();

        Authorizer v = deser2.authorizer();
        System.out.println(v.print_world());
    }

    public void testMultipleAttenuation() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        SecureRandom rng = new SecureRandom();
        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);
        Date date = Date.from(Instant.now());
        authority_builder.add_fact(fact("revocation_id", Arrays.asList(date(date))));

        Biscuit biscuit = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build());

        Block builder = biscuit.create_block();
        builder.add_fact(fact(
                "right",
                Arrays.asList(s("topic"), s("tenant"), s("namespace"), s("topic"), s("produce"))
        ));

        String attenuatedB64 = biscuit.attenuate(rng, new KeyPair(rng), builder.build()).serialize_b64();

        System.out.println("attenuated: " + attenuatedB64);

        Biscuit.from_b64(attenuatedB64, root.public_key());
        String attenuated2B64 = biscuit.attenuate(rng, new KeyPair(rng), builder.build()).serialize_b64();

        System.out.println("attenuated2: " + attenuated2B64);
        Biscuit.from_b64(attenuated2B64, root.public_key());
    }

    public void testReset() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        com.clevercloud.biscuit.token.builder.Biscuit builder = Biscuit.builder(rng, root);

        builder.add_right("/folder1/file1", "read");
        builder.add_right("/folder1/file1", "write");
        builder.add_right("/folder1/file2", "read");
        builder.add_right("/folder1/file2", "write");
        builder.add_right("/folder2/file3", "read");

        System.out.println(builder.build());
        Biscuit b = builder.build();

        System.out.println(b.print());

        Block block2 = b.create_block();
        block2.resource_prefix("/folder1/");
        block2.check_right("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.attenuate(rng, keypair2, block2.build());

        Authorizer v1 = b2.authorizer();
        v1.allow();

        Authorizer v2 = v1.clone();

        v2.add_fact("resource(\"/folder1/file1\")");
        v2.add_fact("operation(\"read\")");


        v2.authorize();

        Authorizer v3 = v1.clone();

        v3.add_fact("resource(\"/folder2/file3\")");
        v3.add_fact("operation(\"read\")");

        Try res = Try.of(() -> v3.authorize());
        System.out.println(v3.print_world());

        Assert.assertTrue(res.isFailure());

        Authorizer v4 = v1.clone();

        v4.add_fact("resource(\"/folder2/file1\")");
        v4.add_fact("operation(\"write\")");

        Error e = (Error) Try.of(() -> v4.authorize()).getCause();

        System.out.println(v4.print_world());
        for (FailedCheck f : e.failed_checks().get()) {
            System.out.println(f.toString());
        }
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                        new FailedCheck.FailedBlock(1, 0, "check if resource($resource), $resource.starts_with(\"/folder1/\")"),
                        new FailedCheck.FailedBlock(1, 1, "check if resource($resource), operation(\"read\"), right($resource, \"read\")")
                ))),
                e);
    }

    public void testEmptyAuthorizer() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        com.clevercloud.biscuit.token.builder.Biscuit builder = Biscuit.builder(rng, root);

        builder.add_right("/folder1/file1", "read");
        builder.add_right("/folder1/file1", "write");
        builder.add_right("/folder1/file2", "read");
        builder.add_right("/folder1/file2", "write");
        builder.add_right("/folder2/file3", "read");

        System.out.println(builder.build());
        Biscuit b = builder.build();

        System.out.println(b.print());

        Block block2 = b.create_block();
        block2.resource_prefix("/folder1/");
        block2.check_right("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.attenuate(rng, keypair2, block2.build());

        Authorizer v1 = new Authorizer();
        v1.allow();

        v1.authorize();

        v1.add_token(b2);

        v1.add_fact("resource(\"/folder2/file1\")");
        v1.add_fact("operation(\"write\")");

        Assert.assertTrue(Try.of(()-> v1.authorize()).isFailure());
    }
}
