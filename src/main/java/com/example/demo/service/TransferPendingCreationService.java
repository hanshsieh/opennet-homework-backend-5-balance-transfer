package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.TransferStatus;
import com.example.demo.messaging.PendingTransferLocalArgs;
import com.example.demo.repository.TransferRepository;

/**
 * Runs inside RocketMQ {@code executeLocalTransaction}: insert PENDING transfer only.
 */
@Service
public class TransferPendingCreationService {

	private final TransferRepository transferRepository;

	public TransferPendingCreationService(TransferRepository transferRepository) {
		this.transferRepository = transferRepository;
	}

	@Transactional
	public void createPending(PendingTransferLocalArgs args) {
		transferRepository.insert(args.transferId(), args.fromUserId(), args.toUserId(), args.amount(),
				TransferStatus.PENDING);
	}
}
