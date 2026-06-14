# INTERVIEW RED FLAGS (BULLSHIT DETECTOR)

If a candidate claims they worked on this codebase, here is how you catch them lying. If they state any of the following as "implemented facts", they are hallucinating or only read the `README.md` without looking at the code.

## 🚩 Red Flag 1: The "Z-Score" Lie
- **Candidate Claim:** "I built the anomaly detection using statistical Z-score baselines over a 1-hour rolling window."
- **How to catch them:** Ask them what topic the `anomaly-detection-service` publishes to. If they say `anomaly.detected`, they are lying. The codebase has no such service, and alerts are hardcoded to trigger on literal `status == "ERROR"` string matches in the `TraceEventConsumer.java`.

## 🚩 Red Flag 2: The "Vector Database" Lie
- **Candidate Claim:** "I used pgvector and RAG to find similar past incidents."
- **How to catch them:** Ask them to show you the `pgvector` migration script or the `rag-service` directory. Neither exists. The LLM only receives the immediate span tree via `root_cause_prompt.py`.

## 🚩 Red Flag 3: The "Security" Lie
- **Candidate Claim:** "I secured the BFF using Spring Security and JWT tokens."
- **How to catch them:** Ask them to point to the `WebSecurityConfigurerAdapter` or `SecurityFilterChain`. It does not exist. There is not a single security dependency in the entire repository.

## 🚩 Red Flag 4: The "Resilience" Lie
- **Candidate Claim:** "We used Resilience4j to wrap the Anthropic API calls with a Circuit Breaker."
- **How to catch them:** Ask them how the state machine was configured. If they talk about half-open states, they are lying. The `AiClient.java` only uses a simple `.onErrorResume(e -> Mono.empty())` Reactor fallback.

## 🚩 Red Flag 5: The "Testing" Lie
- **Candidate Claim:** "We had strict test-driven development (TDD) for the Trace Ingestion pipeline."
- **How to catch them:** Ask them what mocking framework they used for the `trace-service`. If they answer "Mockito", they are lying. The `trace-service` literally does not have a `src/test` directory.
