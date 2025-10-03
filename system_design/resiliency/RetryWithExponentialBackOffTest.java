package system_design.resiliency;

public class RetryWithExponentialBackOffTest {

    public static void main(String[] args) throws InterruptedException {
        RetryWithExponentialBackOff retryer = new RetryWithExponentialBackOff();

        // Configuration
        long baseDelay = 100; // 100 ms
        int maxRetries = 3; // Will attempt 1 initial + 3 retries = 4 total attempts

        System.out.println("--- Retry with Exponential Backoff Demonstration ---");
        System.out.println("Configuration: Base Delay=" + baseDelay + "ms, Max Retries=" + maxRetries);

        // --- Scenario 1: Operation succeeds on the first try ---
        System.out.println("\n1. Testing an operation that succeeds immediately...");
        MockOperation successFirstTry = new MockOperation(1); // Succeeds on the 1st attempt
        boolean result1 = retryer.retry(successFirstTry, baseDelay, maxRetries, false);
        System.out.println("   >> Final Result: " + (result1 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + successFirstTry.getAttempts());

        // --- Scenario 2: Operation fails twice, then succeeds ---
        System.out.println("\n2. Testing an operation that fails twice then succeeds...");
        MockOperation successOnThirdTry = new MockOperation(3); // Succeeds on the 3rd attempt
        boolean result2 = retryer.retry(successOnThirdTry, baseDelay, maxRetries, false);
        System.out.println("   >> Final Result: " + (result2 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + successOnThirdTry.getAttempts());

        // --- Scenario 3: Operation fails all attempts (no jitter) ---
        System.out.println("\n3. Testing an operation that always fails (no jitter)...");
        System.out.println("   Expected delays: 100ms, 200ms, 400ms");
        MockOperation alwaysFailsNoJitter = new MockOperation(Integer.MAX_VALUE); // Always fails
        long startTime3 = System.currentTimeMillis();
        boolean result3 = retryer.retry(alwaysFailsNoJitter, baseDelay, maxRetries, false);
        long duration3 = System.currentTimeMillis() - startTime3;
        System.out.println("   >> Final Result: " + (result3 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + alwaysFailsNoJitter.getAttempts());
        System.out.println("   >> Total time elapsed: " + duration3 + "ms (approx 100+200+400=700ms)");

        // --- Scenario 4: Operation fails all attempts (with jitter) ---
        System.out.println("\n4. Testing an operation that always fails (with FULL JITTER)...");
        System.out.println("   Expected delays: random(0-100), random(0-200), random(0-400)");
        MockOperation alwaysFailsWithJitter = new MockOperation(Integer.MAX_VALUE); // Always fails
        long startTime4 = System.currentTimeMillis();
        boolean result4 = retryer.retry(alwaysFailsWithJitter, baseDelay, maxRetries, true);
        long duration4 = System.currentTimeMillis() - startTime4;
        System.out.println("   >> Final Result: " + (result4 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + alwaysFailsWithJitter.getAttempts());
        System.out.println("   >> Total time elapsed: " + duration4 + "ms (should be less than 700ms on average)");

        System.out.println("\n--- Test Complete ---");
    }

    /**
     * A mock IOperation that can be configured to succeed after a certain number of
     * attempts.
     */
    static class MockOperation implements IOperation {
        private final int succeedOnAttempt;
        private int currentAttempt = 0;

        /**
         * @param succeedOnAttempt The attempt number on which execute() should return
         *                         true.
         *                         (e.g., 1 for immediate success, 3 for success on the
         *                         third try).
         */
        public MockOperation(int succeedOnAttempt) {
            this.succeedOnAttempt = succeedOnAttempt;
        }

        @Override
        public boolean execute() {
            currentAttempt++;
            if (currentAttempt >= succeedOnAttempt) {
                System.out.println("   MockOperation: Success on attempt " + currentAttempt);
                return true;
            } else {
                System.out.println("   MockOperation: Failure on attempt " + currentAttempt);
                return false;
            }
        }

        public int getAttempts() {
            return currentAttempt;
        }
    }
}