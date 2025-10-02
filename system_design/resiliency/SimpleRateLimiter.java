package system_design.resiliency;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Using ConcurrentHashMap for thread-safe access per client.

public class SimpleRateLimiter implements ISimpleRateLimiter {
    private final int maxRequests;
    private final long windowSizeInMillis;
    private final Map<String, Window> clientWindows = new ConcurrentHashMap<>();

    public SimpleRateLimiter(int maxRequests, long windowSizeInMillis) {
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