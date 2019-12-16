package com.zent.jhipstervue.web.rest;

import com.zent.jhipstervue.service.JhipstervueKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jhipstervue-kafka")
public class JhipstervueKafkaResource {

    private final Logger log = LoggerFactory.getLogger(JhipstervueKafkaResource.class);

    private JhipstervueKafkaProducer kafkaProducer;

    public JhipstervueKafkaResource(JhipstervueKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/publish")
    public void sendMessageToKafkaTopic(@RequestParam("message") String message) {
        log.debug("REST request to send to Kafka topic the message : {}", message);
        this.kafkaProducer.send(message);
    }
}
