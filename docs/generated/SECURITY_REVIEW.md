# SECURITY REVIEW

This document details the security posture of the Incident Commander AI codebase based on static analysis.

## 1. Authentication & Authorization
> [!CAUTION]
> **COMPLETELY MISSING:** The entire platform has zero authentication.
> Despite the `README.md` claiming a JWT-based `auth-service` with Spring Security, there is absolutely no `spring-boot-starter-security` dependency in any `pom.xml`. Every REST endpoint across `bff-service`, `trace-service`, `alert-service`, and `ai-analytics-service` is fully unauthenticated and publicly accessible to anyone who can hit the IP.

## 2. API Gateway & CORS
- **BFF Service (`CorsConfig.java`):**
  ```java
  config.addAllowedOriginPattern("*");
  config.addAllowedHeader("*");
  config.addAllowedMethod("*");
  ```
  **Risk Level: HIGH.** The `bff-service` uses wildcard CORS configurations, meaning any website can mount the frontend SPA and execute cross-origin requests against the BFF.

## 3. Data Privacy & Secrets Management
- **LLM Data Exfiltration:** The `ai-analytics-service` sends raw trace payloads directly to Anthropic's Claude API. There is no Data Loss Prevention (DLP) or PII scrubbing mechanism in place. If an `ErrorSpan` contains customer PII or sensitive database queries, it is transmitted externally in plain text.
- **Secrets Management:** Secrets like `ANTHROPIC_API_KEY` and database credentials are provided via plain environment variables (`.env`). No integration with secure enclaves (Azure Key Vault, AWS KMS) exists.

## 4. Input Validation & Injection
- **SQL Injection:** Protected. All services use ORMs (Spring Data JPA, SQLAlchemy) with parameterized queries, preventing classical SQL injection.
- **REST Validation:** Spring Boot `@Valid` is missing on most `@RequestBody` controllers, meaning malformed JSON could cause `NullPointerException`s or 500 internal server errors, though it won't execute arbitrary code.

## 5. Dependency Vulnerabilities
A quick scan of `frontend/package-lock.json` reveals explicitly documented vulnerable dependencies:
- **Next.js:** Version contains a security vulnerability (`https://nextjs.org/blog/security-update-2025-12-11`).
- **Glob:** Old version with "widely publicized security vulnerabilities."
