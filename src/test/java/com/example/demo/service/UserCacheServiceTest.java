package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class UserCacheServiceTest {

	@Test
	void evictBalance_shouldExecuteWithoutException() {
		final var service = new UserCacheService();

		assertThatCode(() -> service.evictBalance("u1")).doesNotThrowAnyException();
	}

	@Test
	void balancesConstant_shouldMatchCacheName() {
		assertThat(UserCacheService.BALANCES).isEqualTo("user-balance");
	}
}
