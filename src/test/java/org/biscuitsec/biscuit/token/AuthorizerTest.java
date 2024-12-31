package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.Error.Parser;
import org.biscuitsec.biscuit.token.builder.Expression;
import org.biscuitsec.biscuit.token.builder.Term;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.biscuitsec.biscuit.token.builder.Utils.constrainedRule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthorizerTest {

    @Test
    public void testAuthorizerPolicy() throws Parser {
        Authorizer authorizer = new Authorizer();
        List<Policy> policies = authorizer.policies;
        authorizer.deny();
        assertEquals(1, policies.size());

        authorizer.addPolicy(new Policy(
                List.of(
                        constrainedRule(
                                "deny",
                                new ArrayList<>(),
                                new ArrayList<>(),
                                List.of(new Expression.Value(new Term.Bool(true)))
                        )
                ), Policy.Kind.Deny));
        assertEquals(2, policies.size());

        authorizer.addPolicy("deny if true");
        assertEquals(3, policies.size());
    }


    @Test
    public void testPuttingSomeFactsInABiscuitAndGettingThemBackOutAgain() throws Exception {

        KeyPair keypair = new KeyPair();

        Biscuit token = Biscuit.builder(keypair)
                .addAuthorityFact("email(\"bob@example.com\")")
                .addAuthorityFact("id(123)")
                .addAuthorityFact("enabled(true)")
                .addAuthorityFact("perms([1,2,3])")
                .build();

        Authorizer authorizer = Biscuit.fromB64Url(token.serializeB64Url(), keypair.publicKey())
                .verify(keypair.publicKey())
                .authorizer();

        Term emailTerm = queryFirstResult(authorizer, "emailfact($name) <- email($name)");
        assertEquals("bob@example.com", ((Term.Str) emailTerm).getValue());

        Term idTerm = queryFirstResult(authorizer, "idfact($name) <- id($name)");
        assertEquals(123, ((Term.Integer) idTerm).getValue());

        Term enabledTerm = queryFirstResult(authorizer, "enabledfact($name) <- enabled($name)");
        assertTrue(((Term.Bool) enabledTerm).getValue());

        Term permsTerm = queryFirstResult(authorizer, "permsfact($name) <- perms($name)");
        assertEquals(
                Set.of(new Term.Integer(1), new Term.Integer(2), new Term.Integer(3)),
                ((Term.Set) permsTerm).getValue()
        );
    }

    private static Term queryFirstResult(Authorizer authorizer, String query) throws Error {
        return authorizer.query(query)
                .iterator()
                .next()
                .terms().get(0);
    }
}
