package com.example.demo.exception;

/**
 * Domain exception carrying a typed error code for API responses.
 */
public class ApiException extends RuntimeException {

	private final ErrorCode code;

	/**
	 * Creates an API exception.
	 *
	 * @param code error code
	 * @param message error message
	 */
	public ApiException(ErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Creates an API exception with root cause.
	 *
	 * @param code error code
	 * @param message error message
	 * @param cause root cause
	 */
	public ApiException(ErrorCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * Returns the error code.
	 *
	 * @return error code
	 */
	public ErrorCode getCode() {
		return code;
	}
}
