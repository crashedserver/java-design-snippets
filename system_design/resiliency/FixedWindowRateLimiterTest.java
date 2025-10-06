/*
 * Copyright 2024 crashedserver
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

public class FixedWindowRateLimiterTest {
    public static void main(String[] args) throws InterruptedException {
        // Configure a rate limiter: 5 requests per 10 seconds.
        int maxRequests = 5;
        long windowSizeInMillis = 10000;
        ISimpleRateLimiter rateLimiter = new FixedWindowRateLimiter(maxRequests, windowSizeInMillis);

        System.out.println("--- Fixed Window Rate Limiter Demonstration ---");
        System.out
                .println("Configuration: " + maxRequests + " requests per " + windowSizeInMillis / 1000 + " seconds.");

        // --- Scenario 1: Exceeding the limit for a client ---
        String client1 = "user-A";
        System.out.println("\n1. Simulating requests for client '" + client1 + "' to hit the limit...");
        for (int i = 0; i < maxRequests + 2; i++) {
            boolean allowed = rateLimiter.allowRequest(client1);
            System.out.println("   Request " + (i + 1) + " for " + client1 + ": " + (allowed ? "Allowed" : "Denied"));
        }

        // --- Scenario 2: A different client is not affected ---
        String client2 = "user-B";
        System.out.println(
                "\n2. Simulating a request for a different client '" + client2 + "' to show they are independent.");
        boolean allowedForClient2 = rateLimiter.allowRequest(client2);
        System.out.println("   Request for " + client2 + ": " + (allowedForClient2 ? "Allowed" : "Denied"));

        // --- Scenario 3: Waiting for the window to reset ---
        System.out
                .println("\n3. Waiting for the time window to reset (" + (windowSizeInMillis / 1000) + " seconds)...");
        Thread.sleep(windowSizeInMillis);

        System.out.println("\nWindow has reset for '" + client1 + "'. New requests should be allowed.");
        boolean allowedAfterReset = rateLimiter.allowRequest(client1);
        System.out.println("   Request for " + client1 + " after reset: " + (allowedAfterReset ? "Allowed" : "Denied"));

        System.out.println("\n--- Test Complete ---");
    }
}
