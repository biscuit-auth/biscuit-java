package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import com.google.gson.*;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.RunLimits;
import org.biscuitsec.biscuit.error.Error;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SamplesTest {
    final RunLimits runLimits = new RunLimits(500,100, Duration.ofMillis(500));
    @TestFactory
    Stream<DynamicTest> jsonTest() {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("samples/samples.json");
        Gson gson = new Gson();
        Sample sample = gson.fromJson(new InputStreamReader(new BufferedInputStream(inputStream)), Sample.class);
        PublicKey publicKey = new PublicKey(Schema.PublicKey.Algorithm.Ed25519, sample.root_public_key);
        KeyPair keyPair = new KeyPair(sample.root_private_key);
        return sample.testcases.stream().map(t -> process_testcase(t, publicKey, keyPair));
    }

    DynamicTest process_testcase(final TestCase testCase, final PublicKey publicKey, final KeyPair privateKey) {
        return DynamicTest.dynamicTest(testCase.title + ": "+testCase.filename, () -> {
            System.out.println("Testcase name: \""+testCase.title+"\"");
            System.out.println("filename: \""+testCase.filename+"\"");
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("samples/" + testCase.filename);
            byte[] data = new byte[inputStream.available()];

            for(Map.Entry<String, JsonElement> validationEntry: testCase.validations.getAsJsonObject().entrySet()) {
                String validationName = validationEntry.getKey();
                JsonObject validation = validationEntry.getValue().getAsJsonObject();

                JsonObject expected_result = validation.getAsJsonObject("result");
                String[] authorizer_facts = validation.getAsJsonPrimitive("authorizer_code").getAsString().split(";");
                Either<Throwable, Long> res = Try.of(() -> {
                    inputStream.read(data);
                    Biscuit token = Biscuit.from_bytes(data, publicKey);

                    List<RevocationIdentifier> revocationIds = token.revocation_identifiers();
                    JsonArray validationRevocationIds = validation.getAsJsonArray("revocation_ids");
                    assertEquals(revocationIds.size(), validationRevocationIds.size());
                    for(int i = 0; i < revocationIds.size(); i++) {
                        assertEquals(validationRevocationIds.get(i).getAsString(), revocationIds.get(i).toHex());
                    }

                    // TODO Add check of the token

                    Authorizer authorizer = token.authorizer();
                    System.out.println(token.print());
                    for (String f : authorizer_facts) {
                        f = f.trim();
                        if (f.length() > 0) {
                            if (f.startsWith("check if") || f.startsWith("check all")) {
                                authorizer.add_check(f);
                            } else if (f.startsWith("allow if") || f.startsWith("deny if")) {
                                authorizer.add_policy(f);
                            } else if (f.startsWith("revocation_id")) {
                                // do nothing
                            } else {
                                authorizer.add_fact(f);
                            }
                        }
                    }
                    System.out.println(authorizer.print_world());
                    try {
                        Long authorizeResult = authorizer.authorize(runLimits);

                        if(validation.has("world") && !validation.get("world").isJsonNull()) {
                            World world = new Gson().fromJson(validation.get("world").getAsJsonObject(), World.class);
                            World authorizerWorld = new World(
                                    authorizer.facts().stream().map(f -> f.toString()).collect(Collectors.toList()),
                                    authorizer.rules().stream().map(r -> r.toString()).collect(Collectors.toList()),
                                    authorizer.checks().stream().map(c -> c.toString()).collect(Collectors.toList()),
                                    authorizer.policies().stream().map(p -> p.toString()).collect(Collectors.toList())
                            );
                            Collections.sort(world.facts);
                            /*Collections.sort(world.rules);
                            Collections.sort(world.checks);
                            Collections.sort(world.policies);*/
                            Collections.sort(authorizerWorld.facts);
                            /*Collections.sort(authorizerWorld.rules);
                            Collections.sort(authorizerWorld.checks);
                            Collections.sort(authorizerWorld.policies);*/
                            System.out.println("validation world"+world);
                            System.out.println("authorizer world"+authorizerWorld);

                            assertEquals(world.facts.size(), authorizerWorld.facts.size());
                            for (int i = 0; i < world.facts.size(); i++) {
                                assertEquals(world.facts.get(i), authorizerWorld.facts.get(i));
                            }
                            /*assertEquals(world.rules.size(), authorizerWorld.rules.size());
                            for (int i = 0; i < world.rules.size(); i++) {
                                assertEquals(world.rules.get(i), authorizerWorld.rules.get(i));
                            }
                            assertEquals(world.checks.size(), authorizerWorld.checks.size());
                            for (int i = 0; i < world.checks.size(); i++) {
                                assertEquals(world.checks.get(i), authorizerWorld.checks.get(i));
                            }
                            assertEquals(world.policies.size(), authorizerWorld.policies.size());
                            for (int i = 0; i < world.policies.size(); i++) {
                                assertEquals(world.policies.get(i), authorizerWorld.policies.get(i));
                            }*/
                        }

                        return authorizeResult;
                    } catch (Exception e) {

                        if(validation.has("world") && !validation.get("world").isJsonNull()) {
                            World world = new Gson().fromJson(validation.get("world").getAsJsonObject(), World.class);
                            World authorizerWorld = new World(
                                    authorizer.facts().stream().map(f -> f.toString()).collect(Collectors.toList()),
                                    authorizer.rules().stream().map(r -> r.toString()).collect(Collectors.toList()),
                                    authorizer.checks().stream().map(c -> c.toString()).collect(Collectors.toList()),
                                    authorizer.policies().stream().map(p -> p.toString()).collect(Collectors.toList())
                            );
                            Collections.sort(world.facts);
                            Collections.sort(authorizerWorld.facts);
                            /*Collections.sort(authorizerWorld.rules);
                            Collections.sort(authorizerWorld.checks);
                            Collections.sort(authorizerWorld.policies);*/

                            assertEquals(world.facts.size(), authorizerWorld.facts.size());
                            for (int i = 0; i < world.facts.size(); i++) {
                                assertEquals(world.facts.get(i), authorizerWorld.facts.get(i));
                            }
                            /*assertEquals(world.rules.size(), authorizerWorld.rules.size());
                            for (int i = 0; i < world.rules.size(); i++) {
                                assertEquals(world.rules.get(i), authorizerWorld.rules.get(i));
                            }
                            assertEquals(world.checks.size(), authorizerWorld.checks.size());
                            for (int i = 0; i < world.checks.size(); i++) {
                                assertEquals(world.checks.get(i), authorizerWorld.checks.get(i));
                            }
                            assertEquals(world.policies.size(), authorizerWorld.policies.size());
                            for (int i = 0; i < world.policies.size(); i++) {
                                assertEquals(world.policies.get(i), authorizerWorld.policies.get(i));
                            }*/
                        }

                        throw e;
                    }
                }).toEither();
                if (res.isLeft()) {
                    if(res.getLeft() instanceof Error) {
                        Error e = (Error) res.getLeft();
                        System.out.println("got error: " + e);
                        JsonElement err_json = e.toJson();
                        assertEquals(expected_result.get("Err"), err_json);
                    } else {
                        throw res.getLeft();
                    }
                } else {
                    assertEquals(expected_result.getAsJsonPrimitive("Ok").getAsLong(), res.get());
                }
            }
        });
    }


    class Block {
        List<String> symbols;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        String code;

        public List<String> getSymbols() {
            return symbols;
        }

        public void setSymbols(List<String> symbols) {
            this.symbols = symbols;
        }
    }

    class Token {
        List<Block> blocks;

        public List<Block> getBlocks() {
            return blocks;
        }

        public void setBlocks(List<Block> blocks) {
            this.blocks = blocks;
        }
    }

    class TestCase {
        String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        String filename;
        List<Token> tokens;
        JsonElement validations;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public List<Token> getTokens() {
            return tokens;
        }

        public void setTokens(List<Token> tokens) {
            this.tokens = tokens;
        }

        public JsonElement getValidations() {
            return validations;
        }

        public void setValidations(JsonElement validations) {
            this.validations = validations;
        }
    }

    class Sample {
        String root_private_key;

        public String getRoot_public_key() {
            return root_public_key;
        }

        public void setRoot_public_key(String root_public_key) {
            this.root_public_key = root_public_key;
        }

        String root_public_key;
        List<TestCase> testcases;

        public String getRoot_private_key() {
            return root_private_key;
        }

        public void setRoot_private_key(String root_private_key) {
            this.root_private_key = root_private_key;
        }

        public List<TestCase> getTestcases() {
            return testcases;
        }

        public void setTestcases(List<TestCase> testcases) {
            this.testcases = testcases;
        }
    }

    class World {
        List<String> facts;
        List<String> rules;
        List<String> checks;
        List<String> policies;

        public World(List<String> facts, List<String> rules, List<String> checks, List<String> policies) {
            this.facts = facts;
            this.rules = rules;
            this.checks = checks;
            this.policies = policies;
        }

        @Override
        public String toString() {
            return "World{\n" +
                    "facts=" + facts +
                    ",\nrules=" + rules +
                    ",\nchecks=" + checks +
                    ",\npolicies=" + policies +
                    '}';
        }
    }
}
