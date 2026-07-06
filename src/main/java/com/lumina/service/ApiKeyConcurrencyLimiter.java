package com.lumina.service;

import com.lumina.exception.ApiKeyConcurrencyLimitExceededException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
public class ApiKeyConcurrencyLimiter {
    private final ConcurrentHashMap<String, LimiterState> limiters = new ConcurrentHashMap<>();

    public Permit tryAcquire(String apiKey, long maxConcurrentRequests) {
        if (!StringUtils.hasText(apiKey) || maxConcurrentRequests <= 0) {
            return new Permit(null, null);
        }
        if (maxConcurrentRequests > Integer.MAX_VALUE) {
            return new Permit(null, null);
        }

        LimiterState state = limiters.compute(apiKey, (key, current) -> {
            int limit = Math.toIntExact(maxConcurrentRequests);
            if (current == null || current.limit != limit) {
                return new LimiterState(limit);
            }
            return current;
        });

        if (!state.semaphore.tryAcquire()) {
            throw new ApiKeyConcurrencyLimitExceededException("API key concurrent request limit exceeded");
        }
        return new Permit(apiKey, state);
    }

    private void release(String apiKey, LimiterState state) {
        state.semaphore.release();
        if (state.semaphore.availablePermits() == state.limit && !state.semaphore.hasQueuedThreads()) {
            limiters.remove(apiKey, state);
        }
    }

    private static final class LimiterState {
        private final int limit;
        private final Semaphore semaphore;

        private LimiterState(int limit) {
            this.limit = limit;
            this.semaphore = new Semaphore(limit);
        }
    }

    public final class Permit {
        private final String apiKey;
        private final LimiterState state;
        private boolean released;

        private Permit(String apiKey, LimiterState state) {
            this.apiKey = apiKey;
            this.state = state;
        }

        public synchronized void release() {
            if (released || state == null) {
                return;
            }
            released = true;
            ApiKeyConcurrencyLimiter.this.release(apiKey, state);
        }
    }
}
