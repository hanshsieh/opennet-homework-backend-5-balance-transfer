package com.example.demo.service.messaging;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ transactional half-message handler scoped by {@link Message#getTopic()}.
 */
public interface TopicLocalTransactionListener {

	/**
	 * Returns the topic handled by this listener.
	 *
	 * @return supported topic
	 */
	MessageTopic topic();

	/**
	 * Executes local transaction branch for a half message.
	 *
	 * @param msg half message
	 * @param arg local transaction arguments
	 * @return local transaction state for broker commit/rollback decision
	 */
	LocalTransactionState executeLocalTransaction(Message msg, Object arg);

	/**
	 * Checks local transaction state when broker initiates transaction check.
	 *
	 * @param msg message to check
	 * @return local transaction state
	 */
	LocalTransactionState checkLocalTransaction(MessageExt msg);
}
