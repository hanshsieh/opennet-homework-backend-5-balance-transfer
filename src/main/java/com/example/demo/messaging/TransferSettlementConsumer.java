package com.example.demo.messaging;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.config.RocketMQProperties;
import com.example.demo.service.TransferSettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class TransferSettlementConsumer {

	private static final Logger log = LoggerFactory.getLogger(TransferSettlementConsumer.class);

	private final RocketMQProperties properties;
	private final ObjectMapper objectMapper;
	private final TransferSettlementService settlementService;

	private DefaultMQPushConsumer consumer;

	public TransferSettlementConsumer(
			RocketMQProperties properties,
			ObjectMapper objectMapper,
			TransferSettlementService settlementService) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.settlementService = settlementService;
	}

	@PostConstruct
	public void start() throws MQClientException {
		consumer = new DefaultMQPushConsumer(properties.getConsumer().getGroup());
		consumer.setNamesrvAddr(properties.getNameServer());
		consumer.subscribe(properties.getTopics().getPendingTransfer(), "*");
		consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
			for (var msg : msgs) {
				try {
					final var transferId = objectMapper.readValue(msg.getBody(), PendingTransferPayload.class)
							.getTransferId();
					settlementService.settle(transferId);
				} catch (IllegalStateException e) {
					log.warn("Settlement will retry: {}", e.getMessage());
					return ConsumeConcurrentlyStatus.RECONSUME_LATER;
				} catch (Exception e) {
					log.error("Failed to process transfer message", e);
					return ConsumeConcurrentlyStatus.RECONSUME_LATER;
				}
			}
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		});
		consumer.start();
	}

	@PreDestroy
	public void shutdown() {
		if (consumer != null) {
			consumer.shutdown();
		}
	}
}
