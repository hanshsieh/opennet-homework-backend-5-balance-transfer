package com.example.demo.messaging;

import lombok.Builder;
import lombok.Getter;

/** Local argument for RocketMQ transactional send (not serialized to the broker). */
@Getter
@Builder
public class PendingTransferLocalArgs {

	private final String transferId;
	private final String fromUserId;
	private final String toUserId;
	private final long amount;
}
