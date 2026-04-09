package com.example.demo.service.messaging;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.config.RocketMQTopic;

@Component
public class AppTransactionListener implements TransactionListener {

	private static final Logger log = LoggerFactory.getLogger(AppTransactionListener.class);

	private final Map<RocketMQTopic, TopicLocalTransactionListener> listenersByTopic;

	public AppTransactionListener(List<TopicLocalTransactionListener> topicListeners) {
		this.listenersByTopic = topicListeners.stream()
				.collect(Collectors.toMap(TopicLocalTransactionListener::topic, Function.identity(), (a, b) -> {
					throw new IllegalArgumentException("Duplicate transaction listener topic: " + a.topic());
				}));
	}

	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		final var listenerOpt = resolve(msg);
		if (listenerOpt.isEmpty()) {
			log.error("No TopicLocalTransactionListener for topic [{}]", msg.getTopic());
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
		return listenerOpt.get().executeLocalTransaction(msg, arg);
	}

	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		final var listenerOpt = resolve(msg);
		if (listenerOpt.isEmpty()) {
			log.error("No TopicLocalTransactionListener for check, topic [{}]", msg.getTopic());
			return LocalTransactionState.UNKNOW;
		}
		return listenerOpt.get().checkLocalTransaction(msg);
	}

	private java.util.Optional<TopicLocalTransactionListener> resolve(Message msg) {
		return RocketMQTopic.fromTopicName(msg.getTopic())
				.map(listenersByTopic::get);
	}
}
