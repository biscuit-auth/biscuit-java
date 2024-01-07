package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import com.google.gson.*;
import com.google.protobuf.MapEntry;
import io.vavr.Tuple2;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.Rule;
import org.biscuitsec.biscuit.datalog.RunLimits;
import org.biscuitsec.biscuit.datalog.TrustedOrigins;
import org.biscuitsec.biscuit.error.Error;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.biscuitsec.biscuit.token.builder.Check;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.*;
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

                            World authorizerWorld = new World(authorizer);

                            //Collections.sort(world.facts);
                            /*Collections.sort(world.rules);
                            Collections.sort(world.checks);
                            Collections.sort(world.policies);*/
                            //Collections.sort(authorizerWorld.facts);
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
                            World authorizerWorld = new World(authorizer);

                            //Collections.sort(world.facts);
                            //Collections.sort(authorizerWorld.facts);
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

                if(expected_result.has("Ok")) {
                    if (res.isLeft()) {
                        System.out.println("validation '"+validationName+"' expected result Ok("+expected_result.getAsJsonPrimitive("Ok").getAsLong()+"), got error");
                        throw res.getLeft();
                    } else {
                        assertEquals(expected_result.getAsJsonPrimitive("Ok").getAsLong(), res.get());
                    }
                } else {
                    if (res.isLeft()) {
                        if(res.getLeft() instanceof Error) {
                            Error e = (Error) res.getLeft();
                            System.out.println("validation '"+validationName+"' got error: " + e);
                            JsonElement err_json = e.toJson();
                            assertEquals(expected_result.get("Err"), err_json);
                        } else {
                            throw res.getLeft();
                        }
                    } else {
                        throw new Exception("validation '"+validationName+"' expected result error("+expected_result.get("Err")+"), got success: "+res.get());
                    }
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
        List<FactSet> facts;
        List<RuleSet> rules;
        List<CheckSet> checks;
        List<String> policies;

        public World(List<FactSet> facts, List<RuleSet> rules, List<CheckSet> checks, List<String> policies) {
            this.facts = facts;
            this.rules = rules;
            this.checks = checks;
            this.policies = policies;
        }

        public World(Authorizer authorizer) {
            this.facts = authorizer.facts().facts().entrySet().stream().map(entry -> {
                        ArrayList<Long> origin = new ArrayList<>(entry.getKey().inner);
                        Collections.sort(origin);
                        ArrayList<String> facts = new ArrayList<>(entry.getValue().stream()
                                .map(r -> r.toString()).collect(Collectors.toList()));
                        Collections.sort(facts);

                        return new FactSet(origin, facts);
                    }).collect(Collectors.toList());

            HashMap<Long, List<String>> rules = new HashMap<>();
            for(List<Tuple2<Long, Rule>> l: authorizer.rules().rules.values()) {
                for(Tuple2<Long, Rule> t: l) {
                    if (!rules.containsKey(t._1)) {
                        rules.put(t._1, new ArrayList<>());
                    }
                    rules.get(t._1).add(authorizer.symbols.print_rule(t._2));
                }
            }
            for(Map.Entry<Long, List<String>> entry: rules.entrySet()) {
                Collections.sort(entry.getValue());
            }
            List<RuleSet> rulesets = rules.entrySet().stream()
                    .map(entry -> new RuleSet(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            Collections.sort(rulesets);

            this.rules = rulesets;

            List<CheckSet> checksets = authorizer.checks().stream()
                    .map((Tuple2<Long, List<Check>> t) -> {
                        List<String> checks = t._2.stream().map(c -> c.toString()).collect(Collectors.toList());
                        Collections.sort(checks);
                        if(t._1 == null) {
                            return new CheckSet(checks);
                        } else {
                            return new CheckSet(t._1, checks);
                        }
                    }).collect(Collectors.toList());

            this.checks = checksets;
            this.policies = authorizer.policies().stream().map(p -> p.toString()).collect(Collectors.toList());
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

    class FactSet {
        List<Long> origin;
        List<String> facts;

        public FactSet(List<Long> origin, List<String> facts) {
            this.origin = origin;
            this.facts = facts;
        }
    }

    class RuleSet implements Comparable<RuleSet> {
        Long origin;
        List<String> rules;

        public RuleSet(Long origin, List<String> rules) {
            this.origin = origin;
            this.rules = rules;
        }

        @Override
        public int compareTo(RuleSet ruleSet) {
            // we only compare origin to sort the list of rulesets
            // there's only one of each origin so we don't need to compare the list of rules
            if(this.origin == null) {
                return -1;
            } else if (ruleSet.origin == null) {
                return 1;
            } else {
                return this.origin.compareTo(ruleSet.origin);
            }
        }
    }

    class CheckSet implements Comparable<RuleSet> {
        Long origin;
        List<String> checks;

        public CheckSet(Long origin, List<String> checks) {
            this.origin = origin;
            this.checks = checks;
        }

        public CheckSet(List<String> checks) {
            this.origin = null;
            this.checks = checks;
        }

        @Override
        public int compareTo(RuleSet ruleSet) {
            // we only compare origin to sort the list of checksets
            // there's only one of each origin so we don't need to compare the list of rules
            if(this.origin == null) {
                return -1;
            } else if (ruleSet.origin == null) {
                return 1;
            } else {
                return this.origin.compareTo(ruleSet.origin);
            }
        }
    }

}
