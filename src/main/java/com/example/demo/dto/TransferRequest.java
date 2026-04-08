package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
		@NotBlank
		String fromUserId,
		@NotBlank
		String toUserId,
		@Positive
		long amount
) {
}
