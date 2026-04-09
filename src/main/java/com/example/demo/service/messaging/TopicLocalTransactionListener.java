package com.example.demo.service.messaging;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import com.example.demo.config.RocketMQTopic;

/**
 * RocketMQ transactional half-message handler scoped by {@link Message#getTopic()}.
 */
public interface TopicLocalTransactionListener {

	RocketMQTopic topic();

	LocalTransactionState executeLocalTransaction(Message msg, Object arg);

	LocalTransactionState checkLocalTransaction(MessageExt msg);
}
