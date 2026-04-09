package com.example.demo.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorCodeTest {

	@Test
	void getStatus_shouldReturnExpectedHttpStatus() {
		assertThat(ErrorCode.USER_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
