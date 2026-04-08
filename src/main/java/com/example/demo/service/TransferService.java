package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.TransferEntity;
import com.example.demo.domain.TransferStatus;
import com.example.demo.dto.PagedTransferResponse;
import com.example.demo.dto.TransferRequest;
import com.example.demo.dto.TransferResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.exception.ErrorCode;
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
		TransferRepository.PagedTransfers pageResult = transferRepository.findByUserInvolved(userId, page, size);
		List<TransferResponse> content = pageResult.content().stream().map(this::toResponse).toList();
		return PagedTransferResponse.builder()
				.content(content)
				.totalElements(pageResult.totalElements())
				.number(page)
				.size(size)
				.build();
	}

	@Transactional
	public TransferResponse cancelTransfer(String transferId) {
		TransferEntity transfer = transferRepository.findByIdForUpdate(transferId)
				.orElseThrow(() -> new ApiException(ErrorCode.TRANSFER_NOT_FOUND,
						"Transfer not found: " + transferId));
		if (transfer.status() == TransferStatus.CANCELLED) {
			return toResponse(transfer);
		}
		if (transfer.status() != TransferStatus.PENDING) {
			throw new ApiException(ErrorCode.TRANSFER_NOT_PENDING,
					"Transfer is not pending: " + transferId);
		}
		if (transfer.createdAt().plus(CANCEL_WINDOW).isBefore(Instant.now())) {
			throw new ApiException(ErrorCode.CANCEL_WINDOW_EXPIRED,
					"Transfer can only be cancelled within " + CANCEL_WINDOW.toMinutes() + " minutes");
		}
		transferRepository.updateStatus(transferId, TransferStatus.CANCELLED);
		return toResponse(transferRepository.findById(transferId).orElseThrow());
	}

	private static void validateTransferRequest(TransferRequest request) {
		if (request.getFromUserId().equals(request.getToUserId())) {
			throw new ApiException(ErrorCode.INVALID_TRANSFER,
					"fromUserId and toUserId must differ");
		}
	}

	private TransferResponse toResponse(TransferEntity e) {
		return TransferResponse.builder()
				.id(e.id())
				.fromUserId(e.fromUserId())
				.toUserId(e.toUserId())
				.amount(e.amount())
				.status(e.status().name())
				.createdAt(e.createdAt())
				.build();
	}
}
