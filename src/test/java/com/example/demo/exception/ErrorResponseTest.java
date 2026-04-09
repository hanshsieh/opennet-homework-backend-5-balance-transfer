package com.example.demo.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

	@Test
	void record_shouldExposeMessageAndCode() {
		final var response = new ErrorResponse("bad request", "VALIDATION_ERROR");

		assertThat(response.message()).isEqualTo("bad request");
		assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
	}
}
