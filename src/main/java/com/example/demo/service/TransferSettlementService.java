package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.domain.TransferEntity;
import com.example.demo.domain.TransferStatus;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.cache.UserCacheService;

@Service
public class TransferSettlementService {

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
		TransferEntity transfer = transferRepository.findByIdForUpdate(transferId).orElse(null);
		if (transfer == null) {
			return;
		}
		if (transfer.status() != TransferStatus.PENDING) {
			return;
		}
		if (!userRepository.existsByUserId(transfer.fromUserId())
				|| !userRepository.existsByUserId(transfer.toUserId())) {
			markFailed(transferId);
			return;
		}
		int debited = userRepository.debitIfSufficient(transfer.fromUserId(), transfer.amount());
		if (debited != 1) {
			markFailed(transferId);
			return;
		}
		int credited = userRepository.credit(transfer.toUserId(), transfer.amount());
		if (credited != 1) {
			userRepository.credit(transfer.fromUserId(), transfer.amount());
			markFailed(transferId);
			return;
		}
		int updated = transferRepository.updateStatus(transfer.id(), TransferStatus.SETTLED);
		if (updated != 1) {
			throw new IllegalStateException("Settlement status update failed for transfer " + transferId);
		}
		registerAfterCommit(() -> {
			cacheEvictor.evictBalance(transfer.fromUserId());
			cacheEvictor.evictBalance(transfer.toUserId());
		});
	}

	private void markFailed(String transferId) {
		transferRepository.updateStatusIf(transferId, TransferStatus.PENDING, TransferStatus.FAILED);
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
