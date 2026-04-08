package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
	@NotBlank
	@JsonProperty("fromUserId")
	private String fromUserId;

	@NotBlank
	@JsonProperty("toUserId")
	private String toUserId;

	@Positive
	@JsonProperty("amount")
	private long amount;
}
