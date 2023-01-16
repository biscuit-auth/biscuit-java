package com.clevercloud.biscuit.token;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.clevercloud.biscuit.error.Error.Parser;
import com.clevercloud.biscuit.token.builder.Expression;
import com.clevercloud.biscuit.token.builder.Term;
import static com.clevercloud.biscuit.token.builder.Utils.*;

public class AuthorizerTest {
 
    @Test
    public void testAuthorizerPolicy() throws Parser {
        Authorizer authorizer = new Authorizer();
        List<Policy> policies = authorizer.policies;
        authorizer.deny();
        assertEquals(1, policies.size());

        authorizer.add_policy(new Policy(
            Arrays.asList(
                constrained_rule(
                    "deny",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    Arrays.asList(new Expression.Value(new Term.Bool(true)))
                )
            ), Policy.Kind.Deny));
        assertEquals(2, policies.size());

        authorizer.add_policy("deny if true");
        assertEquals(3, policies.size());
    }
}
