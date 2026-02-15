package com.event.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Service
public class BaselinePriceService {

    // Baseline average hotel prices per city (EUR)
    private static final Map<String, Double> CITY_BASELINE_PRICES = Map.of(
            "berlin", 95.0,
            "paris", 130.0,
            "london", 145.0,
            "new york", 180.0,
            "tokyo", 110.0,
            "barcelona", 105.0,
            "amsterdam", 120.0,
            "rome", 100.0
    );

    public double estimate(String city, JsonNode stayPeriod) {
        LocalDate checkIn = LocalDate.parse(stayPeriod.get("check_in").asText());

        double basePrice = CITY_BASELINE_PRICES.getOrDefault(
                city.toLowerCase(),
                100.0
        );

        // Adjust for day of week
        DayOfWeek dayOfWeek = checkIn.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
            basePrice *= 1.15; // 15% weekend premium
        }
        return basePrice;
    }
}