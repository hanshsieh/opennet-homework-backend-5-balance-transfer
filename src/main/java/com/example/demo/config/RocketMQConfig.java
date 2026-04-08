package com.example.demo.config;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQConfig {

	@Bean(destroyMethod = "shutdown")
	public DefaultMQProducer defaultMQProducer(RocketMQProperties properties) throws MQClientException {
		DefaultMQProducer producer = new DefaultMQProducer(properties.getProducer().getGroup());
		producer.setNamesrvAddr(properties.getNameServer());
		producer.start();
		return producer;
	}
}
