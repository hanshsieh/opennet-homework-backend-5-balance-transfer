package com.example.demo.service.messaging;

import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.config.RocketMQProperties;
import com.example.demo.config.RocketMQTopic;

@ExtendWith(MockitoExtension.class)
class TransferMessageConsumerTest {

	@Mock
	private TransferMessageListener listener;

	private RocketMQProperties buildProperties() {
		final var properties = new RocketMQProperties();
		properties.setNameServer("127.0.0.1:9876");
		final var consumer = new RocketMQProperties.Consumer();
		consumer.setGroup("g1");
		consumer.setVipChannelEnabled(Boolean.FALSE);
		properties.setConsumer(consumer);
		final var producer = new RocketMQProperties.Producer();
		producer.setGroup("pg");
		producer.setVipChannelEnabled(Boolean.TRUE);
		producer.setSendMsgTimeoutMs(3000);
		properties.setProducer(producer);
		return properties;
	}

	@Test
	void start_shouldInitializeAndStartConsumer() throws MQClientException {
		final var properties = buildProperties();
		final var consumerHolder = new TransferMessageConsumer(properties, listener);
		try (var construction = mockConstruction(DefaultMQPushConsumer.class)) {
			consumerHolder.start();

			final var created = construction.constructed().getFirst();
			verify(created).setNamesrvAddr("127.0.0.1:9876");
			verify(created).setVipChannelEnabled(false);
			verify(created).subscribe(RocketMQTopic.PENDING_TRANSFER.getTopicName(), "*");
			verify(created).registerMessageListener(listener);
			verify(created).start();
		}
	}

	@Test
	void shutdown_shouldShutdownConsumerAfterStart() throws MQClientException {
		final var properties = buildProperties();
		final var consumerHolder = new TransferMessageConsumer(properties, listener);
		try (var construction = mockConstruction(DefaultMQPushConsumer.class)) {
			consumerHolder.start();
			final var created = construction.constructed().getFirst();

			consumerHolder.shutdown();

			verify(created).shutdown();
		}
	}

	@Test
	void shutdown_shouldDoNothingWhenNeverStarted() {
		final var properties = buildProperties();
		final var consumerHolder = new TransferMessageConsumer(properties, listener);

		consumerHolder.shutdown();
	}
}
