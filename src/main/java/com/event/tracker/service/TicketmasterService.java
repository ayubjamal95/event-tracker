package com.event.tracker.service;

import com.event.tracker.model.Event;
import com.event.tracker.model.api.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TicketmasterService {

    @Value("${ticketmaster.api.key}")
    private String apiKey;

    @Value("${ticketmaster.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public TicketmasterService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void init() {


        // Force IPv4 stack to avoid macOS Netty IPv6 issues
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public List<Event> searchEvents(String city, String startDate, String endDate) {
        try {
            // Build full URI including API key and query params
            String uri = UriComponentsBuilder.fromUriString(apiUrl) // full URL from properties
                    .queryParam("apikey", apiKey)
                    .queryParam("city", city)
                    .queryParam("startDateTime", startDate + "T00:00:00Z")
                    .queryParam("endDateTime", endDate + "T23:59:59Z")
                    .queryParam("size", 20)
                    .queryParam("sort", "relevance,desc")
                    .build()
                    .toUriString();

            log.info("Ticketmaster Request URL: {}", uri);

            TicketmasterResponse response = webClient.get()
                    .uri(uri) // use the full URL directly
                    .retrieve()
                    .bodyToMono(TicketmasterResponse.class)
                    .block();

            log.info("Ticketmaster response URL: {}", response);
            if (response != null && response.get_embedded() != null &&
                    response.get_embedded().getEvents() != null) {
                return response.get_embedded().getEvents().stream()
                        .map(this::convertToEvent)
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Ticketmaster API error: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Event convertToEvent(TicketmasterEvent tmEvent) {
        Event event = new Event();
        event.setName(tmEvent.getName());

        if (tmEvent.getDates() != null && tmEvent.getDates().getStart() != null) {
            event.setDate(tmEvent.getDates().getStart().getLocalDate());
        }

        if (tmEvent.getClassifications() != null && !tmEvent.getClassifications().isEmpty() &&
                tmEvent.getClassifications().get(0).getSegment() != null) {
            event.setType(tmEvent.getClassifications().get(0).getSegment().getName());
        } else {
            event.setType("Event");
        }

        if (tmEvent.get_embedded() != null &&
                tmEvent.get_embedded().getVenues() != null &&
                !tmEvent.get_embedded().getVenues().isEmpty()) {
            event.setVenue(tmEvent.get_embedded().getVenues().get(0).getName());
        } else {
            event.setVenue("TBA");
        }

        return event;
    }
}
