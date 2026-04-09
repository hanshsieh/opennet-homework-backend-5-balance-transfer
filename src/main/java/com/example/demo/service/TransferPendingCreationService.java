package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;
import com.example.demo.repository.TransferRepository;
import com.example.demo.service.messaging.localargs.PendingTransferLocalArgs;

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
		transferRepository.save(TransferEntity.builder()
				.id(args.getTransferId())
				.fromUserId(args.getFromUserId())
				.toUserId(args.getToUserId())
				.amount(args.getAmount())
				.status(TransferStatus.PENDING)
				.build());
	}
}
