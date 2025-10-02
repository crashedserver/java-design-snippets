package system_design;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleCircuitBreaker implements ICircuitBreaker {

    private final int failureThreshold;
    private final long resetTimeoutMillis;

    private final ReentrantLock lock = new ReentrantLock();
    private State currentState;
    private int failureCount;
    private long lastStateChangeTime;

    public SimpleCircuitBreaker(int failureThreshold, long resetTimeoutMillis) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMillis = resetTimeoutMillis;
        this.currentState = State.CLOSED;
        this.failureCount = 0;
        this.lastStateChangeTime = Instant.now().toEpochMilli();
    }

    @Override
    public boolean allowRequest() {
        lock.lock();
        try {
            if (currentState == State.OPEN) {
                // Check if the reset timeout has passed
                if (Instant.now().toEpochMilli() - lastStateChangeTime >= resetTimeoutMillis) {
                    // If so, move to HALF_OPEN to allow a trial request
                    transitionTo(State.HALF_OPEN);
                    return true;
                }
                // Otherwise, the circuit is still open, block the request
                return false;
            }
            // Allow requests in CLOSED and HALF_OPEN states
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void recordSuccess() {
        lock.lock();
        try {
            if (currentState == State.HALF_OPEN) {
                // If a trial request succeeds, close the circuit
                transitionTo(State.CLOSED);
            }
            // Reset failure count on any success in a CLOSED state
            failureCount = 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void recordFailure() {
        lock.lock();
        try {
            if (currentState == State.HALF_OPEN) {
                // If the trial request fails, re-open the circuit immediately
                transitionTo(State.OPEN);
            } else {
                failureCount++;
                if (failureCount >= failureThreshold) {
                    // If failure threshold is reached, open the circuit
                    transitionTo(State.OPEN);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public State getState() {
        // To ensure the state is up-to-date, we trigger the timeout check here as well.
        allowRequest();
        return currentState;
    }

    private void transitionTo(State newState) {
        this.currentState = newState;
        this.failureCount = 0;
        this.lastStateChangeTime = Instant.now().toEpochMilli();
    }
}

interface ICircuitBreaker {
    boolean allowRequest(); // Should the call be attempted?

    void recordSuccess(); // Inform the CB that the call succeeded

    void recordFailure(); // Inform the CB that the call failed

    State getState(); // Optional: Expose current state for observability

    enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}