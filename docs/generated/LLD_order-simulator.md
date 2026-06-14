# LOW LEVEL DESIGN: order-simulator

## 1. Class Architecture & Component Interaction

### Controllers
- **`SimulatorController.java`**
  - `@PostMapping("/api/v1/simulate")`: Accepts `SimulationRequest` containing the desired `FailureType`. Delegates to `SimulatorService`.

### Core Services
- **`SimulatorService.java`**
  - Maps `FailureType` (e.g., NONE, TIMEOUT, DB_CONNECTION_ERROR) to specific payload generation strategies.
  - Calls `TraceGenerator.generate(...)` to create synthetic data.
  - Uses `TraceClient` to blockingly send the generated `OtlpTraceRequest` to the `trace-service`.

### Utilities
- **`TraceGenerator.java`**
  - Utility class responsible for faking realistic UUIDs, Timestamps, and Service structures.
  - Generates spans mimicking an `api-gateway` -> `order-service` -> `inventory-service` -> `database` flow.
  - Injects specific artificial error messages based on the requested failure type.

### Clients
- **`TraceClient.java`**
  - Basic `WebClient` setup to POST JSON data to `http://trace-service:8081/api/v1/traces`.
