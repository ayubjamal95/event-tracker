package com.event.tracker.service;

import com.event.tracker.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class PriceAnalysisService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final EventAggregatorService eventAggregatorService;

    public PriceAnalysisService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            EventAggregatorService eventAggregatorService) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.eventAggregatorService = eventAggregatorService;
    }

    public EnhancedAnalysisResponse analyzeWithMultipleFactors(EventAnalysisRequest request) {
        try {
            // 1. Get events
            List<Event> events = eventAggregatorService.getAllEvents(
                    request.getCity(),
                    request.getCheckIn(),
                    request.getCheckOut()
            );

            // 2. Calculate multiple baseline comparisons
            Map<String, String> baselineDates = calculateBaselineDates(
                    request.getCheckIn(),
                    request.getCheckOut()
            );

            // 3. Get seasonality data
            SeasonalityData seasonality = analyzeSeasonality(
                    request.getCheckIn(),
                    request.getCheckOut()
            );

            // 4. Build comprehensive prompt for Claude
            String prompt = buildEnhancedPrompt(
                    request,
                    events,
                    baselineDates,
                    seasonality
            );

            // 5. Call Claude API with MCP access
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "claude-sonnet-4-20250514");
            requestBody.put("max_tokens", 6000);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

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

            return parseEnhancedResponse(response, events);

        } catch (Exception e) {
            throw new RuntimeException("Enhanced analysis failed: " + e.getMessage(), e);
        }
    }

    private Map<String, String> calculateBaselineDates(String checkIn, String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);
        DayOfWeek dayOfWeek = checkInDate.getDayOfWeek();

        Map<String, String> baselines = new HashMap<>();

        // Same day of week, 1 week before
        LocalDate weekBefore = checkInDate.minusWeeks(1);
        LocalDate weekBeforeOut = checkOutDate.minusWeeks(1);
        baselines.put("weekBeforeIn", weekBefore.toString());
        baselines.put("weekBeforeOut", weekBeforeOut.toString());

        // Same day of week, 1 month before
        LocalDate monthBefore = checkInDate.minusMonths(1);
        // Adjust to same day of week
        while (monthBefore.getDayOfWeek() != dayOfWeek) {
            monthBefore = monthBefore.plusDays(1);
        }
        LocalDate monthBeforeOut = monthBefore.plusDays(
                ChronoUnit.DAYS.between(checkInDate, checkOutDate)
        );
        baselines.put("monthBeforeIn", monthBefore.toString());
        baselines.put("monthBeforeOut", monthBeforeOut.toString());

        // Same dates, 1 year before
        LocalDate yearBefore = checkInDate.minusYears(1);
        LocalDate yearBeforeOut = checkOutDate.minusYears(1);
        baselines.put("yearBeforeIn", yearBefore.toString());
        baselines.put("yearBeforeOut", yearBeforeOut.toString());

        // Neighboring dates (day before and day after)
        baselines.put("dayBeforeIn", checkInDate.minusDays(1).toString());
        baselines.put("dayBeforeOut", checkOutDate.minusDays(1).toString());
        baselines.put("dayAfterIn", checkInDate.plusDays(1).toString());
        baselines.put("dayAfterOut", checkOutDate.plusDays(1).toString());

        return baselines;
    }

    private SeasonalityData analyzeSeasonality(String checkIn, String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn);
        SeasonalityData data = new SeasonalityData();

        // Determine season
        int month = checkInDate.getMonthValue();
        if (month >= 3 && month <= 5) data.setSeason("Spring");
        else if (month >= 6 && month <= 8) data.setSeason("Summer");
        else if (month >= 9 && month <= 11) data.setSeason("Fall");
        else data.setSeason("Winter");

        // Check if weekend
        DayOfWeek day = checkInDate.getDayOfWeek();
        data.setIsWeekend(day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);

        // Peak season (simplified - you can enhance this)
        data.setIsPeakSeason(month >= 6 && month <= 8);

        // Major US holidays (simplified)
        List<String> holidays = checkForHolidays(checkInDate);
        data.setIsHoliday(!holidays.isEmpty());
        data.setHolidays(holidays);

        return data;
    }

    private List<String> checkForHolidays(LocalDate date) {
        List<String> holidays = new ArrayList<>();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // Major US holidays
        if (month == 1 && day == 1) holidays.add("New Year's Day");
        if (month == 7 && day == 4) holidays.add("Independence Day");
        if (month == 11 && day >= 22 && day <= 28) holidays.add("Thanksgiving Week");
        if (month == 12 && day >= 20) holidays.add("Christmas/New Year Season");
        if (month == 5 && day >= 25 && day <= 31) holidays.add("Memorial Day Weekend");
        if (month == 9 && day >= 1 && day <= 7) holidays.add("Labor Day Weekend");

        return holidays;
    }

    private String buildEnhancedPrompt(
            EventAnalysisRequest request,
            List<Event> events,
            Map<String, String> baselineDates,
            SeasonalityData seasonality) {

        String eventsJson;
        try {
            eventsJson = objectMapper.writeValueAsString(events);
        } catch (Exception e) {
            eventsJson = "[]";
        }

        return String.format("""
            You are an expert travel price analyst. Perform a COMPREHENSIVE multi-factor analysis of hotel prices.
            
            REQUESTED TRIP:
            City: %s
            Check-in: %s
            Check-out: %s
            
            DETECTED EVENTS:
            %s
            
            SEASONALITY CONTEXT:
            Season: %s
            Is Weekend: %s
            Is Peak Season: %s
            Holidays: %s
            
            YOUR TASK - Use Trivago MCP to search hotels for ALL of these date ranges:
            
            1. TARGET DATES (the user's dates):
               - Check-in: %s, Check-out: %s
            
            2. BASELINE COMPARISONS (to isolate event impact):
               a) Week Before (same day of week): %s to %s
               b) Month Before (same day of week): %s to %s
               c) Year Before (same dates): %s to %s
               d) Day Before: %s to %s
               e) Day After: %s to %s
            
            3. ANALYZE THESE FACTORS:
            
            Factor A: EVENT IMPACT
            - Are there major events during the target dates?
            - Do prices drop significantly before/after the event dates?
            - Are nearby cities cheaper (suggesting event-driven demand)?
            
            Factor B: SEASONALITY
            - Is this peak tourist season?
            - Compare to same season last year
            - Weekend vs weekday premium
            
            Factor C: HOLIDAY IMPACT
            - Is this a major holiday period?
            - Compare to non-holiday periods
            
            Factor D: DAY-OF-WEEK PATTERN
            - Compare weekday vs weekend prices
            - Is the surge consistent with normal weekend premiums?
            
            Factor E: SUPPLY/DEMAND
            - How many hotels are available?
            - Are luxury hotels sold out? (indicates high demand)
            - Price variance (high variance = tight supply)
            
            Factor F: GRADUAL vs SPIKE
            - Do prices gradually increase (seasonal trend)?
            - Or sudden spike (event-driven)?
            
            4. CALCULATE EVENT ATTRIBUTION:
            - What %% of the price surge is due to events vs other factors?
            - Provide confidence level (High/Medium/Low)
            - List evidence supporting event impact
            - List evidence against event impact
            
            5. Return ONLY this JSON structure:
            {
              "priceAnalysis": {
                "currentPeriod": {"avgPrice": 250, "minPrice": 150, "maxPrice": 500, "sampleHotels": ["Hotel A - $200"]},
                "baselineWeekBefore": {"avgPrice": 180, "minPrice": 100, "maxPrice": 350},
                "baselineMonthBefore": {"avgPrice": 190, "minPrice": 110, "maxPrice": 360},
                "baselineYearBefore": {"avgPrice": 160, "minPrice": 90, "maxPrice": 320},
                "neighboringDates": {"avgPrice": 170, "minPrice": 95, "maxPrice": 330},
                "surgePct": 39,
                "confidence": "High"
              },
              "contributingFactors": [
                {"factor": "Major Events", "impact": "High", "description": "Taylor Swift concert on same dates", "contributionPct": 60},
                {"factor": "Weekend Premium", "impact": "Medium", "description": "Friday-Sunday stay", "contributionPct": 20},
                {"factor": "Peak Season", "impact": "Low", "description": "Summer tourism season", "contributionPct": 15},
                {"factor": "Holiday", "impact": "Low", "description": "No major holidays", "contributionPct": 5}
              ],
              "eventImpactScore": {
                "score": 85,
                "confidence": "High",
                "evidencePoints": [
                  "Prices 60%% higher than week before (same day of week)",
                  "Neighboring dates 40%% cheaper",
                  "Year-over-year increase only 5%% (inflation-adjusted)"
                ],
                "counterPoints": [
                  "It's also peak summer season",
                  "Weekend premium accounts for ~20%% of increase"
                ]
              },
              "occupancyData": {
                "availableHotels": 45,
                "totalHotels": 150,
                "occupancyRate": 0.70,
                "demandLevel": "High"
              },
              "alternatives": [
                {"location": "Nearby Suburb", "avgPrice": 160, "distance": "12 miles"}
              ],
              "analysis": "The 39%% price surge is primarily driven by major events (60%% contribution). Baseline comparisons show prices drop significantly before and after the event dates, confirming event-driven demand rather than seasonal trends."
            }
            
            Be rigorous in your analysis. Don't assume events cause price surges without evidence.
            """,
                request.getCity(), request.getCheckIn(), request.getCheckOut(),
                eventsJson,
                seasonality.getSeason(), seasonality.getIsWeekend(), seasonality.getIsPeakSeason(), seasonality.getHolidays(),
                request.getCheckIn(), request.getCheckOut(),
                baselineDates.get("weekBeforeIn"), baselineDates.get("weekBeforeOut"),
                baselineDates.get("monthBeforeIn"), baselineDates.get("monthBeforeOut"),
                baselineDates.get("yearBeforeIn"), baselineDates.get("yearBeforeOut"),
                baselineDates.get("dayBeforeIn"), baselineDates.get("dayBeforeOut"),
                baselineDates.get("dayAfterIn"), baselineDates.get("dayAfterOut")
        );
    }

    private EnhancedAnalysisResponse parseEnhancedResponse(String response, List<Event> events) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            String textContent = root.path("content").get(0).path("text").asText();

            // Extract JSON
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{[\\s\\S]*\\}");
            java.util.regex.Matcher matcher = pattern.matcher(textContent);

            if (matcher.find()) {
                String jsonStr = matcher.group();
                EnhancedAnalysisResponse analysisResponse =
                        objectMapper.readValue(jsonStr, EnhancedAnalysisResponse.class);
                analysisResponse.setEvents(events);
                return analysisResponse;
            }

            throw new RuntimeException("Could not extract JSON from response");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse enhanced response: " + e.getMessage(), e);
        }
    }
}