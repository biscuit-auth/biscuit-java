package org.biscuitsec.biscuit.token;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.datalog.RunLimits;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.FailedCheck;
import org.biscuitsec.biscuit.error.LogicError;
import org.biscuitsec.biscuit.token.builder.Block;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.lang.System.out;
import static org.biscuitsec.biscuit.crypto.TokenSignature.hex;
import static org.biscuitsec.biscuit.token.builder.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

public class BiscuitTest {

    @Test
    public void testBasic() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        Block authority_builder = new Block();

        authority_builder.addFact(fact("right", Arrays.asList(s("file1"), s("read"))));
        authority_builder.addFact(fact("right", Arrays.asList(s("file2"), s("read"))));
        authority_builder.addFact(fact("right", Arrays.asList(s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, authority_builder.build());

        out.println(b.print());

        out.println("serializing the first token");

        byte[] data = b.serialize();

        out.print("data len: ");
        out.println(data.length);
        out.println(hex(data));

        out.println("deserializing the first token");
        Biscuit deser = Biscuit.fromBytes(data, root.publicKey());

        out.println(deser.print());

        // SECOND BLOCK
        out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.createBlock();
        builder.addCheck(check(rule(
                "caveat1",
                List.of(var("resource")),
                Arrays.asList(
                        pred("resource", List.of(var("resource"))),
                        pred("operation", List.of(s("read"))),
                        pred("right", Arrays.asList(var("resource"), s("read")))
                )
        )));

        Biscuit b2 = deser.attenuate(rng, keypair2, builder);

        out.println(b2.print());

        out.println("serializing the second token");

        byte[] data2 = b2.serialize();

        out.print("data len: ");
        out.println(data2.length);
        out.println(hex(data2));

        out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.fromBytes(data2, root.publicKey());

        out.println(deser2.print());

        // THIRD BLOCK
        out.println("preparing the third block");

        KeyPair keypair3 = new KeyPair(rng);

        Block builder3 = deser2.createBlock();
        builder3.addCheck(check(rule(
                "caveat2",
                List.of(s("file1")),
                List.of(
                        pred("resource", List.of(s("file1")))
                )
        )));

        Biscuit b3 = deser2.attenuate(rng, keypair3, builder3);

        out.println(b3.print());

        out.println("serializing the third token");

        byte[] data3 = b3.serialize();

        out.print("data len: ");
        out.println(data3.length);
        out.println(hex(data3));

        out.println("deserializing the third token");
        Biscuit final_token = Biscuit.fromBytes(data3, root.publicKey());

        out.println(final_token.print());

        // check
        out.println("will check the token for resource=file1 and operation=read");

        Authorizer authorizer = final_token.authorizer();
        authorizer.addFact("resource(\"file1\")");
        authorizer.addFact("operation(\"read\")");
        authorizer.addPolicy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2 and operation=write");

        Authorizer authorizer2 = final_token.authorizer();
        authorizer2.addFact("resource(\"file2\")");
        authorizer2.addFact("operation(\"write\")");
        authorizer2.addPolicy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            e.printStackTrace(out);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), operation(\"read\"), right($resource, \"read\")"),
                            new FailedCheck.FailedBlock(2, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testFolders() throws Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        org.biscuitsec.biscuit.token.builder.Biscuit builder = Biscuit.builder(rng, root);

        builder.addRight("/folder1/file1", "read");
        builder.addRight("/folder1/file1", "write");
        builder.addRight("/folder1/file2", "read");
        builder.addRight("/folder1/file2", "write");
        builder.addRight("/folder2/file3", "read");

        out.println(builder.build());
        Biscuit b = builder.build();

        out.println(b.print());

        Block block2 = b.createBlock();
        block2.resourcePrefix("/folder1/");
        block2.checkRight("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.attenuate(rng, keypair2, block2);

        Authorizer v1 = b2.authorizer();
        v1.addFact("resource(\"/folder1/file1\")");
        v1.addFact("operation(\"read\")");
        v1.allow();
        v1.authorize();

        Authorizer v2 = b2.authorizer();
        v2.addFact("resource(\"/folder2/file3\")");
        v2.addFact("operation(\"read\")");
        v2.allow();
        try {
            v2.authorize();
            fail();
        } catch (Error e2) {
            // Empty
        }

        Authorizer v3 = b2.authorizer();
        v3.addFact("resource(\"/folder2/file1\")");
        v3.addFact("operation(\"write\")");
        v3.allow();
        try {
            v3.authorize();
            fail();
        } catch (Error e) {
            out.println(v3.printWorld());
            for (FailedCheck f : e.failedChecks().get()) {
                out.println(f.toString());
            }
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), $resource.starts_with(\"/folder1/\")"),
                            new FailedCheck.FailedBlock(1, 1, "check if resource($resource), operation(\"read\"), right($resource, \"read\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testMultipleAttenuation() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        SecureRandom rng = new SecureRandom();
        KeyPair root = new KeyPair(rng);

        Block authority_builder = new Block();
        Date date = Date.from(Instant.now());
        authority_builder.addFact(fact("revocation_id", List.of(date(date))));

        Biscuit biscuit = Biscuit.make(rng, root, authority_builder.build());

        Block builder = biscuit.createBlock();
        builder.addFact(fact(
                "right",
                Arrays.asList(s("topic"), s("tenant"), s("namespace"), s("topic"), s("produce"))
        ));

        String attenuatedB64 = biscuit.attenuate(rng, new KeyPair(rng), builder).serializeB64Url();

        out.println("attenuated: " + attenuatedB64);

        Biscuit.fromB64Url(attenuatedB64, root.publicKey());
        String attenuated2B64 = biscuit.attenuate(rng, new KeyPair(rng), builder).serializeB64Url();

        out.println("attenuated2: " + attenuated2B64);
        Biscuit.fromB64Url(attenuated2B64, root.publicKey());
    }

    @Test
    public void testReset() throws Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        org.biscuitsec.biscuit.token.builder.Biscuit builder = Biscuit.builder(rng, root);

        builder.addRight("/folder1/file1", "read");
        builder.addRight("/folder1/file1", "write");
        builder.addRight("/folder1/file2", "read");
        builder.addRight("/folder1/file2", "write");
        builder.addRight("/folder2/file3", "read");

        out.println(builder.build());
        Biscuit b = builder.build();

        out.println(b.print());

        Block block2 = b.createBlock();
        block2.resourcePrefix("/folder1/");
        block2.checkRight("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.attenuate(rng, keypair2, block2);

        Authorizer v1 = b2.authorizer();
        v1.allow();

        Authorizer v2 = v1.clone();

        v2.addFact("resource(\"/folder1/file1\")");
        v2.addFact("operation(\"read\")");


        v2.authorize();

        Authorizer v3 = v1.clone();

        v3.addFact("resource(\"/folder2/file3\")");
        v3.addFact("operation(\"read\")");

        Try<Long> res = Try.of(v3::authorize);
        out.println(v3.printWorld());

        assertTrue(res.isFailure());

        Authorizer v4 = v1.clone();

        v4.addFact("resource(\"/folder2/file1\")");
        v4.addFact("operation(\"write\")");

        Error e = (Error) Try.of(v4::authorize).getCause();

        out.println(v4.printWorld());
        e.failedChecks().get().stream().forEach(fc -> out.println(fc.toString()));
        assertEquals(
                new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                        new FailedCheck.FailedBlock(1, 0, "check if resource($resource), $resource.starts_with(\"/folder1/\")"),
                        new FailedCheck.FailedBlock(1, 1, "check if resource($resource), operation(\"read\"), right($resource, \"read\")")
                ))),
                e);
    }

    @Test
    public void testEmptyAuthorizer() throws Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        org.biscuitsec.biscuit.token.builder.Biscuit builder = Biscuit.builder(rng, root);

        builder.addRight("/folder1/file1", "read");
        builder.addRight("/folder1/file1", "write");
        builder.addRight("/folder1/file2", "read");
        builder.addRight("/folder1/file2", "write");
        builder.addRight("/folder2/file3", "read");

        out.println(builder.build());
        Biscuit b = builder.build();

        out.println(b.print());

        Block block2 = b.createBlock();
        block2.resourcePrefix("/folder1/");
        block2.checkRight("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.attenuate(rng, keypair2, block2);

        Authorizer v1 = new Authorizer();
        v1.allow();

        v1.authorize();

        v1.addToken(b2);

        v1.addFact("resource(\"/folder2/file1\")");
        v1.addFact("operation(\"write\")");

        assertTrue(Try.of(v1::authorize).isFailure());
    }

    @Test
    public void testBasicWithNamespaces() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        Block authority_builder = new Block();

        authority_builder.addFact(fact("namespace:right", Arrays.asList(s("file1"), s("read"))));
        authority_builder.addFact(fact("namespace:right", Arrays.asList(s("file1"), s("write"))));
        authority_builder.addFact(fact("namespace:right", Arrays.asList(s("file2"), s("read"))));
        Biscuit b = Biscuit.make(rng, root, authority_builder.build());

        out.println(b.print());

        out.println("serializing the first token");

        byte[] data = b.serialize();

        out.print("data len: ");
        out.println(data.length);
        out.println(hex(data));

        out.println("deserializing the first token");
        Biscuit deser = Biscuit.fromBytes(data, root.publicKey());

        out.println(deser.print());

        // SECOND BLOCK
        out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.createBlock();
        builder.addCheck(check(rule(
                "caveat1",
                List.of(var("resource")),
                Arrays.asList(
                        pred("resource", List.of(var("resource"))),
                        pred("operation", List.of(s("read"))),
                        pred("namespace:right", Arrays.asList(var("resource"), s("read")))
                )
        )));

        Biscuit b2 = deser.attenuate(rng, keypair2, builder);

        out.println(b2.print());

        out.println("serializing the second token");

        byte[] data2 = b2.serialize();

        out.print("data len: ");
        out.println(data2.length);
        out.println(hex(data2));

        out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.fromBytes(data2, root.publicKey());

        out.println(deser2.print());

        // THIRD BLOCK
        out.println("preparing the third block");

        KeyPair keypair3 = new KeyPair(rng);

        Block builder3 = deser2.createBlock();
        builder3.addCheck(check(rule(
                "caveat2",
                List.of(s("file1")),
                List.of(
                        pred("resource", List.of(s("file1")))
                )
        )));

        Biscuit b3 = deser2.attenuate(rng, keypair3, builder3);

        out.println(b3.print());

        out.println("serializing the third token");

        byte[] data3 = b3.serialize();

        out.print("data len: ");
        out.println(data3.length);
        out.println(hex(data3));

        out.println("deserializing the third token");
        Biscuit final_token = Biscuit.fromBytes(data3, root.publicKey());

        out.println(final_token.print());

        // check
        out.println("will check the token for resource=file1 and operation=read");

        Authorizer authorizer = final_token.authorizer();
        authorizer.addFact("resource(\"file1\")");
        authorizer.addFact("operation(\"read\")");
        authorizer.addPolicy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2 and operation=write");

        Authorizer authorizer2 = final_token.authorizer();
        authorizer2.addFact("resource(\"file2\")");
        authorizer2.addFact("operation(\"write\")");
        authorizer2.addPolicy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            e.printStackTrace(out);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), operation(\"read\"), namespace:right($resource, \"read\")"),
                            new FailedCheck.FailedBlock(2, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testBasicWithNamespacesWithAddAuthorityFact() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        org.biscuitsec.biscuit.token.builder.Biscuit o = new org.biscuitsec.biscuit.token.builder.Biscuit(rng, root);
        o.addAuthorityFact("namespace:right(\"file1\",\"read\")");
        o.addAuthorityFact("namespace:right(\"file1\",\"write\")");
        o.addAuthorityFact("namespace:right(\"file2\",\"read\")");
        Biscuit b = o.build();

        out.println(b.print());

        out.println("serializing the first token");

        byte[] data = b.serialize();

        out.print("data len: ");
        out.println(data.length);
        out.println(hex(data));

        out.println("deserializing the first token");
        Biscuit deser = Biscuit.fromBytes(data, root.publicKey());

        out.println(deser.print());

        // SECOND BLOCK
        out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.createBlock();
        builder.addCheck(check(rule(
                "caveat1",
                List.of(var("resource")),
                Arrays.asList(
                        pred("resource", List.of(var("resource"))),
                        pred("operation", List.of(s("read"))),
                        pred("namespace:right", Arrays.asList(var("resource"), s("read")))
                )
        )));

        Biscuit b2 = deser.attenuate(rng, keypair2, builder);

        out.println(b2.print());

        out.println("serializing the second token");

        byte[] data2 = b2.serialize();

        out.print("data len: ");
        out.println(data2.length);
        out.println(hex(data2));

        out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.fromBytes(data2, root.publicKey());

        out.println(deser2.print());

        // THIRD BLOCK
        out.println("preparing the third block");

        KeyPair keypair3 = new KeyPair(rng);

        Block builder3 = deser2.createBlock();
        builder3.addCheck(check(rule(
                "caveat2",
                List.of(s("file1")),
                List.of(
                        pred("resource", List.of(s("file1")))
                )
        )));

        Biscuit b3 = deser2.attenuate(rng, keypair3, builder3);

        out.println(b3.print());

        out.println("serializing the third token");

        byte[] data3 = b3.serialize();

        out.print("data len: ");
        out.println(data3.length);
        out.println(hex(data3));

        out.println("deserializing the third token");
        Biscuit final_token = Biscuit.fromBytes(data3, root.publicKey());

        out.println(final_token.print());

        // check
        out.println("will check the token for resource=file1 and operation=read");

        Authorizer authorizer = final_token.authorizer();
        authorizer.addFact("resource(\"file1\")");
        authorizer.addFact("operation(\"read\")");
        authorizer.addPolicy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2 and operation=write");

        Authorizer authorizer2 = final_token.authorizer();
        authorizer2.addFact("resource(\"file2\")");
        authorizer2.addFact("operation(\"write\")");
        authorizer2.addPolicy("allow if true");
        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            e.printStackTrace(out);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), operation(\"read\"), namespace:right($resource, \"read\")"),
                            new FailedCheck.FailedBlock(2, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testRootKeyId() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        Block authority_builder = new Block();

        authority_builder.addFact(fact("right", Arrays.asList(s("file1"), s("read"))));
        authority_builder.addFact(fact("right", Arrays.asList(s("file2"), s("read"))));
        authority_builder.addFact(fact("right", Arrays.asList(s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, 1, authority_builder.build());

        out.println(b.print());

        out.println("serializing the first token");

        byte[] data = b.serialize();

        out.print("data len: ");
        out.println(data.length);
        out.println(hex(data));

        out.println("deserializing the first token");

        assertThrows(InvalidKeyException.class, () -> Biscuit.fromBytes(data, key_id -> Option.none()));


        assertThrows(Error.FormatError.Signature.InvalidSignature.class, () -> Biscuit.fromBytes(data, key_id -> {
            KeyPair root1 = new KeyPair(rng);
            return Option.some(root1.publicKey());
        }));

        Biscuit.fromBytes(data, key_id -> {
            if (key_id.get() == 1) {
                return Option.some(root.publicKey());
            } else {
                return Option.none();
            }
        });
    }

    @Test
    public void testCheckAll() throws Error, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        Biscuit biscuit = Biscuit.builder(root)
                .addAuthorityCheck("check all operation($op), allowed_operations($allowed), $allowed.contains($op)")
                .build();
        Authorizer authorizer = biscuit.verify(root.publicKey()).authorizer();
        authorizer.addFact("operation(\"read\")");
        authorizer.addFact("operation(\"write\")");
        authorizer.addFact("allowed_operations([\"write\"])");
        authorizer.addPolicy("allow if true");

        try {
            authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error.FailedLogic e) {
            e.printStackTrace(out);
            assertEquals(new Error.FailedLogic(new LogicError.Unauthorized(
                    new LogicError.MatchedPolicy.Allow(0),
                    List.of(
                            new FailedCheck.FailedBlock(0, 0, "check all operation($op), allowed_operations($allowed), $allowed.contains($op)")
                    )
            )), e);
        }

        Authorizer authorizer2 = biscuit.verify(root.publicKey()).authorizer();
        authorizer2.addFact("operation(\"read\")");
        authorizer2.addFact("operation(\"write\")");
        authorizer2.addFact("allowed_operations([\"read\", \"write\"])");
        authorizer2.addPolicy("allow if true");

        authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
    }
}
