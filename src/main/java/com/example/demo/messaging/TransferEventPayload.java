package com.example.demo.messaging;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON payload published to RocketMQ for transfer lifecycle events.
 */
public record TransferEventPayload(
		@JsonProperty("eventType") String eventType,
		@JsonProperty("transferId") String transferId,
		@JsonProperty("fromUserId") String fromUserId,
		@JsonProperty("toUserId") String toUserId,
		@JsonProperty("amount") long amount,
		@JsonProperty("timestamp") Instant timestamp
) {
}
