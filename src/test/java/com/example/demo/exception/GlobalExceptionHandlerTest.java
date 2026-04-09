package com.example.demo.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

class GlobalExceptionHandlerTest {

	@Test
	void handleApi_shouldReturnErrorResponseWithMappedStatus() {
		final var handler = new GlobalExceptionHandler();
		final var exception = new ApiException(ErrorCode.USER_ALREADY_EXISTS, "duplicated user");

		final var response = handler.handleApi(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("duplicated user");
		assertThat(response.getBody().code()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS.name());
	}

	@Test
	void handleApi_internalError_shouldReturnInternalStatusAndBody() {
		final var handler = new GlobalExceptionHandler();
		final var exception = new ApiException(ErrorCode.INTERNAL_ERROR, "unexpected");

		final var response = handler.handleApi(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("unexpected");
		assertThat(response.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR.name());
	}

	@Test
	void handleValidation_shouldJoinAllFieldErrors() throws NoSuchMethodException {
		final var handler = new GlobalExceptionHandler();
		final var target = new ValidationTarget();
		final var bindingResult = new BeanPropertyBindingResult(target, "validationTarget");
		bindingResult.addError(new FieldError("validationTarget", "fromUserId", "must not be blank"));
		bindingResult.addError(new FieldError("validationTarget", "amount", "must be positive"));
		final MethodParameter parameter = new HandlerMethod(
				this,
				GlobalExceptionHandlerTest.class.getDeclaredMethod("validationMethod", ValidationTarget.class))
				.getMethodParameters()[0];
		final var exception = new MethodArgumentNotValidException(parameter, bindingResult);

		final var response = handler.handleValidation(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_ERROR.name());
		assertThat(response.getBody().message()).isEqualTo("fromUserId: must not be blank; amount: must be positive");
	}

	@Test
	void handleConstraintViolation_shouldJoinAllViolations() {
		final var handler = new GlobalExceptionHandler();
		final var first = violation("createTransfer.fromUserId", "must not be blank");
		final var second = violation("createTransfer.amount", "must be positive");
		final var exception = new ConstraintViolationException(Set.of(first, second));

		final var response = handler.handleConstraintViolation(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_ERROR.name());
		assertThat(List.of(response.getBody().message().split("; "))).containsExactlyInAnyOrder(
				"createTransfer.fromUserId: must not be blank",
				"createTransfer.amount: must be positive");
	}

	private ConstraintViolation<Object> violation(String path, String message) {
		@SuppressWarnings("unchecked")
		final var violation = (ConstraintViolation<Object>) Mockito.mock(ConstraintViolation.class);
		final var propertyPath = Mockito.mock(Path.class);
		Mockito.when(violation.getPropertyPath()).thenReturn(propertyPath);
		Mockito.when(propertyPath.toString()).thenReturn(path);
		Mockito.when(violation.getMessage()).thenReturn(message);
		return violation;
	}

	@SuppressWarnings("unused")
	private void validationMethod(ValidationTarget target) {
		// helper method for creating MethodParameter in tests
	}

	private static final class ValidationTarget {
	}
}
