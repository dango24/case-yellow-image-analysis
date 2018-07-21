package com.icarusrises.caseyellowimageanalysis.queues.services;

import com.icarusrises.caseyellowimageanalysis.queues.model.MessageType;

import javax.jms.JMSException;

public interface MessageProducerService {
    <T extends Object> void send(MessageType type, T payload) throws JMSException;
}
