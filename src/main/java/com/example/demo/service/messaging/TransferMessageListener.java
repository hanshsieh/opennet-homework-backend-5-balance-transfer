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
/**
 * Consumes pending transfer events and triggers settlement.
 */
public class TransferMessageListener implements MessageListenerConcurrently {

	private static final Logger log = LoggerFactory.getLogger(TransferMessageListener.class);

	private final ObjectMapper objectMapper;
	private final TransferSettlementService settlementService;

	/**
	 * Creates a transfer message listener.
	 *
	 * @param objectMapper mapper used to decode transfer payloads
	 * @param settlementService service that applies transfer settlement
	 */
	public TransferMessageListener(
			ObjectMapper objectMapper,
			TransferSettlementService settlementService) {
		this.objectMapper = objectMapper;
		this.settlementService = settlementService;
	}

	@Override
	/**
	 * Consumes transfer messages and settles each transfer.
	 *
	 * @param msgs message batch
	 * @param context consume context
	 * @return consume result
	 */
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for (var msg : msgs) {
			try {
				final var transferId = objectMapper.readValue(msg.getBody(), PendingTransferPayload.class)
						.getTransferId();
				settlementService.settle(transferId);
			} catch (Exception e) {
				// Retry later so settlement can be retried when transient failures recover.
				log.error("Failed to process transfer message", e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
}
