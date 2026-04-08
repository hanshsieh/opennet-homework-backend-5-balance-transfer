package com.example.demo.domain;

import java.time.Instant;

public record TransferEntity(
		String id,
		String fromUserId,
		String toUserId,
		long amount,
		TransferStatus status,
		Instant createdAt
) {
}
