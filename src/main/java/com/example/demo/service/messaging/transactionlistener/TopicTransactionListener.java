package com.example.demo.service.messaging.transactionlistener;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import com.example.demo.service.messaging.MessageTopic;

/**
 * RocketMQ transactional half-message handler scoped by {@link Message#getTopic()}.
 */
public interface TopicTransactionListener extends TransactionListener {

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
	@Override
	LocalTransactionState executeLocalTransaction(Message msg, Object arg);

	/**
	 * Checks local transaction state when broker initiates transaction check.
	 *
	 * @param msg message to check
	 * @return local transaction state
	 */
	@Override
	LocalTransactionState checkLocalTransaction(MessageExt msg);
}
