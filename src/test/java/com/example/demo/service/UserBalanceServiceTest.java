package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserBalanceServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserBalanceService userBalanceService;

	@Test
	void getBalance_shouldReturnBalanceWhenUserExists() {
		when(userRepository.findByUserId("u1"))
				.thenReturn(Optional.of(UserEntity.builder().userId("u1").balance(120L).build()));

		final var result = userBalanceService.getBalance("u1");

		assertThat(result).contains(120L);
		verify(userRepository).findByUserId("u1");
	}

	@Test
	void getBalance_shouldReturnEmptyWhenUserDoesNotExist() {
		when(userRepository.findByUserId("missing"))
				.thenReturn(Optional.empty());

		final var result = userBalanceService.getBalance("missing");

		assertThat(result).isEmpty();
		verify(userRepository).findByUserId("missing");
	}
}
