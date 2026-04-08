package com.example.demo.messaging;

/** Local argument for RocketMQ transactional send (not serialized to the broker). */
public record PendingTransferLocalArgs(
		String transferId,
		String fromUserId,
		String toUserId,
		long amount
) {
}
