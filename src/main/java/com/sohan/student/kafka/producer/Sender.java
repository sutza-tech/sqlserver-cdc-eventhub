package com.sohan.student.kafka.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class Sender {

    private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

    @Autowired
    private KafkaTemplate<String, byte[]> producer;

    @Autowired
    private KafkaTemplate<String, Object> producerObject;

    public void sendAsBytes(String topic, byte[] data){
        LOG.info("sending data='{}' to topic='{}'", data.length, topic);

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, data);
        producer.send(record);
    }

    public void sendAsObject(String topic, Object data){
        LOG.info("sending data='{}' to topic='{}'", data.toString(), topic);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, data);
        producerObject.send(record);
    }

}