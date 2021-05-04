package com.clevercloud.biscuit.datalog;

import java.time.Duration;

public class RunLimits {
    public int maxFacts = 1000;
    public int maxIterations = 100;
    public Duration maxTime = Duration.ofMillis(5);

    public RunLimits() {
    }

    public RunLimits(int maxFacts, int maxIterations, Duration maxTime) {
        this.maxFacts = maxFacts;
        this.maxIterations = maxIterations;
        this.maxTime = maxTime;
    }
}
