package com.example.demo.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * Separate component so {@link CacheEvict} runs through the Spring proxy (no self-invocation).
 */
@Component
public class UserBalanceCacheEvictor {

	@CacheEvict(cacheNames = CacheNames.USER_BALANCES, key = "#userId")
	public void evict(String userId) {
		// Eviction is performed by the cache interceptor.
	}
}
