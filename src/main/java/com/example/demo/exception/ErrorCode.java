package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	USER_NOT_FOUND(HttpStatus.NOT_FOUND),
	TRANSFER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),
	TRANSFER_STATE_UNCERTAIN(HttpStatus.SERVICE_UNAVAILABLE),
	TRANSFER_MQ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
	TRANSFER_ROW_MISSING(HttpStatus.INTERNAL_SERVER_ERROR),
	TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND),
	TRANSFER_ALREADY_CANCELLED(HttpStatus.CONFLICT),
	TRANSFER_ALREADY_SETTLED(HttpStatus.CONFLICT),
	CANCEL_WINDOW_EXPIRED(HttpStatus.CONFLICT),
	TRANSFER_NOT_PENDING(HttpStatus.CONFLICT),
	INVALID_TRANSFER(HttpStatus.BAD_REQUEST),
	USER_ALREADY_EXISTS(HttpStatus.CONFLICT),
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
