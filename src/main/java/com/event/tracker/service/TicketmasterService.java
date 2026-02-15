package com.event.tracker.service;

import com.event.tracker.model.Event;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TicketmasterService {

    private final WebClient webClient;
    private final String apiKey;

    public TicketmasterService(
            @Qualifier("ticketmasterWebClient") WebClient webClient,
            @Value("${ticketmaster.api.key}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    public List<Event> fetchEvents(String city, String countryCode,
                                   LocalDate startDate, LocalDate endDate,
                                   int radiusKm) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/events.json")
                            .queryParam("apikey", apiKey)
                            .queryParam("city", city)
                            .queryParam("countryCode", countryCode)
                            .queryParam("startDateTime", startDate.atStartOfDay().format(
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z")
                            .queryParam("endDateTime", endDate.atTime(23, 59).format(
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z")
                            .queryParam("radius", radiusKm)
                            .queryParam("unit", "km")
                            .queryParam("size", 50)
                            .queryParam("sort", "relevance,desc")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEvents(response);

        } catch (Exception e) {
            log.error("Error fetching events from Ticketmaster", e);
            return List.of();
        }
    }

    private List<Event> parseEvents(String jsonResponse) {
        List<Event> events = new ArrayList<>();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            JsonNode embedded = root.path("_embedded");
            if (embedded.isMissingNode()) {
                return events;
            }

            JsonNode eventsNode = embedded.path("events");
            if (!eventsNode.isArray()) {
                return events;
            }

            for (JsonNode eventNode : eventsNode) {
                Event event = parseEvent(eventNode);
                if (event != null) {
                    events.add(event);
                }
            }

        } catch (Exception e) {
        }

        return events;
    }

    private Event parseEvent(JsonNode eventNode) {
        try {
            String id = eventNode.path("id").asText();
            String name = eventNode.path("name").asText();
            String type = eventNode.path("classifications").get(0)
                    .path("segment").path("name").asText("event");

            JsonNode dates = eventNode.path("dates").path("start");
            LocalDate date = LocalDate.parse(dates.path("localDate").asText());
            LocalTime time = dates.has("localTime")
                    ? LocalTime.parse(dates.path("localTime").asText())
                    : null;

            JsonNode venueNode = eventNode.path("_embedded").path("venues").get(0);
            String venue = venueNode.path("name").asText("Unknown Venue");

            // Estimate capacity and visitors
            int capacity = estimateCapacity(type, venueNode);
            int expectedVisitors = (int) (capacity * 0.85); // Assume 85% attendance

            // Calculate impact level
            String impactLevel = calculateImpactLevel(capacity, type);

            return Event.builder()
                    .id(id)
                    .name(name)
                    .type(type.toLowerCase())
                    .venue(venue)
                    .date(date)
                    .time(time)
                    .capacity(capacity)
                    .expectedVisitors(expectedVisitors)
                    .distanceKm(0.0) // Will be calculated if geo data available
                    .impactLevel(impactLevel)
                    .ticketAvailability("available")
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    private int estimateCapacity(String type, JsonNode venueNode) {
        // Try to get actual capacity
        JsonNode capacityNode = venueNode.path("capacity");
        if (!capacityNode.isMissingNode()) {
            return capacityNode.asInt();
        }

        // Estimate based on type
        return switch (type.toLowerCase()) {
            case "music", "concert" -> 15000;
            case "sports" -> 20000;
            case "arts", "theatre" -> 2000;
            case "family" -> 5000;
            default -> 10000;
        };
    }

    private String calculateImpactLevel(int capacity, String type) {
        if (capacity > 15000) return "high";
        if (capacity > 5000) return "medium";
        return "low";
    }
}