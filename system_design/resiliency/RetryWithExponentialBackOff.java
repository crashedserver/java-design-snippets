package system_design.resiliency;

import java.util.concurrent.ThreadLocalRandom;

public class RetryWithExponentialBackOff implements IRetryWithExponentialBackOff {
    private static final long MULTIPLIER = 2;

    @Override
    public boolean retry(IOperation operation, long initialDelayMillis, int maxRetries, boolean useJitter)
            throws InterruptedException {
        int retryCount = 0;
        while (retryCount < maxRetries) {
            if (operation.execute()) {
                return true; // Success
            }
            long delay = calculateDelay(initialDelayMillis, retryCount, useJitter);
            System.out.println("Operation failed. Retrying in " + delay + "ms... (Attempt " + (retryCount + 1) + ")");
            Thread.sleep(delay);
            retryCount++;
        }

        // Final attempt
        System.out.println("Last attempt... (Attempt " + (retryCount + 1) + ")");
        return operation.execute();
    }

    private long calculateDelay(long initialDelayMillis, int retryCount, boolean useJitter) {
        // Calculate delay using exponential backoff: initialDelay * (MULTIPLIER ^
        // retryCount)
        long exponentialDelay = initialDelayMillis * (long) Math.pow(MULTIPLIER, retryCount);

        if (useJitter) {
            // This implementation uses the "Full Jitter" strategy.
            // It selects a random delay between 0 and the calculated exponential backoff
            // time.
            // This maximally spreads out retry attempts from multiple clients, preventing
            // a "thundering herd" where clients retry in clumps.
            // See:
            // https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
            return ThreadLocalRandom.current().nextLong(exponentialDelay + 1);
        } else {
            return exponentialDelay;
        }
    }
}

interface IOperation {
    boolean execute();
}

interface IRetryWithExponentialBackOff {
    boolean retry(IOperation operation, long initialDelayMillis, int maxRetries, boolean useJitter)
            throws InterruptedException;
}