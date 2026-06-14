package com.incident.simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.incident.simulator.client.TraceClient;
import com.incident.simulator.dto.FailureType;
import com.incident.simulator.dto.SimulateRequest;
import com.incident.simulator.dto.SimulateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class SimulatorService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);
    private final TraceClient traceClient;
    private final ObjectMapper mapper;

    public SimulatorService(TraceClient traceClient, ObjectMapper mapper) {
        this.traceClient = traceClient;
        this.mapper = mapper;
    }

    public Mono<SimulateResponse> simulateOrder(SimulateRequest request) {
        String orderId = UUID.randomUUID().toString();
        String traceId = generateHex(32);
        String rootSpanId = generateHex(16);
        String childSpanId = generateHex(16);

        long startNanos = System.currentTimeMillis() * 1_000_000L;
        long defaultDurationNanos = 200_000_000L; // 200ms default
        long addedDelayNanos = request.getDelayMs() * 1_000_000L;
        long endNanos = startNanos + defaultDurationNanos + addedDelayNanos;
        
        int statusCode = 1; // OK
        int httpStatus = 200;
        String errorMessage = "OK";
        
        if (request.getFailureType() != FailureType.NONE) {
            statusCode = 2; // ERROR
            httpStatus = request.getFailureType() == FailureType.SLOW_RESPONSE ? 504 : 500;
            errorMessage = request.getFailureType() == FailureType.DB_TIMEOUT ? "DB timeout after " + request.getDelayMs() + "ms" : 
                           (request.getFailureType() == FailureType.SLOW_RESPONSE ? "Gateway Timeout: Slow response" : "RuntimeException occurred");
        }

        ObjectNode tracePayload = generateOtlpJson(traceId, rootSpanId, childSpanId, startNanos, endNanos, statusCode, httpStatus, errorMessage, request.getFailureType());

        return traceClient.sendTrace(tracePayload)
                .thenReturn(new SimulateResponse(
                        orderId,
                        traceId,
                        request.getFailureType(),
                        "SIMULATED",
                        "Order simulation triggered. Trace will appear in dashboard within 2-3 seconds."
                ));
    }

    private ObjectNode generateOtlpJson(String traceId, String rootSpanId, String childSpanId, long startNanos, long endNanos, int statusCode, int httpStatus, String errorMessage, FailureType failureType) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode resourceSpans = root.putArray("resourceSpans");
        
        ObjectNode resourceSpan = resourceSpans.addObject();
        ObjectNode resource = resourceSpan.putObject("resource");
        ArrayNode resourceAttrs = resource.putArray("attributes");
        addStringAttr(resourceAttrs, "service.name", "order-service");
        
        ArrayNode scopeSpans = resourceSpan.putArray("scopeSpans");
        ObjectNode scopeSpan = scopeSpans.addObject();
        ObjectNode scope = scopeSpan.putObject("scope");
        scope.put("name", "order-simulator");
        scope.put("version", "1.0.0");
        
        ArrayNode spans = scopeSpan.putArray("spans");
        
        // Root Span
        ObjectNode rootSpanNode = spans.addObject();
        rootSpanNode.put("traceId", traceId);
        rootSpanNode.put("spanId", rootSpanId);
        rootSpanNode.putNull("parentSpanId");
        rootSpanNode.put("name", "POST /orders");
        rootSpanNode.put("kind", 2); // SERVER
        rootSpanNode.put("startTimeUnixNano", String.valueOf(startNanos));
        rootSpanNode.put("endTimeUnixNano", String.valueOf(endNanos));
        
        ObjectNode rootStatus = rootSpanNode.putObject("status");
        rootStatus.put("code", statusCode);
        if (statusCode == 2) rootStatus.put("message", errorMessage);
        
        ArrayNode rootAttrs = rootSpanNode.putArray("attributes");
        addStringAttr(rootAttrs, "http.method", "POST");
        addIntAttr(rootAttrs, "http.status_code", httpStatus);
        addStringAttr(rootAttrs, "failure.type", failureType.name());
        addBoolAttr(rootAttrs, "failure.injected", true);
        
        // Child Span
        ObjectNode childSpanNode = spans.addObject();
        childSpanNode.put("traceId", traceId);
        childSpanNode.put("spanId", childSpanId);
        childSpanNode.put("parentSpanId", rootSpanId);
        childSpanNode.put("name", "POST /charge");
        childSpanNode.put("kind", 3); // CLIENT
        childSpanNode.put("startTimeUnixNano", String.valueOf(startNanos + 50_000_000L));
        childSpanNode.put("endTimeUnixNano", String.valueOf(endNanos - 50_000_000L));
        
        ObjectNode childStatus = childSpanNode.putObject("status");
        childStatus.put("code", statusCode);
        if (statusCode == 2) childStatus.put("message", errorMessage);
        
        ArrayNode childAttrs = childSpanNode.putArray("attributes");
        addStringAttr(childAttrs, "http.method", "POST");
        addStringAttr(childAttrs, "http.url", "http://payment-service/charge");
        addIntAttr(childAttrs, "http.status_code", httpStatus);
        addStringAttr(childAttrs, "failure.type", failureType.name());
        
        if (statusCode == 2) {
            ArrayNode events = childSpanNode.putArray("events");
            ObjectNode event = events.addObject();
            event.put("timeUnixNano", String.valueOf(endNanos - 60_000_000L));
            event.put("name", "exception");
            ArrayNode eventAttrs = event.putArray("attributes");
            addStringAttr(eventAttrs, "exception.message", errorMessage);
        } else {
            childSpanNode.putArray("events"); // empty array
        }
        
        return root;
    }

    private void addStringAttr(ArrayNode attrs, String key, String value) {
        ObjectNode attr = attrs.addObject();
        attr.put("key", key);
        attr.putObject("value").put("stringValue", value);
    }

    private void addIntAttr(ArrayNode attrs, String key, int value) {
        ObjectNode attr = attrs.addObject();
        attr.put("key", key);
        attr.putObject("value").put("intValue", value);
    }

    private void addBoolAttr(ArrayNode attrs, String key, boolean value) {
        ObjectNode attr = attrs.addObject();
        attr.put("key", key);
        attr.putObject("value").put("boolValue", value);
    }

    private String generateHex(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append(UUID.randomUUID().toString().replace("-", ""));
        }
        return sb.substring(0, length);
    }
}
