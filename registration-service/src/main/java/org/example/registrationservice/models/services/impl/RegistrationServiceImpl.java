package org.example.registrationservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.registrationservice.models.constants.RegistrationStatus;
import org.example.registrationservice.models.dto.requests.CreateRegistrationDetailRequest;
import org.example.registrationservice.models.dto.requests.CreateRegistrationRequest;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.dto.responses.RegistrationDetailResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.example.registrationservice.models.entities.Registration;
import org.example.registrationservice.models.entities.RegistrationDetail;
import org.example.registrationservice.models.repositories.RegistrationDetailRepository;
import org.example.registrationservice.models.repositories.RegistrationRepository;
import org.example.registrationservice.models.services.RegistrationService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private static final String REGISTRATION_CREATED_TOPIC = "registration-created";

    private final RegistrationRepository registrationRepository;
    private final RegistrationDetailRepository registrationDetailRepository;
    private final EventGatewayService eventGatewayService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public RegistrationResponse createRegistration(CreateRegistrationRequest request) {
        Registration registration = Registration.builder()
                .participantName(request.participantName())
                .participantEmail(request.participantEmail())
                .totalAmount(0D)
                .status(RegistrationStatus.PENDING)
                .build();

        registration = registrationRepository.save(registration);

        List<RegistrationDetailResponse> detailResponses = new ArrayList<>();
        Map<Long, Integer> requestedTickets = new HashMap<>();
        double totalAmount = 0D;

        for (CreateRegistrationDetailRequest item : request.items()) {
            EventResponse event = eventGatewayService.getEventById(item.eventId());

            int totalRequested = requestedTickets.merge(event.id(), item.quantity(), Integer::sum);
            if (totalRequested > event.availableTickets()) {
                throw new IllegalArgumentException(
                        "Số lượng vé không đủ cho mã sự kiện : " + event.id()
                );
            }

            double lineTotal = event.ticketPrice() * item.quantity();

            RegistrationDetail detail = RegistrationDetail.builder()
                    .registration(registration)
                    .eventId(event.id())
                    .quantity(item.quantity())
                    .ticketPrice(event.ticketPrice())
                    .lineTotal(lineTotal)
                    .build();
            detail = registrationDetailRepository.save(detail);

            detailResponses.add(new RegistrationDetailResponse(
                    detail.getId(),
                    event.id(),
                    item.quantity(),
                    event.ticketPrice(),
                    lineTotal
            ));
            totalAmount += lineTotal;
        }

        registration.setTotalAmount(totalAmount);
        registration = registrationRepository.save(registration);

        kafkaTemplate.send(REGISTRATION_CREATED_TOPIC, request.participantEmail());

        return new RegistrationResponse(
                registration.getId(),
                registration.getParticipantName(),
                registration.getParticipantEmail(),
                registration.getTotalAmount(),
                registration.getStatus(),
                detailResponses
        );
    }
}

