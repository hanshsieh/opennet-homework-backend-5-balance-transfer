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
		if (transfer == null || transfer.getStatus() != TransferStatus.PENDING) {
			log.info("Skip settlement, transfer missing or not pending: {}", transferId);
			return;
		}
		if (transfer.getFromUserId().equals(transfer.getToUserId())) {
			transfer.setStatus(TransferStatus.FAILED);
			return;
		}
		final var lockedUsers = userRepository.findByUserIdsForUpdate(
				List.of(transfer.getFromUserId(), transfer.getToUserId()));
		UserEntity fromUser = null;
		UserEntity toUser = null;
		for (var user : lockedUsers) {
			if (transfer.getFromUserId().equals(user.getUserId())) {
				fromUser = user;
			}
			if (transfer.getToUserId().equals(user.getUserId())) {
				toUser = user;
			}
		}
		if (fromUser == null || toUser == null) {
			transfer.setStatus(TransferStatus.FAILED);
			return;
		}
		if (fromUser.getBalance() < transfer.getAmount()) {
			transfer.setStatus(TransferStatus.FAILED);
			return;
		}
		final long fromBalanceAfterTransfer = fromUser.getBalance() - transfer.getAmount();
		final long toBalanceAfterTransfer = toUser.getBalance() + transfer.getAmount();
		fromUser.setBalance(fromBalanceAfterTransfer);
		toUser.setBalance(toBalanceAfterTransfer);
		transfer.setStatus(TransferStatus.SETTLED);
		registerAfterCommit(() -> {
			cacheEvictor.evictBalance(transfer.getFromUserId());
			cacheEvictor.evictBalance(transfer.getToUserId());
		});
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
