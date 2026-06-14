package com.incident.bff.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class IncidentViewResponse {
    private JsonNode trace;
    private JsonNode alert;
    private JsonNode analysis;

    public IncidentViewResponse() {}

    public IncidentViewResponse(JsonNode trace, JsonNode alert, JsonNode analysis) {
        this.trace = trace;
        this.alert = alert;
        this.analysis = analysis;
    }

    public JsonNode getTrace() { return trace; }
    public void setTrace(JsonNode trace) { this.trace = trace; }
    public JsonNode getAlert() { return alert; }
    public void setAlert(JsonNode alert) { this.alert = alert; }
    public JsonNode getAnalysis() { return analysis; }
    public void setAnalysis(JsonNode analysis) { this.analysis = analysis; }
}
