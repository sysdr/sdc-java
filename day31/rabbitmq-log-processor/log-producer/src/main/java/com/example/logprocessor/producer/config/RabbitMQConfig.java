package com.example.logprocessor.producer.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TOPIC_EXCHANGE = "logs.topic";
    public static final String CRITICAL_QUEUE = "logs-critical";
    public static final String PROCESSING_QUEUE = "logs-processing";
    public static final String MONITORING_QUEUE = "logs-monitoring";
    public static final String DLQ_EXCHANGE = "logs.dlx";
    public static final String DLQ_QUEUE = "logs-dlq";

    @Bean
    public TopicExchange logsExchange() {
        return ExchangeBuilder
            .topicExchange(TOPIC_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public Queue criticalQueue() {
        return QueueBuilder
            .durable(CRITICAL_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "dlq")
            .withArgument("x-message-ttl", 86400000) // 24 hours
            .build();
    }

    @Bean
    public Queue processingQueue() {
        return QueueBuilder
            .durable(PROCESSING_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "dlq")
            .withArgument("x-message-ttl", 86400000)
            .build();
    }

    @Bean
    public Queue monitoringQueue() {
        return QueueBuilder
            .durable(MONITORING_QUEUE)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "dlq")
            .withArgument("x-message-ttl", 86400000)
            .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
            .directExchange(DLQ_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
            .durable(DLQ_QUEUE)
            .withArgument("x-message-ttl", 604800000) // 7 days
            .build();
    }

    @Bean
    public Binding criticalBinding() {
        return BindingBuilder
            .bind(criticalQueue())
            .to(logsExchange())
            .with("logs.error.*");
    }

    @Bean
    public Binding processingBinding() {
        return BindingBuilder
            .bind(processingQueue())
            .to(logsExchange())
            .with("logs.#");
    }

    @Bean
    public Binding monitoringBinding() {
        return BindingBuilder
            .bind(monitoringQueue())
            .to(logsExchange())
            .with("*.*.auth");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message not confirmed: " + cause);
            }
        });
        return template;
    }
}
