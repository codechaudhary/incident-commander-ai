package com.incident.bff.dto;

import java.time.Instant;

public class DashboardSummaryResponse {
    private long totalTraces;
    private long errorTraces;
    private long openAlerts;
    private long criticalAlerts;
    private long completedAnalyses;
    private long avgTraceDurationMs;
    private Instant lastUpdated;

    public DashboardSummaryResponse() {}

    public long getTotalTraces() { return totalTraces; }
    public void setTotalTraces(long totalTraces) { this.totalTraces = totalTraces; }
    public long getErrorTraces() { return errorTraces; }
    public void setErrorTraces(long errorTraces) { this.errorTraces = errorTraces; }
    public long getOpenAlerts() { return openAlerts; }
    public void setOpenAlerts(long openAlerts) { this.openAlerts = openAlerts; }
    public long getCriticalAlerts() { return criticalAlerts; }
    public void setCriticalAlerts(long criticalAlerts) { this.criticalAlerts = criticalAlerts; }
    public long getCompletedAnalyses() { return completedAnalyses; }
    public void setCompletedAnalyses(long completedAnalyses) { this.completedAnalyses = completedAnalyses; }
    public long getAvgTraceDurationMs() { return avgTraceDurationMs; }
    public void setAvgTraceDurationMs(long avgTraceDurationMs) { this.avgTraceDurationMs = avgTraceDurationMs; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
