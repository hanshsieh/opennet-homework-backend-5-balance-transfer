package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.service.messaging.AppTransactionListener;

@ExtendWith(MockitoExtension.class)
class RocketMQConfigTest {

	@Mock
	private AppTransactionListener transactionListener;

	@Test
	void transactionMQProducer_shouldInitializeAndStartProducer() throws MQClientException {
		final var properties = buildProperties();
		final var config = new RocketMQConfig();
		final var constructorArg = new AtomicReference<Object>();
		try (var construction = mockConstruction(
				TransactionMQProducer.class,
				(mock, context) -> constructorArg.set(context.arguments().getFirst()))) {
			final var producer = config.transactionMQProducer(properties, transactionListener);

			final var created = construction.constructed().getFirst();
			assertThat(producer).isSameAs(created);
			assertThat(constructorArg.get()).isEqualTo("producer-group");
			verify(created).setNamesrvAddr("127.0.0.1:9876");
			verify(created).setVipChannelEnabled(false);
			verify(created).setSendMsgTimeout(3000);
			verify(created).setTransactionListener(transactionListener);
			verify(created).start();
		}
	}

	private RocketMQProperties buildProperties() {
		final var properties = new RocketMQProperties();
		properties.setNameServer("127.0.0.1:9876");
		final var producer = new RocketMQProperties.Producer();
		producer.setGroup("producer-group");
		producer.setVipChannelEnabled(Boolean.FALSE);
		producer.setSendMsgTimeoutMs(3000);
		properties.setProducer(producer);
		final var consumer = new RocketMQProperties.Consumer();
		consumer.setGroup("consumer-group");
		consumer.setVipChannelEnabled(Boolean.TRUE);
		properties.setConsumer(consumer);
		return properties;
	}
}
