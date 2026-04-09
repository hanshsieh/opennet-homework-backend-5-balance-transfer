package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UserBalanceResponse;
import com.example.demo.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserService userService;

	@Test
	void createUser_shouldReturnCreatedResponseWithUserId() {
		final var controller = new UserController(userService);
		final var request = CreateUserRequest.builder()
				.userId("u1")
				.initialBalance(100L)
				.build();
		when(userService.createUser(request)).thenReturn("u1");

		final var response = controller.createUser(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo("u1");
		verify(userService).createUser(request);
	}

	@Test
	void getBalance_shouldReturnBalanceFromService() {
		final var controller = new UserController(userService);
		final var expected = UserBalanceResponse.builder()
				.userId("u1")
				.balance(200L)
				.build();
		when(userService.getBalance("u1")).thenReturn(expected);

		final var response = controller.getBalance("u1");

		assertThat(response).isSameAs(expected);
		verify(userService).getBalance("u1");
	}
}
