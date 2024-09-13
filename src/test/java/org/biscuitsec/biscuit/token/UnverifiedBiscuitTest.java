package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.datalog.RunLimits;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.Error.FailedLogic;
import org.biscuitsec.biscuit.error.FailedCheck.FailedBlock;
import org.biscuitsec.biscuit.error.LogicError.MatchedPolicy.Allow;
import org.biscuitsec.biscuit.error.LogicError.Unauthorized;
import org.biscuitsec.biscuit.token.builder.Block;
import org.biscuitsec.biscuit.token.builder.Utils;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnverifiedBiscuitTest {

    @Test
    public void testBasic() throws Error, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block, block0");

        KeyPair keypair0 = new KeyPair(rng);

        // org.biscuitsec.biscuit.token.builder.Block block0 = new org.biscuitsec.biscuit.token.builder.Block(0);
        org.biscuitsec.biscuit.token.builder.Biscuit block0 = Biscuit.builder(rng, keypair0);
        block0.add_authority_fact(Utils.fact("right", List.of(Utils.s("file1"), Utils.s("read"))));
        block0.add_authority_fact(Utils.fact("right", List.of(Utils.s("file2"), Utils.s("read"))));
        block0.add_authority_fact(Utils.fact("right", List.of(Utils.s("file1"), Utils.s("write"))));


        Biscuit biscuit0 = block0.build();

        out.println(biscuit0.print());
        out.println("serializing the first token");

        String data = biscuit0.serialize_b64url();

        out.print("data len: ");
        out.println(data.length());
        out.println(data);

        out.println("deserializing the first token");
        UnverifiedBiscuit deser0 = UnverifiedBiscuit.from_b64url(data);
        out.println(deser0.print());

        // SECOND BLOCK
        out.println("preparing the second block");

        KeyPair keypair1 = new KeyPair(rng);
        org.biscuitsec.biscuit.token.builder.Block block1 = deser0.create_block();
        block1.add_check(Utils.check(Utils.rule(
                "caveat1",
                List.of(Utils.var("resource")),
                List.of(
                        Utils.pred("resource", List.of(Utils.var("resource"))),
                        Utils.pred("operation", List.of(Utils.s("read"))),
                        Utils.pred("right", List.of(Utils.var("resource"), Utils.s("read")))
                )
        )));
        UnverifiedBiscuit unverifiedBiscuit1 = deser0.attenuate(rng, keypair1, block1.build());

        out.println(unverifiedBiscuit1.print());

        out.println("serializing the second token");

        String data1 = unverifiedBiscuit1.serialize_b64url();

        out.print("data len: ");
        out.println(data1.length());
        out.println(data1);

        out.println("deserializing the second token");
        UnverifiedBiscuit deser1 = UnverifiedBiscuit.from_b64url(data1);

        out.println(deser1.print());

        // THIRD BLOCK
        out.println("preparing the third block");

        KeyPair keypair2 = new KeyPair(rng);

        Block block2 = unverifiedBiscuit1.create_block();
        block2.add_check(Utils.check(Utils.rule(
                "caveat2",
                List.of(Utils.s("file1")),
                List.of(
                        Utils.pred("resource", List.of(Utils.s("file1")))
                )
        )));

        UnverifiedBiscuit unverifiedBiscuit2 = unverifiedBiscuit1.attenuate(rng, keypair2, block2);

        out.println(unverifiedBiscuit2.print());

        out.println("serializing the third token");

        String data2 = unverifiedBiscuit2.serialize_b64url();

        out.print("data len: ");
        out.println(data2.length());
        out.println(data2);

        out.println("deserializing the third token");
        UnverifiedBiscuit finalUnverifiedBiscuit = UnverifiedBiscuit.from_b64url(data2);

        out.println(finalUnverifiedBiscuit.print());

        // Crate Biscuit from UnverifiedBiscuit
        Biscuit finalBiscuit = finalUnverifiedBiscuit.verify(keypair0.publicKey());

        // check
        out.println("will check the token for resource=file1 and operation=read");

        Authorizer authorizer = finalBiscuit.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_fact("operation(\"read\")");
        authorizer.add_policy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2 and operation=write");

        Authorizer authorizer2 = finalBiscuit.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_fact("operation(\"write\")");
        authorizer2.add_policy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            out.println(e);
            assertEquals(
                    new FailedLogic(new Unauthorized(new Allow(0), Arrays.asList(
                            new FailedBlock(1, 0, "check if resource($resource), operation(\"read\"), right($resource, \"read\")"),
                            new FailedBlock(2, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }
}