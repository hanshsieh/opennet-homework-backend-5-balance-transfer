package com.example.demo.service.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Separate component so {@link CacheEvict} runs through the Spring proxy (no self-invocation).
 */
@Component
public class UserCacheService {
	public static final String BALANCES = "users:balances";

	private static final Logger log = LoggerFactory.getLogger(UserCacheService.class);

	@CacheEvict(cacheNames = BALANCES, key = "#userId")
	public void evictBalance(String userId) {
		// Eviction is performed by the cache interceptor.
		log.debug("Evicting user balance for user: {}", userId);
	}
}
