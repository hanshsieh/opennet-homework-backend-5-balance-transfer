package com.example.demo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
/**
 * Cache helper component for user-balance cache eviction.
 */
public class UserCacheService {
	public static final String BALANCES = "user-balance";

	private static final Logger log = LoggerFactory.getLogger(UserCacheService.class);

	@CacheEvict(cacheNames = BALANCES, key = "#userId")
	/**
	 * Evicts one user's balance cache entry.
	 *
	 * @param userId user id
	 */
	public void evictBalance(String userId) {
		// Eviction is performed by the cache interceptor.
		log.debug("Evicting user balance for user: {}", userId);
	}
}
