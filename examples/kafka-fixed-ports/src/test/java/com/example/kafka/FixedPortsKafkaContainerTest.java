package com.example.kafka;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FixedPortsKafkaContainerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedPortsKafkaContainerTest.class.getCanonicalName());

    private KafkaContainer kafka = new FixedPortsKafkaContainer()
        .withFixedExposedPort(9093, 9093)
        .withEmbeddedZookeeper()
        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
        .waitingFor(Wait.forLogMessage(".*KafkaServer id=1.*", 1));

    @BeforeAll
    void startContainer() throws Exception {
        kafka.start();
    }

    @AfterAll
    void stopContainer() {
        kafka.stop();
    }

    @Test
    public void test() throws Exception {
        testKafkaFunctionality(kafka.getBootstrapServers());
    }

    protected void testKafkaFunctionality(String bootstrapServers) throws Exception {
        try (
            KafkaProducer<String, String> producer = new KafkaProducer<>(
                Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                    ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString()
                ),
                new StringSerializer(),
                new StringSerializer()
            );

            KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(
                Map.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID(),
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                new StringDeserializer()
            );
        ) {
            String topicName = "messages";
            consumer.subscribe(Arrays.asList(topicName));

            producer.send(new ProducerRecord<>(topicName, "testcontainers", "rulezzz")).get();

            Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                if (records.isEmpty()) {
                    return false;
                }

                assertThat(records)
                    .hasSize(1)
                    .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
                    .containsExactly(tuple(topicName, "testcontainers", "rulezzz"));

                return true;
            });

            consumer.unsubscribe();
        }
    }

}
