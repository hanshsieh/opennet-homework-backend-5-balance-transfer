package com.example.demo.service.messaging;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.stereotype.Component;

import com.example.demo.config.RocketMQProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
/**
 * Manages RocketMQ push consumer lifecycle for transfer events.
 */
public class TransferMessageConsumer {

	private final RocketMQProperties properties;
	private final TransferMessageListener listener;

	private DefaultMQPushConsumer consumer;

	/**
	 * Creates a transfer message consumer component.
	 *
	 * @param properties RocketMQ properties
	 * @param listener transfer message listener
	 */
	public TransferMessageConsumer(
			RocketMQProperties properties,
			TransferMessageListener listener) {
		this.properties = properties;
		this.listener = listener;
	}

	@PostConstruct
	/**
	 * Starts the RocketMQ consumer.
	 */
	public void start() throws MQClientException {
		consumer = new DefaultMQPushConsumer(properties.getConsumer().getGroup());
		consumer.setNamesrvAddr(properties.getNameServer());
		consumer.setVipChannelEnabled(properties.getConsumer().getVipChannelEnabled());
		consumer.subscribe(MessageTopic.PENDING_TRANSFER.getTopicName(), "*");
		consumer.registerMessageListener(listener);
		consumer.start();
	}

	@PreDestroy
	/**
	 * Shuts down the RocketMQ consumer if initialized.
	 */
	public void shutdown() {
		if (consumer != null) {
			consumer.shutdown();
		}
	}
}
