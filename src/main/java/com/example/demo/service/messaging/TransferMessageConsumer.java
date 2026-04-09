package com.example.demo.service.messaging;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.stereotype.Component;

import com.example.demo.config.RocketMQProperties;
import com.example.demo.config.RocketMQTopic;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class TransferMessageConsumer {

	private final RocketMQProperties properties;
	private final TransferMessageListener listener;

	private DefaultMQPushConsumer consumer;

	public TransferMessageConsumer(
			RocketMQProperties properties,
			TransferMessageListener listener) {
		this.properties = properties;
		this.listener = listener;
	}

	@PostConstruct
	public void start() throws MQClientException {
		consumer = new DefaultMQPushConsumer(properties.getConsumer().getGroup());
		consumer.setNamesrvAddr(properties.getNameServer());
		consumer.setVipChannelEnabled(properties.getConsumer().getVipChannelEnabled());
		consumer.subscribe(RocketMQTopic.PENDING_TRANSFER.getTopicName(), "*");
		consumer.registerMessageListener(listener);
		consumer.start();
	}

	@PreDestroy
	public void shutdown() {
		if (consumer != null) {
			consumer.shutdown();
		}
	}
}
