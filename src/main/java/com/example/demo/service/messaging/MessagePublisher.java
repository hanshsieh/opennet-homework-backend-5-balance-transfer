package com.example.demo.service.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Service;

import com.example.demo.dto.TransferRequest;
import com.example.demo.service.messaging.localargs.PendingTransferLocalArgs;
import com.example.demo.service.messaging.payload.PendingTransferPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
/**
 * Represents the MessagePublisher class.
 */
public class MessagePublisher {

	private final TransactionMQProducer producer;
	private final ObjectMapper objectMapper;

	public MessagePublisher(
			final TransactionMQProducer producer,
			final ObjectMapper objectMapper) {
		this.producer = producer;
		this.objectMapper = objectMapper;
	}

	public TransactionSendResult sendPendingTransfer(String transferId, TransferRequest request)
			throws Exception {
		final var body = objectMapper.writeValueAsString(PendingTransferPayload.builder()
				.transferId(transferId)
				.build())
				.getBytes(StandardCharsets.UTF_8);
		final var msg = new Message(MessageTopic.PENDING_TRANSFER.getTopicName(), body);
		final var localArgs = PendingTransferLocalArgs.builder()
				.transferId(transferId)
				.fromUserId(request.getFromUserId())
				.toUserId(request.getToUserId())
				.amount(request.getAmount())
				.build();
		return producer.sendMessageInTransaction(msg, localArgs);
	}
}
