package com.zent.jhipstervue.web.rest;

import com.zent.jhipstervue.JhipstervueApp;
import com.zent.jhipstervue.service.JhipstervueKafkaProducer;
import com.zent.jhipstervue.service.JhipstervueKafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.KafkaContainer;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JhipstervueApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class JhipstervueKafkaResourceIT {

    private MockMvc restMockMvc;

    private static boolean started = false;

    private static KafkaContainer kafkaContainer;

    @Autowired
    private JhipstervueKafkaProducer producer;

    @Autowired
    private JhipstervueKafkaConsumer consumer;

    private static final int MAX_ATTEMPT = 5;

    @BeforeAll
    public static void startServer() {
        if (!started) {
            startTestcontainer();
            started = true;
        }
    }

    private static void startTestcontainer() {
        kafkaContainer = new KafkaContainer("5.3.1");
        kafkaContainer.start();
        System.setProperty("kafkaBootstrapServers", kafkaContainer.getBootstrapServers());
    }

    @BeforeEach
    public void setup() {
        JhipstervueKafkaResource kafkaResource = new JhipstervueKafkaResource(producer);

        this.restMockMvc = MockMvcBuilders.standaloneSetup(kafkaResource)
            .build();

        producer.init();
        consumer.start();
    }

    @Test
    public void producedMessageHasBeenConsumed() throws Exception {
        restMockMvc.perform(post("/api/jhipstervue-kafka/publish?message=test"))
            .andExpect(status().isOk());

        Map<MetricName, ? extends Metric> metrics = consumer.getKafkaConsumer().metrics();

        Metric recordsConsumedTotalMetric = metrics.entrySet().stream()
            .filter(entry -> "records-consumed-total".equals(entry.getKey().name()))
            .findFirst()
            .get()
            .getValue();

        Double expectedTotalConsumedMessage = 1.0;
        Double totalConsumedMessage;
        int attempt = 0;
        do {
            totalConsumedMessage = (Double) recordsConsumedTotalMetric.metricValue();
            Thread.sleep(200);
        } while (!totalConsumedMessage.equals(expectedTotalConsumedMessage) && attempt++ < MAX_ATTEMPT);

        Assertions.assertThat(attempt).isLessThan(MAX_ATTEMPT);
        Assertions.assertThat(totalConsumedMessage).isEqualTo(expectedTotalConsumedMessage);
    }

}

