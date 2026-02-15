package com.event.tracker.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.event.tracker.model.Factor;
import com.event.tracker.model.SurgeCalculation;

@Service
public class SurgeCalculationEngine {

    public SurgeCalculation calculate(JsonNode analysisParams,
                                      double currentPrice,
                                      double baselinePrice,
                                      String mode) {

        List<Factor> factors = new ArrayList<>();
        double surgeFactor = 0.0;

        // 1. EVENT IMPACT (75% weight)
        JsonNode events = analysisParams.get("events");
        double eventImpact = calculateEventImpact(events, factors);
        surgeFactor += eventImpact * 0.75;

        // 2. SEASONALITY IMPACT (10% weight)
        JsonNode seasonality = analysisParams.get("seasonality");
        double seasonalImpact = calculateSeasonalImpact(seasonality, factors);
        surgeFactor += seasonalImpact * 0.10;

        // 3. CALENDAR IMPACT (5% weight)
        JsonNode calendar = analysisParams.get("calendar_factors");
        double calendarImpact = calculateCalendarImpact(calendar, factors);
        surgeFactor += calendarImpact * 0.05;

        // 4. DEMAND-SUPPLY IMPACT (10% weight)
        JsonNode demand = analysisParams.get("demand_indicators");
        double demandImpact = calculateDemandImpact(demand, factors);
        surgeFactor += demandImpact * 0.10;

        // Calculate actual surge percentage
        double actualSurge = ((currentPrice - baselinePrice) / baselinePrice) * 100;
        double modelSurge = surgeFactor * 100;

        // Build result
        SurgeCalculation result = new SurgeCalculation();
        result.setSurgePercentage(actualSurge);
        result.setModelSurge(modelSurge);
        result.setSurgeCategory(categorizeSurge(actualSurge));
        result.setConfidenceLevel(calculateConfidence(actualSurge, modelSurge));
        result.setFactors(factors);
        result.setMode(mode);
        result.setPrimaryDriver(determinePrimaryDriver(factors));
        result.setSurgeJustified(actualSurge > 20); // Surge > 20% is considered justified
        result.setExplanation(generateExplanation(actualSurge, factors, analysisParams));
        result.setRecommendations(generateRecommendations(actualSurge, analysisParams));

        // Set weights
        result.setEventWeight(0.75);
        result.setSeasonalityWeight(0.10);
        result.setCalendarWeight(0.05);
        result.setDemandSupplyWeight(0.10);

        Map<String, Double> weights = new HashMap<>();
        weights.put("events", 0.75);
        weights.put("seasonality", 0.10);
        weights.put("calendar", 0.05);
        weights.put("demand_supply", 0.10);
        result.setFactorWeights(weights);

        return result;
    }

    private double calculateEventImpact(JsonNode events, List<Factor> factors) {
        if (events == null || !events.isArray() || events.size() == 0) {
            return 0.0;
        }

        double totalImpact = 0.0;

        for (JsonNode event : events) {
            String name = event.get("name").asText();
            String impactLevel = event.get("impact_level").asText();
            int expectedVisitors = event.get("expected_visitors").asInt();
            double distanceKm = event.has("distance_km")
                    ? event.get("distance_km").asDouble()
                    : 5.0;

            // Distance decay
            double distanceFactor = Math.max(0, 1 - (distanceKm / 50.0));

            // Visitor impact
            double visitorFactor = Math.min(1.0, expectedVisitors / 10000.0);

            // Impact level multiplier
            double levelMultiplier = switch(impactLevel) {
                case "critical" -> 1.5;
                case "high" -> 1.2;
                case "medium" -> 0.8;
                default -> 0.4;
            };

            double eventImpact = visitorFactor * distanceFactor * levelMultiplier;
            totalImpact += eventImpact;

            // Add to factors list
            factors.add(Factor.builder()
                    .factor(impactLevel.toUpperCase() + " Event")
                    .description(name + " (" + expectedVisitors + " visitors)")
                    .impactPercentage(eventImpact * 100)
                    .weight(0.75)
                    .severity(impactLevel)
                    .build());
        }

        return Math.min(2.5, totalImpact);
    }

    private double calculateSeasonalImpact(JsonNode seasonality, List<Factor> factors) {
        boolean isPeak = seasonality.get("is_peak_season").asBoolean();
        String tourismLevel = seasonality.get("tourism_level").asText();

        double baseImpact = isPeak ? 0.3 : 0.0;
        double tourismMultiplier = switch(tourismLevel) {
            case "very_high" -> 0.4;
            case "high" -> 0.2;
            case "medium" -> 0.1;
            default -> 0.0;
        };

        double impact = baseImpact + tourismMultiplier;

        if (impact > 0) {
            factors.add(Factor.builder()
                    .factor("Seasonality")
                    .description(isPeak ? "Peak season" : "Tourism level: " + tourismLevel)
                    .impactPercentage(impact * 100)
                    .weight(0.10)
                    .severity(isPeak ? "medium" : "low")
                    .build());
        }

        return impact;
    }

    private double calculateCalendarImpact(JsonNode calendar, List<Factor> factors) {
        double impact = 0.0;
        List<String> calendarFactors = new ArrayList<>();

        if (calendar.get("is_weekend").asBoolean()) {
            impact += 0.15;
            calendarFactors.add("weekend");
        }
        if (calendar.get("is_holiday").asBoolean()) {
            impact += 0.25;
            calendarFactors.add("public holiday");
        }
        if (calendar.get("is_long_weekend").asBoolean()) {
            impact += 0.35;
            calendarFactors.add("long weekend");
        }

        if (impact > 0) {
            factors.add(Factor.builder()
                    .factor("Calendar Factors")
                    .description(String.join(", ", calendarFactors))
                    .impactPercentage(impact * 100)
                    .weight(0.05)
                    .severity("low")
                    .build());
        } else {
            // Weekday discount
            factors.add(Factor.builder()
                    .factor("Weekday Discount")
                    .description("Booking on weekday")
                    .impactPercentage(-5.0)
                    .weight(0.05)
                    .severity("low")
                    .build());
        }

        return impact;
    }

    private double calculateDemandImpact(JsonNode demand, List<Factor> factors) {
        String level = demand.get("overall_demand_level").asText();

        double impact = switch(level) {
            case "very_high" -> 0.5;
            case "high" -> 0.3;
            case "medium" -> 0.1;
            default -> 0.0;
        };

        if (impact > 0) {
            factors.add(Factor.builder()
                    .factor("Demand-Supply Pressure")
                    .description("Overall demand level: " + level)
                    .impactPercentage(impact * 100)
                    .weight(0.10)
                    .severity(impact > 0.3 ? "high" : "medium")
                    .build());
        }

        return impact;
    }

    private String categorizeSurge(double surgePercentage) {
        if (surgePercentage >= 150) return "VERY_HIGH";
        if (surgePercentage >= 100) return "HIGH";
        if (surgePercentage >= 50) return "MODERATE";
        if (surgePercentage >= 20) return "LOW";
        return "MINIMAL";
    }

    private double calculateConfidence(double actualSurge, double modelSurge) {
        double difference = Math.abs(actualSurge - modelSurge);
        return Math.max(0.5, 1.0 - (difference / 100.0));
    }

    private String determinePrimaryDriver(List<Factor> factors) {
        return factors.stream()
                .max((f1, f2) -> Double.compare(f1.getImpactPercentage(), f2.getImpactPercentage()))
                .map(f -> f.getFactor())
                .orElse("UNKNOWN");
    }

    private String generateExplanation(double surge, List<Factor> factors, JsonNode params) {
        StringBuilder explanation = new StringBuilder();

        explanation.append(String.format("The %.0f%% price surge is primarily driven by ", surge));

        Factor primaryFactor = factors.stream()
                .filter(f -> f.getImpactPercentage() > 0)
                .max((f1, f2) -> Double.compare(f1.getImpactPercentage(), f2.getImpactPercentage()))
                .orElse(null);

        if (primaryFactor != null) {
            explanation.append(primaryFactor.getDescription().toLowerCase());
        }

        // Add contributing factors
        long contributingFactors = factors.stream()
                .filter(f -> f.getImpactPercentage() > 10)
                .count();

        if (contributingFactors > 1) {
            explanation.append(" combined with ");
            explanation.append(contributingFactors - 1);
            explanation.append(" other significant factor");
            if (contributingFactors > 2) explanation.append("s");
        }

        explanation.append(".");

        return explanation.toString();
    }

    private List<Map<String, Object>> generateRecommendations(double surge, JsonNode params) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        if (surge > 100) {
            Map<String, Object> altDates = new HashMap<>();
            altDates.put("type", "alternative_dates");
            altDates.put("suggestion", "Consider booking 1-2 days earlier or later");
            altDates.put("potential_saving", surge * 0.4);
            altDates.put("urgency", "high");
            recommendations.add(altDates);

            Map<String, Object> altLocation = new HashMap<>();
            altLocation.put("type", "alternative_location");
            altLocation.put("suggestion", "Consider nearby cities or suburbs");
            altLocation.put("potential_saving", surge * 0.5);
            recommendations.add(altLocation);
        } else if (surge > 50) {
            Map<String, Object> bookNow = new HashMap<>();
            bookNow.put("type", "booking_timing");
            bookNow.put("suggestion", "Prices are elevated - book soon if dates are fixed");
            bookNow.put("urgency", "medium");
            recommendations.add(bookNow);
        }

        return recommendations;
    }
}
