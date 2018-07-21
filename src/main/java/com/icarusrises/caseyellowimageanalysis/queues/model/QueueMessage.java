package com.icarusrises.caseyellowimageanalysis.queues.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class QueueMessage {

    @JsonProperty("message_type")
    private MessageType messageType;

    private String payload;
}
