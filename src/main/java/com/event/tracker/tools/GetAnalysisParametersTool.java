package com.event.tracker.tools;

import com.event.tracker.model.*;
import com.event.tracker.service.HolidayService;
import com.event.tracker.service.SeasonalityService;
import com.event.tracker.service.TicketmasterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GetAnalysisParametersTool {

    private final TicketmasterService ticketmasterService;
    private final HolidayService holidayService;
    private final SeasonalityService seasonalityService;
    private final ObjectMapper objectMapper;

    public GetAnalysisParametersTool(
            TicketmasterService ticketmasterService,
            HolidayService holidayService,
            SeasonalityService seasonalityService) {
        this.ticketmasterService = ticketmasterService;
        this.holidayService = holidayService;
        this.seasonalityService = seasonalityService;
        this.objectMapper = new ObjectMapper();
    }

    @Tool (
            name = "get_analysis_parameters",
            description = """
            [HOTEL SURGE ANALYSIS - STEP 1 of 3]

            Analyzes all factors affecting hotel prices in a city for specific dates:
            - Fetches major events from Ticketmaster
            - Determines seasonality and tourism levels
            - Identifies holidays and calendar factors
            - Calculates demand indicators

            OUTPUTS: Events, seasonality, calendar factors, demand indicators
            NEXT STEP: Use Trivago MCP to search hotels for the same city and dates
            """
    )
    public JsonNode getAnalysisParameters(
            @ToolParam(description = "City name (e.g., Berlin, New York)") String city,
            @ToolParam(description = "ISO country code (e.g., DE, US)") String countryCode,
            @ToolParam(description = "Check-in date (YYYY-MM-DD)") String checkInDate,
            @ToolParam(description = "Check-out date (YYYY-MM-DD)") String checkOutDate,
            @ToolParam(description = "Search radius in km", required = false) Integer searchRadiusKm
    ) {
        try {
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            int radius = searchRadiusKm != null ? searchRadiusKm : 30;
            // Fetch events
            List<Event> events = ticketmasterService.fetchEvents(city, countryCode, checkIn, checkOut, radius);

            // Get seasonality
            SeasonalityInfo seasonality = seasonalityService.analyze(city, checkIn);

            // Get holidays
            List<Holiday> holidays = holidayService.fetchHolidays(countryCode, checkIn.getYear());

            // Analyze calendar factors
            CalendarInfo calendarInfo = analyzeCalendar(checkIn, checkOut, holidays);

            // Calculate demand indicators
            DemandIndicators demand = calculateDemand(events, seasonality, calendarInfo);

            // Build response
            return buildResponse(city, checkIn, checkOut, events, seasonality, calendarInfo, demand);

        } catch (Exception e) {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", "EXECUTION_ERROR");
            errorNode.put("message", e.getMessage());
            return errorNode;
        }
    }

    private CalendarInfo analyzeCalendar(LocalDate checkIn, LocalDate checkOut, List<Holiday> holidays) {
        boolean isWeekend = checkIn.getDayOfWeek() == DayOfWeek.SATURDAY || checkIn.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isHoliday = holidayService.isHoliday(checkIn, holidays);
        boolean isLongWeekend = holidayService.isLongWeekend(checkIn, holidays);

        List<Holiday> relevantHolidays = holidays.stream()
                .filter(h -> !h.getDate().isBefore(checkIn) && !h.getDate().isAfter(checkOut))
                .toList();

        return CalendarInfo.builder()
                .isWeekend(isWeekend)
                .isHoliday(isHoliday)
                .isLongWeekend(isLongWeekend)
                .isSchoolHoliday(false) // Optional enhancement
                .holidays(relevantHolidays)
                .build();
    }

    private DemandIndicators calculateDemand(List<Event> events, SeasonalityInfo seasonality, CalendarInfo calendar) {
        int majorEventsCount = (int) events.stream()
                .filter(e -> "high".equals(e.getImpactLevel()) || "critical".equals(e.getImpactLevel()))
                .count();

        int totalVisitors = events.stream().mapToInt(Event::getExpectedVisitors).sum();

        double eventImpactScore = Math.min(10.0, (majorEventsCount * 2.0) + (totalVisitors / 5000.0));

        String demandLevel;
        if (eventImpactScore > 7 || majorEventsCount > 2) demandLevel = "very_high";
        else if (eventImpactScore > 5 || majorEventsCount > 1) demandLevel = "high";
        else if (eventImpactScore > 3 || seasonality.isPeakSeason()) demandLevel = "medium";
        else demandLevel = "low";

        return DemandIndicators.builder()
                .majorEventsCount(majorEventsCount)
                .totalExpectedVisitors(totalVisitors)
                .eventImpactScore(eventImpactScore)
                .demandLevel(demandLevel)
                .build();
    }

    private JsonNode buildResponse(String city, LocalDate checkIn, LocalDate checkOut,
                                   List<Event> events, SeasonalityInfo seasonality,
                                   CalendarInfo calendar, DemandIndicators demand) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("city", city);
        response.put("analysis_date", LocalDate.now().toString());

        // Stay period
        ObjectNode stayPeriod = objectMapper.createObjectNode();
        stayPeriod.put("check_in", checkIn.toString());
        stayPeriod.put("check_out", checkOut.toString());
        stayPeriod.put("nights", ChronoUnit.DAYS.between(checkIn, checkOut));
        response.set("stay_period", stayPeriod);

        // Events
        ArrayNode eventsArray = objectMapper.createArrayNode();
        for (Event event : events) {
            ObjectNode eventNode = objectMapper.createObjectNode();
            eventNode.put("id", event.getId());
            eventNode.put("name", event.getName());
            eventNode.put("type", event.getType());
            eventNode.put("venue", event.getVenue());
            eventNode.put("date", event.getDate().toString());
            if (event.getTime() != null) eventNode.put("time", event.getTime().toString());
            eventNode.put("capacity", event.getCapacity());
            eventNode.put("expected_visitors", event.getExpectedVisitors());
            eventNode.put("distance_km", event.getDistanceKm());
            eventNode.put("impact_level", event.getImpactLevel());
            eventNode.put("ticket_availability", event.getTicketAvailability());
            eventsArray.add(eventNode);
        }
        response.set("events", eventsArray);

        // Seasonality
        ObjectNode seasonalityNode = objectMapper.createObjectNode();
        seasonalityNode.put("season", seasonality.getSeason());
        seasonalityNode.put("is_peak_season", seasonality.isPeakSeason());
        seasonalityNode.put("tourism_level", seasonality.getTourismLevel());
        seasonalityNode.put("typical_occupancy_rate", seasonality.getTypicalOccupancy());
        response.set("seasonality", seasonalityNode);

        // Calendar factors
        ObjectNode calendarNode = objectMapper.createObjectNode();
        calendarNode.put("is_weekend", calendar.isWeekend());
        calendarNode.put("is_holiday", calendar.isHoliday());
        calendarNode.put("is_long_weekend", calendar.isLongWeekend());
        calendarNode.put("is_school_holiday", calendar.isSchoolHoliday());

        ArrayNode holidaysArray = objectMapper.createArrayNode();
        for (Holiday holiday : calendar.getHolidays()) {
            ObjectNode holidayNode = objectMapper.createObjectNode();
            holidayNode.put("name", holiday.getName());
            holidayNode.put("date", holiday.getDate().toString());
            holidayNode.put("type", holiday.getType());
            holidayNode.put("is_national", holiday.isNational());
            holidaysArray.add(holidayNode);
        }
        calendarNode.set("holidays", holidaysArray);
        response.set("calendar_factors", calendarNode);

        // Demand indicators
        ObjectNode demandNode = objectMapper.createObjectNode();
        demandNode.put("major_events_count", demand.getMajorEventsCount());
        demandNode.put("total_expected_visitors", demand.getTotalExpectedVisitors());
        demandNode.put("event_impact_score", demand.getEventImpactScore());
        demandNode.put("overall_demand_level", demand.getDemandLevel());
        response.set("demand_indicators", demandNode);

        // Metadata
        ObjectNode metadata = objectMapper.createObjectNode();
        ArrayNode dataSources = objectMapper.createArrayNode();
        dataSources.add("ticketmaster");
        dataSources.add("holiday_api");
        dataSources.add("internal_seasonality");
        metadata.set("data_sources", dataSources);
        metadata.put("analysis_timestamp", Instant.now().toString());
        response.set("metadata", metadata);

        // Workflow hint
        ObjectNode workflowHint = objectMapper.createObjectNode();
        workflowHint.put("next_step", "trivago-accommodation-search");
        workflowHint.put("next_step_description", "Search for hotels in " + city + " for the specified dates");
        ArrayNode requiredForFinal = objectMapper.createArrayNode();
        requiredForFinal.add("average_price from Trivago results");
        requiredForFinal.add("this analysis_parameters object");
        workflowHint.set("required_for_final_step", requiredForFinal);
        workflowHint.put("final_step", "calculate_final_surge");
        response.set("_workflow_hint", workflowHint);

        return response;
    }
}
