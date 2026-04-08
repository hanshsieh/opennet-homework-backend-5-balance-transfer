package com.example.demo.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
	@JsonProperty("id")
	private String id;

	@JsonProperty("fromUserId")
	private String fromUserId;

	@JsonProperty("toUserId")
	private String toUserId;

	@JsonProperty("amount")
	private long amount;

	@JsonProperty("status")
	private String status;

	@JsonProperty("createdAt")
	private Instant createdAt;
}
