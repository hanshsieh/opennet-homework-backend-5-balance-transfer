package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.example.demo.service.messaging.EventPublisher;
import com.example.demo.service.messaging.PendingTransferLocalArgs;

@Service
public class TransferService {
	private static final Duration CANCEL_WINDOW = Duration.ofMinutes(10);
	private static final Logger log = LoggerFactory.getLogger(TransferService.class);
	private final UserRepository userRepository;
	private final TransferRepository transferRepository;
	private final EventPublisher eventPublisher;

	public TransferService(UserRepository userRepository, TransferRepository transferRepository,
			EventPublisher eventPublisher) {
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
				throw new ApiException(ErrorCode.TRANSFER_STATE_UNCERTAIN,
						"Transfer could not be confirmed; check transfer id: " + id);
			}
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(ErrorCode.TRANSFER_MQ_ERROR,
					"Failed to submit transfer", e);
		}
		return id;
	}

	public PagedTransferResponse listTransfers(String userId, int page, int size) {
		if (!userRepository.existsByUserId(userId)) {
			throw new ApiException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId);
		}
		final var pageable = PageRequest.of(page, size);
		final var entities = transferRepository.findByFromUserIdOrToUserIdOrderByCreatedAtDesc(userId, userId, pageable);
		final var totalElements = transferRepository.countByFromUserIdOrToUserId(userId, userId);
		List<TransferResponse> content = entities.stream().map(this::toResponse).toList();
		return PagedTransferResponse.builder()
				.content(content)
				.totalElements(totalElements)
				.number(page)
				.size(size)
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
			throw new ApiException(ErrorCode.TRANSFER_NOT_PENDING,
					"Transfer is not pending: " + transferId);
		}
		if (transfer.getCreatedAt().plus(CANCEL_WINDOW).isBefore(Instant.now())) {
			throw new ApiException(ErrorCode.CANCEL_WINDOW_EXPIRED,
					"Transfer can only be cancelled within " + CANCEL_WINDOW.toMinutes() + " minutes");
		}
		transfer.setStatus(TransferStatus.CANCELLED);
		return toResponse(transfer);
	}

	@Transactional
	public LocalTransactionState createTransferLocally(PendingTransferLocalArgs args) {
		try {
			transferRepository.save(TransferEntity.builder()
					.id(args.getTransferId())
					.fromUserId(args.getFromUserId())
					.toUserId(args.getToUserId())
					.amount(args.getAmount())
					.status(TransferStatus.PENDING)
					.build());
			return LocalTransactionState.COMMIT_MESSAGE;
		} catch (Exception e) {
			log.error("Failed to create transfer locally: {}", args.getTransferId(), e);
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
	}

	private static void validateTransferRequest(TransferRequest request) {
		if (request.getFromUserId().equals(request.getToUserId())) {
			throw new ApiException(ErrorCode.INVALID_TRANSFER,
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
