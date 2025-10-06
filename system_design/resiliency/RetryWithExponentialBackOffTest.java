/*
 * Copyright 2025 crashedserver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package system_design.resiliency;

public final class RetryWithExponentialBackOffTest {

    private RetryWithExponentialBackOffTest() {
        // Preventing instantiation
    }

    public static void main(String[] args) throws InterruptedException {
        RetryWithExponentialBackOff retryer = new RetryWithExponentialBackOff();

        // Configuration
        long initialDelayMillis = 100; // 100 ms
        int maxRetries = 3; // Will attempt 1 initial + 3 retries = 4 total attempts
        long maxWaitInMillis = 5000; // A general max wait time for most tests

        System.out.println("--- Retry with Exponential Backoff Demonstration ---");
        System.out.println(
                "Configuration: Initial Delay=" + initialDelayMillis + "ms, Max Retries=" + maxRetries + ", Max Wait="
                        + maxWaitInMillis + "ms");

        // --- Scenario 1: Operation succeeds on the first try ---
        System.out.println("\n1. Testing an operation that succeeds immediately...");
        MockOperation successFirstTry = new MockOperation(1); // Succeeds on the 1st attempt
        boolean result1 = retryer.retry(successFirstTry, initialDelayMillis, maxRetries, false, maxWaitInMillis);
        System.out.println("   >> Final Result: " + (result1 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + successFirstTry.getAttempts());

        // --- Scenario 2: Operation fails twice, then succeeds ---
        System.out.println("\n2. Testing an operation that fails twice then succeeds...");
        MockOperation successOnThirdTry = new MockOperation(3); // Succeeds on the 3rd attempt
        boolean result2 = retryer.retry(successOnThirdTry, initialDelayMillis, maxRetries, false, maxWaitInMillis);
        System.out.println("   >> Final Result: " + (result2 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + successOnThirdTry.getAttempts());

        // --- Scenario 3: Operation fails all attempts (no jitter) ---
        System.out.println("\n3. Testing an operation that always fails (no jitter)...");
        System.out.println("   Expected delays: 100ms, 200ms, 400ms");
        MockOperation alwaysFailsNoJitter = new MockOperation(Integer.MAX_VALUE);
        long startTime3 = System.currentTimeMillis();
        boolean result3 = retryer.retry(alwaysFailsNoJitter, initialDelayMillis, maxRetries, false, maxWaitInMillis);
        long duration3 = System.currentTimeMillis() - startTime3;
        System.out.println("   >> Final Result: " + (result3 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + alwaysFailsNoJitter.getAttempts());
        System.out.println("   >> Total time elapsed: " + duration3 + "ms (approx 100+200+400=700ms)");

        // --- Scenario 4: Operation fails all attempts (with full jitter) ---
        System.out.println("\n4. Testing an operation that always fails (with FULL JITTER)...");
        System.out.println("   Expected delays: random(0-100), random(0-200), random(0-400)");
        MockOperation alwaysFailsWithJitter = new MockOperation(Integer.MAX_VALUE);
        long startTime4 = System.currentTimeMillis();
        boolean result4 = retryer.retry(alwaysFailsWithJitter, initialDelayMillis, maxRetries, true, maxWaitInMillis);
        long duration4 = System.currentTimeMillis() - startTime4;
        System.out.println("   >> Final Result: " + (result4 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + alwaysFailsWithJitter.getAttempts());
        System.out.println("   >> Total time elapsed: " + duration4 + "ms (should be less than 700ms on average)");

        // --- Scenario 5: Operation fails due to max wait time ---
        System.out.println("\n5. Testing an operation that hits the max wait time limit...");
        long shortMaxWait = 350; // 350ms. Should allow 1st retry (100ms) and 2nd retry (200ms), but not the 3rd.
        System.out.println("   Configuration: Max Wait=" + shortMaxWait + "ms");
        System.out.println("   Expected delays: 100ms, 200ms. Should stop before the 400ms delay.");
        MockOperation alwaysFailsForTimeout = new MockOperation(Integer.MAX_VALUE);
        long startTime5 = System.currentTimeMillis();
        boolean result5 = retryer.retry(alwaysFailsForTimeout, initialDelayMillis, maxRetries, false, shortMaxWait);
        long duration5 = System.currentTimeMillis() - startTime5;
        System.out.println("   >> Final Result: " + (result5 ? "Success" : "Failure"));
        System.out.println("   >> Total attempts: " + alwaysFailsForTimeout.getAttempts());
        System.out.println("   >> Total time elapsed: " + duration5 + "ms (approx 100+200=300ms)");

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