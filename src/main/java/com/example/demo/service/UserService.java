package com.example.demo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.cache.CacheNames;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UserBalanceResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.exception.ApiException;
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
	@CacheEvict(cacheNames = CacheNames.USER_BALANCES, key = "#request.userId")
	public UserResponse createUser(CreateUserRequest request) {
		if (userRepository.existsByUserId(request.userId())) {
			throw new ApiException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS",
					"User already exists: " + request.userId());
		}
		userRepository.insert(request.userId(), request.initialBalance());
		return new UserResponse(request.userId(), request.initialBalance());
	}

	public UserBalanceResponse getBalance(String userId) {
		Long balance = userBalanceService.getBalanceOrNull(userId);
		if (balance == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found: " + userId);
		}
		return new UserBalanceResponse(userId, balance);
	}
}
