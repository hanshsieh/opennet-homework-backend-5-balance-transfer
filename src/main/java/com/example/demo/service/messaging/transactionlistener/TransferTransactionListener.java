package com.example.demo.service.messaging.transactionlistener;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;
import com.example.demo.repository.TransferRepository;
import com.example.demo.service.messaging.MessageTopic;
import com.example.demo.service.messaging.localargs.PendingTransferLocalArgs;
import com.example.demo.service.messaging.payload.PendingTransferPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
/**
 * Local transaction handler for pending transfer messages.
 */
public class TransferTransactionListener implements TopicTransactionListener {

	private static final Logger log = LoggerFactory.getLogger(TransferTransactionListener.class);

	private final TransferRepository transferRepository;
	private final EntityManager entityManager;
	private final ObjectMapper objectMapper;

	/**
	 * Creates a transfer topic transaction listener.
	 *
	 * @param transferRepository repository used to create/check transfer records
	 * @param objectMapper mapper used to parse payload in transaction checks
	 */
	public TransferTransactionListener(
			TransferRepository transferRepository,
			EntityManager entityManager,
			ObjectMapper objectMapper) {
		this.transferRepository = transferRepository;
		this.entityManager = entityManager;
		this.objectMapper = objectMapper;
	}

	@Override
	/**
	 * Returns the topic this listener handles.
	 *
	 * @return transfer pending topic
	 */
	public MessageTopic topic() {
		return MessageTopic.PENDING_TRANSFER;
	}

	@Override
	@Transactional
	/**
	 * Persists a pending transfer as the local transaction step.
	 *
	 * @param msg RocketMQ message
	 * @param arg local argument with transfer details
	 * @return local transaction state
	 */
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		final var args = (PendingTransferLocalArgs) arg;
		try {
			// Insert transfer in PENDING state first; balance changes happen asynchronously later.
			// Use `persist` instead of `save` to insert instead of insert-or-update.
			entityManager.persist(TransferEntity.builder()
					.id(args.getTransferId())
					.fromUserId(args.getFromUserId())
					.toUserId(args.getToUserId())
					.amount(args.getAmount())
					.status(TransferStatus.PENDING)
					.build());
			entityManager.flush();
			return LocalTransactionState.COMMIT_MESSAGE;
		} catch (Exception e) {
			log.error("Failed to create transfer locally: {}", args.getTransferId(), e);
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
	}

	@Override
	/**
	 * Checks whether the local transaction has committed by transfer persistence.
	 *
	 * @param msg RocketMQ message
	 * @return local transaction state
	 */
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		try {
			final var transferId = objectMapper.readValue(msg.getBody(), PendingTransferPayload.class)
				.getTransferId();
			return transferRepository.findById(transferId).isPresent()
					? LocalTransactionState.COMMIT_MESSAGE
					: LocalTransactionState.ROLLBACK_MESSAGE;
		} catch (Exception e) {
			log.warn("checkLocalTransaction failed: {}", e.getMessage());
			return LocalTransactionState.UNKNOW;
		}
	}
}
