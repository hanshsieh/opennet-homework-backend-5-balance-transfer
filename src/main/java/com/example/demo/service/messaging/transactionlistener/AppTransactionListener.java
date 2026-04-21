package com.example.demo.service.messaging.transactionlistener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.service.messaging.MessageTopic;

@Component
/**
 * Delegates RocketMQ transaction callbacks by topic.
 */
public class AppTransactionListener implements TransactionListener {

	private static final Logger log = LoggerFactory.getLogger(AppTransactionListener.class);

	private final Map<MessageTopic, TopicTransactionListener> listenersByTopic;

	/**
	 * Creates a listener router for all topic-specific local transaction handlers.
	 *
	 * @param topicListeners all registered topic listeners
	 */
	public AppTransactionListener(List<TopicTransactionListener> topicListeners) {
		this.listenersByTopic = topicListeners.stream()
				.collect(Collectors.toMap(TopicTransactionListener::topic, Function.identity(), (a, b) -> {
					// Duplicate topic handlers make transaction routing ambiguous and unsafe.
					throw new IllegalArgumentException("Duplicate transaction listener topic: " + a.topic());
				}));
	}

	@Override
	/**
	 * Executes local transaction logic for the incoming half message.
	 *
	 * @param msg RocketMQ message
	 * @param arg local transaction argument
	 * @return local transaction state
	 */
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		final var listenerOpt = resolve(msg);
		if (listenerOpt.isEmpty()) {
			log.error("No TopicLocalTransactionListener for topic [{}]", msg.getTopic());
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
		return listenerOpt.get().executeLocalTransaction(msg, arg);
	}

	@Override
	/**
	 * Checks local transaction state for broker transaction check requests.
	 *
	 * @param msg RocketMQ message
	 * @return local transaction state
	 */
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		final var listenerOpt = resolve(msg);
		if (listenerOpt.isEmpty()) {
			log.error("No TopicLocalTransactionListener for check, topic [{}]", msg.getTopic());
			return LocalTransactionState.UNKNOW;
		}
		return listenerOpt.get().checkLocalTransaction(msg);
	}

	/**
	 * Resolves a topic-specific listener from message topic metadata.
	 *
	 * @param msg RocketMQ message
	 * @return matched topic listener if present
	 */
	private Optional<TopicTransactionListener> resolve(Message msg) {
		return MessageTopic.fromTopicName(msg.getTopic())
				.map(listenersByTopic::get);
	}
}
