package com.logpilot.server.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CommitOffsetRequest {

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "Consumer ID is required")
    private String consumerId;

    @Min(value = 0, message = "Last Log ID must be non-negative")
    private long lastLogId;

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

    public long getLastLogId() {
        return lastLogId;
    }

    public void setLastLogId(long lastLogId) {
        this.lastLogId = lastLogId;
    }
}
