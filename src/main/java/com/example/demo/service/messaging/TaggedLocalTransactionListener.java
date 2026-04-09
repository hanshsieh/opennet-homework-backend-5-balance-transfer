package com.example.demo.service.messaging;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ transactional half-message handler scoped by {@link Message#getTags()}.
 */
public interface TaggedLocalTransactionListener {

	String tag();

	LocalTransactionState executeLocalTransaction(Message msg, Object arg);

	LocalTransactionState checkLocalTransaction(MessageExt msg);
}
