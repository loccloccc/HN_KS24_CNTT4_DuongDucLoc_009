package org.example.registrationservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.registrationservice.clients.EventClient;
import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.exceptions.EventServiceException;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventGatewayService {

    private final EventClient eventClient;

    @CircuitBreaker(name = "registrationService", fallbackMethod = "getEventByIdFallback")
    public EventResponse getEventById(Long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (FeignException.NotFound exception) {
            throw new EventNotFoundException(eventId);
        } catch (FeignException exception) {
            throw new EventServiceException("Lỗi : ", exception);
        }
    }

    private EventResponse getEventByIdFallback(Long eventId, Throwable throwable) {
        if (throwable instanceof EventNotFoundException notFoundException) {
            throw notFoundException;
        }
        if (throwable instanceof EventServiceException serviceException) {
            throw serviceException;
        }
        throw new EventServiceException("Lỗi : ", throwable);
    }
}
