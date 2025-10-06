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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Using ConcurrentHashMap for thread-safe access per client.

/**
 * A simple, thread-safe rate limiter implementation using the **Fixed Window
 * Counter** algorithm.
 *
 * <h3>Behavior</h3>
 * This algorithm divides time into fixed-size windows (e.g., 10 seconds). For
 * each client, it maintains a counter for the number of requests made within
 * the current window.
 * <ul>
 * <li>When a request arrives, the limiter checks if the current window has
 * expired. If so, it resets the counter and starts a new window.</li>
 * <li>It then checks if the request count for the current window is below the
 * configured maximum.</li>
 * <li>If the count is below the limit, the request is allowed, and the counter
 * is incremented. Otherwise, it is denied.</li>
 * </ul>
 *
 * <h3>Limitation: Burstiness at Window Boundaries</h3>
 * The primary drawback of this simple algorithm is that it can allow a burst of
 * traffic that is double the configured rate. For example, if the limit is 5
 * requests per 10 seconds, a client could make 5 requests at the very end of a
 * window (e.g., at second 9.9) and another 5 requests at the very beginning of
 * the next window (e.g., at second 10.1). This results in 10 requests being
 * processed in a very short period, which may overwhelm a downstream service.
 * More advanced algorithms like Token Bucket or Sliding Window Log address this
 * issue.
 */
public class FixedWindowRateLimiter implements ISimpleRateLimiter {
    private final int maxRequests;
    private final long windowSizeInMillis;
    private final Map<String, Window> clientWindows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequests, long windowSizeInMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
    }

    @Override
    public boolean allowRequest(String clientId) {
        long currentTime = Instant.now().toEpochMilli();
        // Get the window for the client, or create a new one if it's their first
        // request.
        Window clientWindow = clientWindows.computeIfAbsent(clientId, k -> new Window(currentTime));

        // Synchronize on the specific client's window object to handle concurrent
        // requests for the same client.
        synchronized (clientWindow) {
            // Check if the current window has expired.
            if (currentTime - clientWindow.startTime > windowSizeInMillis) {
                // If so, reset the window.
                clientWindow.startTime = currentTime;
                clientWindow.requestCount = 0;
            }

            // Check if the client has exceeded their request limit.
            if (clientWindow.requestCount < maxRequests) {
                clientWindow.requestCount++;
                return true; // Request is allowed.
            }

            return false; // Request is denied.
        }
    }

    /**
     * Inner class to hold the state for a single client's time window.
     */
    private static class Window {
        long startTime;
        int requestCount;

        public Window(long startTime) {
            this.startTime = startTime;
            this.requestCount = 0;
        }
    }
}

interface ISimpleRateLimiter {
    boolean allowRequest(String clientId);
}