package com.example.demo.domain;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserEntity {

	private final String userId;
	private final long balance;
	private final Instant createdAt;
	private final Instant updatedAt;
}
