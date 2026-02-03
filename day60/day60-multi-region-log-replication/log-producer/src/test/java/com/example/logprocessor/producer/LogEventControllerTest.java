package com.example.logprocessor.producer;

import com.example.logprocessor.producer.model.ProduceRequest;
import com.example.logprocessor.producer.service.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for LogEventController.
 * KafkaProducerService is mocked so no Kafka broker is required.
 */
@WebMvcTest(LogEventController.class)
class LogEventControllerTest {

    private final MockMvc mockMvc;
    @MockBean
    private final KafkaProducerService producerService;

    LogEventControllerTest(MockMvc mockMvc, KafkaProducerService producerService) {
        this.mockMvc = mockMvc;
        this.producerService = producerService;
    }

    @Test
    void produceEvent_returnsAccepted() throws Exception {
        // Mock returns a completed future (event "sent")
        SettableListenableFuture<org.springframework.kafka.support.SendResult<String, com.example.logprocessor.producer.model.LogEvent>> future = new SettableListenableFuture<>();
        future.set(null);
        org.mockito.Mockito.when(producerService.publish(org.mockito.ArgumentMatchers.any()))
                .thenReturn(future);

        String body = """
                {
                    "serviceName": "auth-service",
                    "level": "INFO",
                    "message": "User login successful",
                    "correlationId": "trace-abc-123"
                }
                """;

        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.eventId").isNotEmpty());
    }

    @Test
    void produceBatch_rejectsEmptyBatch() throws Exception {
        mockMvc.perform(post("/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
