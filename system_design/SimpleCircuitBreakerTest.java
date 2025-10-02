package system_design;

public class SimpleCircuitBreakerTest {
    public static void main(String[] args) throws InterruptedException {
        // Configure a circuit breaker with a failure threshold of 3 and a 5-second reset timeout.
        int failureThreshold = 3;
        long resetTimeoutMillis = 5000;
        SimpleCircuitBreaker breaker = new SimpleCircuitBreaker(failureThreshold, resetTimeoutMillis);

        System.out.println("--- Circuit Breaker Demonstration ---");
        System.out.println("Initial State: " + breaker.getState());

        // --- Scenario 1: Tripping the Circuit Breaker ---
        System.out.println("\n1. Simulating failures to trip the circuit...");
        for (int i = 0; i < failureThreshold; i++) {
            System.out.println("   Request allowed: " + breaker.allowRequest());
            breaker.recordFailure();
            System.out.println("   Recorded failure. Current state: " + breaker.getState());
        }

        // The circuit should now be OPEN
        System.out.println("\nCircuit should be OPEN. Let's test it.");
        System.out.println("   Request allowed: " + breaker.allowRequest()); // Should be false
        System.out.println("   Current state: " + breaker.getState());

        // --- Scenario 2: Resetting to HALF_OPEN and then CLOSED ---
        System.out.println("\n2. Waiting for reset timeout (" + resetTimeoutMillis / 1000 + " seconds)...");
        Thread.sleep(resetTimeoutMillis + 100); // Wait for the timeout to expire

        System.out.println("\nTimeout passed. Breaker should move to HALF_OPEN on next request.");
        System.out.println("   Request allowed (trial request): " + breaker.allowRequest()); // Should be true
        System.out.println("   Current state: " + breaker.getState());

        System.out.println("\nSimulating a successful call in HALF_OPEN state.");
        breaker.recordSuccess();
        System.out.println("   Recorded success. Current state: " + breaker.getState()); // Should be CLOSED

        System.out.println("\nCircuit is CLOSED. Requests should flow normally.");
        System.out.println("   Request allowed: " + breaker.allowRequest());

        // --- Scenario 3: Resetting to HALF_OPEN and then back to OPEN ---
        System.out.println("\n3. Tripping the circuit again...");
        for (int i = 0; i < failureThreshold; i++) {
            breaker.recordFailure();
        }
        System.out.println("   Circuit tripped. Current state: " + breaker.getState());
        System.out.println("   Request allowed: " + breaker.allowRequest());

        System.out.println("\nWaiting for reset timeout again...");
        Thread.sleep(resetTimeoutMillis + 100);

        System.out.println("\nTimeout passed. Breaker should move to HALF_OPEN on next request.");
        System.out.println("   Request allowed (trial request): " + breaker.allowRequest());
        System.out.println("   Current state: " + breaker.getState());

        System.out.println("\nSimulating a FAILED call in HALF_OPEN state.");
        breaker.recordFailure();
        System.out.println("   Recorded failure. Current state: " + breaker.getState()); // Should be OPEN again

        System.out.println("\nCircuit is OPEN again. Requests should be blocked.");
        System.out.println("   Request allowed: " + breaker.allowRequest());

        System.out.println("\n--- Test Complete ---");
    }
}
