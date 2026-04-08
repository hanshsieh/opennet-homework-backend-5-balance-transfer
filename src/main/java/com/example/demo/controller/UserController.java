package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.CreateUserResponse;
import com.example.demo.dto.UserBalanceResponse;
import com.example.demo.service.UserService;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<CreateUserResponse> createUser(@Validated @RequestBody CreateUserRequest request) {
		final var userId = userService.createUser(request);
		final var body = CreateUserResponse.builder()
				.id(userId)
				.build();
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/{userId}/balance")
	public UserBalanceResponse getBalance(@PathVariable @NotBlank String userId) {
		return userService.getBalance(userId);
	}
}
