# System Design Components

This directory contains practical Java implementations of common components used in building resilient and scalable distributed systems. Each component is designed to be a clear, concise example for learning and interview preparation.

## Components

### 1. Simple Circuit Breaker

A circuit breaker is a stability pattern that prevents a client from repeatedly trying to call a service that is likely to fail, allowing the failing service time to recover.

*   **Implementation:** `SimpleCircuitBreaker.java`
*   **Description:** A straightforward, thread-safe implementation of the circuit breaker pattern that transitions between `CLOSED`, `OPEN`, and `HALF_OPEN` states based on failure thresholds and timeouts.

#### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run the test to see the state transitions in action:

```bash
javac system_design/resiliency/SimpleCircuitBreaker.java system_design/resiliency/SimpleCircuitBreakerTest.java
java system_design.resiliency.SimpleCircuitBreakerTest
```

### 2. Simple rate Limiter

A rate limiter is a stability and fairness pattern that prevents a client from repeatedly trying to call a service that is likely to fail or can potentially use teh service unfairly with respect to other clients. This rate limiter is a very basic rate limiter implementation for learning purpose.

*   **Implementation:** `SimplerateLimiter.java`
*   **Description:** A simple threadsafe implementation of the rate limiting pattern that based on a fixed window rate limiting startegy.

#### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run the test to see the state transitions in action:

```bash
javac system_design/resiliency/SimpleRateLimiter.java system_design/resiliency/SimpleRateLimiterTest.java
java system_design.resiliency.SimpleRateLimiterTest
```

---

## Future Components (Coming Soon)

This section will be expanded with more resiliency patterns, including:

*   **Rate Limiter:** Control the rate of traffic sent to a service (e.g., using Token Bucket or Leaky Bucket algorithms).
*   **Load Shedding:** Proactively drop requests when a system is under heavy load to maintain stability for the remaining traffic.
*   **Retry Mechanism:** Automatically retry failed operations with configurable strategies like exponential backoff.
*   **Timeout Handler:** Enforce time limits on operations to prevent threads from being blocked indefinitely.
