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

@Component
public class AppTransactionListener implements TransactionListener {

	private static final Logger log = LoggerFactory.getLogger(AppTransactionListener.class);

	private final Map<String, TaggedLocalTransactionListener> listenersByTag;

	public AppTransactionListener(List<TaggedLocalTransactionListener> taggedListeners) {
		this.listenersByTag = taggedListeners.stream()
				.collect(Collectors.toMap(TaggedLocalTransactionListener::tag, Function.identity(), (a, b) -> {
					throw new IllegalArgumentException("Duplicate transaction listener tag: " + a.tag());
				}));
	}

	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		final var listener = resolve(msg);
		if (listener == null) {
			log.error("No TaggedLocalTransactionListener for tag [{}]", msg.getTags());
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
		return listener.executeLocalTransaction(msg, arg);
	}

	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		final var listener = resolve(msg);
		if (listener == null) {
			log.error("No TaggedLocalTransactionListener for check, tag [{}]", msg.getTags());
			return LocalTransactionState.UNKNOW;
		}
		return listener.checkLocalTransaction(msg);
	}

	private TaggedLocalTransactionListener resolve(Message msg) {
		final var tag = msg.getTags();
		if (tag == null || tag.isBlank()) {
			return null;
		}
		return listenersByTag.get(tag);
	}
}
