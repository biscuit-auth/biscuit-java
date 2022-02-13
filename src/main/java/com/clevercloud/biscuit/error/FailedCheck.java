package com.clevercloud.biscuit.error;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;

public class FailedCheck {

    public JsonElement toJson(){ return new JsonObject();}

    public static class FailedBlock extends FailedCheck {
        final public long block_id;
        final public long check_id;
        final public String rule;

        public FailedBlock(long block_id, long check_id, String rule) {
            this.block_id = block_id;
            this.check_id = check_id;
            this.rule = rule;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedBlock b = (FailedBlock) o;
            return block_id == b.block_id && check_id == b.check_id && rule.equals(b.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(block_id, check_id, rule);
        }

        @Override
        public String toString() {
            return "Block(FailedBlockCheck " + new Gson().toJson(toJson())+")";
        }

        @Override
        public JsonElement toJson() {
            JsonObject jo = new JsonObject();
            jo.addProperty("block_id", block_id);
            jo.addProperty("check_id", check_id);
            jo.addProperty("rule", rule);
            JsonObject block = new JsonObject();
            block.add("Block", jo);
            return jo;
        }
    }

    public static class FailedAuthorizer extends FailedCheck {
        final public long check_id;
        final public String rule;

        public FailedAuthorizer(long check_id, String rule) {
            this.check_id = check_id;
            this.rule = rule;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedAuthorizer b = (FailedAuthorizer) o;
            return check_id == b.check_id && rule.equals(b.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(check_id, rule);
        }

        @Override
        public String toString() {
            return "FailedCaveat.FailedAuthorizer { check_id: "+check_id+
                    ", rule: "+rule+" }";
        }

        @Override
        public JsonElement toJson() {
            JsonObject jo = new JsonObject();
            jo.addProperty("check_id", check_id);
            jo.addProperty("rule", rule);
            JsonObject authorizer = new JsonObject();
            authorizer.add("Authorizer", jo);
            return authorizer;
        }
    }
}