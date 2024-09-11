package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.Rule;
import org.biscuitsec.biscuit.datalog.RunLimits;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.builder.Check;
import org.biscuitsec.biscuit.token.builder.parser.Parser;
import org.biscuitsec.biscuit.token.format.SignedBlock;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static org.biscuitsec.biscuit.token.Block.from_bytes;
import static org.junit.jupiter.api.Assertions.*;

class SamplesTest {
    final RunLimits runLimits = new RunLimits(500, 100, Duration.ofMillis(500));

    @TestFactory
    Stream<DynamicTest> jsonTest() {
        Sample sample = null;
        // Using try-with-resources block automatically closes InputStream after try-catch block.
        try (InputStream inputStream = currentThread().getContextClassLoader().getResourceAsStream("samples/samples.json")) {
            sample = new Gson().fromJson(new InputStreamReader(
                            new BufferedInputStream(requireNonNull(inputStream, "InputStream resource cannot be found"))),
                    Sample.class);
        } catch (IOException e) {
            fail(e);
        }
        PublicKey publicKey = new PublicKey(Schema.PublicKey.Algorithm.Ed25519, sample.root_public_key);
        KeyPair keyPair = new KeyPair(sample.root_private_key);
        return sample.testcases.stream().map(t -> processTestcase(t, publicKey, keyPair));
    }

    private void compareBlocks(KeyPair root, List<Block> sampleBlocks, Biscuit token) throws Error {
        assertEquals(sampleBlocks.size(), 1 + token.blocks.size());
        Option<Biscuit> sampleToken = Option.none();
        Biscuit b = compareBlock(root, sampleToken, 0, sampleBlocks.get(0), token.authority, token.symbols);
        sampleToken = Option.some(b);

        for (int i = 0; i < token.blocks.size(); i++) {
            b = compareBlock(root, sampleToken, i + 1, sampleBlocks.get(i + 1), token.blocks.get(i), token.symbols);
            sampleToken = Option.some(b);
        }
    }

    private Biscuit compareBlock(KeyPair root, Option<Biscuit> sampleToken, long sampleBlockIndex, Block sampleBlock, org.biscuitsec.biscuit.token.Block tokenBlock, SymbolTable tokenSymbols) throws Error {
        String sampleDatalog = sampleBlock.getCode().replace("\"", "\\\"");

        Either<Map<Integer, List<org.biscuitsec.biscuit.token.builder.parser.Error>>, org.biscuitsec.biscuit.token.builder.Block> outputSample = Parser.datalog(sampleBlockIndex, sampleDatalog);

        // the invalid block rule with unbound variable cannot be parsed
        if (outputSample.isLeft()) {
            return sampleToken.get();
        }

        Biscuit newSampleToken;
        if (!sampleToken.isDefined()) {
            org.biscuitsec.biscuit.token.builder.Biscuit builder = new org.biscuitsec.biscuit.token.builder.Biscuit(new SecureRandom(), root, Option.none(), outputSample.get());
            newSampleToken = builder.build();
        } else {
            Biscuit s = sampleToken.get();
            newSampleToken = s.attenuate(outputSample.get());
        }

        org.biscuitsec.biscuit.token.Block generatedSampleBlock;
        if (!sampleToken.isDefined()) {
            generatedSampleBlock = newSampleToken.authority;
        } else {
            generatedSampleBlock = newSampleToken.blocks.get((int) sampleBlockIndex - 1);
        }

        System.out.println("generated block: ");
        System.out.println(generatedSampleBlock.print(newSampleToken.symbols));
        System.out.println("deserialized block: ");
        System.out.println(tokenBlock.print(newSampleToken.symbols));

        SymbolTable generatedBlockSymbols = newSampleToken.symbols;
        assertEquals(generatedSampleBlock.printCode(generatedBlockSymbols), tokenBlock.printCode(tokenSymbols));

        /* FIXME: to generate the same sample block, we need the samples to provide the external private key
        assertEquals(generatedSampleBlock, tokenBlock);
        assertArrayEquals(generatedSampleBlock.to_bytes().get(), tokenBlock.to_bytes().get());
        */

        return newSampleToken;
    }

    private DynamicTest processTestcase(final TestCase testCase, final PublicKey publicKey, final KeyPair privateKey) {
        return DynamicTest.dynamicTest(testCase.title + ": " + testCase.filename, () -> {
            System.out.println("Testcase name: \"" + testCase.title + "\"");
            System.out.println("filename: \"" + testCase.filename + "\"");
            InputStream inputStream = currentThread().getContextClassLoader().getResourceAsStream("samples/" + testCase.filename);
            byte[] data = new byte[requireNonNull(inputStream, "InputStream cannot be null").available()];

            for (Map.Entry<String, JsonElement> validationEntry : testCase.validations.getAsJsonObject().entrySet()) {
                String validationName = validationEntry.getKey();
                JsonObject validation = validationEntry.getValue().getAsJsonObject();

                JsonObject expected_result = validation.getAsJsonObject("result");
                String[] authorizer_facts = validation.getAsJsonPrimitive("authorizer_code").getAsString().split(";");
                Either<Throwable, Long> res = Try.of(() -> {
                    int ignoreNumBytesRead = inputStream.read(data);
                    Biscuit token = Biscuit.from_bytes(data, publicKey);
                    assertArrayEquals(token.serialize(), data);

                    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                    List<org.biscuitsec.biscuit.token.Block> allBlocks = new ArrayList<>();
                    allBlocks.add(token.authority);
                    allBlocks.addAll(token.blocks);

                    compareBlocks(privateKey, testCase.token, token);

                    byte[] ser_block_authority = token.authority.to_bytes().get();
                    System.out.println(Arrays.toString(ser_block_authority));
                    System.out.println(Arrays.toString(token.serializedBiscuit.authority.block));
                    org.biscuitsec.biscuit.token.Block deser_block_authority = from_bytes(ser_block_authority, token.authority.externalKey).get();
                    assertEquals(token.authority.print(token.symbols), deser_block_authority.print(token.symbols));
                    assert (Arrays.equals(ser_block_authority, token.serializedBiscuit.authority.block));

                    for (int i = 0; i < token.blocks.size() - 1; i++) {
                        org.biscuitsec.biscuit.token.Block block = token.blocks.get(i);
                        SignedBlock signed_block = token.serializedBiscuit.blocks.get(i);
                        byte[] ser_block = block.to_bytes().get();
                        org.biscuitsec.biscuit.token.Block deser_block = from_bytes(ser_block, block.externalKey).get();
                        assertEquals(block.print(token.symbols), deser_block.print(token.symbols));
                        assert (Arrays.equals(ser_block, signed_block.block));
                    }

                    List<RevocationIdentifier> revocationIds = token.revocation_identifiers();
                    JsonArray validationRevocationIds = validation.getAsJsonArray("revocation_ids");
                    assertEquals(revocationIds.size(), validationRevocationIds.size());
                    for (int i = 0; i < revocationIds.size(); i++) {
                        assertEquals(validationRevocationIds.get(i).getAsString(), revocationIds.get(i).toHex());
                    }

                    // TODO Add check of the token

                    Authorizer authorizer = token.authorizer();
                    System.out.println(token.print());
                    for (String f : authorizer_facts) {
                        f = f.trim();
                        if (!f.isEmpty()) {
                            if (f.startsWith("check if") || f.startsWith("check all")) {
                                authorizer.add_check(f);
                            } else if (f.startsWith("allow if") || f.startsWith("deny if")) {
                                authorizer.add_policy(f);
                            } else if (!f.startsWith("revocation_id")) {
                                authorizer.add_fact(f);
                            }
                        }
                    }
                    System.out.println(authorizer.print_world());
                    try {
                        Long authorizeResult = authorizer.authorize(runLimits);

                        if (validation.has("world") && !validation.get("world").isJsonNull()) {
                            World world = new Gson().fromJson(validation.get("world").getAsJsonObject(), World.class);
                            world.fixOrigin();

                            World authorizerWorld = new World(authorizer);
                            assertEquals(world.factMap(), authorizerWorld.factMap());
                            assertEquals(world.rules, authorizerWorld.rules);
                            assertEquals(world.checks, authorizerWorld.checks);
                            assertEquals(world.policies, authorizerWorld.policies);
                        }

                        return authorizeResult;
                    } catch (Exception e) {

                        if (validation.has("world") && !validation.get("world").isJsonNull()) {
                            World world = new Gson().fromJson(validation.get("world").getAsJsonObject(), World.class);
                            world.fixOrigin();

                            World authorizerWorld = new World(authorizer);
                            assertEquals(world.factMap(), authorizerWorld.factMap());
                            assertEquals(world.rules, authorizerWorld.rules);
                            assertEquals(world.checks, authorizerWorld.checks);
                            assertEquals(world.policies, authorizerWorld.policies);
                        }

                        throw e;
                    }
                }).toEither();

                if (expected_result.has("Ok")) {
                    if (res.isLeft()) {
                        System.out.println("validation '" + validationName + "' expected result Ok(" + expected_result.getAsJsonPrimitive("Ok").getAsLong() + "), got error");
                        throw res.getLeft();
                    } else {
                        assertEquals(expected_result.getAsJsonPrimitive("Ok").getAsLong(), res.get());
                    }
                } else {
                    if (res.isLeft()) {
                        if (res.getLeft() instanceof Error) {
                            Error e = (Error) res.getLeft();
                            System.out.println("validation '" + validationName + "' got error: " + e);
                            JsonElement err_json = e.toJson();
                            assertEquals(expected_result.get("Err"), err_json);
                        } else {
                            throw res.getLeft();
                        }
                    } else {
                        throw new Exception("validation '" + validationName + "' expected result error(" + expected_result.get("Err") + "), got success: " + res.get());
                    }
                }
            }
        });
    }

    private static class Block {
        String code;

        public String getCode() {
            return code;
        }
    }

    private static class TestCase {
        String title;
        String filename;
        List<Block> token;
        JsonElement validations;
    }

    private static class Sample {
        String root_private_key;
        String root_public_key;
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        List<TestCase> testcases;
    }

    private static class World {
        final List<FactSet> facts;
        final List<RuleSet> rules;
        final List<CheckSet> checks;
        final List<String> policies;

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
                ArrayList<String> facts = entry.getValue().stream()
                        .map(f -> authorizer.symbols.print_fact(f))
                        .sorted()
                        .collect(Collectors.toCollection(ArrayList::new));
                return new FactSet(origin, facts);
            }).collect(Collectors.toList());

            HashMap<Long, List<String>> rules = new HashMap<>();
            for (List<Tuple2<Long, Rule>> l : authorizer.rules().rules.values()) {
                for (Tuple2<Long, Rule> t : l) {
                    if (!rules.containsKey(t._1)) {
                        rules.put(t._1, new ArrayList<>());
                    }
                    rules.get(t._1).add(authorizer.symbols.print_rule(t._2));
                }
            }
            for (Map.Entry<Long, List<String>> entry : rules.entrySet()) {
                Collections.sort(entry.getValue());
            }

            this.rules = rules.entrySet().stream()
                    .map(entry -> new RuleSet(entry.getKey(), entry.getValue()))
                    .sorted()
                    .collect(Collectors.toList());

            this.checks = authorizer.checks().stream()
                    .map((Tuple2<Long, List<Check>> t) -> {
                        List<String> checks1 = t._2.stream()
                                .map(Check::toString)
                                .sorted()
                                .collect(Collectors.toList());
                        if (t._1 == null) {
                            return new CheckSet(checks1);
                        } else {
                            return new CheckSet(t._1, checks1);
                        }
                    }).collect(Collectors.toList());
            this.policies = authorizer.policies().stream().map(Policy::toString).collect(Collectors.toList());
            Collections.sort(this.rules);
            Collections.sort(this.checks);
        }

        public void fixOrigin() {
            for (FactSet f : this.facts) {
                f.fixOrigin();
            }
            for (RuleSet r : this.rules) {
                r.fixOrigin();
            }
            Collections.sort(this.rules);
            for (CheckSet c : this.checks) {
                c.fixOrigin();
            }
            Collections.sort(this.checks);
        }

        public HashMap<List<Long>, List<String>> factMap() {
            HashMap<List<Long>, List<String>> worldFacts = new HashMap<>();
            for (FactSet f : this.facts) {
                worldFacts.put(f.origin, f.facts);
            }

            return worldFacts;
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

    private static class FactSet {
        final List<Long> origin;
        final List<String> facts;

        public FactSet(List<Long> origin, List<String> facts) {
            this.origin = origin;
            this.facts = facts;
        }

        // JSON cannot represent Long.MAX_VALUE so it is stored as null, fix the origin list
        public void fixOrigin() {
            for (int i = 0; i < this.origin.size(); i++) {
                if (this.origin.get(i) == null) {
                    this.origin.set(i, Long.MAX_VALUE);
                }
            }
            Collections.sort(this.origin);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FactSet factSet = (FactSet) o;

            if (!Objects.equals(origin, factSet.origin)) return false;
            return Objects.equals(facts, factSet.facts);
        }

        @Override
        public int hashCode() {
            int result = origin != null ? origin.hashCode() : 0;
            result = 31 * result + (facts != null ? facts.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "FactSet{" +
                    "origin=" + origin +
                    ", facts=" + facts +
                    '}';
        }
    }

    private static class RuleSet implements Comparable<RuleSet> {
        Long origin;
        final List<String> rules;

        public RuleSet(Long origin, List<String> rules) {
            this.origin = origin;
            this.rules = rules;
        }

        public void fixOrigin() {
            if (this.origin == null || this.origin == -1) {
                this.origin = Long.MAX_VALUE;
            }
        }

        @Override
        public int compareTo(RuleSet ruleSet) {
            // we only compare origin to sort the list of rulesets
            // there's only one of each origin so we don't need to compare the list of rules
            if (this.origin == null) {
                return -1;
            } else if (ruleSet.origin == null) {
                return 1;
            } else {
                return this.origin.compareTo(ruleSet.origin);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RuleSet ruleSet = (RuleSet) o;

            if (!Objects.equals(origin, ruleSet.origin)) return false;
            return Objects.equals(rules, ruleSet.rules);
        }

        @Override
        public int hashCode() {
            int result = origin != null ? origin.hashCode() : 0;
            result = 31 * result + (rules != null ? rules.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "RuleSet{" +
                    "origin=" + origin +
                    ", rules=" + rules +
                    '}';
        }
    }

    private static class CheckSet implements Comparable<CheckSet> {
        Long origin;
        final List<String> checks;

        public CheckSet(Long origin, List<String> checks) {
            this.origin = origin;
            this.checks = checks;
        }

        public CheckSet(List<String> checks) {
            this.origin = null;
            this.checks = checks;
        }

        public void fixOrigin() {
            if (this.origin == null || this.origin == -1) {
                this.origin = Long.MAX_VALUE;
            }
        }

        @Override
        public int compareTo(CheckSet checkSet) {
            // we only compare origin to sort the list of checksets
            // there's only one of each origin so we don't need to compare the list of rules
            if (this.origin == null) {
                return -1;
            } else if (checkSet.origin == null) {
                return 1;
            } else {
                return this.origin.compareTo(checkSet.origin);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CheckSet checkSet = (CheckSet) o;

            if (!Objects.equals(origin, checkSet.origin)) return false;
            return Objects.equals(checks, checkSet.checks);
        }

        @Override
        public int hashCode() {
            int result = origin != null ? origin.hashCode() : 0;
            result = 31 * result + (checks != null ? checks.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "CheckSet{" +
                    "origin=" + origin +
                    ", checks=" + checks +
                    '}';
        }
    }
}
