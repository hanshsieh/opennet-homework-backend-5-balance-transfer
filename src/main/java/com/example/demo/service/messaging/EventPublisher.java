package com.example.demo.service.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Service;

import com.example.demo.config.RocketMQProperties;
import com.example.demo.dto.TransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EventPublisher {

	private final TransactionMQProducer producer;
	private final RocketMQProperties properties;
	private final ObjectMapper objectMapper;

	public EventPublisher(
			final TransactionMQProducer producer,
			final RocketMQProperties properties,
			final ObjectMapper objectMapper) {
		this.producer = producer;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public TransactionSendResult sendPendingTransfer(String transferId, TransferRequest request)
			throws Exception {
		final var body = objectMapper.writeValueAsString(PendingTransferPayload.builder()
				.transferId(transferId)
				.build())
				.getBytes(StandardCharsets.UTF_8);
		final var msg = new Message(properties.getTopics().getPendingTransfer(), body);
		final var localArgs = PendingTransferLocalArgs.builder()
				.transferId(transferId)
				.fromUserId(request.getFromUserId())
				.toUserId(request.getToUserId())
				.amount(request.getAmount())
				.build();
		return producer.sendMessageInTransaction(msg, localArgs);
	}
}
