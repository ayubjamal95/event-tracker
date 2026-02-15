package com.event.tracker.service;

import com.event.tracker.model.SeasonalityInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
@Slf4j
@Service
public class SeasonalityService {

    // City-specific peak seasons (simplified)
    private static final Map<String, Map<Month, String>> CITY_SEASONS = Map.of(
            "berlin", Map.of(
                    Month.DECEMBER, "peak",
                    Month.JUNE, "peak",
                    Month.JULY, "peak",
                    Month.AUGUST, "peak"
            ),
            "paris", Map.of(
                    Month.APRIL, "peak",
                    Month.MAY, "peak",
                    Month.JUNE, "peak",
                    Month.SEPTEMBER, "peak"
            ),
            "london", Map.of(
                    Month.JUNE, "peak",
                    Month.JULY, "peak",
                    Month.AUGUST, "peak",
                    Month.DECEMBER, "peak"
            )
    );

    public SeasonalityInfo analyze(String city, LocalDate date) {
        String season = getSeason(date);
        boolean isPeak = isPeakSeason(city.toLowerCase(), date.getMonth());
        String tourismLevel = getTourismLevel(city, date.getMonth(), isPeak);
        double occupancy = getTypicalOccupancy(isPeak, season);

        return SeasonalityInfo.builder()
                .season(season)
                .isPeakSeason(isPeak)
                .tourismLevel(tourismLevel)
                .typicalOccupancy(occupancy)
                .build();
    }

    private String getSeason(LocalDate date) {
        Month month = date.getMonth();
        return switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> "winter";
            case MARCH, APRIL, MAY -> "spring";
            case JUNE, JULY, AUGUST -> "summer";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "autumn";
        };
    }

    private boolean isPeakSeason(String city, Month month) {
        Map<Month, String> citySeason = CITY_SEASONS.getOrDefault(city, Map.of());
        return "peak".equals(citySeason.get(month));
    }

    private String getTourismLevel(String city, Month month, boolean isPeak) {
        if (isPeak) {
            return "high";
        }

        // Summer is generally medium-high for European cities
        if (month == Month.JUNE || month == Month.JULY || month == Month.AUGUST) {
            return "medium";
        }

        // Winter is low except December
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return "low";
        }

        return "medium";
    }

    private double getTypicalOccupancy(boolean isPeak, String season) {
        if (isPeak) return 0.85;
        if ("summer".equals(season)) return 0.70;
        if ("winter".equals(season)) return 0.60;
        return 0.65;
    }
}
