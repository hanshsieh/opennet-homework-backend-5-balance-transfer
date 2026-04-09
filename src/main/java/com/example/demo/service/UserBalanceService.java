package com.example.demo.service;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UserRepository;

@Service
/**
 * Provides balance lookup with cache support.
 */
public class UserBalanceService {

	private final UserRepository userRepository;

	/**
	 * Creates a balance service.
	 *
	 * @param userRepository user repository
	 */
	public UserBalanceService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Cacheable(cacheNames = UserCacheService.BALANCES, key = "#userId",
			unless = "#result == null")
	/**
	 * Gets user balance from cache or database.
	 *
	 * @param userId user id
	 * @return user balance if the user exists
	 */
	public Optional<Long> getBalance(String userId) {
		return userRepository.findByUserId(userId).map(user -> user.getBalance());
	}
}
