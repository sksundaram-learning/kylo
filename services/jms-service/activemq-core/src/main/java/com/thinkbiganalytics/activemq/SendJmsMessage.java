/*
 * Copyright (c) 2016.
 */

package com.thinkbiganalytics.activemq;

import com.thinkbiganalytics.activemq.config.ActiveMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * Created by sr186054 on 3/3/16.
 */
@Component
public class SendJmsMessage {
    private static final Logger LOG = LoggerFactory.getLogger(SendJmsMessage.class);

    @Autowired
    ObjectMapperSerializer objectMapperSerializer;

    @Autowired
    @Qualifier("jmsTemplate")
    private JmsMessagingTemplate jmsMessagingTemplate;



    public void sendMessage(Topic topic, String msg) {
        this.sendObject(topic,msg);
    }



    public void sendObject(Topic topic, final Object obj,final String objectClassType) throws JmsException{
        LOG.info("Sending ActiveMQ message ["+obj+"] to topic ["+topic+"]");
        MessageCreator creator = new MessageCreator() {
            TextMessage message = null;
            @Override
            public javax.jms.Message createMessage(Session session) throws JMSException {
                message = session.createTextMessage();
                message.setStringProperty("jms_javatype", objectClassType);
                message.setText(objectMapperSerializer.serialize(obj));
                return message;
            }
        };
        this.jmsMessagingTemplate.getJmsTemplate().send(topic, creator);
    }

    public void sendObject(Topic topic, final Object obj){
        sendObject(topic, obj, obj.getClass().getName());
    }
}