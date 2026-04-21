package com.example.demo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UserBalanceResponse;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.exception.ErrorCode;

@Service
/**
 * Handles user creation and balance query use cases.
 */
public class UserService {

	private final UserBalanceService userBalanceService;
	private final EntityManager entityManager;

	/**
	 * Creates a user service.
	 *
	 * @param userBalanceService balance query service
	 * @param entityManager JPA entity manager for explicit persistence control
	 */
	public UserService(UserBalanceService userBalanceService, EntityManager entityManager) {
		this.userBalanceService = userBalanceService;
		this.entityManager = entityManager;
	}

	@Transactional
	@CacheEvict(cacheNames = UserCacheService.BALANCES, key = "#request.userId")
	/**
	 * Creates a user with initial balance.
	 *
	 * @param request create user request
	 * @return created user id
	 */
	public String createUser(CreateUserRequest request) {
		try {
			// Use `persist` instead of `save` to insert instead of insert-or-update.
			entityManager.persist(UserEntity.builder()
					.userId(request.getUserId())
					.balance(request.getInitialBalance())
					.build());
			// Explicitly flush to ensure the conflict is detected before return.
			entityManager.flush();
		} catch (DataIntegrityViolationException | PersistenceException ex) {
			throw new ApiException(ErrorCode.USER_ALREADY_EXISTS,
					"User already exists: " + request.getUserId());
		}
		return request.getUserId();
	}

	/**
	 * Gets current balance for one user.
	 *
	 * @param userId user id
	 * @return user balance response
	 */
	public UserBalanceResponse getBalance(String userId) {
		final var balance = userBalanceService.getBalance(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
		return UserBalanceResponse.builder()
				.userId(userId)
				.balance(balance)
				.build();
	}
}
