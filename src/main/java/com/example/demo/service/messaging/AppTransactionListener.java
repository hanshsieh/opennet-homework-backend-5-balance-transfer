package com.example.demo.service.messaging;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.repository.TransferRepository;
import com.example.demo.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AppTransactionListener implements TransactionListener {

	private static final Logger log = LoggerFactory.getLogger(AppTransactionListener.class);

	private final TransferRepository transferRepository;
	private final TransferService transferService;
	private final ObjectMapper objectMapper;

	public AppTransactionListener(
			TransferService transferService,
			TransferRepository transferRepository,
			ObjectMapper objectMapper) {
		this.transferService = transferService;
		this.transferRepository = transferRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		PendingTransferLocalArgs args = (PendingTransferLocalArgs) arg;
		return transferService.createTransferLocally(args);
	}

	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		try {
			final var transferId = objectMapper.readValue(msg.getBody(), PendingTransferPayload.class).getTransferId();
			return transferRepository.findById(transferId).isPresent()
					? LocalTransactionState.COMMIT_MESSAGE
					: LocalTransactionState.ROLLBACK_MESSAGE;
		} catch (Exception e) {
			log.warn("checkLocalTransaction failed: {}", e.getMessage());
			return LocalTransactionState.UNKNOW;
		}
	}
}
