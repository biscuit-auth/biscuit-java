package com.clevercloud.biscuit.error;

import java.util.Objects;

public class FailedCheck {
    public static class FailedBlock extends FailedCheck {
        final public long block_id;
        final public long caveat_id;
        final public String rule;

        public FailedBlock(long block_id, long caveat_id, String rule) {
            this.block_id = block_id;
            this.caveat_id = caveat_id;
            this.rule = rule;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedBlock b = (FailedBlock) o;
            return block_id == b.block_id && caveat_id == b.caveat_id && rule.equals(b.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(block_id, caveat_id, rule);
        }

        @Override
        public String toString() {
            return "FailedCheck.FailedBlock { block_id: " + block_id + ", caveat_id: "+caveat_id+
                    ", rule: "+rule+" }";
        }
    }

    public static class FailedVerifier extends FailedCheck {
        final public long caveat_id;
        final public String rule;

        public FailedVerifier(long caveat_id, String rule) {
            this.caveat_id = caveat_id;
            this.rule = rule;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedVerifier b = (FailedVerifier) o;
            return caveat_id == b.caveat_id && rule.equals(b.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(caveat_id, rule);
        }

        @Override
        public String toString() {
            return "FailedCaveat.FailedVerifier { caveat_id: "+caveat_id+
                    ", rule: "+rule+" }";
        }
    }
}