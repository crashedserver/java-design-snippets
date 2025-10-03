package system_design.resiliency;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class RetryWithExponentialBackOff implements IRetryWithExponentialBackOff {
    private static final long MULTIPLIER = 2;

    @Override
    public boolean retry(IOperation operation, long initialDelayMillis, int maxRetries, boolean useJitter,
            long maxWaitInMillis)
            throws InterruptedException {

        int retryCount = 0;
        // Use a monotonic clock for measuring elapsed time to avoid issues with system
        // time changes.
        long startTimeNanos = System.nanoTime();

        while (true) {
            if (operation.execute()) {
                return true; // Success
            }

            if (retryCount >= maxRetries) {
                System.out.println("Operation failed after reaching max retries (" + maxRetries + ").");
                return false; // Failure: max retries reached
            }

            long elapsedNanos = System.nanoTime() - startTimeNanos;
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
            if (elapsedMillis >= maxWaitInMillis) {
                System.out.println("Operation failed after reaching max wait time (" + maxWaitInMillis + "ms).");
                return false; // Failure: max wait time reached
            }

            long delay = calculateDelay(initialDelayMillis, retryCount, useJitter, maxWaitInMillis - elapsedMillis);
            System.out.println("Operation failed. Retrying in " + delay + "ms... (Attempt " + (retryCount + 1) + ")");
            Thread.sleep(delay);
            retryCount++;
        }
    }

    private long calculateDelay(long initialDelayMillis, int retryCount, boolean useJitter, long remainingTime) {
        // Calculate delay using exponential backoff: initialDelay * (MULTIPLIER ^
        // retryCount)
        long exponentialDelay = initialDelayMillis * (long) Math.pow(MULTIPLIER, retryCount);
        long delay;
        if (useJitter) {
            // This implementation uses the "Full Jitter" strategy.
            // It selects a random delay between 0 and the calculated exponential backoff
            // time. This maximally spreads out retry attempts from multiple clients.
            // See:
            // https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
            delay = ThreadLocalRandom.current().nextLong(exponentialDelay + 1);
        } else {
            delay = exponentialDelay;
        }

        // Ensure the delay does not exceed the remaining time before the
        // maxWaitInMillis deadline.
        return Math.min(delay, remainingTime);
    }
}

interface IOperation {
    boolean execute();
}

interface IRetryWithExponentialBackOff {
    boolean retry(IOperation operation, long initialDelayMillis, int maxRetries, boolean useJitter,
            long maxWaitInMillis)
            throws InterruptedException;
}