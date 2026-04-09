package com.example.demo.service.messaging.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RocketMQ message body: transfer id only (create / cancel notifications).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Represents the PendingTransferPayload class.
 */
public class PendingTransferPayload {
	@JsonProperty("transferId")
	private String transferId;
}
