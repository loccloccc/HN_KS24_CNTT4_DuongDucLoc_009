package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.util.StringUtils;


public class RegistrationCreatedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationCreatedConsumer.class);

    private final EmailService emailService;

    public RegistrationCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${notification.kafka.order-created-topic}")
    public void consume(String email) {
        if (!StringUtils.hasText(email)) {
            LOGGER.warn("email trống hoặc email không gửi được thông báo");
            return;
        }
        emailService.sendOrderCreatedEmail(email.trim());
    }
}
