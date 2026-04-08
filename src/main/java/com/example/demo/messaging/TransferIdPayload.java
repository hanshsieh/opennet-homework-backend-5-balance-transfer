package com.example.demo.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RocketMQ message body: transfer id only (create / cancel notifications).
 */
public record TransferIdPayload(@JsonProperty("transferId") String transferId) {
}
