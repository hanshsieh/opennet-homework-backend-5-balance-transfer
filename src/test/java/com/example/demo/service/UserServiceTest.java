package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserBalanceService userBalanceService;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private UserService userService;

	@Test
	void createUser_shouldPersistAndReturnUserId() {
		final var request = CreateUserRequest.builder()
				.userId("alice")
				.initialBalance(100L)
				.build();

		final var result = userService.createUser(request);

		final var captor = ArgumentCaptor.forClass(UserEntity.class);
		verify(entityManager).persist(captor.capture());
		verify(entityManager).flush();
		assertThat(captor.getValue().getUserId()).isEqualTo("alice");
		assertThat(captor.getValue().getBalance()).isEqualTo(100L);
		assertThat(result).isEqualTo("alice");
	}

	@Test
	void createUser_shouldThrowConflictWhenConstraintViolationOccurs() {
		final var request = CreateUserRequest.builder()
				.userId("alice")
				.initialBalance(100L)
				.build();
		doThrow(new DataIntegrityViolationException("duplicate")).when(entityManager).persist(any(UserEntity.class));

		assertThatThrownBy(() -> userService.createUser(request))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> {
					final var apiException = (ApiException) ex;
					assertThat(apiException.getCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
					assertThat(apiException.getMessage()).contains("User already exists: alice");
				});
	}

	@Test
	void createUser_shouldPropagatePersistenceException() {
		final var request = CreateUserRequest.builder()
				.userId("alice")
				.initialBalance(100L)
				.build();
		doThrow(new PersistenceException("unexpected persistence error"))
				.when(entityManager).persist(any(UserEntity.class));

		assertThatThrownBy(() -> userService.createUser(request))
				.isInstanceOf(PersistenceException.class)
				.isNotInstanceOf(ApiException.class)
				.hasMessageContaining("unexpected persistence error");
	}

	@Test
	void getBalance_shouldReturnResponseWhenUserExists() {
		when(userBalanceService.getBalance("alice")).thenReturn(Optional.of(300L));

		final var response = userService.getBalance("alice");

		assertThat(response.getUserId()).isEqualTo("alice");
		assertThat(response.getBalance()).isEqualTo(300L);
	}

	@Test
	void getBalance_shouldThrowNotFoundWhenUserMissing() {
		when(userBalanceService.getBalance("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getBalance("ghost"))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> {
					final var apiException = (ApiException) ex;
					assertThat(apiException.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
					assertThat(apiException.getMessage()).contains("User not found: ghost");
				});
	}
}
