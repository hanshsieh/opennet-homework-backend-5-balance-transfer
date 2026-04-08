package com.example.demo.api.dto;

import java.time.Instant;

public record TransferResponse(
		String id,
		String fromUserId,
		String toUserId,
		long amount,
		String status,
		Instant createdAt
) {
}
