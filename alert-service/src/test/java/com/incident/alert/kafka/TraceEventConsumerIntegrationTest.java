package com.incident.alert.kafka;

import com.incident.alert.model.entity.AlertEntity;
import com.incident.alert.model.kafka.TraceIngestedEvent;
import com.incident.alert.model.kafka.TraceIngestedPayload;
import com.incident.alert.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class TraceEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private AlertRepository alertRepository;

    @MockBean
    private com.incident.alert.redis.AlertRedisPublisher alertRedisPublisher;

    @Test
    void shouldConsumeEventAndCreateAlert() throws Exception {
        String traceId = "trace-int-123";
        TraceIngestedPayload payload = TraceIngestedPayload.builder()
                .traceId(traceId)
                .status("ERROR")
                .failureType("RUNTIME_EXCEPTION")
                .rootService("api-gateway")
                .rootOperation("GET /users")
                .build();

        TraceIngestedEvent event = TraceIngestedEvent.builder()
                .eventId("event-123")
                .payload(payload)
                .build();

        kafkaTemplate.send("trace-ingested-test-topic", traceId, event);

        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            Optional<AlertEntity> found = alertRepository.findFirstByTraceId(traceId);
            return found.isPresent();
        });

        Optional<AlertEntity> found = alertRepository.findFirstByTraceId(traceId);
        assertTrue(found.isPresent());
        assertTrue(found.get().getAlertId().startsWith("ALT-"));
    }
}
