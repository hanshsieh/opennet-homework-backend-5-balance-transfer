package com.example.demo.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.config.RocketMQProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TransferEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(TransferEventPublisher.class);

	private final DefaultMQProducer producer;
	private final RocketMQProperties properties;
	private final ObjectMapper objectMapper;

	public TransferEventPublisher(
		final DefaultMQProducer producer,
		final RocketMQProperties properties,
		final ObjectMapper objectMapper) {
		this.producer = producer;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public void publishSettled(String transferId, String fromUserId, String toUserId, long amount) {
		publish(new TransferEventPayload("TRANSFER_SETTLED", transferId, fromUserId, toUserId, amount,
				Instant.now()));
	}

	public void publishCancelled(String transferId, String fromUserId, String toUserId, long amount) {
		publish(new TransferEventPayload("TRANSFER_CANCELLED", transferId, fromUserId, toUserId, amount,
				Instant.now()));
	}

	private void publish(TransferEventPayload payload) {
		try {
			final var body = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
			final var msg = new Message(properties.getTopic().getEvents(), body);
			producer.send(msg);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize transfer event, skipping MQ send: {}", e.getMessage());
		} catch (InterruptedException e) {
			// Restore interrupt status
			Thread.currentThread().interrupt();
			log.warn("Interrupted while sending transfer event to RocketMQ");
		} catch (Exception e) {
			log.warn("Failed to send transfer event to RocketMQ (DB already committed): {}", e.getMessage());
		}
	}
}
