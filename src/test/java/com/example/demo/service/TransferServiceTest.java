package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.TransferRequest;
import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.messaging.MessagePublisher;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private TransferRepository transferRepository;

	@Mock
	private MessagePublisher eventPublisher;

	@InjectMocks
	private TransferService transferService;

	@Test
	void createTransfer_shouldReturnTransferIdWhenMessageCommitted() throws Exception {
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u2")
				.amount(50L)
				.build();
		final var sendResult = new TransactionSendResult();
		sendResult.setLocalTransactionState(LocalTransactionState.COMMIT_MESSAGE);
		when(userRepository.existsByUserId("u1")).thenReturn(true);
		when(userRepository.existsByUserId("u2")).thenReturn(true);
		when(eventPublisher.sendPendingTransfer(any(String.class), eq(request))).thenReturn(sendResult);

		final var id = transferService.createTransfer(request);

		assertThat(id).isNotBlank();
		verify(eventPublisher).sendPendingTransfer(eq(id), eq(request));
	}

	@Test
	void createTransfer_shouldThrowInternalErrorWhenMessageNotCommitted() throws Exception {
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u2")
				.amount(50L)
				.build();
		final var sendResult = new TransactionSendResult();
		sendResult.setLocalTransactionState(LocalTransactionState.ROLLBACK_MESSAGE);
		when(userRepository.existsByUserId("u1")).thenReturn(true);
		when(userRepository.existsByUserId("u2")).thenReturn(true);
		when(eventPublisher.sendPendingTransfer(any(String.class), eq(request))).thenReturn(sendResult);

		assertThatThrownBy(() -> transferService.createTransfer(request))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
	}

	@Test
	void createTransfer_shouldWrapUnexpectedException() throws Exception {
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u2")
				.amount(50L)
				.build();
		when(userRepository.existsByUserId("u1")).thenReturn(true);
		when(userRepository.existsByUserId("u2")).thenReturn(true);
		when(eventPublisher.sendPendingTransfer(any(String.class), eq(request)))
				.thenThrow(new RuntimeException("boom"));

		assertThatThrownBy(() -> transferService.createTransfer(request))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
	}

	@Test
	void createTransfer_shouldThrowValidationErrorWhenFromEqualsTo() {
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u1")
				.amount(10L)
				.build();

		assertThatThrownBy(() -> transferService.createTransfer(request))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	@Test
	void createTransfer_shouldThrowValidationErrorWhenFromUserDoesNotExist() {
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u2")
				.amount(10L)
				.build();
		when(userRepository.existsByUserId("u1")).thenReturn(false);

		assertThatThrownBy(() -> transferService.createTransfer(request))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	@Test
	void createTransfer_shouldThrowValidationErrorWhenToUserDoesNotExist() {
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u2")
				.amount(10L)
				.build();
		when(userRepository.existsByUserId("u1")).thenReturn(true);
		when(userRepository.existsByUserId("u2")).thenReturn(false);

		assertThatThrownBy(() -> transferService.createTransfer(request))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	@Test
	void listTransfers_shouldThrowNotFoundWhenUserMissing() {
		when(userRepository.existsByUserId("missing")).thenReturn(false);

		assertThatThrownBy(() -> transferService.listTransfers("missing", 0, 10))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
	}

	@Test
	void listTransfers_shouldReturnPagedResponseWhenUserExists() {
		final var transfer = TransferEntity.builder()
				.id("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(88L)
				.status(TransferStatus.PENDING)
				.createdAt(Instant.parse("2026-04-09T10:00:00Z"))
				.build();
		when(userRepository.existsByUserId("u1")).thenReturn(true);
		when(transferRepository.findByUserId(eq("u1"), any(Pageable.class))).thenReturn(List.of(transfer));
		when(transferRepository.countByUserId("u1")).thenReturn(1L);

		final var response = transferService.listTransfers("u1", 0, 20);

		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getId()).isEqualTo("t1");
		assertThat(response.getTotal()).isEqualTo(1L);
		assertThat(response.getPageNumber()).isEqualTo(0);
		assertThat(response.getPageSize()).isEqualTo(20);
		verify(transferRepository).findByUserId(eq("u1"), argThat(pageable ->
				pageable.getPageNumber() == 0 && pageable.getPageSize() == 20));
	}

	@Test
	void cancelTransfer_shouldSetCancelledWhenTransferPendingWithinWindow() {
		final var transfer = TransferEntity.builder()
				.id("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(30L)
				.status(TransferStatus.PENDING)
				.createdAt(Instant.now().minusSeconds(60))
				.build();
		when(transferRepository.findByIdForUpdate("t1")).thenReturn(Optional.of(transfer));

		final var response = transferService.cancelTransfer("t1");

		assertThat(transfer.getStatus()).isEqualTo(TransferStatus.CANCELLED);
		assertThat(response.getStatus()).isEqualTo(TransferStatus.CANCELLED.name());
	}

	@Test
	void cancelTransfer_shouldReturnCancelledWhenTransferAlreadyCancelled() {
		final var transfer = TransferEntity.builder()
				.id("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(30L)
				.status(TransferStatus.CANCELLED)
				.createdAt(Instant.now().minusSeconds(60))
				.build();
		when(transferRepository.findByIdForUpdate("t1")).thenReturn(Optional.of(transfer));

		final var response = transferService.cancelTransfer("t1");

		assertThat(response.getId()).isEqualTo("t1");
		assertThat(response.getStatus()).isEqualTo(TransferStatus.CANCELLED.name());
	}

	@Test
	void cancelTransfer_shouldThrowConflictWhenTransferNotPending() {
		final var transfer = TransferEntity.builder()
				.id("t1")
				.status(TransferStatus.SETTLED)
				.createdAt(Instant.now())
				.build();
		when(transferRepository.findByIdForUpdate("t1")).thenReturn(Optional.of(transfer));

		assertThatThrownBy(() -> transferService.cancelTransfer("t1"))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.TRANSFER_STATE_CONFLICT));
	}

	@Test
	void cancelTransfer_shouldThrowWhenCancelWindowExpired() {
		final var cancelWindow = TransferService.CANCEL_WINDOW;
		final var transfer = TransferEntity.builder()
				.id("t1")
				.status(TransferStatus.PENDING)
				.createdAt(Instant.now().minusSeconds(cancelWindow.plusSeconds(1).toSeconds()))
				.build();
		when(transferRepository.findByIdForUpdate("t1")).thenReturn(Optional.of(transfer));

		assertThatThrownBy(() -> transferService.cancelTransfer("t1"))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.CANCEL_WINDOW_EXPIRED));
	}

	@Test
	void cancelTransfer_shouldThrowWhenTransferNotFound() {
		when(transferRepository.findByIdForUpdate("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transferService.cancelTransfer("missing"))
				.isInstanceOf(ApiException.class)
				.satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.TRANSFER_NOT_FOUND));
	}
}
