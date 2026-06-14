package com.incident.simulator.dto;

public class SimulateResponse {
    private String orderId;
    private String traceId;
    private FailureType failureType;
    private String status;
    private String message;

    public SimulateResponse() {}

    public SimulateResponse(String orderId, String traceId, FailureType failureType, String status, String message) {
        this.orderId = orderId;
        this.traceId = traceId;
        this.failureType = failureType;
        this.status = status;
        this.message = message;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public FailureType getFailureType() { return failureType; }
    public void setFailureType(FailureType failureType) { this.failureType = failureType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
