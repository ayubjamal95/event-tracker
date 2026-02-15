package com.event.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.event.tracker.model.Holiday;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class HolidayService {

    private final WebClient webClient;

    public HolidayService(@Qualifier("holidayWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Holiday> fetchHolidays(String countryCode, int year) {
        try {
            String response = webClient.get()
                    .uri("/PublicHolidays/{year}/{countryCode}", year, countryCode)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseHolidays(response);

        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Holiday> parseHolidays(String jsonResponse) {
        List<Holiday> holidays = new ArrayList<>();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            if (!root.isArray()) {
                return holidays;
            }

            for (JsonNode holidayNode : root) {
                Holiday holiday = Holiday.builder()
                        .name(holidayNode.path("localName").asText())
                        .date(LocalDate.parse(holidayNode.path("date").asText()))
                        .type(holidayNode.path("types").isArray() &&
                                holidayNode.path("types").size() > 0
                                ? holidayNode.path("types").get(0).asText()
                                : "PUBLIC")
                        .isNational(holidayNode.path("global").asBoolean(true))
                        .build();

                holidays.add(holiday);
            }

        } catch (Exception e) {
        }

        return holidays;
    }

    public boolean isHoliday(LocalDate date, List<Holiday> holidays) {
        return holidays.stream()
                .anyMatch(h -> h.getDate().equals(date));
    }

    public boolean isLongWeekend(LocalDate date, List<Holiday> holidays) {
        // Check if there's a holiday within 1 day of a weekend
        for (Holiday holiday : holidays) {
            LocalDate holidayDate = holiday.getDate();

            // Check if holiday is Friday or Monday
            if (holidayDate.getDayOfWeek().getValue() == 5 ||
                    holidayDate.getDayOfWeek().getValue() == 1) {

                // Check if our date is within this long weekend
                if (!date.isBefore(holidayDate.minusDays(2)) &&
                        !date.isAfter(holidayDate.plusDays(2))) {
                    return true;
                }
            }
        }

        return false;
    }
}