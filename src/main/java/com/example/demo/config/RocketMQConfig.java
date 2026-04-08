package com.example.demo.config;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.service.messaging.AppTransactionListener;

@Configuration
public class RocketMQConfig {

	@Bean(destroyMethod = "shutdown")
	public TransactionMQProducer transactionMQProducer(
			RocketMQProperties properties,
			AppTransactionListener transactionListener) throws MQClientException {
		final var producer = new TransactionMQProducer(properties.getProducer().getGroup());
		producer.setNamesrvAddr(properties.getNameServer());
		producer.setTransactionListener(transactionListener);
		producer.start();
		return producer;
	}
}
