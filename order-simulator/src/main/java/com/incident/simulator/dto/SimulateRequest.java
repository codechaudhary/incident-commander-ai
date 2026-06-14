package com.incident.simulator.dto;

public class SimulateRequest {
    private FailureType failureType = FailureType.NONE;
    private int delayMs = 0;

    public SimulateRequest() {}

    public FailureType getFailureType() {
        return failureType;
    }

    public void setFailureType(FailureType failureType) {
        this.failureType = failureType;
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }
}
