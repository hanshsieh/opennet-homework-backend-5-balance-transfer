package com.example.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Defines API error codes and their mapped HTTP status.
 */
public enum ErrorCode {
	USER_NOT_FOUND(HttpStatus.NOT_FOUND),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
	TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND),
	CANCEL_WINDOW_EXPIRED(HttpStatus.CONFLICT),
	TRANSFER_STATE_CONFLICT(HttpStatus.CONFLICT),
	USER_ALREADY_EXISTS(HttpStatus.CONFLICT),
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	/**
	 * Returns HTTP status mapped to this error code.
	 *
	 * @return HTTP status
	 */
	public HttpStatus getStatus() {
		return status;
	}
}
