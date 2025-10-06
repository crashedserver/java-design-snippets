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

public final class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiterTest() {
        // Preventing instantiation
    }

    public static void main(String[] args) throws InterruptedException {
        // Configure a rate limiter: Bucket size of 10, refills at 2 tokens/sec.
        long maxTokens = 10;
        long refillRate = 2; // tokens per second
        ITokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(maxTokens, refillRate);

        System.out.println("--- Token Bucket Rate Limiter Demonstration ---");
        System.out.println("Configuration: Bucket Size=" + maxTokens + ", Refill Rate=" + refillRate + " tokens/sec.");

        String client1 = "user-A";

        // --- Scenario 1: Initial Burst ---
        System.out.println("\n1. Simulating an initial burst of requests for client '" + client1 + "'...");
        // The first 10 requests should be allowed immediately as the bucket starts
        // full.
        for (int i = 0; i < maxTokens; i++) {
            boolean allowed = rateLimiter.allowRequest(client1);
            System.out.println("   Burst Request " + (i + 1) + ": " + (allowed ? "Allowed" : "Denied"));
        }

        // --- Scenario 2: Exceeding the limit ---
        System.out.println("\n2. Bucket is now empty. The next request should be denied.");
        boolean deniedRequest = rateLimiter.allowRequest(client1);
        System.out.println("   Request immediately after burst: " + (deniedRequest ? "Allowed" : "Denied"));

        // --- Scenario 3: Waiting for tokens to refill ---
        // We need to wait for tokens to be added. At 2 tokens/sec, waiting 3 seconds
        // should add 6 tokens.
        long waitTime = 3000;
        System.out.println("\n3. Waiting for " + waitTime / 1000 + " seconds for the bucket to refill...");
        Thread.sleep(waitTime);

        // --- Scenario 4: Consuming refilled tokens ---
        System.out.println("\n4. Bucket has refilled. Simulating new requests...");
        for (int i = 0; i < maxTokens; i++) {
            boolean allowed = rateLimiter.allowRequest(client1);
            System.out.println("   Request " + (i + 1) + " after refill: " + (allowed ? "Allowed" : "Denied"));
        }

        System.out.println("\n--- Test Complete ---");
    }
}