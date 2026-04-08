package com.example.demo.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.cache.CacheNames;
import com.example.demo.repository.UserRepository;

@Service
public class UserBalanceService {

	private final UserRepository userRepository;

	public UserBalanceService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Cacheable(cacheNames = CacheNames.USER_BALANCES, key = "#userId", unless = "#result == null")
	public Long getBalanceOrNull(String userId) {
		return userRepository.findBalanceByUserId(userId).orElse(null);
	}
}
