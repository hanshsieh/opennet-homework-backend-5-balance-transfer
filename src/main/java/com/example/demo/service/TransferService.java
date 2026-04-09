package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.PagedTransferResponse;
import com.example.demo.dto.TransferRequest;
import com.example.demo.dto.TransferResponse;
import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.messaging.MessagePublisher;

@Service
public class TransferService {
	private static final Duration CANCEL_WINDOW = Duration.ofMinutes(10);
	private final UserRepository userRepository;
	private final TransferRepository transferRepository;
	private final MessagePublisher eventPublisher;

	public TransferService(UserRepository userRepository, TransferRepository transferRepository,
			MessagePublisher eventPublisher) {
		this.userRepository = userRepository;
		this.transferRepository = transferRepository;
		this.eventPublisher = eventPublisher;
	}

	public String createTransfer(TransferRequest request) {
		validateTransferRequest(request);
		final var id = UUID.randomUUID().toString();
		try {
			final var sendResult = eventPublisher.sendPendingTransfer(id, request);
			final var state = sendResult.getLocalTransactionState();
			if (state != LocalTransactionState.COMMIT_MESSAGE) {
				throw new ApiException(ErrorCode.INTERNAL_ERROR,
						"Transfer could not be confirmed; check transfer id: " + id);
			}
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR,
					"Failed to submit transfer", e);
		}
		return id;
	}

	public PagedTransferResponse listTransfers(String userId, int pageNumber, int pageSize) {
		if (!userRepository.existsByUserId(userId)) {
			throw new ApiException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId);
		}
		final var pageable = PageRequest.of(pageNumber, pageSize);
		final var entities = transferRepository.findByUserId(userId, pageable);
		final var totalElements = transferRepository.countByUserId(userId);
		final var items = entities.stream().map(this::toResponse).toList();
		return PagedTransferResponse.builder()
				.items(items)
				.total(totalElements)
				.pageNumber(pageNumber)
				.pageSize(pageSize)
				.build();
	}

	@Transactional
	public TransferResponse cancelTransfer(String transferId) {
		TransferEntity transfer = transferRepository.findByIdForUpdate(transferId)
				.orElseThrow(() -> new ApiException(ErrorCode.TRANSFER_NOT_FOUND,
						"Transfer not found: " + transferId));
		if (transfer.getStatus() == TransferStatus.CANCELLED) {
			return toResponse(transfer);
		}
		if (transfer.getStatus() != TransferStatus.PENDING) {
			throw new ApiException(ErrorCode.TRANSFER_STATE_CONFLICT,
					"Transfer is not pending: " + transferId);
		}
		if (transfer.getCreatedAt().plus(CANCEL_WINDOW).isBefore(Instant.now())) {
			throw new ApiException(ErrorCode.CANCEL_WINDOW_EXPIRED,
					"Transfer can only be cancelled within " + CANCEL_WINDOW.toMinutes() + " minutes");
		}
		transfer.setStatus(TransferStatus.CANCELLED);
		return toResponse(transfer);
	}

	private static void validateTransferRequest(TransferRequest request) {
		if (request.getFromUserId().equals(request.getToUserId())) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
					"fromUserId and toUserId must differ");
		}
	}

	private TransferResponse toResponse(TransferEntity e) {
		return TransferResponse.builder()
				.id(e.getId())
				.fromUserId(e.getFromUserId())
				.toUserId(e.getToUserId())
				.amount(e.getAmount())
				.status(e.getStatus().name())
				.createdAt(e.getCreatedAt())
				.build();
	}
}
