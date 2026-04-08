package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.api.dto.CreateUserRequest;
import com.example.demo.api.dto.UserBalanceResponse;
import com.example.demo.api.dto.UserResponse;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
		UserResponse body = userService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/{userId}/balance")
	public UserBalanceResponse getBalance(@PathVariable String userId) {
		return userService.getBalance(userId);
	}
}
