package com.example.demo.service.messaging;

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
public class PendingTransferPayload {
	@JsonProperty("transferId")
	private String transferId;
}
