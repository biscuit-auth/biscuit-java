package org.biscuitsec.biscuit.token;

import io.vavr.control.Option;
import org.biscuitsec.biscuit.crypto.KeyDelegate;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
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
import java.util.Arrays;

import static org.biscuitsec.biscuit.crypto.TokenSignature.hex;
import static org.biscuitsec.biscuit.token.builder.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ThirdPartyTest {
    @Test
    public void testBasic() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CloneNotSupportedException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);
        KeyPair external = new KeyPair(rng);
        System.out.println("external: ed25519/"+external.public_key().toHex());


        org.biscuitsec.biscuit.token.builder.Block authority_builder = new Block();
        authority_builder.add_fact("right(\"read\")");
        authority_builder.add_check("check if group(\"admin\") trusting ed25519/D75712CB4091E53D850E032DFBCD8D003CA9BD1B60BAFF4D92DC98145448BCC5");

        Biscuit b1 = Biscuit.make(rng, root, authority_builder.build());
        ThirdPartyRequest request = b1.thirdPartyRequest();
        Block builder = new Block();
        builder.add_fact("group(\"admin\")");
        builder.add_check("check if resource(\"file1\")");

        ThirdPartyBlock blockResponse = request.createBlock(external, builder).get();
        Biscuit b2 = b1.appendThirdPartyBlock(external.public_key(), blockResponse);

        byte[] data = b2.serialize();
        Biscuit deser = Biscuit.from_bytes(data, root.public_key());
        assertEquals(b2.print(), deser.print());

        System.out.println("will check the token for resource=file1");
        Authorizer authorizer = deser.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_policy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        System.out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_policy("allow if true");
        authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        /*try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            System.out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), operation(\"read\"), right($resource, \"read\")"),
                            new FailedCheck.FailedBlock(2, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
        assertThrows(InvalidKeyException.class, () -> {
            Biscuit deser = Biscuit.from_bytes(data, new KeyDelegate() {
                @Override
                public Option<PublicKey> root_key(Option<Integer> key_id) {
                    return Option.none();
                }
            });
        });*/
    }
}
