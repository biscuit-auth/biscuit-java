package com.clevercloud.biscuit.datalog;

import java.time.Duration;

public class RunLimits {
    public int maxFacts = 1000;
    public int maxIterations = 100;
    public Duration maxTime = Duration.ofMillis(10);
}
