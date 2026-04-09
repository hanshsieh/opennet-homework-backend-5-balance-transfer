package com.example.demo.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiExceptionTest {

	@Test
	void constructorWithoutCause_shouldKeepCodeAndMessage() {
		final var exception = new ApiException(ErrorCode.USER_NOT_FOUND, "user missing");

		assertThat(exception.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
		assertThat(exception.getMessage()).isEqualTo("user missing");
		assertThat(exception.getCause()).isNull();
	}

	@Test
	void constructorWithCause_shouldKeepCodeMessageAndCause() {
		final var cause = new IllegalStateException("state");
		final var exception = new ApiException(ErrorCode.INTERNAL_ERROR, "internal", cause);

		assertThat(exception.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
		assertThat(exception.getMessage()).isEqualTo("internal");
		assertThat(exception.getCause()).isEqualTo(cause);
	}
}
