package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.TransferEntity;
import com.example.demo.domain.TransferStatus;
import com.example.demo.dto.PagedTransferResponse;
import com.example.demo.dto.TransferRequest;
import com.example.demo.dto.TransferResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.messaging.TransferEventPublisher;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;

@Service
public class TransferService {

	private static final Duration CANCEL_WINDOW = Duration.ofMinutes(10);

	private final UserRepository userRepository;
	private final TransferRepository transferRepository;
	private final TransferEventPublisher eventPublisher;

	public TransferService(UserRepository userRepository, TransferRepository transferRepository,
			TransferEventPublisher eventPublisher) {
		this.userRepository = userRepository;
		this.transferRepository = transferRepository;
		this.eventPublisher = eventPublisher;
	}

	public TransferResponse transfer(TransferRequest request) {
		validateTransferRequest(request);
		if (!userRepository.existsByUserId(request.fromUserId()) || !userRepository.existsByUserId(request.toUserId())) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
					"One or both users do not exist");
		}
		final var id = UUID.randomUUID().toString();
		try {
			var sendResult = eventPublisher.sendPendingTransferTransactional(id, request);
			LocalTransactionState state = sendResult.getLocalTransactionState();
			if (state == LocalTransactionState.ROLLBACK_MESSAGE) {
				throw new ApiException(HttpStatus.CONFLICT, "TRANSFER_CREATE_FAILED",
						"Could not persist pending transfer (e.g. invalid users or database error)");
			}
			if (state != LocalTransactionState.COMMIT_MESSAGE) {
				throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TRANSFER_STATE_UNCERTAIN",
						"Transfer could not be confirmed; check transfer id: " + id);
			}
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TRANSFER_MQ_ERROR",
					"Failed to submit transfer: " + e.getMessage());
		}
		return toResponse(transferRepository.findById(id).orElseThrow(() -> new ApiException(
				HttpStatus.INTERNAL_SERVER_ERROR, "TRANSFER_ROW_MISSING",
				"Transfer row missing after commit: " + id)));
	}

	public PagedTransferResponse listTransfers(String userId, int page, int size) {
		if (!userRepository.existsByUserId(userId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found: " + userId);
		}
		TransferRepository.PagedTransfers pageResult = transferRepository.findByUserInvolved(userId, page, size);
		List<TransferResponse> content = pageResult.content().stream().map(this::toResponse).toList();
		return new PagedTransferResponse(content, pageResult.totalElements(), page, size);
	}

	@Transactional
	public TransferResponse cancelTransfer(String transferId) {
		TransferEntity transfer = transferRepository.findById(transferId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND",
						"Transfer not found: " + transferId));
		if (transfer.status() == TransferStatus.CANCELLED) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSFER_ALREADY_CANCELLED",
					"Transfer already cancelled: " + transferId);
		}
		if (transfer.status() == TransferStatus.SETTLED) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSFER_ALREADY_SETTLED",
					"Cannot cancel a settled transfer: " + transferId);
		}
		if (transfer.createdAt().plus(CANCEL_WINDOW).isBefore(Instant.now())) {
			throw new ApiException(HttpStatus.CONFLICT, "CANCEL_WINDOW_EXPIRED",
					"Transfer can only be cancelled within " + CANCEL_WINDOW.toMinutes() + " minutes");
		}
		int updated = transferRepository.updateStatusIf(transferId, TransferStatus.PENDING, TransferStatus.CANCELLED);
		if (updated != 1) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSFER_NOT_PENDING",
					"Transfer was already settled or cancelled: " + transferId);
		}
		return toResponse(transferRepository.findById(transferId).orElseThrow());
	}

	private static void validateTransferRequest(TransferRequest request) {
		if (request.fromUserId().equals(request.toUserId())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TRANSFER",
					"fromUserId and toUserId must differ");
		}
	}

	private TransferResponse toResponse(TransferEntity e) {
		return new TransferResponse(e.id(), e.fromUserId(), e.toUserId(), e.amount(), e.status().name(),
				e.createdAt());
	}
}
