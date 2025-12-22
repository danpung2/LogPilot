package com.logpilot.server.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SeekRequest {

    public enum SeekOperation {
        EARLIEST,
        LATEST,
        SPECIFIC
    }

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "Consumer ID is required")
    private String consumerId;

    @NotNull(message = "Operation is required")
    private SeekOperation operation;

    private Long logId; // Required if operation is SPECIFIC

    // Getters and Setters
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public SeekOperation getOperation() {
        return operation;
    }

    public void setOperation(SeekOperation operation) {
        this.operation = operation;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }
}
