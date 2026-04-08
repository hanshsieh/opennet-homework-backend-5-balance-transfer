package com.example.demo.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;

import com.example.demo.config.RocketMQProperties;
import com.example.demo.dto.TransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TransferEventPublisher {

	private final TransactionMQProducer producer;
	private final RocketMQProperties properties;
	private final ObjectMapper objectMapper;

	public TransferEventPublisher(
			final TransactionMQProducer producer,
			final RocketMQProperties properties,
			final ObjectMapper objectMapper) {
		this.producer = producer;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public TransactionSendResult sendPendingTransferTransactional(String transferId, TransferRequest request)
			throws Exception {
		byte[] body = objectMapper.writeValueAsString(new TransferIdPayload(transferId))
				.getBytes(StandardCharsets.UTF_8);
		Message msg = new Message(properties.getTopics().getPendingTransfer(), body);
		return producer.sendMessageInTransaction(msg,
				PendingTransferLocalArgs.builder()
						.transferId(transferId)
						.fromUserId(request.getFromUserId())
						.toUserId(request.getToUserId())
						.amount(request.getAmount())
						.build());
	}
}
