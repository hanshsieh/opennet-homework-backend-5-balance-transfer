package com.example.demo.service;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.cache.UserCacheService;

@Service
public class UserBalanceService {

	private final UserRepository userRepository;

	public UserBalanceService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Cacheable(cacheNames = UserCacheService.BALANCES, key = "#userId",
			unless = "#result.isEmpty()")
	public Optional<Long> getBalance(String userId) {
		return userRepository.findByUserId(userId).map(user -> user.getBalance());
	}
}
