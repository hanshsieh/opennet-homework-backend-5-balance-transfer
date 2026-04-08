package com.example.demo.messaging;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.repository.TransferRepository;
import com.example.demo.service.TransferPendingCreationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TransferTransactionListener implements TransactionListener {

	private static final Logger log = LoggerFactory.getLogger(TransferTransactionListener.class);

	private final TransferPendingCreationService pendingCreationService;
	private final TransferRepository transferRepository;
	private final ObjectMapper objectMapper;

	public TransferTransactionListener(
			TransferPendingCreationService pendingCreationService,
			TransferRepository transferRepository,
			ObjectMapper objectMapper) {
		this.pendingCreationService = pendingCreationService;
		this.transferRepository = transferRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		PendingTransferLocalArgs args = (PendingTransferLocalArgs) arg;
		try {
			pendingCreationService.createPending(args);
			return LocalTransactionState.COMMIT_MESSAGE;
		} catch (Exception e) {
			log.error("executeLocalTransaction failed for transfer {}", args.getTransferId(), e);
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
	}

	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		try {
			final var transferId = objectMapper.readValue(msg.getBody(), TransferIdPayload.class).transferId();
			return transferRepository.findById(transferId).isPresent()
					? LocalTransactionState.COMMIT_MESSAGE
					: LocalTransactionState.ROLLBACK_MESSAGE;
		} catch (Exception e) {
			log.warn("checkLocalTransaction failed: {}", e.getMessage());
			return LocalTransactionState.UNKNOW;
		}
	}
}
