package com.clevercloud.biscuit.error;

public class FailedCaveat {
    public class FailedBlock extends FailedCaveat {
        final public long block_id;
        final public long caveat_id;
        final public String rule;

        public FailedBlock(long block_id, long caveat_id, String rule) {
            this.block_id = block_id;
            this.caveat_id = caveat_id;
            this.rule = rule;
        }
    }

    public class FailedVerifier extends FailedCaveat {
        final public long caveat_id;
        final public String rule;

        public FailedVerifier(long caveat_id, String rule) {
            this.caveat_id = caveat_id;
            this.rule = rule;
        }
    }
}