package org.biscuitsec.biscuit.datalog;

import java.time.Duration;

public class RunLimits {
    public final int maxFacts;
    public final int maxIterations;
    public final Duration maxTime;

    public RunLimits() {
        this(1000, 100, Duration.ofMillis(5));
    }

    public RunLimits(int maxFacts, int maxIterations, Duration maxTime) {
        this.maxFacts = maxFacts;
        this.maxIterations = maxIterations;
        this.maxTime = maxTime;
    }
}
