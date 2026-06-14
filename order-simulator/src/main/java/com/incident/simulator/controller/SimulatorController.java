package com.incident.simulator.controller;

import com.incident.simulator.dto.SimulateRequest;
import com.incident.simulator.dto.SimulateResponse;
import com.incident.simulator.service.SimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Order Simulator API", description = "Simulates incoming orders and failures")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping(value = "/simulate", consumes = "application/json", produces = "application/json")
    @Operation(summary = "Simulate Order", description = "Triggers a simulated order that generates a trace.")
    public Mono<SimulateResponse> simulateOrder(@RequestBody(required = false) SimulateRequest request) {
        if (request == null) {
            request = new SimulateRequest(); // defaults
        }
        return simulatorService.simulateOrder(request);
    }
}
