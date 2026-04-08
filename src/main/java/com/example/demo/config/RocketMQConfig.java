package com.example.demo.config;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.messaging.TransferTransactionListener;

@Configuration
public class RocketMQConfig {

	@Bean(destroyMethod = "shutdown")
	public TransactionMQProducer transactionMQProducer(
			RocketMQProperties properties,
			TransferTransactionListener transactionListener) throws MQClientException {
		TransactionMQProducer producer = new TransactionMQProducer(properties.getProducer().getGroup());
		producer.setNamesrvAddr(properties.getNameServer());
		producer.setTransactionListener(transactionListener);
		producer.start();
		return producer;
	}
}
