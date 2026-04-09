package com.example.demo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UserBalanceResponse;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final UserBalanceService userBalanceService;

	public UserService(UserRepository userRepository, UserBalanceService userBalanceService) {
		this.userRepository = userRepository;
		this.userBalanceService = userBalanceService;
	}

	@Transactional
	@CacheEvict(cacheNames = UserCacheService.BALANCES, key = "#request.userId")
	public String createUser(CreateUserRequest request) {
		if (userRepository.existsByUserId(request.getUserId())) {
			throw new ApiException(ErrorCode.USER_ALREADY_EXISTS,
					"User already exists: " + request.getUserId());
		}

		try {
			userRepository.save(UserEntity.builder()
					.userId(request.getUserId())
					.balance(request.getInitialBalance())
					.build());
		} catch (DataIntegrityViolationException ex) {
			throw new ApiException(ErrorCode.USER_ALREADY_EXISTS,
					"User already exists: " + request.getUserId());
		}
		return request.getUserId();
	}

	public UserBalanceResponse getBalance(String userId) {
		final var balance = userBalanceService.getBalance(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
		return UserBalanceResponse.builder()
				.userId(userId)
				.balance(balance)
				.build();
	}
}
