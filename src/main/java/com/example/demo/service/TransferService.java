package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.domain.TransferEntity;
import com.example.demo.domain.TransferStatus;
import com.example.demo.dto.PagedTransferResponse;
import com.example.demo.dto.TransferRequest;
import com.example.demo.dto.TransferResponse;
import com.example.demo.exception.ApiException;
import com.example.demo.messaging.TransferEventPublisher;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.cache.UserCacheService;

@Service
public class TransferService {

	private static final Duration CANCEL_WINDOW = Duration.ofMinutes(10);

	private final UserRepository userRepository;
	private final TransferRepository transferRepository;
	private final TransferEventPublisher eventPublisher;
	private final UserCacheService cacheEvictor;

	public TransferService(UserRepository userRepository, TransferRepository transferRepository,
			TransferEventPublisher eventPublisher, UserCacheService cacheEvictor) {
		this.userRepository = userRepository;
		this.transferRepository = transferRepository;
		this.eventPublisher = eventPublisher;
		this.cacheEvictor = cacheEvictor;
	}

	@Transactional
	public TransferResponse transfer(TransferRequest request) {
		validateTransferRequest(request);
		if (!userRepository.existsByUserId(request.fromUserId()) || !userRepository.existsByUserId(request.toUserId())) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
					"One or both users do not exist");
		}
		int debited = userRepository.debitIfSufficient(request.fromUserId(), request.amount());
		if (debited != 1) {
			throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE",
					"Insufficient balance for user: " + request.fromUserId());
		}
		int credited = userRepository.credit(request.toUserId(), request.amount());
		if (credited != 1) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
					"Credit failed; recipient not found: " + request.toUserId());
		}
		final var id = UUID.randomUUID().toString();
		transferRepository.insert(id, request.fromUserId(), request.toUserId(), request.amount(),
				TransferStatus.SETTLED);
		registerAfterCommit(() -> {
			cacheEvictor.evictBalance(request.fromUserId());
			cacheEvictor.evictBalance(request.toUserId());
			eventPublisher.publishSettled(id, request.fromUserId(), request.toUserId(), request.amount());
		});
		return toResponse(transferRepository.findById(id).orElseThrow());
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
		if (transfer.createdAt().plus(CANCEL_WINDOW).isBefore(Instant.now())) {
			throw new ApiException(HttpStatus.CONFLICT, "CANCEL_WINDOW_EXPIRED",
					"Transfer can only be cancelled within " + CANCEL_WINDOW.toMinutes() + " minutes");
		}
		int debitedFromRecipient = userRepository.debitIfSufficient(transfer.toUserId(), transfer.amount());
		if (debitedFromRecipient != 1) {
			throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE",
					"Cannot cancel: recipient has insufficient balance to reverse transfer");
		}
		int creditedToSender = userRepository.credit(transfer.fromUserId(), transfer.amount());
		if (creditedToSender != 1) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
					"Refund failed; sender not found: " + transfer.fromUserId());
		}
		int updated = transferRepository.updateStatus(transferId, TransferStatus.CANCELLED);
		if (updated != 1) {
			throw new ApiException(HttpStatus.CONFLICT, "TRANSFER_UPDATE_FAILED", "Failed to update transfer status");
		}
		registerAfterCommit(() -> {
			cacheEvictor.evictBalance(transfer.fromUserId());
			cacheEvictor.evictBalance(transfer.toUserId());
			eventPublisher.publishCancelled(transferId, transfer.fromUserId(), transfer.toUserId(), transfer.amount());
		});
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

	private static void registerAfterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		} else {
			action.run();
		}
	}
}
