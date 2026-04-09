package com.example.demo.service.messaging;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;
import com.example.demo.repository.TransferRepository;
import com.example.demo.service.messaging.localargs.PendingTransferLocalArgs;
import com.example.demo.service.messaging.payload.PendingTransferPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TransferTransactionListener implements TaggedLocalTransactionListener {

	public static final String TAG = "PENDING_TRANSFER";

	private static final Logger log = LoggerFactory.getLogger(TransferTransactionListener.class);

	private final TransferRepository transferRepository;
	private final ObjectMapper objectMapper;

	public TransferTransactionListener(
			TransferRepository transferRepository,
			ObjectMapper objectMapper) {
		this.transferRepository = transferRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public String tag() {
		return TAG;
	}

	@Override
	@Transactional
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		final var args = (PendingTransferLocalArgs) arg;
		try {
			transferRepository.save(TransferEntity.builder()
					.id(args.getTransferId())
					.fromUserId(args.getFromUserId())
					.toUserId(args.getToUserId())
					.amount(args.getAmount())
					.status(TransferStatus.PENDING)
					.build());
			return LocalTransactionState.COMMIT_MESSAGE;
		} catch (Exception e) {
			log.error("Failed to create transfer locally: {}", args.getTransferId(), e);
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
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
