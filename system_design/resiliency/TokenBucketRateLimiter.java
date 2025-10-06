// Copyright 2025 crashedserver
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package system_design.resiliency;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple, thread-safe rate limiter implementation using the **Token Bucket**
 * algorithm.
 *
 * <h3>Behavior</h3>
 * This algorithm uses the metaphor of a bucket that holds tokens.
 * <ul>
 * <li>The bucket has a maximum capacity (`maxTokenBucketSize`).</li>
 * <li>Tokens are added to the bucket at a fixed rate
 * (`refillRateTokensSec`).</li>
 * <li>When a request arrives, it must consume one token from the bucket to be
 * allowed.</li>
 * <li>If the bucket is empty, the request is denied.</li>
 * </ul>
 * This approach smooths out traffic bursts, as a burst of requests can consume
 * all available tokens, after which requests are limited to the refill rate.
 */
public class TokenBucketRateLimiter implements ITokenBucketRateLimiter {

    private final long maxTokenBucketSize;
    private final long refillRateTokensSec;
    private final Map<String, Bucket> clientRateInfoMap;

    public TokenBucketRateLimiter(long maxTokenBucketSize, long refillRateTokensSec) {
        this.maxTokenBucketSize = maxTokenBucketSize;
        this.refillRateTokensSec = refillRateTokensSec;
        this.clientRateInfoMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allowRequest(String clientId) {
        // Get the bucket for the client, or create a new one if it's their first
        // request.
        Bucket bucket = clientRateInfoMap.computeIfAbsent(clientId,
                k -> new Bucket(maxTokenBucketSize, System.currentTimeMillis()));

        // Synchronize on the specific client's bucket object to handle concurrent
        // requests safely.
        synchronized (bucket) {
            long currentTime = Instant.now().toEpochMilli();
            long elapsedTime = currentTime - bucket.lastRefillTimestamp;

            // Calculate how many new tokens to add based on the elapsed time.
            long tokensToAdd = elapsedTime * refillRateTokensSec / 1000;
            if (tokensToAdd > 0) {
                bucket.tokens = Math.min(maxTokenBucketSize, bucket.tokens + tokensToAdd);
                bucket.lastRefillTimestamp = currentTime;
            }

            // If there's at least one token, allow the request and consume a token.
            if (bucket.tokens >= 1) {
                bucket.tokens--;
                return true;
            }

            // Otherwise, deny the request.
            return false;
        }
    }

}

interface ITokenBucketRateLimiter {
    boolean allowRequest(String clientId);
}

/**
 * Inner class to hold the state for a single client's token bucket.
 */
class Bucket {
    long tokens;
    long lastRefillTimestamp;

    public Bucket(long initialTokens, long lastRefillTimestamp) {
        this.tokens = initialTokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }
}