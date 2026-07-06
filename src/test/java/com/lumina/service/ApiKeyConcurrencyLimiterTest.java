package com.lumina.service;

import com.lumina.exception.ApiKeyConcurrencyLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiKeyConcurrencyLimiterTest {

    @Test
    void enforcesConfiguredConcurrentRequestLimit() {
        ApiKeyConcurrencyLimiter limiter = new ApiKeyConcurrencyLimiter();

        ApiKeyConcurrencyLimiter.Permit first = limiter.tryAcquire("sk-test", 1);

        assertThrows(ApiKeyConcurrencyLimitExceededException.class, () -> limiter.tryAcquire("sk-test", 1));

        first.release();

        ApiKeyConcurrencyLimiter.Permit second = assertDoesNotThrow(() -> limiter.tryAcquire("sk-test", 1));
        second.release();
    }

    @Test
    void zeroLimitMeansUnlimited() {
        ApiKeyConcurrencyLimiter limiter = new ApiKeyConcurrencyLimiter();

        ApiKeyConcurrencyLimiter.Permit first = limiter.tryAcquire("sk-test", 0);
        ApiKeyConcurrencyLimiter.Permit second = limiter.tryAcquire("sk-test", 0);

        first.release();
        second.release();
    }
}
