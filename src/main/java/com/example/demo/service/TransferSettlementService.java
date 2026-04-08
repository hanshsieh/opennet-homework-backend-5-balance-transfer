package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.entity.TransferStatus;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.cache.UserCacheService;

@Service
public class TransferSettlementService {

	private static final Logger log = LoggerFactory.getLogger(TransferSettlementService.class);

	private final TransferRepository transferRepository;
	private final UserRepository userRepository;
	private final UserCacheService cacheEvictor;

	public TransferSettlementService(
			TransferRepository transferRepository,
			UserRepository userRepository,
			UserCacheService cacheEvictor) {
		this.transferRepository = transferRepository;
		this.userRepository = userRepository;
		this.cacheEvictor = cacheEvictor;
	}

	@Transactional
	public void settle(String transferId) {
		final var transfer = transferRepository.findByIdForUpdate(transferId).orElse(null);
		if (transfer == null || transfer.status() != TransferStatus.PENDING) {
			log.info("Skip settlement, transfer missing or not pending: {}", transferId);
			return;
		}
		if (transfer.fromUserId().equals(transfer.toUserId())) {
			markFailed(transferId);
			return;
		}
		final var lockedUsers = userRepository.findByUserIdsForUpdate(
				List.of(transfer.fromUserId(), transfer.toUserId()));
		UserEntity fromUser = null;
		UserEntity toUser = null;
		for (var user : lockedUsers) {
			if (transfer.fromUserId().equals(user.getUserId())) {
				fromUser = user;
			}
			if (transfer.toUserId().equals(user.getUserId())) {
				toUser = user;
			}
		}
		if (fromUser == null || toUser == null) {
			markFailed(transferId);
			return;
		}
		if (fromUser.getBalance() < transfer.amount()) {
			markFailed(transferId);
			return;
		}
		final long fromBalanceAfterTransfer = fromUser.getBalance() - transfer.amount();
		final long toBalanceAfterTransfer = toUser.getBalance() + transfer.amount();
		fromUser.setBalance(fromBalanceAfterTransfer);
		toUser.setBalance(toBalanceAfterTransfer);
		final var updated = transferRepository.updateStatus(transfer.id(), TransferStatus.SETTLED);
		if (updated != 1) {
			throw new IllegalStateException("Settlement status update failed for transfer " + transferId);
		}
		registerAfterCommit(() -> {
			cacheEvictor.evictBalance(transfer.fromUserId());
			cacheEvictor.evictBalance(transfer.toUserId());
		});
	}

	private void markFailed(String transferId) {
		final var updated = transferRepository.updateStatus(transferId, TransferStatus.FAILED);
		if (updated != 1) {
			throw new IllegalStateException("Failed status update failed for transfer " + transferId);
		}
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
