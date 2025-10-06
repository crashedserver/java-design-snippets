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

### 2. Fixed Window Rate Limiter

A rate limiter is a stability and fairness pattern that prevents a client from overwhelming a service. This implementation uses the **Fixed Window Counter** algorithm.

*   **Implementation:** `FixedWindowRateLimiter.java`
*   **Description:** A simple, thread-safe implementation of the Fixed Window rate-limiting strategy. It's easy to understand but has limitations regarding traffic bursts at window boundaries.

#### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run the test to see the state transitions in action:

```bash
javac system_design/resiliency/FixedWindowRateLimiter.java system_design/resiliency/FixedWindowRateLimiterTest.java 
java system_design.resiliency.FixedWindowRateLimiterTest
```

### 3. Token Bucket Rate Limiter

A rate limiter is a stability and fairness pattern that prevents a client from overwhelming a service. This implementation uses the **Token Bucket** algorithm.

*   **Implementation:** `TokenBucketRateLimiter.java`
*   **Description:** A simple, thread-safe implementation of the Token Bucket rate-limiting strategy. It's easy to understand and addresses the challenges of the fixed window rate limiting  regarding traffic bursts at window boundaries.

#### How to Run the Test

From the root `java-design-snippets` directory, you can compile and run the test to see the state transitions in action:

```bash
javac system_design/resiliency/TokenBucketRateLimiter.java system_design/resiliency/TokenBucketRateLimiterTest.java 
java system_design.resiliency.TokenBucketRateLimiterTest
```

### 4. Retry with Exponential Backoff 
A retry mechanism is a resiliency pattern that allows a client to automatically re-attempt an operation that has failed, typically due to transient issues like network glitches or temporary service unavailability. 
This implementation uses an exponential backoff strategy, where the wait time between retries increases exponentially. This prevents a failed system from being overwhelmed by many retries. 
*  **Implementation:** RetryWithExponentialBackOff.java 
*  **Description:** A straightforward implementation that wraps an IOperation. It calculates delays using initialDelay * (2 ^ retryCount) and includes an optional "full jitter" strategy to further distribute the retry attempts from multiple clients by selecting a random wait time between 0 and the calculated delay. 

#### How to Run the Test 

From the root `java-design-snippets` directory, you can compile and run the test to see the state transitions in action:

```bash 
javac system_design/resiliency/RetryWithExponentialBackOff.java system_design/resiliency/RetryWithExponentialBackOffTest.java 
java system_design.resiliency.RetryWithExponentialBackOffTest

---

## Future Components (Coming Soon)

This section will be expanded with more resiliency patterns, including:

*   **Rate Limiter:** Control the rate of traffic sent to a service (e.g., using Token Bucket or Leaky Bucket algorithms).
*   **Load Shedding:** Proactively drop requests when a system is under heavy load to maintain stability for the remaining traffic.
*   **Timeout Handler:** Enforce time limits on operations to prevent threads from being blocked indefinitely.
