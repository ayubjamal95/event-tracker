package com.event.tracker.service;

import com.event.tracker.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
@Service
public class EventAnalysisService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final EventAggregatorService eventAggregatorService;

    public EventAnalysisService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            EventAggregatorService eventAggregatorService) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.eventAggregatorService = eventAggregatorService;
    }

    public EventAnalysisResponse analyzeEvents(EventAnalysisRequest request) {
        // First, get events from APIs
        List<Event> events = eventAggregatorService.getAllEvents(
                request.getCity(),
                request.getCheckIn(),
                request.getCheckOut()
        );
        // Convert events to JSON string for Claude
        String eventsJson;
        try {
            eventsJson = objectMapper.writeValueAsString(events);
        } catch (Exception e) {
            eventsJson = "[]";
        }

        String prompt = buildPrompt(request, eventsJson);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-sonnet-4-20250514");
        requestBody.put("max_tokens", 4000);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        try {
            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Anthropic response URL: {}", response);

            EventAnalysisResponse analysisResponse = parseResponse(response);
            // Add the events we found via APIs
            analysisResponse.setEvents(events);
            return analysisResponse;

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze events: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(EventAnalysisRequest request, String eventsJson) {
        return String.format("""
            You are a travel analysis assistant. I have already found the following events in %s from %s to %s:
            
            Events (JSON):
            %s
            
            Now please:
            1. Use the Trivago MCP server to search for hotels in %s for check-in %s and check-out %s
            2. Also search for hotel prices one week before (same day of week) as a baseline for comparison
            3. Calculate the price surge percentage if these events are causing price increases
            4. Suggest alternative nearby suburbs or cities if prices are elevated
            5. Provide analysis on how these events are impacting hotel availability and pricing
            
            Return your analysis as a JSON object with this structure:
            {
              "hotelPrices": {
                "eventDates": {"avgPrice": 250, "minPrice": 150, "maxPrice": 500, "sampleHotels": ["Hotel A - $200", "Hotel B - $300"]},
                "baseline": {"avgPrice": 120, "minPrice": 80, "maxPrice": 200}
              },
              "surgePct": 108,
              "alternatives": [{"location": "Nearby Suburb", "avgPrice": 130, "distance": "15 miles"}],
              "analysis": "Brief text analysis"
            }
            
            Only return the JSON, no other text.
            """,
                request.getCity(), request.getCheckIn(), request.getCheckOut(),
                eventsJson,
                request.getCity(), request.getCheckIn(), request.getCheckOut()
        );
    }

    private EventAnalysisResponse parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String textContent = root.path("content").get(0).path("text").asText();

            // Extract JSON from response
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
            Matcher matcher = pattern.matcher(textContent);

            if (matcher.find()) {
                String jsonStr = matcher.group();
                return objectMapper.readValue(jsonStr, EventAnalysisResponse.class);
            }

            throw new RuntimeException("Could not extract JSON from response");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
        }
    }
}
