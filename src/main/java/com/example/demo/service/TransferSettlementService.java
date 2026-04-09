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

@Service
/**
 * Settles pending transfers by applying account balance updates.
 */
public class TransferSettlementService {

	private static final Logger log = LoggerFactory.getLogger(TransferSettlementService.class);

	private final TransferRepository transferRepository;
	private final UserRepository userRepository;
	private final UserCacheService cacheEvictor;

	/**
	 * Creates a settlement service instance.
	 *
	 * @param transferRepository repository for transfer row locking and updates
	 * @param userRepository repository for user row locking and balance updates
	 * @param cacheEvictor cache service for balance eviction
	 */
	public TransferSettlementService(
			TransferRepository transferRepository,
			UserRepository userRepository,
			UserCacheService cacheEvictor) {
		this.transferRepository = transferRepository;
		this.userRepository = userRepository;
		this.cacheEvictor = cacheEvictor;
	}

	@Transactional
	/**
	 * Settles one transfer in a transaction.
	 *
	 * @param transferId transfer id
	 */
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
		// Mark the transfer as failed if the from user has insufficient balance.
		if (fromUser.getBalance() < transfer.getAmount()) {
			transfer.setStatus(TransferStatus.FAILED);
			return;
		}
		final long fromBalanceAfterTransfer = fromUser.getBalance() - transfer.getAmount();
		final long toBalanceAfterTransfer = toUser.getBalance() + transfer.getAmount();
		fromUser.setBalance(fromBalanceAfterTransfer);
		toUser.setBalance(toBalanceAfterTransfer);
		transfer.setStatus(TransferStatus.SETTLED);
		// Evict cache only after commit to avoid exposing uncommitted values.
		registerAfterCommit(() -> {
			cacheEvictor.evictBalance(transfer.getFromUserId());
			cacheEvictor.evictBalance(transfer.getToUserId());
		});
	}

	/**
	 * Registers an action that runs after successful commit.
	 *
	 * @param action callback to execute
	 */
	private static void registerAfterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				/**
				 * Executes the deferred callback after transaction commit.
				 */
				public void afterCommit() {
					action.run();
				}
			});
		} else {
			action.run();
		}
	}
}
