package com.example.demo.service.messaging;

import java.util.List;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.service.TransferSettlementService;
import com.example.demo.service.messaging.payload.PendingTransferPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TransferMessageListener implements MessageListenerConcurrently {

	private static final Logger log = LoggerFactory.getLogger(TransferMessageListener.class);

	private final ObjectMapper objectMapper;
	private final TransferSettlementService settlementService;

	public TransferMessageListener(
			ObjectMapper objectMapper,
			TransferSettlementService settlementService) {
		this.objectMapper = objectMapper;
		this.settlementService = settlementService;
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
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
	}
}
