package com.example.demo.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
		@NotBlank
		String userId,
		@Min(0)
		long initialBalance
) {
}
